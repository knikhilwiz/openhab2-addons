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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

enum MainCommand {
    CHECK("\0\0"),
    POWER("PW", "0", "OFF", "ON", "Cooling", "Warming", "Error"),
    INPUT("IP", "6", "HDMI1", "HDMI2"),
    REMOTE("RC"),
    SOURCE("SC", "0", "No signal", "Signal available"),
    MODEL("MD"),
    INFORMATION("IF");

    static {
        CHECK.queryable = false;
        REMOTE.queryable = false;
        SOURCE.modifyable = false;
        MODEL.modifyable = false;
        INFORMATION.modifyable = false;
    }

    private final String command;
    private boolean queryable = true;
    private boolean modifyable = true;
    private int offset;
    private String[] returnValues;

    boolean isModifyable() {
        return modifyable;
    }

    boolean isQueryable() {
        return queryable;
    }

    static Map<String, MainCommand> lookup = new HashMap<String, MainCommand>();
    static {
        for (MainCommand e : values()) {
            lookup.put(e.command, e);
        }
    }

    static MainCommand lookupCommand(String cmd) {
        return lookup.get(cmd);
    }

    MainCommand(String command) {
        this.command = command;
    }

    MainCommand(String command, String offset, String... returnValues) {
        this(command);
        this.offset = Integer.parseInt(offset, 16);
        this.returnValues = returnValues;
    }

    String decodeParameter(String parameter) {
        if (parameter.length() == 1) {
            int index = Integer.parseInt(parameter, 16) - offset;
            return returnValues[index];
        }

        return parameter;
    }

    String encodeParameter(String parameter) {
        for (int i = 0; i < returnValues.length; ++i) {
            if (parameter == returnValues[i]) {
                return String.valueOf(this.offset + i);
            }
        }

        return null;
    }

    byte[] getBytes() {
        return command.getBytes();
    }
}

enum InformationCommand {
    INPUT("IN", "6", "HDMI1", "HDMI2"),
    SOURCE("IS", "0", "480i", "576i", "480p", "576p", "720p50", "720p60", "1080i50", "1080i60", "1080p24", "1080p50",
            "1080p60", "No signal", "720p 3D", "1080i 3D", "1080p 3D", "4K"),
    HORIZONTAL_RESOLUTION("RH"),
    VERTICAL_RESOLUTION("RV"),
    HORIZONTAL_FREQUENCY("FH"),
    VERTICAL_FREQUENCY("FH"),
    DEEP_COLOR("DC", "0", "8 bit", "10 bit", "12 bit"),
    COLOR_SPACE("XV", "0", "RGB", "YUV", "x.v.Color"),
    LAMP_TIME("LT"),
    SOFTWARE_VERSION("SV");

    static {
        HORIZONTAL_RESOLUTION.isNumeric = true;
        VERTICAL_RESOLUTION.isNumeric = true;
        HORIZONTAL_FREQUENCY.isNumeric = true;
        VERTICAL_FREQUENCY.isNumeric = true;
        LAMP_TIME.isNumeric = true;
    }

    private final String command;
    private int offset;
    private boolean isNumeric = false;
    private String[] returnValues;
    private static Map<String, InformationCommand> lookup = new HashMap<String, InformationCommand>();
    static {
        for (InformationCommand e : values()) {
            lookup.put(e.command, e);
        }
    }

    static InformationCommand lookupCommand(String cmd) {
        return lookup.get(cmd);
    }

    String decodeParameter(String parameter) {
        if (isNumeric) {
            try {
                return String.valueOf(Integer.parseInt(parameter, 16));
            } catch (NumberFormatException e) {
                return e.toString();
            }
        }

        if (parameter.length() == 1) {
            int index = Integer.parseInt(parameter, 16) - offset;
            return returnValues[index];
        }

        return parameter;
    }

    private InformationCommand(String command) {
        this.command = command;
    }

    private InformationCommand(String command, String offset, String... returnValues) {
        this(command);
        this.offset = Integer.parseInt(offset, 16);
        this.returnValues = returnValues;
    }

    byte[] getBytes() {
        return command.getBytes();
    }
}

