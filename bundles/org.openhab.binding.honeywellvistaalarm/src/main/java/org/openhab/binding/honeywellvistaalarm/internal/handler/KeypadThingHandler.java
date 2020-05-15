/**
 *
 */
package org.openhab.binding.honeywellvistaalarm.internal.handler;

import static org.openhab.binding.honeywellvistaalarm.internal.HoneywellVistaAlarmBindingConstants.*;
import static org.openhab.binding.honeywellvistaalarm.internal.handler.HoneywellVistaAlarmMessage.SendMessageType.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.smarthome.core.library.types.DateTimeType;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.HSBType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.openhab.binding.honeywellvistaalarm.internal.config.KeypadConfiguration;
import org.openhab.binding.honeywellvistaalarm.internal.handler.HoneywellVistaAlarmMessage.SendMessageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Nikhil
 *
 */
public class KeypadThingHandler extends BaseThingHandler {
    private final Logger logger = LoggerFactory.getLogger(KeypadThingHandler.class);

    public enum ArmingStatus {
        UNKNOWN(""),
        NOT_READY("-"),
        BYPASSED_READY("B"),
        DISARMED("D"),
        NOT_READY2("N"),
        ARMED_AWAY("A"),
        ARMED_STAY("H"),
        ARMED_MAXIMUM("M"),
        ARMED_INSTANT("I");

        static Map<String, ArmingStatus> index = new HashMap<String, ArmingStatus>();
        static Set<SendMessageType> armCommands = Stream
                .of(DISARM, ARM_STAY, ARM_AWAY, ARM_FORCE_STAY, ARM_FORCE_AWAY, ARM_INSTANT, ARM_MAX)
                .collect(Collectors.toCollection(HashSet::new));
        String code;

        static {
            for (ArmingStatus s : values()) {
                index.put(s.code, s);
            }
        }

        static ArmingStatus getArmingStatusForCode(String code) {
            return index.getOrDefault(code, UNKNOWN);
        }

        ArmingStatus(String code) {
            this.code = code;
        }

    }

    private int partition;
    PanelBridgeHandler panelBridgeHandler;

    /**
     * @param thing
     */
    public KeypadThingHandler(Thing thing) {
        super(thing);
        // TODO Auto-generated constructor stub
    }

    @Override
    public void initialize() {
        KeypadConfiguration configuration = getConfigAs(KeypadConfiguration.class);
        partition = configuration.partitionNumber;
        panelBridgeHandler = ((PanelBridgeHandler) getBridge().getHandler());

        // set the Thing offline since we are waiting for the bridge to come up. This will come online once the bridge
        // is up.
        updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (command instanceof RefreshType) {
            updateState(CHANNEL_KEYPAD_PARTITION, new DecimalType(partition));

            return;
        }

        switch (channelUID.getId()) {
            case CHANNEL_KEYPAD_ARM_COMMAND:
                if (!(command instanceof StringType)) {
                    return;
                }

                String data = String.format("%4d%08d", panelBridgeHandler.getUserCode(), partition);
                SendMessageType msg;
                try {
                    msg = SendMessageType.valueOf(command.toString());
                } catch (IllegalArgumentException e) {
                    return;
                }
                if (ArmingStatus.armCommands.contains(msg)) {
                    panelBridgeHandler.sendCommand(msg, data);
                    panelBridgeHandler.sendCommand(KEYPAD_DISPLAY_REQUEST, String.valueOf(partition));
                }
        }

    }

    public void updateArmingStatus(ArmingStatus armingStatus) {
        logger.debug("Updating the arming state for partition {} to {}", partition, armingStatus.toString());
        updateState(CHANNEL_KEYPAD_ARM_MODE, new StringType(armingStatus.toString()));
        updateState(CHANNEL_KEYPAD_ARMED_LED, armingStatus.name().startsWith("ARMED") ? HSBType.RED : OnOffType.OFF);

        if (armingStatus.name().startsWith("NOT_READY")) {
            updateState(CHANNEL_KEYPAD_READY_LED, OnOffType.OFF);
        }
    }

    public void updateKeypadStatus(boolean isReady, boolean isTrouble, boolean isArmed, boolean turnBacklightOn,
            String message) {
        updateState(CHANNEL_KEYPAD_DISPLAY, new StringType(message));
        updateState(CHANNEL_KEYPAD_READY_LED, isReady ? HSBType.GREEN : OnOffType.OFF);
        updateState(CHANNEL_KEYPAD_TROUBLE_LED, isTrouble ? HSBType.RED : OnOffType.OFF);
        updateState(CHANNEL_KEYPAD_ARMED_LED, isArmed ? HSBType.RED : OnOffType.OFF);
        updateState(CHANNEL_KEYPAD_BACKLIGHT, turnBacklightOn ? HSBType.WHITE : OnOffType.OFF);
    }

    private void updateEventTimestamp(String channelName, LocalDateTime timestamp) {
        updateState(channelName, new DateTimeType(timestamp.toString()));
    }

    public void handleSystemEvent(SystemEvent event, int user, int zone, LocalDateTime timestamp) {
        switch (event) {
            case CLOSE_ARM: // arm_away
                panelBridgeHandler.sendCommand(ARMING_STATUS_REQUEST);
                break;

            case ARM_STAY_CLOSE: // arm_stay
                panelBridgeHandler.sendCommand(ARMING_STATUS_REQUEST);
                panelBridgeHandler.sendCommand(ZONE_STATUS_REQUEST);
                break;

            case OPEN_DISARM: // disarm
                updateArmingStatus(ArmingStatus.DISARMED);
                panelBridgeHandler.sendCommand(ZONE_STATUS_REQUEST);
                break;

            case QUICK_ARM_CLOSE:
                panelBridgeHandler.sendCommand(ARMING_STATUS_REQUEST);
                panelBridgeHandler.sendCommand(ZONE_STATUS_REQUEST);
                break;

            case SILENT_ALARM:
            case PERIMETER_ALARM:
            case DAY_NIGHT_ALARM:
            case ENTRY_EXIT_ALARM:
            case RECENT_CLOSE_BY_USER:
                updateState(CHANNEL_KEYPAD_ALARM_ACTIVE, OnOffType.ON);
                break;

            case ALARM_CANCEL:
            case SILENT_ALARM_RESTORE:
            case PERIMETER_ALARM_RESTORE:
            case DAY_NIGHT_ALARM_RESTORE:
            case ENTRY_EXIT_ALARM_RESTORE:
                updateState(CHANNEL_KEYPAD_ALARM_ACTIVE, OnOffType.OFF);
                break;

        }

        if (zone == 0 || zone > 900) {
            updateState(CHANNEL_KEYPAD_LAST_EVENT,
                    new StringType(String.format("[user %s] %s", user, event.description())));
            updateEventTimestamp(CHANNEL_KEYPAD_LAST_EVENT_TIME, timestamp);
        }
    }

}
