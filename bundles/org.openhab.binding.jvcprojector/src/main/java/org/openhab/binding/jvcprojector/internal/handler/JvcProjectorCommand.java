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

import java.util.AbstractMap.SimpleEntry;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang.ArrayUtils;
import org.openhab.binding.jvcprojector.internal.JvcProjectorBindingConstants;
import org.openhab.core.library.types.OnOffType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link JvcProjectorCommand} class provides the codes for each command available for JVC Projectors. Command
 * channel names are converted into the relevant code bytes that are sent to a provided request queue.
 *
 * @author Nick Hill - Initial contribution
 */
public class JvcProjectorCommand {
    private final Logger logger = LoggerFactory.getLogger(JvcProjectorCommand.class);

    private LinkedBlockingQueue<RequestMessage> requestQueue;

    private final int RESPONSE_QUEUE_TIMEOUT = 30000;

    protected String thingID;

    // Actual command strings sent to/received from the device (without CR)
    protected byte[] deviceCommand;
    protected String deviceReply;

    // Short human-readable name of the command
    protected String commandName;

    protected String errorCode;
    protected String errorMessage;

    private boolean isQuiet;
    private boolean isQuery;

    private static final byte[] COMMAND_PREAMBLE = { 0x21, (byte) 0x89, 1 };
    private static final byte[] QUERY_PREAMBLE = { 0x3F, (byte) 0x89, 1 };

    private static final byte[] CHECK_COMMAND = { 0, 0 };

    private static final Map<String, byte[]> statusCommands = Stream
            .of(new SimpleEntry<>(JvcProjectorBindingConstants.CHANNEL_POWER_STATE, new byte[] { 0x50, 0x57 }),
                    new SimpleEntry<>(JvcProjectorBindingConstants.CHANNEL_INPUT_STATE, new byte[] { 0x49, 0x50 }))
            .collect(Collectors.toMap(SimpleEntry::getKey, SimpleEntry::getValue));

