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
package org.openhab.binding.honeywellvistaalarm.internal.config;

/**
 * Configuration class for the RS232 link to the Honeywell Vista Alarm panel
 *
 * @author Nick Hill - initial contribution
 */

public class PanelConfiguration {

    // IT-100 Bridge Thing constants
    public static final String SERIAL_PORT = "serialPort";
    public static final String POLL_PERIOD = "pollPeriod";
    public static final String USER_CODE = "userCode";

    /**
     * The port name for a serial connection. Valid values are e.g. COM1 for Windows and /dev/ttyS0 or
     * /dev/ttyUSB0 for Linux. Also supports a rfc2217://host:port style address for a remote serial port.
     */
    public String serialPort;

    /**
     * The Panel Poll Period. Can be set in range 1-15 minutes. Default is 1 minute;
     */
    public Integer pollPeriod;

    /**
     * The user code for all the operations.
     */
    public Integer userCode;
}
