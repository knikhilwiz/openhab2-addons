/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.jvcprojector.internal.handler;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Arrays;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.smarthome.core.common.ThreadPoolManager;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.openhab.binding.jvcprojector.internal.JvcProjectorBindingConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link JvcProjectorHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Mark Hilbush - Initial contribution for GlobalCacheHandler
 * @author Nick Hill - Heavily repurposed for handling JVC Projectors
 */
public class JvcProjectorHandler extends BaseThingHandler {
    private Logger logger = LoggerFactory.getLogger(JvcProjectorHandler.class);

    private static final String JVCPROJECTOR_THREAD_POOL = "jvcProjectorHandler";

    private CommandProcessor commandProcessor;
    private ScheduledExecutorService scheduledExecutorService = ThreadPoolManager
            .getScheduledPool(JVCPROJECTOR_THREAD_POOL + "-" + thingID());
    private ScheduledFuture<?> scheduledFuture;

    private LinkedBlockingQueue<RequestMessage> sendQueue = null;

    private boolean powerOn = false;

    public JvcProjectorHandler(@NonNull Thing device) {
        super(device);
        commandProcessor = new CommandProcessor();
        scheduledFuture = null;
    }

    @Override
    public void initialize() {
        logger.debug("Initializing thing {}", thingID());
        scheduledFuture = scheduledExecutorService.schedule(commandProcessor, 2, TimeUnit.SECONDS);
    }

    @Override
    public void dispose() {
        logger.debug("Disposing thing {}", thingID());
        commandProcessor.terminate();
        if (scheduledFuture != null) {
            scheduledFuture.cancel(false);
        }
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        // Don't try to send command if the device is not online
        if (!isOnline()) {
            logger.debug("Can't handle command {} for {} because handler for thing {} is not ONLINE", command,
                    channelUID.getId(), thingID());
            return;
        }

        Channel channel = thing.getChannel(channelUID.getId());
        if (channel == null) {
            logger.warn("Unknown channel {} for thing {}; is item defined correctly", channelUID.getId(), thingID());
            return;
        }

        if (command instanceof RefreshType) {
            handleRefresh(channel);
            return;
        }

        switch (channel.getChannelTypeUID().getId()) {
            case JvcProjectorBindingConstants.CHANNEL_TYPE_SWITCH:
            case JvcProjectorBindingConstants.CHANNEL_TYPE_INPUT:
            case JvcProjectorBindingConstants.CHANNEL_TYPE_REMOTE_BUTTON:
                handleSend(command, channelUID);
                break;
            default:
                logger.warn("Thing {} has unknown channel type {}", thingID(), channel.getChannelTypeUID().getId());
                break;
        }
    }

    private void handleSend(Command command, ChannelUID channelUID) {
        logger.debug("Handling Send command {} on channel {} of thing {}", command, channelUID.getId(), thingID());

        String op;
        String id = channelUID.getIdWithoutGroup();
        switch (id) {
            case JvcProjectorBindingConstants.CHANNEL_POWER_STATE:
                op = id + command.toString();
                break;
            case JvcProjectorBindingConstants.CHANNEL_INPUT_STATE:
                if (command.toString().equals("1") || command.toString().equals("2")) {
                    op = id + command.toString();

                } else {
                    logger.error("Only acceptable values for {} are \"1\" and \"2\"", channelUID.toString());
                    return;
                }
            default:
                op = id;
        }
        JvcProjectorCommand cmd = new JvcProjectorCommand(thingID(), sendQueue, op);
        cmd.execute();
        if (cmd.isSuccessful()) {
            switch (id) {
                case JvcProjectorBindingConstants.CHANNEL_POWER_STATE:
                    updateState(channelUID, (OnOffType) command);
                    break;
                case JvcProjectorBindingConstants.CHANNEL_INPUT_STATE:
                    updateState(channelUID, (DecimalType) command);
                    break;
            }
        }
    }