enum RemoteCommand {
    STANDBY(0x37, 0x33, 0x30, 0x36),
    ON(0x37, 0x33, 0x30, 0x35),
    INPUT(0x37, 0x33, 0x30, 0x38),
    INFO(0x37, 0x33, 0x37, 0x34),
    ENVIRONMENT_SETTING(0x37, 0x33, 0x35, 0x45),
    SETTING_3D(0x37, 0x33, 0x44, 0x35),
    CLEAR_MOTION_DRIVE(0x37, 0x33, 0x38, 0x41),
    LENS_CONTROL(0x37, 0x33, 0x33, 0x30),
    LENS_MEMORY(0x37, 0x33, 0x44, 0x34),
    LENS_APERTURE(0x37, 0x33, 0x32, 0x30),
    MPC(0x37, 0x33, 0x46, 0x30),
    PICTURE_ANALYSER(0x37, 0x33, 0x35, 0x43),
    BEFORE_AFTER(0x37, 0x33, 0x43, 0x35),
    HIDE(0x37, 0x33, 0x31, 0x44),
    UP(0x37, 0x33, 0x30, 0x31),
    DOWN(0x37, 0x33, 0x30, 0x32),
    RIGHT(0x37, 0x33, 0x33, 0x34),
    LEFT(0x37, 0x33, 0x33, 0x36),
    OK(0x37, 0x33, 0x32, 0x46),
    MENU(0x37, 0x33, 0x32, 0x45),
    BACK(0x37, 0x33, 0x30, 0x33),
    FILM(0x37, 0x33, 0x36, 0x39),
    CINEMA(0x37, 0x33, 0x36, 0x38),
    ANIME(0x37, 0x33, 0x36, 0x36),
    NATURAL(0x37, 0x33, 0x36, 0x41),
    PHOTO(0x37, 0x33, 0x38, 0x42),
    STAGE(0x37, 0x33, 0x36, 0x37),
    THX(0x37, 0x33, 0x36, 0x46),
    USER(0x37, 0x33, 0x44, 0x37),
    FORMAT_3D(0x37, 0x33, 0x44, 0x36),
    ADVANCED_MENU(0x37, 0x33, 0x37, 0x33),
    GAMMA(0x37, 0x33, 0x37, 0x35),
    COLOR_TEMPERATURE(0x37, 0x33, 0x37, 0x36),
    COLOR_PROFILE(0x37, 0x33, 0x38, 0x38),
    PICTURE_ADJUST(0x37, 0x33, 0x37, 0x32);

    private final byte[] bytes;

    RemoteCommand(int... bytes) {
        this.bytes = new byte[bytes.length];
        for (int i = 0; i < bytes.length; ++i) {
            this.bytes[i] = (byte) bytes[i];
        }
    }

    byte[] getBytes() {
        return bytes;
    }
}

/**
 * The {@link JvcProjectorCommand} class provides the codes for each command available for JVC Projectors. Command
 * channel names are converted into the relevant code bytes that are sent to a provided request queue.
 *
 * @author Nick Hill - Initial contribution
 */
public class JvcProjectorCommand {
    private final Logger logger = LoggerFactory.getLogger(JvcProjectorCommand.class);

    private LinkedBlockingQueue<RequestMessage> requestQueue;

    private final int RESPONSE_QUEUE_TIMEOUT = 5000;

    // Actual command strings sent to/received from the device (without CR)
    private byte[] commandBytes = null;
    private String decodedResponse = null;

    // Short human-readable name of the command
    private MainCommand command;
    private InformationCommand infoCommand;

    private String errorCode = null;
    private String errorMessage = null;

    private boolean isQuiet = false;
    private boolean isQuery;

    private static final byte[] COMMAND_PREAMBLE = { '!', (byte) 0x89, 1 };
    private static final byte[] QUERY_PREAMBLE = { '?', (byte) 0x89, 1 };

    /*
     * The {@link JvcProjectorCommand} class issues the command codes necessary for JVC projectors. The byte codes are
     * placed on the provided request queue.
     *
     * @author Nick Hill - Initial contribution
     */
    private JvcProjectorCommand(LinkedBlockingQueue<RequestMessage> queue, MainCommand command, byte[] data) {
        requestQueue = queue;
        this.command = command;
        commandBytes = ArrayUtils.addAll(command.getBytes(), data);
    }

    public JvcProjectorCommand(LinkedBlockingQueue<RequestMessage> queue, MainCommand command) {
        this(queue, command, new byte[] {});
        isQuery = command.isQueryable();
    }

    public JvcProjectorCommand(LinkedBlockingQueue<RequestMessage> queue, MainCommand command, String arg) {
        this(queue, command, command.encodeParameter(arg).getBytes());
        isQuery = false;
    }

    public JvcProjectorCommand(LinkedBlockingQueue<RequestMessage> queue, InformationCommand infoCmd) {
        this(queue, MainCommand.INFORMATION, infoCmd.getBytes());
        infoCommand = infoCmd;
        isQuery = true;
    }

    public JvcProjectorCommand(LinkedBlockingQueue<RequestMessage> queue, RemoteCommand remoteCmd) {
        this(queue, MainCommand.REMOTE, remoteCmd.getBytes());
        isQuery = false;
    }

    public String getCommandName() {
        return command.toString();
    }

