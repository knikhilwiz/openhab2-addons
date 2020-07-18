/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
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
package org.openhab.binding.honeywellvistaalarm.internal.handler;

import static org.openhab.binding.honeywellvistaalarm.internal.HoneywellVistaAlarmBindingConstants.*;
import static org.openhab.binding.honeywellvistaalarm.internal.handler.HoneywellVistaAlarmMessage.SendMessageType.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.binding.BaseBridgeHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.eclipse.smarthome.io.transport.serial.SerialPortManager;
import org.openhab.binding.honeywellvistaalarm.internal.HoneywellVistaAlarmDiscoveryService;
import org.openhab.binding.honeywellvistaalarm.internal.config.KeypadConfiguration;
import org.openhab.binding.honeywellvistaalarm.internal.config.PanelConfiguration;
import org.openhab.binding.honeywellvistaalarm.internal.config.ZoneConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The bridge handler for the DSC IT100 RS232 Serial interface.
 *
 * @author Russell Stephens - Initial Contribution
 */

public class PanelBridgeHandler extends BaseBridgeHandler implements RS232InterfaceEvent {

    private SerialPortManager serialPortManager;

    private final Logger logger = LoggerFactory.getLogger(PanelBridgeHandler.class);

    /** Discovery service */
    private HoneywellVistaAlarmDiscoveryService discoveryService = null;

    private String serialPortName = "";
    private RS232Interface serialInterface;

    /** Determines if things have changed. */
    // private boolean thingsHaveChanged = false;

    private Map<Integer, KeypadThingHandler> keypadHandlers = null;
    private Map<Integer, ZoneThingHandler> zoneHandlers = null;

    /** Thing count. */
    private int thingCount = 0;

    private Integer userCode = null;

    // Polling variables
    public int pollPeriod = 0;
    private long pollElapsedTime = 0;
    private long pollStartTime = 0;
    private long commandSendTime = 0;
    private long commandTimeoutMs = 30000;
    private long refreshIntervalMs = 5000;
    private ScheduledFuture<?> pollingTask;

    private Pattern validKeystrokes = Pattern.compile("^[0-9*#]+$");
    private Pattern systemEvent = Pattern.compile(
            "(?<code>[A-Z0-9]{2})(?<zone>[0-9]{3})(?<user>[0-9]){3}(?<partition>[0-9])(?<timestamp>[0-9]{10})");
    private DateTimeFormatter systemEventTimestampFormat = DateTimeFormatter.ofPattern("mmHHddMMuu");

    private boolean readyForNextCommand = false;
    private ConcurrentLinkedQueue<HoneywellVistaAlarmMessage> writeQueue;

    private HoneywellVistaAlarmMessage activeCommand;

    public PanelBridgeHandler(Bridge bridge, SerialPortManager serialPortManager) {
        super(bridge);
        this.serialPortManager = serialPortManager;
    }

    @Override
    public void initialize() {
        logger.debug("Initializing the Panel Bridge handler.");

        PanelConfiguration configuration = getConfigAs(PanelConfiguration.class);
        keypadHandlers = new HashMap<Integer, KeypadThingHandler>();
        zoneHandlers = new HashMap<Integer, ZoneThingHandler>();

        userCode = configuration.userCode;
        serialPortName = configuration.serialPort;

        serialInterface = new RS232Interface(serialPortManager, serialPortName, 9600, scheduler, this);

        if (serialPortName != null) {
            pollPeriod = configuration.pollPeriod.intValue();

            if (this.pollPeriod > 15) {
                this.pollPeriod = 15;
            } else if (this.pollPeriod < 1) {
                this.pollPeriod = 1;
            }

            updateStatus(ThingStatus.OFFLINE);
            startPolling();
        }
    }

    @Override
    public void dispose() {
        stopPolling();
        serialInterface.disconnect();
        super.dispose();
    }

    /**
     * Runs when connected.
     */
    @Override
    public void onConnected() {
        logger.debug("onConnected(): Bridge Connected!");

        setBridgeOnline(true);
        writeQueue = new ConcurrentLinkedQueue<HoneywellVistaAlarmMessage>();
        readyForNextCommand = true;
        commandSendTime = -1;
        sendCommand(ARMING_STATUS_REQUEST);
    }

    /**
     * Runs when disconnected.
     */
    @Override
    public void onDisconnected() {
        setBridgeOnline(false);
        readyForNextCommand = false;
        if (writeQueue != null) {
            writeQueue.clear();
        }
        commandSendTime = -1;
    }