    private void handleRefresh(Channel channel) {
        if (!channel.getUID().getGroupId().equals(JvcProjectorBindingConstants.CHANNEL_GROUP_TYPE_STATUS)) {
            // Nothing to refresh
            return;
        }

        if (!channel.getUID().getIdWithoutGroup().equals(JvcProjectorBindingConstants.CHANNEL_POWER_STATE)
                && !powerOn) {
            logger.trace(
                    "Channels other than power can only be queried when thing {} is powered on. Skipping REFRESH for {}.",
                    thingID(), channel.getUID().getIdWithoutGroup());
            return;
        }

        logger.debug("Handle REFRESH command on channel {} for thing {}", channel.getUID().getId(), thingID());

        JvcProjectorCommand cmd = new JvcProjectorCommand(thingID(), sendQueue, channel.getUID().getIdWithoutGroup());
        cmd.execute();
        if (cmd.isSuccessful()) {
            String[] parts = cmd.deviceReply.split(" ");
            if (!parts[0].equals("40")) {
                logger.error("Unable to parse response from thing {}: {}", thing.getLabel(), cmd.deviceReply);
            }
            switch (Integer.parseInt(parts[5])) {
                case 30:
                    updateState(channel.getUID(), OnOffType.OFF);
                    triggerChannel(channel.getUID(), OnOffType.OFF.toString());
                    markDevicePowerOff();
                    break;
                case 31:
                    updateState(channel.getUID(), OnOffType.ON);
                    triggerChannel(channel.getUID(), OnOffType.OFF.toString());
                    markDevicePowerOn();
                    break;
                case 32:
                    triggerChannel(channel.getUID(), "unit in cool down mode");
                    markDevicePowerOff();
                    break;
                case 34:
                    triggerChannel(channel.getUID(), "unit in error mode");
                    markDevicePowerOff();
                    break;
                case 36:
                    updateState(channel.getUID(), new DecimalType(1));
                    triggerChannel(channel.getUID(), "HDMI1");
                    break;
                case 37:
                    updateState(channel.getUID(), new DecimalType(2));
                    triggerChannel(channel.getUID(), "HDMI2");
                    break;
            }
        }
    }

    public String getConfigIP() {
        return thing.getConfiguration().get(JvcProjectorBindingConstants.THING_PROPERTY_IP).toString();
    }

    public Number getConfigPort() {
        return (Number) thing.getConfiguration().get(JvcProjectorBindingConstants.THING_PROPERTY_PORT);
    }

    private String thingID() {
        // Return segments 2 & 3 only
        String s = thing.getUID().getAsString();
        return s.substring(s.indexOf(':') + 1);
    }

    /*
     * Manage the ONLINE/OFFLINE status of the thing
     */
    private void markThingOnline() {
        if (!isOnline()) {
            logger.debug("Changing status of {} from {}({}) to ONLINE", thingID(), getStatus(), getDetail());
            updateStatus(ThingStatus.ONLINE);

            // When we go online the first time, disable commands until we get a power state update.
            markDevicePowerOff();
        }
    }

    private void markThingOffline() {
        if (isOnline()) {
            logger.debug("Changing status of {} from {}({}) to OFFLINE", thingID(), getStatus(), getDetail());
            updateStatus(ThingStatus.OFFLINE);
        }
        markDevicePowerOff();
    }

    private void markDevicePowerOn() {
        logger.trace("Device is marked power ON for thing {}. All commands ENABLED.", thingID());
        powerOn = true;
    }

    private void markDevicePowerOff() {
        logger.trace("Device is marked power OFF for thing {}. All commands DISABLED.", thingID());
        powerOn = false;
    }

    private void markThingOfflineWithError(ThingStatusDetail statusDetail, String statusMessage) {
        // If it's offline with no detail or if it's not offline, mark it offline with detailed status
        if ((isOffline() && getDetail().equals(ThingStatusDetail.NONE)) || !isOffline()) {
            logger.debug("Changing status of {} from {}({}) to OFFLINE({})", thingID(), getStatus(), getDetail(),
                    statusDetail);
            updateStatus(ThingStatus.OFFLINE, statusDetail, statusMessage);
            return;
        }
        markDevicePowerOff();
    }

    private boolean isOnline() {
        return thing.getStatus().equals(ThingStatus.ONLINE);
    }

