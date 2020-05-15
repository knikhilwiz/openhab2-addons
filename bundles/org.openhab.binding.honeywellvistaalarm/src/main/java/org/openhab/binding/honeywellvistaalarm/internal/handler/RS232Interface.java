package org.openhab.binding.honeywellvistaalarm.internal.handler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.TooManyListenersException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.io.IOUtils;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.smarthome.io.transport.serial.PortInUseException;
import org.eclipse.smarthome.io.transport.serial.SerialPort;
import org.eclipse.smarthome.io.transport.serial.SerialPortEvent;
import org.eclipse.smarthome.io.transport.serial.SerialPortEventListener;
import org.eclipse.smarthome.io.transport.serial.SerialPortIdentifier;
import org.eclipse.smarthome.io.transport.serial.SerialPortManager;
import org.eclipse.smarthome.io.transport.serial.UnsupportedCommOperationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RS232Interface implements SerialPortEventListener {
    private final Logger logger = LoggerFactory.getLogger(RS232Interface.class);
    private String serialPortName = "";
    private int baudRate;
    private SerialPort serialPort = null;
    private SerialPortManager serialPortManager = null;
    private OutputStreamWriter serialOutput = null;
    private BufferedReader serialInput = null;
    private ExecutorService executorService = null;

    /** Connection status for the bridge. */
    private boolean connected = false;

    RS232InterfaceEvent handler;
    private ReaderThread readerThread;

    class ReaderThread extends Thread {

        private String readWithTimeout(int timeoutMs)
                throws InterruptedException, ExecutionException, IOException, TimeoutException {
            String result = null;
            Callable<String> readTask = () -> {
                try {
                    return serialInput.readLine();
                } catch (IOException e) {
                    logger.debug("Error reading from serial port: {}", e.getMessage());
                    throw new IOException(e);
                }
            };
            Future<String> readResult = executorService.submit(readTask);
            try {
                return readResult.get(timeoutMs, TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                readResult.cancel(true);
                throw new TimeoutException(e.getMessage());
            }
        }

        @Override
        public void run() {
            while (connected) {
                try {
                    synchronized (this) {
                        this.wait();
                    }
                    logger.debug("ReaderThread woken up!");
                } catch (InterruptedException e1) {
                    break;
                }
                if (!connected) {
                    break;
                }
                try {
                    if (serialInput != null && serialInput.ready()) {
                        while (serialInput != null && serialInput.ready()) {
                            String message = readWithTimeout(5000);
                            handler.handleIncomingMessage(message);
                        }
                    } else {
                        logger.info("nothing to read. Going back to sleep...");
                    }
                } catch (InterruptedException | ExecutionException e) {
                    logger.error("Serial port read error: {}", e.getMessage());
                    break;
                } catch (IOException | TimeoutException e) {
                    logger.error("Exception reading from Serial port: {}: {}", e, e.getMessage());
                    break;
                }
            }
            logger.debug("ReaderThread exiting!");
            disconnect();
        }

    }

    public RS232Interface(SerialPortManager serialPortManager, String serialPortName, int baudRate,
            @NonNull ExecutorService executorService, RS232InterfaceEvent handler) {
        this.serialPortManager = serialPortManager;
        this.serialPortName = serialPortName;
        this.baudRate = baudRate;
        this.handler = handler;
        this.executorService = executorService;

        logger.debug("HoneywellVistaAlarm RS232 Handler Initialized.");
        logger.debug("   Serial Port: {},", serialPortName);
        logger.debug("   Baud:        {},", baudRate);
    }

    synchronized private void openConnection() {
        try {
            logger.debug("openConnection(): Connecting to Panel RS232 port ");

            SerialPortIdentifier portIdentifier = serialPortManager.getIdentifier(serialPortName);
            if (portIdentifier == null) {
                throw new IOException("Could not find a gateway on given path '" + serialPortName + "', "
                        + serialPortManager.getIdentifiers().count() + " ports available.");
            }

            serialPort = portIdentifier.open(this.getClass().getName(), 2000);
            serialPort.setSerialPortParams(baudRate, SerialPort.DATABITS_8, SerialPort.STOPBITS_1,
                    SerialPort.PARITY_NONE);

            serialPort.disableReceiveTimeout();

            serialOutput = new OutputStreamWriter(serialPort.getOutputStream(), "US-ASCII");
            serialInput = new BufferedReader(new InputStreamReader(serialPort.getInputStream(), "ISO-8859-1"));

            setConnected(true);
            readerThread = new ReaderThread();
            readerThread.start();
            serialPort.addEventListener(this);
            serialPort.notifyOnDataAvailable(true);
            logger.info("Connected to the Honeywell Alarm!");

        } catch (PortInUseException portInUseException) {
            logger.error("openConnection(): Port in Use Exception: {}", portInUseException.getMessage());
            setConnected(false);
        } catch (UnsupportedCommOperationException unsupportedCommOperationException) {
            logger.error("openConnection(): Unsupported Comm Operation Exception: {}",
                    unsupportedCommOperationException.getMessage());
            setConnected(false);
        } catch (UnsupportedEncodingException unsupportedEncodingException) {
            logger.error("openConnection(): Unsupported Encoding Exception: {}",
                    unsupportedEncodingException.getMessage());
            setConnected(false);
        } catch (IOException ioException) {
            logger.error("openConnection(): IO Exception: {}", ioException.getMessage());
            setConnected(false);
        } catch (TooManyListenersException e) {
            logger.error("openConnection(): Too Many Listeners: {}", e.getMessage());
            setConnected(false);
        } catch (Exception e) {
            logger.error("openConnection(): Exception:");
            e.printStackTrace();
        }
    }

    synchronized public void write(String writeString) {
        try {
            serialOutput.write(writeString);
            serialOutput.flush();
        } catch (IOException ioException) {
            logger.error("write(): {}", ioException.getMessage());
            return;
        } catch (Exception exception) {
            logger.error("write(): Unable to write to serial port: {} ", exception.getMessage(), exception);
            return;
        }
        logger.debug("write(): Message Sent: {}", writeString);
    }

    synchronized private void closeConnection() {
        if (serialPort == null) {
            logger.debug("closeConnection(): Was never connected.");
            setConnected(false);
            return;
        }

        logger.debug("closeConnection(): Closing Serial Connection!");

        serialPort.removeEventListener();

        if (serialInput != null) {
            IOUtils.closeQuietly(serialInput);
            serialInput = null;
        }

        if (serialOutput != null) {
            IOUtils.closeQuietly(serialOutput);
            serialOutput = null;
        }

        serialPort.close();
        serialPort = null;
        setConnected(false);
        logger.debug("close(): Serial Connection Closed!");

        synchronized (readerThread) {
            readerThread.notify(); // notify the readerThread to shut down
        }

    }

    /**
     * Gets the Serial Port Name of the IT-100
     *
     * @return serialPortName
     */
    public String getSerialPortName() {
        return serialPortName;
    }

    /**
     * Connect The Bridge.
     */
    synchronized void connect() {
        openConnection();

        if (isConnected()) {
            handler.onConnected();
        }
    }

    /**
     * Disconnect The Bridge.
     */
    synchronized void disconnect() {
        closeConnection();
        handler.onDisconnected();
    }

    /**
     * Returns Connected.
     */
    synchronized public boolean isConnected() {
        return this.connected;
    }

    /**
     * Sets Connected.
     */
    synchronized private void setConnected(boolean connected) {
        this.connected = connected;
    }

    @Override
    public void serialEvent(SerialPortEvent event) {
        if (event.getEventType() == SerialPortEvent.DATA_AVAILABLE) {
            synchronized (readerThread) {
                readerThread.notify();
            }
        }
    }

}