    public boolean isSuccessful() {
        return errorCode == null ? true : false;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    protected boolean isQuiet() {
        return isQuiet;
    }

    private void setQuiet(boolean quiet) {
        isQuiet = quiet;
    }

    /*
     * Execute a GlobalCache device command
     */
    public void executeQuiet() {
        setQuiet(true);
        execute();
    }

    public void execute() {
        if (requestQueue == null) {
            createGenericError("Execute method was called with a null requestQueue");
            return;
        }

        if (commandBytes == null) {
            createGenericError("Execute method was called with a null commandBytes");
            return;
        }

        // Send command & get response
        if (sendCommand()) {
            if (!isQuiet()) {
                logSuccess();
            }
        } else {
            if (!isQuiet()) {
                logFailure();
            }
        }
        return;
    }

    public void logSuccess() {
        logger.debug("Execute '{}' succeeded", getCommandName());
    }

    public void logFailure() {
        logger.error("Execute '{}' failed: errorCode={}, errorMessage={}", getCommandName(), errorCode, errorMessage);
    }

    /*
     * Place a request message onto the request queue, then wait on the response queue for the
     * response message. The CommandHandler private class in GlobalCacheHandler.java
     * is responsible for the actual device interaction.
     */
    private boolean sendCommand() {
        // Create a response queue. The command processor will use this queue to return the device's reply.
        LinkedBlockingQueue<byte[]> responseQueue = new LinkedBlockingQueue<byte[]>();

        byte[] header;
        int numResponses;
        if (isQuery) {
            header = QUERY_PREAMBLE;
            // 2 responses are expected (ACK + Response)
            numResponses = 2;
        } else {
            header = COMMAND_PREAMBLE;
            // Only ACK is expected
            numResponses = 1;
        }

        // Create the request message
        RequestMessage requestMsg = new RequestMessage(getCommandName(), ArrayUtils.addAll(header, commandBytes),
                responseQueue, numResponses);
        byte[] response;

        try {
            // Put the request message on the request queue
            requestQueue.put(requestMsg);
            logger.trace("Put request on queue (depth={}), sent command '{}'", requestQueue.size(),
                    getAsHexString(commandBytes));

            // Wait on the response queue for the response message
            logger.trace("Waiting for ACK to command");
            response = responseQueue.poll(RESPONSE_QUEUE_TIMEOUT, TimeUnit.MILLISECONDS);

            if (response == null) {
                logger.trace("Timed out waiting on response queue for message");
                createGenericError("Timed out waiting on response queue for message");
                return false;
            }

            if (response.length == 0) {
                createGenericError("No ACK received");
                return false;
            }

            if (!isAck(response)) {
                logger.trace("Did not receive Ack message for request. Got {}", decodedResponse);
                createGenericError("Did not receive ACK for request");
                return false;
            }
            logger.trace("Got ACK message off response queue, received reply '{}'", decodedResponse);

            // There will be another message issued if the request was a query request
            if (isQuery) {
                logger.trace("Waiting for response to query");

                // Wait on the response queue for the response message
                response = responseQueue.poll(RESPONSE_QUEUE_TIMEOUT, TimeUnit.MILLISECONDS);

                if (response == null) {
                    createGenericError("Timed out waiting on response queue for message");
                    return false;
                }

                if (response.length == 0) {
                    createGenericError("No ACK received");
                    return false;
                }

                logger.trace("Got response to query off response queue, received reply '{}'", getAsHexString(response));
                if (!isResponse(response)) {
                    createGenericError("Did not receive response for request");
                    return false;
                }

                decodedResponse = parseResponse(response);
            }
        } catch (InterruptedException e) {
            createGenericError("Poll of response queue was interrupted");
            return false;
        }

        return true;
    }

    String parseResponse(byte[] response) {
        String parameter = new String(response, 5, response.length - 5);
        if (command == MainCommand.INFORMATION) {
            return infoCommand.decodeParameter(parameter);
        }
        return command.decodeParameter(parameter);
    }

    String getResponse() {
        return decodedResponse;
    }

    static String getAsHexString(byte[] b) {
        StringBuilder sb = new StringBuilder();

        for (int j = 0; j < b.length; j++) {
            String s = String.format("%02x ", b[j]);
            sb.append(s);
        }
        return sb.toString();
    }

    private void createGenericError(String s) {
        errorCode = "N/A";
        errorMessage = s;
    }

    private boolean isAck(byte[] bytes) {
        return (bytes[0] == 0x06 && bytes[1] == (byte) 0x89 && bytes[2] == 0x01 && bytes[3] == commandBytes[0]
                && bytes[4] == commandBytes[1]);
    }

    private boolean isResponse(byte[] bytes) {
        return (bytes[0] == '@' && bytes[1] == (byte) 0x89 && bytes[2] == 0x01 && bytes[3] == commandBytes[0]
                && bytes[4] == commandBytes[1]);
    }

}