    private boolean isOffline() {
        return thing.getStatus().equals(ThingStatus.OFFLINE);
    }

    private ThingStatus getStatus() {
        return thing.getStatus();
    }

    private ThingStatusDetail getDetail() {
        return thing.getStatusInfo().getStatusDetail();
    }

    /**
     * The {@link CommandProcessor} class is responsible for handling communication with the JvcProjector
     * device. It waits for requests to arrive on a queue. When a request arrives, it sends the command to the
     * JvcProjector device, waits for a response from the device, parses one or more responses, then responds to the
     * caller by placing a message in a response queue. Device response time is typically well below 100 ms, hence the
     * reason for a relatively low timeout when reading the response queue.
     *
     * @author Mark Hilbush - Initial contribution for GlobalCacheHandler
     * @author Nick Hill - Heavily re-purposed for handling JVC Projectors
     */
    private class CommandProcessor extends Thread {
        private Logger logger = LoggerFactory.getLogger(CommandProcessor.class);

        private boolean terminate = false;
        private final String TERMINATE_COMMAND = "terminate";

        private final int SEND_QUEUE_MAX_DEPTH = 10;
        private final int SEND_QUEUE_TIMEOUT = 2000;

        private ConnectionManager connectionManager;

        public CommandProcessor() {
            super("JvcProjector JvcProjectorCommand Processor");
            sendQueue = new LinkedBlockingQueue<RequestMessage>(SEND_QUEUE_MAX_DEPTH);
            logger.debug("Processor for thing {} created request queue, depth={}", thingID(), SEND_QUEUE_MAX_DEPTH);
        }

        public void terminate() {
            logger.debug("Processor for thing {} is being marked ready to terminate.", thingID());

            try {
                // Send the command processor a terminate message
                sendQueue.put(new RequestMessage(TERMINATE_COMMAND, null, null, 0));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                terminate = true;
            }
        }

        @Override
        public void run() {
            logger.debug("JvcProjectorCommand processor STARTING for thing {} at IP {}", thingID(), getConfigIP());
            connectionManager = new ConnectionManager();
            connectionManager.scheduleConnectionMonitorJob();
            sendQueue.clear();
            terminate = false;

            try {
                RequestMessage requestMessage;
                while (!terminate) {
                    requestMessage = sendQueue.poll(SEND_QUEUE_TIMEOUT, TimeUnit.MILLISECONDS);
                    if (requestMessage != null) {
                        if (requestMessage.getCommandName().equals(TERMINATE_COMMAND)) {
                            logger.debug("Processor for thing {} received terminate message", thingID());
                            break;
                        }

                        byte[] deviceReply = {};
                        connectionManager.connect();
                        if (connectionManager.isConnected()) {
                            try {
                                long startTime = System.currentTimeMillis();
                                writeCommandToDevice(requestMessage);
                                int responsesNeeded = requestMessage.getNumResponses();
                                while (responsesNeeded-- > 0) {
                                    deviceReply = readReplyFromDevice();
                                    long endTime = System.currentTimeMillis();
                                    logger.debug("Transaction '{}' for thing {} at {} took {} ms",
                                            requestMessage.getCommandName(), thingID(), getConfigIP(),
                                            endTime - startTime);

                                    logger.trace("Processor for thing {} queuing response message: {}", thingID(),
                                            getAsHexString(deviceReply));
                                    requestMessage.getReceiveQueue().put(deviceReply);
                                }
                            } catch (IOException e) {
                                logger.error("Comm error for thing {} at {}: {}", thingID(), getConfigIP(),
                                        e.getMessage());
                                connectionManager.setCommError("ERROR: " + e.getMessage());
                                connectionManager.disconnect();
                            }
                        } else {
                            connectionManager.setCommError("ERROR: No connection to device");
                        }
                    }
                }
            } catch (InterruptedException e) {
                logger.warn("Processor for thing {} was interrupted: {}", thingID(), e.getMessage());
                Thread.currentThread().interrupt();
            }

            connectionManager.cancelConnectionMonitorJob();
            connectionManager.disconnect();
            connectionManager = null;
            logger.debug("JvcProjectorCommand processor TERMINATING for thing {} at IP {}", thingID(), getConfigIP());
        }