    private static final Map<String, byte[]> operatingCommands = Stream.of(
            new SimpleEntry<>(JvcProjectorBindingConstants.CHANNEL_POWER_STATE + OnOffType.ON,
                    new byte[] { 0x50, 0x57, 0x31 }),
            new SimpleEntry<>(JvcProjectorBindingConstants.CHANNEL_POWER_STATE + OnOffType.OFF,
                    new byte[] { 0x50, 0x57, 0x30 }),
            new SimpleEntry<>(JvcProjectorBindingConstants.CHANNEL_INPUT_STATE + "1", new byte[] { 0x49, 0x50, 0x36 }),
            new SimpleEntry<>(JvcProjectorBindingConstants.CHANNEL_INPUT_STATE + "2", new byte[] { 0x49, 0x50, 0x37 }),
            new SimpleEntry<>(JvcProjectorBindingConstants.CHANNEL_STANDBY,
                    new byte[] { 0x52, 0x43, 0x37, 0x33, 0x30, 0x36 }),
            new SimpleEntry<>(JvcProjectorBindingConstants.CHANNEL_ON,
                    new byte[] { 0x52, 0x43, 0x37, 0x33, 0x30, 0x35 }),
            new SimpleEntry<>(JvcProjectorBindingConstants.CHANNEL_INPUT,
                    new byte[] { 0x52, 0x43, 0x37, 0x33, 0x30, 0x38 }),
            new SimpleEntry<>(JvcProjectorBindingConstants.CHANNEL_INFO,
                    new byte[] { 0x52, 0x43, 0x37, 0x33, 0x37, 0x34 }),
            new SimpleEntry<>(JvcProjectorBindingConstants.CHANNEL_ENVIRONMENT_SETTING,
                    new byte[] { 0x52, 0x43, 0x37, 0x33, 0x35, 0x45 }),
            new SimpleEntry<>(JvcProjectorBindingConstants.CHANNEL_3D_SETTING,
                    new byte[] { 0x52, 0x43, 0x37, 0x33, 0x44, 0x35 }),
            new SimpleEntry<>(JvcProjectorBindingConstants.CHANNEL_CLEAR_MOTION_DRIVE,
                    new byte[] { 0x52, 0x43, 0x37, 0x33, 0x38, 0x41 }),
            new SimpleEntry<>(JvcProjectorBindingConstants.CHANNEL_LENS_CONTROL,
                    new byte[] { 0x52, 0x43, 0x37, 0x33, 0x33, 0x30 }),
            new SimpleEntry<>(JvcProjectorBindingConstants.CHANNEL_LENS_MEMORY,
                    new byte[] { 0x52, 0x43, 0x37, 0x33, 0x44, 0x34 }),
            new SimpleEntry<>(JvcProjectorBindingConstants.CHANNEL_LENS_APERTURE,
                    new byte[] { 0x52, 0x43, 0x37, 0x33, 0x32, 0x30 }),
            new SimpleEntry<>(JvcProjectorBindingConstants.CHANNEL_MPC,
                    new byte[] { 0x52, 0x43, 0x37, 0x33, 0x46, 0x30 }),
            new SimpleEntry<>(JvcProjectorBindingConstants.CHANNEL_PICTURE_ANALYSER,
                    new byte[] { 0x52, 0x43, 0x37, 0x33, 0x35, 0x43 }),
            new SimpleEntry<>(JvcProjectorBindingConstants.CHANNEL_BEFORE_AFTER,
                    new byte[] { 0x52, 0x43, 0x37, 0x33, 0x43, 0x35 }),
            new SimpleEntry<>(JvcProjectorBindingConstants.CHANNEL_HIDE,
                    new byte[] { 0x52, 0x43, 0x37, 0x33, 0x31, 0x44 }),
            new SimpleEntry<>(JvcProjectorBindingConstants.CHANNEL_UP,
                    new byte[] { 0x52, 0x43, 0x37, 0x33, 0x30, 0x31 }),
            new SimpleEntry<>(JvcProjectorBindingConstants.CHANNEL_DOWN,
                    new byte[] { 0x52, 0x43, 0x37, 0x33, 0x30, 0x32 }),
            new SimpleEntry<>(JvcProjectorBindingConstants.CHANNEL_RIGHT,
                    new byte[] { 0x52, 0x43, 0x37, 0x33, 0x33, 0x34 }),
            new SimpleEntry<>(JvcProjectorBindingConstants.CHANNEL_LEFT,
                    new byte[] { 0x52, 0x43, 0x37, 0x33, 0x33, 0x36 }),
            new SimpleEntry<>(JvcProjectorBindingConstants.CHANNEL_OK,
                    new byte[] { 0x52, 0x43, 0x37, 0x33, 0x32, 0x46 }),
            new SimpleEntry<>(JvcProjectorBindingConstants.CHANNEL_MENU,
                    new byte[] { 0x52, 0x43, 0x37, 0x33, 0x32, 0x35 }),
            new SimpleEntry<>(JvcProjectorBindingConstants.CHANNEL_BACK,
                    new byte[] { 0x52, 0x43, 0x37, 0x33, 0x30, 0x33 }),
            new SimpleEntry<>(JvcProjectorBindingConstants.CHANNEL_FILM,
                    new byte[] { 0x52, 0x43, 0x37, 0x33, 0x36, 0x39 }),
            new SimpleEntry<>(JvcProjectorBindingConstants.CHANNEL_CINEMA,
                    new byte[] { 0x52, 0x43, 0x37, 0x33, 0x36, 0x38 }),
            new SimpleEntry<>(JvcProjectorBindingConstants.CHANNEL_ANIME,
                    new byte[] { 0x52, 0x43, 0x37, 0x33, 0x36, 0x36 }),
            new SimpleEntry<>(JvcProjectorBindingConstants.CHANNEL_NATURAL,
                    new byte[] { 0x52, 0x43, 0x37, 0x33, 0x36, 0x41 }),
            new SimpleEntry<>(JvcProjectorBindingConstants.CHANNEL_PHOTO,
                    new byte[] { 0x52, 0x43, 0x37, 0x33, 0x38, 0x42 }),
            new SimpleEntry<>(JvcProjectorBindingConstants.CHANNEL_STAGE,
                    new byte[] { 0x52, 0x43, 0x37, 0x33, 0x36, 0x37 }),
            new SimpleEntry<>(JvcProjectorBindingConstants.CHANNEL_THX,
                    new byte[] { 0x52, 0x43, 0x37, 0x33, 0x36, 0x46 }),
            new SimpleEntry<>(JvcProjectorBindingConstants.CHANNEL_USER,
                    new byte[] { 0x52, 0x43, 0x37, 0x33, 0x44, 0x37 }),
            new SimpleEntry<>(JvcProjectorBindingConstants.CHANNEL_3D_FORMAT,
                    new byte[] { 0x52, 0x43, 0x37, 0x33, 0x44, 0x36 }),
            new SimpleEntry<>(JvcProjectorBindingConstants.CHANNEL_ADVANCED_MENU,
                    new byte[] { 0x52, 0x43, 0x37, 0x33, 0x37, 0x33 }),
            new SimpleEntry<>(JvcProjectorBindingConstants.CHANNEL_GAMMA,
                    new byte[] { 0x52, 0x43, 0x37, 0x33, 0x37, 0x35 }),
            new SimpleEntry<>(JvcProjectorBindingConstants.CHANNEL_COLOR_TEMPERATURE,
                    new byte[] { 0x52, 0x43, 0x37, 0x33, 0x37, 0x36 }),
            new SimpleEntry<>(JvcProjectorBindingConstants.CHANNEL_COLOR_PROFILE,
                    new byte[] { 0x52, 0x43, 0x37, 0x33, 0x38, 0x38 }),
            new SimpleEntry<>(JvcProjectorBindingConstants.CHANNEL_PICTURE_ADJUST,
                    new byte[] { 0x52, 0x43, 0x37, 0x33, 0x37, 0x32 }))
            .collect(Collectors.toMap(SimpleEntry::getKey, SimpleEntry::getValue));

