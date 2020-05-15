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
package org.openhab.binding.honeywellvistaalarm.internal;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.thing.ThingTypeUID;

/**
 * The {@link HoneywellVistaAlarmBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Nick Hill - Initial contribution
 */
@NonNullByDefault
public class HoneywellVistaAlarmBindingConstants {

    private static final String BINDING_ID = "honeywellvistaalarm";

    // List of all Thing Type UIDs
    public static final ThingTypeUID THING_TYPE_PANEL = new ThingTypeUID(BINDING_ID, "panel");
    public static final ThingTypeUID THING_TYPE_KEYPAD = new ThingTypeUID(BINDING_ID, "keypad");
    public static final ThingTypeUID THING_TYPE_ZONE = new ThingTypeUID(BINDING_ID, "zone");

    // List of all Channel ids
    public static final String BRIDGE_RESET = "bridge_reset";
    public static final String SEND_COMMAND = "send_command";

    public static final String CHANNEL_ZONE_NAME = "name";
    public static final String CHANNEL_ZONE_MESSAGE = "message";
    public static final String CHANNEL_ZONE_CONTACT = "contact";
    public static final String CHANNEL_ZONE_TROUBLE = "trouble";
    public static final String CHANNEL_ZONE_ALARM = "alarm";
    public static final String CHANNEL_ZONE_PARTITION = "partition";
    public static final String CHANNEL_ZONE_BYPASS = "bypass";
    public static final String CHANNEL_ZONE_LAST_OPEN_TIME = "lastOpenTime";
    public static final String CHANNEL_ZONE_LAST_CLOSE_TIME = "lastCloseTime";
    public static final String CHANNEL_ZONE_LAST_ALARM_TIME = "lastAlarmTime";
    public static final String CHANNEL_ZONE_LAST_TROUBLE_TIME = "lastTroubleTime";
    public static final String CHANNEL_ZONE_LAST_EVENT_TIME = "lastEventTime";

    public static final String CHANNEL_KEYPAD_ARM_COMMAND = "arm_command";
    public static final String CHANNEL_KEYPAD_ARM_MODE = "arm_mode";
    public static final String CHANNEL_KEYPAD_PARTITION = "partition";
    public static final String CHANNEL_KEYPAD_DISPLAY = "display";
    public static final String CHANNEL_KEYPAD_BACKLIGHT = "backlight";
    public static final String CHANNEL_KEYPAD_TROUBLE_LED = "trouble_led";
    public static final String CHANNEL_KEYPAD_READY_LED = "ready_led";
    public static final String CHANNEL_KEYPAD_ARMED_LED = "armed_led";
    public static final String CHANNEL_KEYPAD_ALARM_ACTIVE = "alarmActive";
    public static final String CHANNEL_KEYPAD_LAST_EVENT = "lastEvent";
    public static final String CHANNEL_KEYPAD_LAST_EVENT_TIME = "lastEventTime";

    // Set of all supported Thing Type UIDs
    public static final Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = Collections.unmodifiableSet(
            Stream.of(THING_TYPE_PANEL, THING_TYPE_KEYPAD, THING_TYPE_ZONE).collect(Collectors.toSet()));
}