    /**
     * Set Bridge Status.
     *
     * @param isOnline
     */
    public void setBridgeOnline(boolean isOnline) {
        logger.debug("setBridgeConnection(): Setting Bridge to {}",
                isOnline ? ThingStatus.ONLINE : ThingStatus.OFFLINE);

        updateStatus(isOnline ? ThingStatus.ONLINE : ThingStatus.OFFLINE);

        updateState(BRIDGE_RESET, isOnline ? OnOffType.ON : OnOffType.OFF);
    }

    private void parseArmingStatusReport(String message) {
        byte[] data = message.getBytes();
        for (int i = 0; i < data.length; ++i) {
            Integer partition = i + 1;
            if (this.keypadHandlers.containsKey(partition)) {
                this.keypadHandlers.get(partition).updateArmingStatus(
                        KeypadThingHandler.ArmingStatus.getArmingStatusForCode(new String(new byte[] { data[i] })));
            }
        }
    }

    private int parseZoneDescriptor(String data) {
        int zone = Integer.parseInt(data.substring(0, 3));
        if (zone == 0) {
            return zone;
        }
        String label = String.format("Zone %d: %s", zone, data.substring(4, data.length() - 1));
        discoveryService.addZoneThing(getThing(), zone, label);
        return zone;
    }

    private void parseZonePartitionReport(String message) {
        byte[] data = message.getBytes();
        int block = data[0] - '0' - 1;
        for (int i = 1; i < data.length; ++i) {
            Integer partition = data[i] - '0';

            if (partition == 0) {
                continue;
            }

            discoveryService.addKeypadThing(getThing(), partition);

            Integer zone = block * 64 + i;
            ZoneThingHandler handler = this.zoneHandlers.get(zone);

            if (handler == null) {
                continue;
            }

            handler.updatePartition(partition);
        }
    }

    private void parseZoneStatusReport(String message) {
        int block = Integer.parseInt(message.substring(0, 1));
        for (int i = 1; i < message.length(); ++i) {
            Integer zone = (block - 1) * 64 + i;
            int status = Integer.parseInt(message.substring(i, i + 1), 16);
            ZoneThingHandler handler = this.zoneHandlers.get(zone);

            if (handler == null) {
                continue;
            }

            handler.updateContact((status & 0x1) == 0);
            handler.updateTrouble((status & 0x2) > 0);
            handler.updateAlarm((status & 0x4) > 0);
            handler.updateBypass((status & 0x8) > 0);
        }
    }

    private void parseSystemEvent(String data) {
        Matcher matcher = systemEvent.matcher(data);
        if (!matcher.matches()) {
            logger.error("Unable to parse system event: {}", data);
            return;
        }

        int eventCode, user;
        Integer zone, partition;

        try {
            eventCode = Integer.parseInt(matcher.group("code"), 16);
            zone = Integer.parseInt(matcher.group("zone"));
            user = Integer.parseInt(matcher.group("user"));
            partition = Integer.parseInt(matcher.group("partition"));
        } catch (NumberFormatException e) {
            logger.error("Unable to parse system event ({}) : {}", matcher.toString(), e.getMessage());
            return;
        }
        LocalDateTime timestamp = LocalDateTime.parse(matcher.group("timestamp"), systemEventTimestampFormat);

        SystemEvent event = SystemEvent.lookupEventCode(eventCode);
        logger.debug("{}: zone {} [partition {}] [user {}] => event {} ({})", timestamp.toString(), zone, partition,
                user, event.toString(), eventCode);

        if (zone > 0 && zoneHandlers.containsKey(zone)) {
            zoneHandlers.get(zone).handleSystemEvent(event, timestamp);
        }

        if (partition > 0 && keypadHandlers.containsKey(partition)) {
            keypadHandlers.get(partition).handleSystemEvent(event, user, zone, timestamp);
        }

    }

    private void parseKeypadDisplayResponse(String data) {
        if (activeCommand.messageType() != KEYPAD_DISPLAY_REQUEST) {
            return;
        }

        int partition = Integer.parseInt(activeCommand.getData());
        char[] chars = new char[data.length()];
        data.getChars(0, data.length(), chars, 0);
        boolean turnBacklightOn = (chars[0] & 0x80) > 0;
        chars[0] = (char) (chars[0] & 0x7f);

        int ledByte = Integer.parseInt(Character.toString(chars[chars.length - 1]), 16);
        String message = new String(Arrays.copyOf(chars, chars.length - 1));

        if (this.keypadHandlers.containsKey(partition)) {
            this.keypadHandlers.get(partition).updateKeypadStatus((ledByte & 0x1) > 0, (ledByte & 0x2) > 0,
                    (ledByte & 0x4) > 0, turnBacklightOn, message);
        }
    }

