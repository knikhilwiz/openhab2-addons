/**
 *
 */
package org.openhab.binding.honeywellvistaalarm.internal.handler;

import static org.openhab.binding.honeywellvistaalarm.internal.HoneywellVistaAlarmBindingConstants.*;
import static org.openhab.binding.honeywellvistaalarm.internal.handler.HoneywellVistaAlarmMessage.SendMessageType.KEYSTROKE_COMMAND;
import static org.openhab.binding.honeywellvistaalarm.internal.handler.SystemEvent.*;

import java.time.LocalDateTime;

import org.eclipse.smarthome.core.library.types.DateTimeType;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.HSBType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.OpenClosedType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.openhab.binding.honeywellvistaalarm.internal.config.ZoneConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Nikhil
 *
 */
public class ZoneThingHandler extends BaseThingHandler {
    private final Logger logger = LoggerFactory.getLogger(ZoneThingHandler.class);

    private int zone;
    private int partition;
    PanelBridgeHandler panelBridgeHandler;

    /**
     * @param thing
     */
    public ZoneThingHandler(Thing thing) {
        super(thing);
        // TODO Auto-generated constructor stub
    }

    @Override
    public void initialize() {
        ZoneConfiguration configuration = getConfigAs(ZoneConfiguration.class);
        zone = configuration.zoneNumber;

        final Bridge bridge = getBridge();
        if (bridge == null || !(bridge.getHandler() instanceof PanelBridgeHandler)) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Zone must have a Panel bridge");
            return;
        }

        panelBridgeHandler = ((PanelBridgeHandler) bridge.getHandler());

        if (panelBridgeHandler.getThing().getStatus() != ThingStatus.ONLINE) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE);
            return;
        }

        updateStatus(ThingStatus.ONLINE);
    }

    public void updateTrouble(boolean isTroubled) {
        updateState(CHANNEL_ZONE_TROUBLE, isTroubled ? HSBType.RED : OnOffType.OFF);
    }

    public void updateContact(boolean isClosed) {
        updateState(CHANNEL_ZONE_CONTACT, isClosed ? OpenClosedType.CLOSED : OpenClosedType.OPEN);
    }

    public void updateAlarm(boolean isAlarmed) {
        updateState(CHANNEL_ZONE_ALARM, isAlarmed ? HSBType.RED : OnOffType.OFF);
    }

    public void updateBypass(boolean isBypassed) {
        updateState(CHANNEL_ZONE_BYPASS, isBypassed ? OnOffType.ON : OnOffType.OFF);
    }

    public void updatePartition(Integer partition) {
        updateState(CHANNEL_ZONE_PARTITION, new DecimalType(partition));
        this.partition = partition;
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (command instanceof RefreshType) {
            updateState(CHANNEL_ZONE_PARTITION, new DecimalType(partition));
            updateState(CHANNEL_ZONE_NAME, new DecimalType(zone));
            return;
        }

        switch (channelUID.getId()) {
            case CHANNEL_ZONE_BYPASS:
                if (command instanceof OnOffType) {
                    // Simulate the following key-presses: <user-code> 6 <zone> * *
                    String cmd = String.format("%4d" + "6" + "%03d" + "AA", panelBridgeHandler.getUserCode(), zone);
                    for (char c : cmd.toCharArray()) {
                        String ksCmd = String.format("%d%c", partition, c);
                        panelBridgeHandler.sendCommand(KEYSTROKE_COMMAND, ksCmd);
                    }
                }
                break;
            default:
                logger.warn("Ignoring command for zone {} on channel {}", zone, channelUID.getId());
        }
    }

    private void updateEventTimestamp(String channelName, LocalDateTime timestamp) {
        updateState(channelName, new DateTimeType(timestamp.toString()));
    }

    public void handleSystemEvent(SystemEvent event, LocalDateTime timestamp) {
        switch (event) {
            case TROUBLE:
                updateEventTimestamp(CHANNEL_ZONE_LAST_TROUBLE_TIME, timestamp);
            case TROUBLE_RESTORE:
                updateTrouble(event == TROUBLE);
                break;

            case FAULTS:
                updateEventTimestamp(CHANNEL_ZONE_LAST_OPEN_TIME, timestamp);
                updateContact(false);
                break;
            case FAULT_RESTORED:
                updateContact(true);
                updateEventTimestamp(CHANNEL_ZONE_LAST_CLOSE_TIME, timestamp);
                break;

            case PERIMETER_ALARM:
            case INTERIOR_ALARM:
            case ENTRY_EXIT_ALARM:
                updateAlarm(true);
                break;
            case PERIMETER_ALARM_RESTORE:
            case INTERIOR_ALARM_RESTORE:
            case ENTRY_EXIT_ALARM_RESTORE:
                updateAlarm(false);
                break;

            case BYPASS:
            case BYPASS_RESTORE:
            case VENT_ZONE_BYPASS:
            case VENT_ZONE_BYPASS_RESTORE:
                updateBypass(event == BYPASS || event == VENT_ZONE_BYPASS);
                break;

            default:
                logger.warn("Not handling event for zone {}: {}", zone, event.description());
        }

        updateEventTimestamp(CHANNEL_ZONE_LAST_EVENT_TIME, timestamp);
        updateState(CHANNEL_ZONE_MESSAGE, new StringType(event.description()));
    }
}