        /*
         * Write the command to the device.
         */
        private void writeCommandToDevice(RequestMessage requestMessage) throws IOException {
            DataOutputStream stream = connectionManager.getCommandOut();
            if (stream == null) {
                logger.debug("Error writing to device because output stream object is null");
                return;
            }

            byte[] deviceCommand = requestMessage.getDeviceCommand();

            logger.trace("Processor for thing {} writing command to device: {}", thingID(),
                    getAsHexString(deviceCommand));

            stream.write(deviceCommand);
            stream.write(0x0a);
            stream.flush();
        }

        /*
         * Read command reply from the device, then remove the CR at the end of the line.
         */
        private byte[] readReplyFromDevice() throws IOException {
            logger.trace("Processor for thing {} reading reply from device", thingID());

            DataInputStream stream = connectionManager.getCommandIn();

            if (stream == null) {
                logger.debug("Error reading from device because input stream object is null");
                throw new IOException("ERROR: BufferedReader is null!");
            }

            logger.trace("Processor for thing {} reading response from device", thingID());
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            final int MAX_RESPONSE_SIZE = 50;

            while (bytes.size() < MAX_RESPONSE_SIZE) {
                int i = stream.read();
                if (i == 0x0a || i == -1) {
                    break;
                }
                bytes.write(i & 0xff);
            }
            return bytes.toByteArray();
        }

        private String getAsHexString(byte[] b) {
            StringBuilder sb = new StringBuilder();

            for (int j = 0; j < b.length; j++) {
                String s = String.format("%02x ", b[j]);
                sb.append(s);
            }
            return sb.toString();
        }
    }

    /*
     * The {@link ConnectionManager} class is responsible for managing the state of the connections to the
     * socket and perform the initial handshake sequence for JVC Projectors.
     *
     * @author Mark Hilbush - Initial contribution for GlobalCacheHandler
     *
     * @author Nick Hill - Heavily re-purposed for handling JVC Projectors
     */
    private class ConnectionManager {
        private Logger logger = LoggerFactory.getLogger(ConnectionManager.class);

        private DeviceConnection commandConnection;

        private boolean deviceIsConnected;

        private final int SOCKET_CONNECT_TIMEOUT = 1500;
        // used for all other operations on socket.
        private final int SOCKET_TIMEOUT = 1000;

        private ScheduledFuture<?> connectionMonitorJob;
        private final int CONNECTION_MONITOR_FREQUENCY = 10;
        private final int CONNECTION_MONITOR_START_DELAY = 5;

        private Runnable connectionMonitorRunnable = () -> {
            logger.trace("Performing connection check for thing {} at IP {}", thingID(), commandConnection.getIP());
            checkConnection();
        };

        public ConnectionManager() {
            commandConnection = new DeviceConnection();

            commandConnection.setIP(getIPAddress());
            commandConnection.setPort(getPort());

            deviceIsConnected = false;
        }

        private String getIPAddress() {
            String ipAddress = ((JvcProjectorHandler) thing.getHandler()).getConfigIP();
            if (StringUtils.isEmpty(ipAddress)) {
                logger.debug("Handler for thing {} could not get IP address from config", thingID());
                markThingOfflineWithError(ThingStatusDetail.OFFLINE.CONFIGURATION_ERROR, "IP address not set");
            }
            return ipAddress;
        }

        private int getPort() {
            Number port = ((JvcProjectorHandler) thing.getHandler()).getConfigPort();
            if (port == null) {
                logger.debug("Handler for thing {} could not get IP address from config", thingID());
                markThingOfflineWithError(ThingStatusDetail.OFFLINE.CONFIGURATION_ERROR, "Port not set");
            }
            return port.intValue();
        }

        /*
         * Connect to the command and serial port(s) on the device. The serial connections are established only for
         * devices that support serial.
         */
        protected void connect() {
            if (isConnected()) {
                return;
            }

            // Get a connection to the command port
            if (!commandConnect(commandConnection)) {
                return;
            }

            if (!commandHandshake(commandConnection)) {
                return;
            }

            /*
             * All connections opened successfully, so we can mark the thing online
             * and start the serial port readers
             */
            markThingOnline();
            deviceIsConnected = true;
        }