    /*
     * The {@link JvcProjectorCommand} class issues the command codes necessary for JVC projectors. The byte codes are
     * placed on the provided request queue.
     *
     * @author Nick Hill - Initial contribution
     */
    public JvcProjectorCommand(String t, LinkedBlockingQueue<RequestMessage> q, String n) {
        thingID = t;
        requestQueue = q;
        commandName = n;
        setQuiet(false);
        isQuery = false;

        if (commandName.isEmpty()) {
            deviceCommand = CHECK_COMMAND;
        } else {
            if (operatingCommands.containsKey(commandName)) {
                deviceCommand = operatingCommands.get(commandName);
            } else if (statusCommands.containsKey(commandName)) {
                deviceCommand = statusCommands.get(commandName);
                isQuery = true;
            } else {
                logger.error("Unknown command {} being sent for thing {}", commandName, thingID);
                deviceCommand = null;
            }
        }

        deviceReply = null;
        errorCode = null;
        errorMessage = null;
    }

    public String getCommandName() {
        return commandName;
    }

    public void parseSuccessfulReply() {
    }

    public boolean isSuccessful() {
        return errorCode == null ? true : false;
    }

    public String successAsString() {
        return errorCode == null ? "succeeded" : "failed";
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

        if (deviceCommand == null) {
            createGenericError("Execute method was called with a null deviceCommand");
            return;
        }

        if (thingID == null) {
            createGenericError("Execute method was called with a null thing");
            return;
        }

        // Send command & get response
        if (sendCommand()) {
            parseSuccessfulReply();
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
        logger.debug("Execute '{}' succeeded on thing {}", commandName, thingID);
    }

    public void logFailure() {
        logger.error("Execute '{}' failed on thing {}: errorCode={}, errorMessage={}", commandName, thingID, errorCode,
                errorMessage);
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
        RequestMessage requestMsg = new RequestMessage(commandName, ArrayUtils.addAll(header, deviceCommand),
                responseQueue, numResponses);
        byte[] response;

        try {
            // Put the request message on the request queue
            requestQueue.put(requestMsg);
            logger.trace("Put request on queue (depth={}), sent command '{}'", requestQueue.size(),
                    getAsHexString(deviceCommand));

            // Wait on the response queue for the response message
            response = responseQueue.poll(RESPONSE_QUEUE_TIMEOUT, TimeUnit.MILLISECONDS);

            if (response == null) {
                logger.trace("Timed out waiting on response queue for message");
                createGenericError("Timed out waiting on response queue for message");
                return false;
            }

            deviceReply = getAsHexString(response);

            if (!isAck(response)) {
                logger.trace("Did not receive Ack message for request. Got {}", deviceReply);
                createGenericError("Did not receive ACK for request");
                return false;
            }
            logger.trace("Got ACK message off response queue, received reply '{}'", deviceReply);

            // There will be another message issued if the request was a query request
            if (isQuery) {
                logger.trace("Waiting for response to query");

                // Wait on the response queue for the response message
                response = responseQueue.poll(RESPONSE_QUEUE_TIMEOUT, TimeUnit.MILLISECONDS);

                if (response == null) {
                    createGenericError("Timed out waiting on response queue for message");
                    return false;
                }

                deviceReply = getAsHexString(response);
                logger.trace("Got response to query off response queue, received reply '{}'", deviceReply);
            }
        } catch (InterruptedException e) {
            createGenericError("Poll of response queue was interrupted");
            return false;
        }

        return true;
    }

    private static String getAsHexString(byte[] b) {
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
        return (bytes[0] == 0x06 && bytes[1] == (byte) 0x89 && bytes[2] == 0x01 && bytes[3] == deviceCommand[0]
                && bytes[4] == deviceCommand[1]);
    }

}
