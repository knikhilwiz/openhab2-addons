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
import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang.StringUtils;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.common.ThreadPoolManager;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.StringType;
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
 * @author Nick Hill - Heavily repurposed for handling JVC Projectors
 */
public class JvcProjectorHandler extends BaseThingHandler {
    private Logger logger = LoggerFactory.getLogger(JvcProjectorHandler.class);

    private static final String JVCPROJECTOR_THREAD_POOL = "jvcProjectorHandler";

    private CommandProcessor commandProcessor;
    private ScheduledExecutorService scheduledExecutorService = ThreadPoolManager
            .getScheduledPool(JVCPROJECTOR_THREAD_POOL + "-" + thingID());
    private ScheduledFuture<?> scheduledFuture;
    private @NonNullByDefault({}) ScheduledFuture<?> refreshJob;

    private LinkedBlockingQueue<RequestMessage> sendQueue = null;
    ChannelUID powerStateChannelUID;
    ChannelUID powerSwitchChannelUID;
    int CHANNEL_REFRESH_SECONDS = 15;
    boolean sourceSignal;

    Map<String, Object> channelCommandMap = Stream.of(new Object[][] {
            { JvcProjectorBindingConstants.CHANNEL_GROUP_TYPE_STATUS
                    + JvcProjectorBindingConstants.CHANNEL_POWER_SWITCH, MainCommand.POWER },
            { JvcProjectorBindingConstants.CHANNEL_GROUP_TYPE_STATUS + JvcProjectorBindingConstants.CHANNEL_POWER_STATE,
                    MainCommand.POWER },
            { JvcProjectorBindingConstants.CHANNEL_GROUP_TYPE_STATUS + JvcProjectorBindingConstants.CHANNEL_INPUT,
                    MainCommand.INPUT },
            { JvcProjectorBindingConstants.CHANNEL_GROUP_TYPE_STATUS + JvcProjectorBindingConstants.CHANNEL_SOURCE,
                    MainCommand.SOURCE },
            { JvcProjectorBindingConstants.CHANNEL_GROUP_TYPE_STATUS + JvcProjectorBindingConstants.CHANNEL_MODEL,
                    MainCommand.MODEL },

            { JvcProjectorBindingConstants.CHANNEL_GROUP_TYPE_INFO + JvcProjectorBindingConstants.CHANNEL_INPUT,
                    InformationCommand.INPUT },
            { JvcProjectorBindingConstants.CHANNEL_GROUP_TYPE_INFO + JvcProjectorBindingConstants.CHANNEL_SOURCE_FORMAT,
                    InformationCommand.SOURCE },
            { JvcProjectorBindingConstants.CHANNEL_GROUP_TYPE_INFO + JvcProjectorBindingConstants.CHANNEL_LAMP_TIME,
                    InformationCommand.LAMP_TIME },
            { JvcProjectorBindingConstants.CHANNEL_GROUP_TYPE_INFO
                    + JvcProjectorBindingConstants.CHANNEL_SOFTWARE_VERSION, InformationCommand.SOFTWARE_VERSION },

            { JvcProjectorBindingConstants.CHANNEL_GROUP_TYPE_SOURCE_INFO
                    + JvcProjectorBindingConstants.CHANNEL_HORIZONTAL_RESOLUTION,
                    InformationCommand.HORIZONTAL_RESOLUTION },
            { JvcProjectorBindingConstants.CHANNEL_GROUP_TYPE_SOURCE_INFO
                    + JvcProjectorBindingConstants.CHANNEL_VERTICAL_RESOLUTION,
                    InformationCommand.VERTICAL_RESOLUTION },
            { JvcProjectorBindingConstants.CHANNEL_GROUP_TYPE_SOURCE_INFO
                    + JvcProjectorBindingConstants.CHANNEL_HORIZONTAL_FREQUENCY,
                    InformationCommand.HORIZONTAL_FREQUENCY },
            { JvcProjectorBindingConstants.CHANNEL_GROUP_TYPE_SOURCE_INFO
                    + JvcProjectorBindingConstants.CHANNEL_VERTICAL_FREQUENCY, InformationCommand.VERTICAL_FREQUENCY },
            { JvcProjectorBindingConstants.CHANNEL_GROUP_TYPE_SOURCE_INFO
                    + JvcProjectorBindingConstants.CHANNEL_DEEP_COLOR, InformationCommand.DEEP_COLOR },
            { JvcProjectorBindingConstants.CHANNEL_GROUP_TYPE_SOURCE_INFO
                    + JvcProjectorBindingConstants.CHANNEL_COLOR_SPACE, InformationCommand.COLOR_SPACE },

            { JvcProjectorBindingConstants.CHANNEL_GROUP_TYPE_REMOTE + JvcProjectorBindingConstants.CHANNEL_B_STANDBY,
                    RemoteCommand.STANDBY },
            { JvcProjectorBindingConstants.CHANNEL_GROUP_TYPE_REMOTE + JvcProjectorBindingConstants.CHANNEL_B_ON,
                    RemoteCommand.ON },
            { JvcProjectorBindingConstants.CHANNEL_GROUP_TYPE_REMOTE + JvcProjectorBindingConstants.CHANNEL_B_INPUT,
                    RemoteCommand.INPUT },
            { JvcProjectorBindingConstants.CHANNEL_GROUP_TYPE_REMOTE + JvcProjectorBindingConstants.CHANNEL_B_INFO,
                    RemoteCommand.INFO },
            { JvcProjectorBindingConstants.CHANNEL_GROUP_TYPE_REMOTE
                    + JvcProjectorBindingConstants.CHANNEL_B_ENVIRONMENT_SETTING, RemoteCommand.ENVIRONMENT_SETTING },
            { JvcProjectorBindingConstants.CHANNEL_GROUP_TYPE_REMOTE
                    + JvcProjectorBindingConstants.CHANNEL_B_3D_SETTING, RemoteCommand.SETTING_3D },
            { JvcProjectorBindingConstants.CHANNEL_GROUP_TYPE_REMOTE
                    + JvcProjectorBindingConstants.CHANNEL_B_CLEAR_MOTION_DRIVE, RemoteCommand.CLEAR_MOTION_DRIVE },
            { JvcProjectorBindingConstants.CHANNEL_GROUP_TYPE_REMOTE
                    + JvcProjectorBindingConstants.CHANNEL_B_LENS_CONTROL, RemoteCommand.LENS_CONTROL },
            { JvcProjectorBindingConstants.CHANNEL_GROUP_TYPE_REMOTE
                    + JvcProjectorBindingConstants.CHANNEL_B_LENS_MEMORY, RemoteCommand.LENS_MEMORY },
            { JvcProjectorBindingConstants.CHANNEL_GROUP_TYPE_REMOTE
                    + JvcProjectorBindingConstants.CHANNEL_B_LENS_APERTURE, RemoteCommand.LENS_APERTURE },
            { JvcProjectorBindingConstants.CHANNEL_GROUP_TYPE_REMOTE + JvcProjectorBindingConstants.CHANNEL_B_MPC,
                    RemoteCommand.MPC },
            { JvcProjectorBindingConstants.CHANNEL_GROUP_TYPE_REMOTE
                    + JvcProjectorBindingConstants.CHANNEL_B_PICTURE_ANALYSER, RemoteCommand.PICTURE_ANALYSER },
            { JvcProjectorBindingConstants.CHANNEL_GROUP_TYPE_REMOTE
                    + JvcProjectorBindingConstants.CHANNEL_B_BEFORE_AFTER, RemoteCommand.BEFORE_AFTER },
            { JvcProjectorBindingConstants.CHANNEL_GROUP_TYPE_REMOTE + JvcProjectorBindingConstants.CHANNEL_B_HIDE,
                    RemoteCommand.HIDE },
            { JvcProjectorBindingConstants.CHANNEL_GROUP_TYPE_REMOTE + JvcProjectorBindingConstants.CHANNEL_B_UP,
                    RemoteCommand.UP },
            { JvcProjectorBindingConstants.CHANNEL_GROUP_TYPE_REMOTE + JvcProjectorBindingConstants.CHANNEL_B_DOWN,
                    RemoteCommand.DOWN },
            { JvcProjectorBindingConstants.CHANNEL_GROUP_TYPE_REMOTE + JvcProjectorBindingConstants.CHANNEL_B_RIGHT,
                    RemoteCommand.RIGHT },
            { JvcProjectorBindingConstants.CHANNEL_GROUP_TYPE_REMOTE + JvcProjectorBindingConstants.CHANNEL_B_LEFT,
                    RemoteCommand.LEFT },
            { JvcProjectorBindingConstants.CHANNEL_GROUP_TYPE_REMOTE + JvcProjectorBindingConstants.CHANNEL_B_OK,
                    RemoteCommand.OK },
            { JvcProjectorBindingConstants.CHANNEL_GROUP_TYPE_REMOTE + JvcProjectorBindingConstants.CHANNEL_B_MENU,
                    RemoteCommand.MENU },
            { JvcProjectorBindingConstants.CHANNEL_GROUP_TYPE_REMOTE + JvcProjectorBindingConstants.CHANNEL_B_BACK,
                    RemoteCommand.BACK },
            { JvcProjectorBindingConstants.CHANNEL_GROUP_TYPE_REMOTE + JvcProjectorBindingConstants.CHANNEL_B_FILM,
                    RemoteCommand.FILM },
            { JvcProjectorBindingConstants.CHANNEL_GROUP_TYPE_REMOTE + JvcProjectorBindingConstants.CHANNEL_B_CINEMA,
                    RemoteCommand.CINEMA },
            { JvcProjectorBindingConstants.CHANNEL_GROUP_TYPE_REMOTE + JvcProjectorBindingConstants.CHANNEL_B_ANIME,
                    RemoteCommand.ANIME },
            { JvcProjectorBindingConstants.CHANNEL_GROUP_TYPE_REMOTE + JvcProjectorBindingConstants.CHANNEL_B_NATURAL,
                    RemoteCommand.NATURAL },
            { JvcProjectorBindingConstants.CHANNEL_GROUP_TYPE_REMOTE + JvcProjectorBindingConstants.CHANNEL_B_PHOTO,
                    RemoteCommand.PHOTO },
            { JvcProjectorBindingConstants.CHANNEL_GROUP_TYPE_REMOTE + JvcProjectorBindingConstants.CHANNEL_B_STAGE,
                    RemoteCommand.STAGE },
            { JvcProjectorBindingConstants.CHANNEL_GROUP_TYPE_REMOTE + JvcProjectorBindingConstants.CHANNEL_B_THX,
                    RemoteCommand.THX },
            { JvcProjectorBindingConstants.CHANNEL_GROUP_TYPE_REMOTE + JvcProjectorBindingConstants.CHANNEL_B_USER,
                    RemoteCommand.USER },
            { JvcProjectorBindingConstants.CHANNEL_GROUP_TYPE_REMOTE + JvcProjectorBindingConstants.CHANNEL_B_3D_FORMAT,
                    RemoteCommand.FORMAT_3D },
            { JvcProjectorBindingConstants.CHANNEL_GROUP_TYPE_REMOTE
                    + JvcProjectorBindingConstants.CHANNEL_B_ADVANCED_MENU, RemoteCommand.ADVANCED_MENU },
            { JvcProjectorBindingConstants.CHANNEL_GROUP_TYPE_REMOTE + JvcProjectorBindingConstants.CHANNEL_B_GAMMA,
                    RemoteCommand.GAMMA },
            { JvcProjectorBindingConstants.CHANNEL_GROUP_TYPE_REMOTE
                    + JvcProjectorBindingConstants.CHANNEL_B_COLOR_TEMPERATURE, RemoteCommand.COLOR_TEMPERATURE },
            { JvcProjectorBindingConstants.CHANNEL_GROUP_TYPE_REMOTE
                    + JvcProjectorBindingConstants.CHANNEL_B_COLOR_PROFILE, RemoteCommand.COLOR_PROFILE },
            { JvcProjectorBindingConstants.CHANNEL_GROUP_TYPE_REMOTE
                    + JvcProjectorBindingConstants.CHANNEL_B_PICTURE_ADJUST, RemoteCommand.PICTURE_ADJUST },

    }).collect(Collectors.toMap(data -> (String) data[0], data -> data[1]));

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
        JvcProjectorCommand cmd = new JvcProjectorCommand(sendQueue, MainCommand.CHECK);
        cmd.execute();

