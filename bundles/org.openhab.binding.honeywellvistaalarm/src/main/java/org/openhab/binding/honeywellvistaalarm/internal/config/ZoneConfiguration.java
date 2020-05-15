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
 * Configuration class for the Zone Thing.
 *
 * @author Nick Hill - initial contribution
 */

public class ZoneConfiguration {

    // Partition Thing constants
    public static final String ZONE_NUMBER = "zoneNumber";

    /**
     * The Zone Number. This is a required parameter for a zone.
     */
    public Integer zoneNumber;
}