        private boolean commandHandshake(DeviceConnection conn) {
            boolean handshakeComplete = false;
            final String PJ_OK = "PJ_OK";
            final String PJACK = "PJACK";
            final String PJREQ = "PJREQ";

            logger.debug("Starting handshake with thing {} at {}:{}", thingID(), conn.getIP(), conn.getPort());
            // Ensure that the handshake completes quickly
            try {
                byte[] cbuf = new byte[PJ_OK.length()];
                int bytesRead = conn.getCommandIn().read(cbuf, 0, PJ_OK.length());
                logger.trace("Recieved from {} at {}:{} => {}", thingID(), conn.getIP(), conn.getPort(), cbuf);

                if (bytesRead == PJ_OK.length() && Arrays.equals(cbuf, PJ_OK.getBytes())) {
                    conn.getCommandOut().write(PJREQ.getBytes());
                    conn.getCommandOut().flush();

                    bytesRead = conn.getCommandIn().read(cbuf, 0, PJACK.length());
                    if (bytesRead == PJACK.length() && Arrays.equals(cbuf, PJACK.getBytes())) {
                        handshakeComplete = true;
                    } else {
                        logger.error("Projector (thing {}) at {}:{} unresponsive on protocol handshake. Got {}",
                                thingID(), conn.getIP(), conn.getPort(), cbuf);
                        markThingOfflineWithError(ThingStatusDetail.OFFLINE.COMMUNICATION_ERROR,
                                "protocol handshake response timeout/error");
                        closeSocket(conn);
                    }
                } else {
                    logger.error("Projector (thing {}) at {}:{} did not initiate protocol handshake. Got {}", thingID(),
                            conn.getIP(), conn.getPort(), cbuf);
                    markThingOfflineWithError(ThingStatusDetail.OFFLINE.COMMUNICATION_ERROR,
                            "did not indicate protocol handshake");
                    closeSocket(conn);
                }
            } catch (IOException e) {
                logger.debug("Error performing handshake with thing {} at {}:{}, exception={}", thingID(), conn.getIP(),
                        conn.getPort(), e.getMessage());
                closeSocket(conn);
            }

            return handshakeComplete;
        }

        private boolean commandConnect(DeviceConnection conn) {
            logger.debug("Connecting to {} port for thing {} at IP {}", conn.getPort(), thingID(), conn.getIP());
            if (!openSocket(conn)) {
                return false;
            }
            // create streams
            try {
                conn.setCommandIn(new DataInputStream(conn.getSocket().getInputStream()));
                conn.setCommandOut(new DataOutputStream(conn.getSocket().getOutputStream()));
            } catch (IOException e) {
                logger.debug("Error getting streams to {} port for thing {} at {}, exception={}", conn.getPort(),
                        thingID(), conn.getIP(), e.getMessage());
                markThingOfflineWithError(ThingStatusDetail.OFFLINE.COMMUNICATION_ERROR, e.getMessage());
                closeSocket(conn);
                return false;
            }
            logger.info("Got a connection to {} port for thing {} at {}", conn.getPort(), thingID(), conn.getIP());

            return true;
        }

        private boolean openSocket(DeviceConnection conn) {
            try {
                conn.setSocket(new Socket());
                conn.getSocket().setSoTimeout(SOCKET_TIMEOUT);
                conn.getSocket().connect(new InetSocketAddress(conn.getIP(), conn.getPort()), SOCKET_CONNECT_TIMEOUT);
            } catch (IOException e) {
                logger.debug("Failed to get socket on {} port for thing {} at {}: {}", conn.getPort(), thingID(),
                        conn.getIP(), e.getMessage());
                return false;
            }
            return true;
        }

        private void closeSocket(DeviceConnection conn) {
            if (conn.getSocket() != null) {
                try {
                    conn.getSocket().close();
                } catch (IOException e) {
                    logger.debug("Failed to close socket on {} port for thing {} at {}", conn.getPort(), thingID(),
                            conn.getIP());
                }
            }
        }

