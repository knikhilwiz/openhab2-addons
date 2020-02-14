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

    public static final String CHANNEL_POWER_STATE = "power-state";
    public static final String CHANNEL_INPUT_STATE = "input-state";

    public static final String CHANNEL_STANDBY = "standby";
    public static final String CHANNEL_ON = "on";
    public static final String CHANNEL_INPUT = "input";
    public static final String CHANNEL_INFO = "info";
    public static final String CHANNEL_ENVIRONMENT_SETTING = "environment-setting";
    public static final String CHANNEL_3D_SETTING = "3d-setting";
    public static final String CHANNEL_CLEAR_MOTION_DRIVE = "clear-motion-drive";
    public static final String CHANNEL_LENS_CONTROL = "lens-control";
    public static final String CHANNEL_LENS_MEMORY = "lens-memory";
    public static final String CHANNEL_LENS_APERTURE = "lens-aperture";
    public static final String CHANNEL_MPC = "mpc";
    public static final String CHANNEL_PICTURE_ANALYSER = "picture-analyser";
    public static final String CHANNEL_BEFORE_AFTER = "before-after";
    public static final String CHANNEL_HIDE = "hide";
    public static final String CHANNEL_UP = "up";
    public static final String CHANNEL_DOWN = "down";
    public static final String CHANNEL_RIGHT = "right";
    public static final String CHANNEL_LEFT = "left";
    public static final String CHANNEL_OK = "ok";
    public static final String CHANNEL_MENU = "menu";
    public static final String CHANNEL_BACK = "back";
    public static final String CHANNEL_FILM = "film";
    public static final String CHANNEL_CINEMA = "cinema";
    public static final String CHANNEL_ANIME = "anime";
    public static final String CHANNEL_NATURAL = "natural";
    public static final String CHANNEL_PHOTO = "photo";
    public static final String CHANNEL_STAGE = "stage";
    public static final String CHANNEL_THX = "thx";
    public static final String CHANNEL_USER = "user";
    public static final String CHANNEL_3D_FORMAT = "3d-format";
    public static final String CHANNEL_ADVANCED_MENU = "advanced-menu";
    public static final String CHANNEL_GAMMA = "gamma";
    public static final String CHANNEL_COLOR_TEMPERATURE = "color-temperature";
    public static final String CHANNEL_COLOR_PROFILE = "color-profile";
    public static final String CHANNEL_PICTURE_ADJUST = "picture-adjust";

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
}