    @Override
    public synchronized void handleIncomingMessage(String incomingMessage) {
        if (incomingMessage == null || incomingMessage.isEmpty()) {
            logger.debug("handleIncomingMessage(): No Message Received!");
            return;
        }

        HoneywellVistaAlarmMessage message;
        try {
            message = new HoneywellVistaAlarmMessage(incomingMessage);
        } catch (Exception e) {
            logger.error("Error parsing {}: {}", incomingMessage, e);
            return;
        }

        logger.debug("handleIncomingMessage(): Message received: {} - {}", incomingMessage, message.messageType());

        switch ((HoneywellVistaAlarmMessage.ReceivedMessageType) message.messageType()) {
            case ARMING_STATUS_REPORT:
                parseArmingStatusReport(message.getData());
                break;
            case ZONE_PARTITION_REPORT:
                parseZonePartitionReport(message.getData());
                break;
            case ZONE_STATUS_REPORT:
                parseZoneStatusReport(message.getData());
                break;
            case KEYPAD_DISPLAY_RESPONSE:
                parseKeypadDisplayResponse(message.getData());
                break;
            case COMMUNICATIONS_OFF:
                logger.warn("Communications off.");
                onDisconnected();
                break;
            case COMMUNICATIONS_ON:
                logger.warn("Communications back online.");
                onConnected();
                break;
            case ZONE_DESCRIPTOR_REPORT:
                if (parseZoneDescriptor(message.getData()) > 0) {
                    break;
                }
                // flow over to read for next, since no explicit READY_FOR_NEXT is sent after the end of the zone
                // descriptor report.
            case READY_FOR_NEXT:
                readyForNextCommand = true;
                commandSendTime = -1;
                processWriteQueue();
                break;
            case SYSTEM_EVENT:
                parseSystemEvent(message.getData());
                break;
            case ACK:
                break;
            default:
                logger.warn("Not processing " + message.getDescription());
        }
    }

    /**
     * Get Panel User Code.
     */
    public Integer getUserCode() {
        return this.userCode;
    }

    /**
     * Set Panel User Code.
     *
     * @param userCode
     */
    public void setUserCode(Integer userCode) {
        this.userCode = userCode;
    }