        /*
         * Disconnect from the command and serial port(s) on the device. Only disconnect the serial port
         * connections if the devices have serial ports.
         */
        protected void disconnect() {
            logger.trace("Disconnecting from thing {} at {}:{}", thingID(), commandConnection.getIP(),
                    commandConnection.getPort());

            commandDisconnect(commandConnection);
            markThingOffline();
            deviceIsConnected = false;
        }

        private void commandDisconnect(DeviceConnection conn) {
            deviceDisconnect(conn);
        }

        private void deviceDisconnect(DeviceConnection conn) {
            logger.debug("Disconnecting from {} port for thing {} at IP {}", conn.getPort(), thingID(), conn.getIP());

            closeSocket(conn);
            conn.reset();
        }

        private boolean isConnected() {
            return deviceIsConnected;
        }

        public void setCommError(String errorMessage) {
            markThingOfflineWithError(ThingStatusDetail.OFFLINE.COMMUNICATION_ERROR, errorMessage);
        }

        /*
         * Retrieve the input/output streams for command and serial connections.
         */
        protected DataInputStream getCommandIn() {
            return commandConnection.getCommandIn();
        }

        protected DataOutputStream getCommandOut() {
            return commandConnection.getCommandOut();
        }

        /*
         * Periodically validate the command connection to the device by executing a check connection command.
         */
        private void scheduleConnectionMonitorJob() {
            logger.debug("Starting connection monitor job for thing {} at IP {}", thingID(), commandConnection.getIP());
            connectionMonitorJob = scheduler.scheduleWithFixedDelay(connectionMonitorRunnable,
                    CONNECTION_MONITOR_START_DELAY, CONNECTION_MONITOR_FREQUENCY, TimeUnit.SECONDS);
        }

        private void cancelConnectionMonitorJob() {
            if (connectionMonitorJob != null) {
                logger.debug("Canceling connection monitor job for thing {} at IP {}", thingID(),
                        commandConnection.getIP());
                connectionMonitorJob.cancel(true);
                connectionMonitorJob = null;
            }
        }

        private void checkConnection() {
            connect();
            if (!isConnected()) {
                return;
            }

            JvcProjectorCommand checkCommand = new JvcProjectorCommand(thingID(), sendQueue, "");
            checkCommand.executeQuiet();

            if (checkCommand.isSuccessful()) {
                logger.trace("Connection check successful for thing {} at IP {}", thingID(), commandConnection.getIP());
                markThingOnline();
                deviceIsConnected = true;
            } else {
                logger.debug("Connection check failed for thing {} at IP {}", thingID(), commandConnection.getIP());
                disconnect();
            }
        }
    }

    /*
     * The {@link DeviceConnection} class stores information about the connection to a jvcprojector device.
     *
     * @author Mark Hilbush - Initial contribution for GlobalCacheHandler
     *
     * @author Nick Hill - Re-purposed for handling JVC Projectors
     */
    private class DeviceConnection {
        private int port;
        private String ipAddress;
        private Socket socket;
        private DataInputStream commandIn;
        private DataOutputStream commandOut;

        DeviceConnection() {
            setPort(-1);
            setIP(null);
            setSocket(null);
            setCommandIn(null);
            setCommandOut(null);
        }

        public void reset() {
            setSocket(null);
            setCommandIn(null);
            setCommandOut(null);
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public String getIP() {
            return ipAddress;
        }

        public void setIP(String ipAddress) {
            this.ipAddress = ipAddress;
        }

        public Socket getSocket() {
            return socket;
        }

        public void setSocket(Socket socket) {
            this.socket = socket;
        }

        public DataInputStream getCommandIn() {
            return commandIn;
        }

        public void setCommandIn(DataInputStream dataInputStream) {
            this.commandIn = dataInputStream;
        }

        public DataOutputStream getCommandOut() {
            return commandOut;
        }

        public void setCommandOut(DataOutputStream commandOut) {
            this.commandOut = commandOut;
        }
    }
}