        powerStateChannelUID = new ChannelUID(thing.getUID(), JvcProjectorBindingConstants.CHANNEL_GROUP_TYPE_STATUS,
                JvcProjectorBindingConstants.CHANNEL_POWER_STATE);
        powerSwitchChannelUID = new ChannelUID(thing.getUID(), JvcProjectorBindingConstants.CHANNEL_GROUP_TYPE_STATUS,
                JvcProjectorBindingConstants.CHANNEL_POWER_SWITCH);

        refreshJob = scheduler.scheduleWithFixedDelay(this::refreshChannels, CHANNEL_REFRESH_SECONDS,
                CHANNEL_REFRESH_SECONDS, TimeUnit.SECONDS);
    }

    @Override
    public void dispose() {
        logger.debug("Disposing thing {}", thingID());
        commandProcessor.terminate();
        if (scheduledFuture != null) {
            scheduledFuture.cancel(false);
        }
        if (refreshJob != null && !refreshJob.isCancelled()) {
            refreshJob.cancel(true);
            refreshJob = null;
        }
    }

    @Override
    public void handleConfigurationUpdate(Map<@NonNull String, @NonNull Object> configurationParameters) {
        super.handleConfigurationUpdate(configurationParameters);
        super.thingUpdated(getThing());
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        // Don't try to send command if the device is not online
        if (!isOnline()) {
            logger.trace("Can't handle command {} for {} because handler for thing {} is not ONLINE", command,
                    channelUID.getId(), thingID());
            return;
        }

        Channel channel = thing.getChannel(channelUID.getId());
        if (channel == null) {
            logger.warn("Unknown channel {} for thing {}; is item defined correctly?", channelUID.getId(), thingID());
            return;
        }

        if (command instanceof RefreshType) {
            handleRefresh(channel);
            return;
        }

        handleSend(command, channelUID);
    }

    Object getCommandType(String groupId, String channelId) {
        return channelCommandMap.get(groupId + channelId);
    }

    void refreshChannels() {
        if (!isOnline()) {
            return;
        }
        logger.trace("Update Channels for:{}", thing.getUID());
        getThing().getChannels().forEach(channel -> handleCommand(channel.getUID(), RefreshType.REFRESH));
    }

    private void handleSend(Command command, ChannelUID channelUID) {
        logger.debug("Handling Send command {} on channel {} of thing {}", command, channelUID.getId(), thingID());

        String groupId = channelUID.getGroupId();
        String channelId = channelUID.getIdWithoutGroup();

        JvcProjectorCommand sendReq;

        switch (groupId) {
            case JvcProjectorBindingConstants.CHANNEL_GROUP_TYPE_STATUS:
                MainCommand mainCmd = (MainCommand) getCommandType(groupId, channelId);
                if (!mainCmd.isModifyable()) {
                    return;
                }
                sendReq = new JvcProjectorCommand(sendQueue, mainCmd, command.toString());
                break;
            case JvcProjectorBindingConstants.CHANNEL_GROUP_TYPE_REMOTE:
                RemoteCommand remoteCmd = (RemoteCommand) getCommandType(groupId, command.toString());
                sendReq = new JvcProjectorCommand(sendQueue, remoteCmd);
                break;
            default:
                // no other channel groups accept commands
                return;
        }

        // ping first
        JvcProjectorCommand checkReq = new JvcProjectorCommand(sendQueue, MainCommand.CHECK);
        checkReq.execute();
        sendReq.execute();

        if (sendReq.isSuccessful()) {
            switch (channelId) {
                case JvcProjectorBindingConstants.CHANNEL_POWER_SWITCH:
                case JvcProjectorBindingConstants.CHANNEL_POWER_STATE:
                    handlePowerStateChange(sendReq.getResponse());
                    break;
                default:
                    updateState(channelUID.getId(), StringType.valueOf(sendReq.getResponse()));
                    break;
            }
        }
    }

    private void handlePowerStateChange(String response) {
        OnOffType state = OnOffType.from(response);
        if (state.equals(OnOffType.ON)) {
            markDevicePowerOn();
        } else {
            markDevicePowerOff();
        }
        updateState(powerSwitchChannelUID, state);
        updateState(powerStateChannelUID, StringType.valueOf(response));
    }

    private void handleRefresh(Channel channel) {
        String groupId = channel.getUID().getGroupId();
        String channelId = channel.getUID().getIdWithoutGroup();

        JvcProjectorCommand refreshReq;

        switch (groupId) {
            case JvcProjectorBindingConstants.CHANNEL_GROUP_TYPE_STATUS:
                MainCommand mainCmd = (MainCommand) getCommandType(groupId, channelId);
                if (!mainCmd.isQueryable()) {
                    return;
                }
                refreshReq = new JvcProjectorCommand(sendQueue, mainCmd);
                break;
            case JvcProjectorBindingConstants.CHANNEL_GROUP_TYPE_SOURCE_INFO:
                if (!sourceSignal || !powerOn) {
                    return;
                }
            case JvcProjectorBindingConstants.CHANNEL_GROUP_TYPE_INFO:
                if (!powerOn) {
                    return;
                }
                InformationCommand infoCmd = (InformationCommand) getCommandType(groupId, channelId);
                if (infoCmd == null) {
                    logger.error("Unable to find command for Channel: group: {} ; id: {}", groupId, channelId);
                    return;
                }
                refreshReq = new JvcProjectorCommand(sendQueue, infoCmd);
                break;
            default:
                // Nothing to refresh
                return;
        }

        logger.debug("Handle REFRESH command on channel {} for thing {}", channel.getUID(), thingID());
        refreshReq.execute();

        if (refreshReq.isSuccessful()) {
            switch (channelId) {
                case JvcProjectorBindingConstants.CHANNEL_POWER_SWITCH:
                case JvcProjectorBindingConstants.CHANNEL_POWER_STATE:
                    handlePowerStateChange(refreshReq.getResponse());
                    break;
                case JvcProjectorBindingConstants.CHANNEL_SOURCE:
                    handleSource(refreshReq.getResponse());
                default:
                    updateState(channel.getUID().getId(), StringType.valueOf(refreshReq.getResponse()));
                    break;
            }
        }
    }

    private void handleSource(String response) {
        sourceSignal = !response.equals("No signal");
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
        getThing().getChannels().forEach(channel -> updateState(channel.getUID().getId(), StringType.EMPTY));
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
     * @author Nick Hill - Heavily re-purposed for handling JVC Projectors
     */
    private class CommandProcessor extends Thread {
        private Logger logger = LoggerFactory.getLogger(CommandProcessor.class);

        private boolean terminate = false;
        private final String TERMINATE_COMMAND = "terminate";

        private final int SEND_QUEUE_MAX_DEPTH = 10;
        private final int SEND_QUEUE_TIMEOUT = 5000;

        private ProjectorConnection connectionManager;

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
            connectionManager = new ProjectorConnection();
            terminate = false;

            try {
                RequestMessage requestMessage;
                while (!terminate) {
                    connectionManager.connect();
                    requestMessage = sendQueue.poll(SEND_QUEUE_TIMEOUT, TimeUnit.MILLISECONDS);
                    if (requestMessage != null) {
                        if (requestMessage.getCommandName().equals(TERMINATE_COMMAND)) {
                            logger.debug("Processor for thing {} received terminate message", thingID());
                            break;
                        }

                        byte[] deviceReply = {};

                        if (connectionManager.isConnected()) {
                            try {
                                long startTime = System.currentTimeMillis();
                                writeCommandToDevice(requestMessage);
                                int responsesNeeded = requestMessage.getNumResponses();
                                while (responsesNeeded-- > 0) {
                                    deviceReply = connectionManager.readReplyFromDevice();
                                    long endTime = System.currentTimeMillis();
                                    logger.debug("Response for '{}' for thing {} at {} received in {} ms: {}",
                                            requestMessage.getCommandName(), thingID(), getConfigIP(),
                                            endTime - startTime, JvcProjectorCommand.getAsHexString(deviceReply));

                                    requestMessage.getReceiveQueue().put(deviceReply);
                                }
                            } catch (IOException e) {
                                logger.error("Comm error for thing {} at {}: {}", thingID(), getConfigIP(),
                                        e.getMessage());
                                if (!(e instanceof SocketTimeoutException)) {
                                    connectionManager.setCommError("ERROR: " + e.getMessage());
                                    connectionManager.disconnect();
                                }
                                requestMessage.getReceiveQueue().put(new byte[] {});
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

            connectionManager.disconnect();
            connectionManager = null;
            logger.debug("JvcProjectorCommand processor TERMINATING for thing {} at IP {}", thingID(), getConfigIP());
        }

        /*
         * Write the command to the device.
         */
        private void writeCommandToDevice(RequestMessage requestMessage) throws IOException {
            byte[] deviceCommand = requestMessage.getCommandBytes();

            logger.trace("Processor for thing {} writing command to device: {}", thingID(),
                    JvcProjectorCommand.getAsHexString(deviceCommand));

            connectionManager.writeCommand(deviceCommand);
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
    private class ProjectorConnection {
        private Logger logger = LoggerFactory.getLogger(ProjectorConnection.class);

        private boolean deviceIsConnected;

        private final int SOCKET_CONNECT_TIMEOUT = 1500;
        // used for all other operations on socket.
        private final int SOCKET_TIMEOUT = 1000;
        private Socket socket;
        private DataInputStream dataInputStream;
        private DataOutputStream dataOutputStream;

        public ProjectorConnection() {
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
                return -1;
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

            logger.debug("Connecting to {} port for thing {} at IP {}", getPort(), thingID(), getIPAddress());
            if (!openSocket()) {
                return;
            }
            // create streams
            try {
                dataInputStream = new DataInputStream(socket.getInputStream());
                dataOutputStream = new DataOutputStream(socket.getOutputStream());
            } catch (IOException e) {
                logger.debug("Error getting streams to {} port for thing {} at {}, exception={}", getPort(), thingID(),
                        getIPAddress(), e.getMessage());
                markThingOfflineWithError(ThingStatusDetail.OFFLINE.COMMUNICATION_ERROR, e.getMessage());
                closeSocket();
                return;
            }
            logger.info("Got a connection to {} port for thing {} at {}", getPort(), thingID(), getIPAddress());

            if (!doHandshake()) {
                return;
            }

            markThingOnline();
            deviceIsConnected = true;
        }

        private boolean doHandshake() {
            boolean handshakeComplete = false;
            final String PJ_OK = "PJ_OK";
            final String PJACK = "PJACK";
            final String PJREQ = "PJREQ";

            logger.debug("Starting handshake with thing {}", thingID());
            // Ensure that the handshake completes quickly
            try {
                byte[] cbuf = new byte[PJ_OK.length()];
                int bytesRead = dataInputStream.read(cbuf, 0, PJ_OK.length());
                logger.trace("Recieved from {} at {}:{} => {}", thingID(), getIPAddress(), getPort(), cbuf);

                if (bytesRead == PJ_OK.length() && Arrays.equals(cbuf, PJ_OK.getBytes())) {
                    dataOutputStream.write(PJREQ.getBytes());
                    dataOutputStream.flush();

                    bytesRead = dataInputStream.read(cbuf, 0, PJACK.length());
                    if (bytesRead == PJACK.length() && Arrays.equals(cbuf, PJACK.getBytes())) {
                        handshakeComplete = true;
                    } else {
                        logger.error("Projector (thing {}) at {}:{} unresponsive on protocol handshake. Got {}",
                                thingID(), getIPAddress(), getPort(), cbuf);
                        markThingOfflineWithError(ThingStatusDetail.OFFLINE.COMMUNICATION_ERROR,
                                "protocol handshake response timeout/error");
                        closeSocket();
                    }
                } else {
                    logger.error("Projector (thing {}) at {}:{} did not initiate protocol handshake. Got {}", thingID(),
                            getIPAddress(), getPort(), cbuf);
                    markThingOfflineWithError(ThingStatusDetail.OFFLINE.COMMUNICATION_ERROR,
                            "did not indicate protocol handshake");
                    closeSocket();
                }
            } catch (IOException e) {
                logger.debug("Error performing handshake with thing {} at {}:{}, exception={}", thingID(),
                        getIPAddress(), getPort(), e.getMessage());
                closeSocket();
            }

            return handshakeComplete;
        }

        private boolean openSocket() {
            try {
                socket = new Socket();
                socket.setSoTimeout(SOCKET_TIMEOUT);
                socket.connect(new InetSocketAddress(getIPAddress(), getPort()), SOCKET_CONNECT_TIMEOUT);
            } catch (IOException e) {
                logger.debug("Failed to get socket on {} port for thing {} at {}: {}", getPort(), thingID(),
                        getIPAddress(), e.getMessage());
                socket = null;
                return false;
            }
            return true;
        }

        void closeSocket() {
            if (socket != null) {
                try {
                    socket.close();
                    dataInputStream = null;
                    dataOutputStream = null;
                    socket = null;
                    logger.debug("Closed socket on {} port for thing {} at {}", getPort(), thingID(), getIPAddress());
                } catch (IOException e) {
                    logger.debug("Failed to close socket on {} port for thing {} at {}", getPort(), thingID(),
                            getIPAddress());
                }
            }
            deviceIsConnected = false;
        }

        /*
         * Disconnect from the command and serial port(s) on the device. Only disconnect the serial port
         * connections if the devices have serial ports.
         */
        protected void disconnect() {
            logger.trace("Disconnecting from thing {} at {}:{}", thingID(), getIPAddress(), getPort());

            markThingOffline();
            closeSocket();
        }

        private boolean isConnected() {
            return deviceIsConnected;
        }

        public void setCommError(String errorMessage) {
            markThingOfflineWithError(ThingStatusDetail.OFFLINE.COMMUNICATION_ERROR, errorMessage);
        }

        public void writeCommand(byte[] commandBytes) throws IOException {
            dataOutputStream.write(commandBytes);
            dataOutputStream.write(0x0a);
            dataOutputStream.flush();
        }

        /*
         * Read command reply from the device, then remove the CR at the end of the line.
         */
        private byte[] readReplyFromDevice() throws IOException {
            logger.trace("Processor for thing {} reading reply from device", thingID());

            if (dataInputStream == null) {
                logger.debug("Error reading from device because input stream object is null");
                throw new IOException("ERROR: BufferedReader is null!");
            }

            logger.trace("Processor for thing {} reading response from device", thingID());
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            final int MAX_RESPONSE_SIZE = 50;

            int i;
            while (bytes.size() < MAX_RESPONSE_SIZE) {
                i = dataInputStream.read();
                if (i == 0x0a) {
                    break;
                }
                if (i == -1) {
                    throw new IOException("ERROR: truncated read");
                }
                bytes.write(i & 0xff);
            }
            return bytes.toByteArray();
        }
    }
}