    /**
     * Method to start the polling task.
     */
    public void startPolling() {
        logger.debug("Starting Polling Task.");
        if (pollingTask == null || pollingTask.isCancelled()) {
            pollingTask = scheduler.scheduleWithFixedDelay(this::polling, 0, refreshIntervalMs, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Method to stop the polling task.
     */
    public void stopPolling() {
        logger.debug("Stopping Polling Task.");
        if (pollingTask != null && !pollingTask.isCancelled()) {
            pollingTask.cancel(true);
            pollingTask = null;
        }
    }

    /**
     * Method for polling the Alarm System.
     */
    public void polling() {
        // logger.debug("Honeywell Alarm Polling Task - '{}' is {}", getThing().getUID(), getThing().getStatus());

        if (commandSendTime > 0 && System.currentTimeMillis() - commandSendTime > commandTimeoutMs) {
            logger.error("Command Response Timeout. Disconnecting");
            serialInterface.disconnect();
        }

        if (!serialInterface.isConnected()) {
            logger.error("Not Connected to the Honeywell Alarm!");
            serialInterface.connect();
            return;
        }

        if (pollStartTime == 0) {
            pollStartTime = System.currentTimeMillis();
        }

        pollElapsedTime = ((System.currentTimeMillis() - pollStartTime) / 1000) / 60;

        // Send Poll command to the Alarm if idle for 'pollPeriod'
        // minutes
        if (pollElapsedTime >= pollPeriod) {
            for (int partition : keypadHandlers.keySet()) {
                sendCommand(KEYPAD_DISPLAY_REQUEST, String.valueOf(partition));
            }
            pollStartTime = 0;
        }

        checkThings();

        processWriteQueue();
    }

    /**
     * Check if things have changed.
     */
    public void checkThings() {
        // logger.debug("Checking Things!");

        List<Thing> things = getThing().getThings();

        if (things.size() == thingCount) {
            return;
        }
        thingCount = things.size();

        for (Thing thing : things) {

            ThingHandler handler = thing.getHandler();

            if (handler != null) {
                // logger.debug("***Checking '{}' - Status: {}", thing.getUID(), thing.getStatus());

                if (!thing.getStatus().equals(ThingStatus.ONLINE)) {
                    if (getThing().getStatus().equals(ThingStatus.ONLINE)) {
                        handler.bridgeStatusChanged(getThing().getStatusInfo());
                    }
                }
            } else {
                logger.error("checkThings(): Thing handler not found!");
            }
        }

        sendCommand(ZONE_PARTITION_REQUEST);
        sendCommand(ZONE_STATUS_REQUEST);
        sendCommand(ARMING_STATUS_REQUEST);
    }

    @Override
    public void childHandlerInitialized(ThingHandler handler, Thing thing) {
        if (thing.getThingTypeUID().equals(THING_TYPE_ZONE)) {
            zoneHandlers.put(thing.getConfiguration().as(ZoneConfiguration.class).zoneNumber,
                    (ZoneThingHandler) handler);
        } else if (thing.getThingTypeUID().equals(THING_TYPE_KEYPAD)) {
            int partition = thing.getConfiguration().as(KeypadConfiguration.class).partitionNumber;
            keypadHandlers.put(partition, (KeypadThingHandler) handler);
        } else {
            logger.error("Unknown thing type initialized with bridge: {}!", thing.getThingTypeUID().getAsString());
        }
    }

    @Override
    public void childHandlerDisposed(ThingHandler handler, Thing thing) {

        if (thing.getThingTypeUID().equals(THING_TYPE_ZONE)) {
            zoneHandlers.remove(thing.getConfiguration().as(ZoneConfiguration.class).zoneNumber);
        } else if (thing.getThingTypeUID().equals(THING_TYPE_KEYPAD)) {
            keypadHandlers.remove(thing.getConfiguration().as(KeypadConfiguration.class).partitionNumber);
        } else {
            logger.error("Unknown thing type removed from bridge: {}!", thing.getThingTypeUID().getAsString());
        }
    }

    public void sendCommand(HoneywellVistaAlarmMessage msg) {
        logger.debug("Queueing to send {}", msg.messageType());
        writeQueue.add(msg);
        scheduler.execute(() -> processWriteQueue());
    }

    public void sendCommand(HoneywellVistaAlarmMessage.SendMessageType cmd) {
        sendCommand(cmd, "");
    }

    public void sendCommand(HoneywellVistaAlarmMessage.SendMessageType cmd, String data) {
        sendCommand(new HoneywellVistaAlarmMessage(cmd, data));
    }

    private synchronized void processWriteQueue() {
        if (writeQueue == null) {
            return;
        }

        synchronized (writeQueue) {
            if (readyForNextCommand && !writeQueue.isEmpty()) {
                HoneywellVistaAlarmMessage newCommand = writeQueue.remove();
                if (newCommand == activeCommand) {
                    return;
                }
                activeCommand = newCommand;
                serialInterface.write(activeCommand.getMessage() + "\r\n");
                readyForNextCommand = false;
                commandSendTime = System.currentTimeMillis();
            }
        }
    }

    private void sendKeySequence(String keySequence) {
        logger.debug("sendKeySequence(): Sending key sequence '{}'.", keySequence);

        if (!validKeystrokes.matcher(keySequence).matches()) {
            logger.error("SendKeySequence(): invalid characters in keysequence: '{}'", keySequence);
            return;
        }

        if (keySequence.length() > 5) {
            logger.error("SendKeySequence(): only supports up to 5 characters for now. Received: {}", keySequence);
            return;
        }

        sendCommand(KEYSTROKE_COMMAND, keySequence);
    }

    public void registerDiscoveryService(HoneywellVistaAlarmDiscoveryService discoveryService) {
        this.discoveryService = discoveryService;
    }

    public void unregisterDiscoveryService() {
        this.discoveryService = null;
        logger.trace("unregisterDiscoveryService(): Discovery Service Unregistered!");
    }

    public void startScan() {
        if (serialInterface.isConnected()) {
            sendCommand(ZONE_DESCRIPTOR_REQUEST);
        }
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        logger.debug("handleCommand(): Command Received - {} {}.", channelUID, command);

        if (command instanceof RefreshType) {
            return;
        }

        if (!serialInterface.isConnected()) {
            return;
        }

        switch (channelUID.getId()) {
            case BRIDGE_RESET:
                if (command == OnOffType.OFF) {
                    serialInterface.disconnect();
                }
                break;
            case SEND_COMMAND:
                if (!command.toString().isEmpty()) {
                    String[] tokens = command.toString().split(",");

                    String cmd = tokens[0];
                    String data = "";
                    if (tokens.length > 1) {
                        data = tokens[1];
                    }

                    try {
                        HoneywellVistaAlarmMessage msg = new HoneywellVistaAlarmMessage(cmd, data);
                        sendCommand(msg);
                    } catch (IllegalArgumentException e) {
                        logger.error("Unknown command '{}'", cmd);
                    }
                }
                break;
            default:
                break;
        }
    }
}
