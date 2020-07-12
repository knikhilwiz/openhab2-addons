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
package org.openhab.binding.jvcprojector.internal;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.thing.ThingTypeUID;

/**
 * The {@link JvcProjectorBindingConstants} class defines common constants that are
 * used by the jvcprojector binding.
 *
 * @author Nick Hill - Initial contribution
 */
@NonNullByDefault
public class JvcProjectorBindingConstants {

    public static final String BINDING_ID = "jvcprojector";

    /*
     * JvcProjector thing definitions
     */
    public static final ThingTypeUID THING_TYPE_PROJECTOR = new ThingTypeUID(BINDING_ID, "projector");

    public static final String CHANNEL_POWER_SWITCH = "power-switch";
    public static final String CHANNEL_POWER_STATE = "power-state";
    public static final String CHANNEL_INPUT = "input";
    public static final String CHANNEL_SOURCE = "source";
    public static final String CHANNEL_MODEL = "model";

    // info
    public static final String CHANNEL_SOURCE_FORMAT = "source-format";
    public static final String CHANNEL_LAMP_TIME = "lamp-time";
    public static final String CHANNEL_SOFTWARE_VERSION = "software-version";

    // source-info
    public static final String CHANNEL_HORIZONTAL_RESOLUTION = "horizontal-resolution";
    public static final String CHANNEL_VERTICAL_RESOLUTION = "vertical-resolution";
    public static final String CHANNEL_HORIZONTAL_FREQUENCY = "horizontal-frequency";
    public static final String CHANNEL_VERTICAL_FREQUENCY = "vertical-frequency";
    public static final String CHANNEL_DEEP_COLOR = "deep-color";
    public static final String CHANNEL_COLOR_SPACE = "color-space";

    // remote
    public static final String CHANNEL_B_STANDBY = "standby";
    public static final String CHANNEL_B_ON = "on";
    public static final String CHANNEL_B_INPUT = "input";
    public static final String CHANNEL_B_INFO = "info";
    public static final String CHANNEL_B_ENVIRONMENT_SETTING = "environment-setting";
    public static final String CHANNEL_B_3D_SETTING = "3d-setting";
    public static final String CHANNEL_B_CLEAR_MOTION_DRIVE = "clear-motion-drive";
    public static final String CHANNEL_B_LENS_CONTROL = "lens-control";
    public static final String CHANNEL_B_LENS_MEMORY = "lens-memory";
    public static final String CHANNEL_B_LENS_APERTURE = "lens-aperture";
    public static final String CHANNEL_B_MPC = "mpc";
    public static final String CHANNEL_B_PICTURE_ANALYSER = "picture-analyser";
    public static final String CHANNEL_B_BEFORE_AFTER = "before-after";
    public static final String CHANNEL_B_HIDE = "hide";
    public static final String CHANNEL_B_UP = "up";
    public static final String CHANNEL_B_DOWN = "down";
    public static final String CHANNEL_B_RIGHT = "right";
    public static final String CHANNEL_B_LEFT = "left";
    public static final String CHANNEL_B_OK = "ok";
    public static final String CHANNEL_B_MENU = "menu";
    public static final String CHANNEL_B_BACK = "back";
    public static final String CHANNEL_B_FILM = "film";
    public static final String CHANNEL_B_CINEMA = "cinema";
    public static final String CHANNEL_B_ANIME = "anime";
    public static final String CHANNEL_B_NATURAL = "natural";
    public static final String CHANNEL_B_PHOTO = "photo";
    public static final String CHANNEL_B_STAGE = "stage";
    public static final String CHANNEL_B_THX = "thx";
    public static final String CHANNEL_B_USER = "user";
    public static final String CHANNEL_B_3D_FORMAT = "3d-format";
    public static final String CHANNEL_B_ADVANCED_MENU = "advanced-menu";
    public static final String CHANNEL_B_GAMMA = "gamma";
    public static final String CHANNEL_B_COLOR_TEMPERATURE = "color-temperature";
    public static final String CHANNEL_B_COLOR_PROFILE = "color-profile";
    public static final String CHANNEL_B_PICTURE_ADJUST = "picture-adjust";

    /*
     * JvcProjector thing configuration items
     */
    // Network address of the device
    public static final String THING_PROPERTY_IP = "ipAddress";
    // Network port of the device
    public static final String THING_PROPERTY_PORT = "port";

    /*
     * Channel constants
     */
    // JvcProjector Channel Types
    public static final String CHANNEL_TYPE_SWITCH = "channel-type-switch";
    public static final String CHANNEL_TYPE_INPUT = "channel-type-input";
    public static final String CHANNEL_TYPE_REMOTE_BUTTON = "channel-type-remote-button";

    public static final String CHANNEL_GROUP_TYPE_STATUS = "status";
    public static final String CHANNEL_GROUP_TYPE_REMOTE = "remote";
    public static final String CHANNEL_GROUP_TYPE_INFO = "info";
    public static final String CHANNEL_GROUP_TYPE_SOURCE_INFO = "source-info";
}
