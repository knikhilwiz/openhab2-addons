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

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.smarthome.config.discovery.AbstractDiscoveryService;
import org.eclipse.smarthome.config.discovery.DiscoveryResult;
import org.eclipse.smarthome.config.discovery.DiscoveryResultBuilder;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.openhab.binding.honeywellvistaalarm.internal.config.KeypadConfiguration;
import org.openhab.binding.honeywellvistaalarm.internal.config.ZoneConfiguration;
import org.openhab.binding.honeywellvistaalarm.internal.handler.PanelBridgeHandler;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is responsible for discovering DSC Alarm Things via the bridge.
 *
 * @author Russell Stephens - Initial Contribution
 *
 */
@Component(service = HoneywellVistaAlarmDiscoveryService.class, immediate = true, configurationPid = "discovery.honeywellvistaalarm")
public class HoneywellVistaAlarmDiscoveryService extends AbstractDiscoveryService {

    private final Logger logger = LoggerFactory.getLogger(HoneywellVistaAlarmDiscoveryService.class);

    /**
     * Panel Bridge handler.
     */
    private PanelBridgeHandler panelBridgeHandler;

    /**
     * Constructor.
     *
     * @param PanelBridgeHandler
     */
    HoneywellVistaAlarmDiscoveryService(PanelBridgeHandler panelBridgeHandler) {
        super(HoneywellVistaAlarmBindingConstants.SUPPORTED_THING_TYPES_UIDS, 15, true);
        this.panelBridgeHandler = panelBridgeHandler;
    }

    /**
     * Activates the Discovery Service.
     */
    public void activate() {
        removeOlderResults(new Date().getTime());
        panelBridgeHandler.registerDiscoveryService(this);
    }

    /**
     * Deactivates the Discovery Service.
     */
    @Override
    public void deactivate() {
        panelBridgeHandler.unregisterDiscoveryService();
    }

    public void addZoneThing(Bridge bridge, int zone, String label) {
        String thingID = "zone" + String.valueOf(zone);
        Map<String, Object> properties = new HashMap<>(0);

        ThingUID thingUID = new ThingUID(HoneywellVistaAlarmBindingConstants.THING_TYPE_ZONE, bridge.getUID(), thingID);
        properties.put(ZoneConfiguration.ZONE_NUMBER, zone);
        // properties.put(ZoneConfiguration.ZONE_LABEL, label);

        DiscoveryResult discoveryResult;

        discoveryResult = DiscoveryResultBuilder.create(thingUID).withBridge(bridge.getUID()).withLabel(label)
                .withProperties(properties).build();

        thingDiscovered(discoveryResult);
    }

    public void addKeypadThing(Bridge bridge, int partition) {
        String thingID = "keypad" + String.valueOf(partition);
        String thingLabel = "Keypad for Partition " + String.valueOf(partition);
        Map<String, Object> properties = new HashMap<>(0);

        ThingUID thingUID = new ThingUID(HoneywellVistaAlarmBindingConstants.THING_TYPE_KEYPAD, bridge.getUID(),
                thingID);
        properties.put(KeypadConfiguration.PARTITION_NUMBER, partition);

        DiscoveryResult discoveryResult;

        discoveryResult = DiscoveryResultBuilder.create(thingUID).withBridge(bridge.getUID()).withLabel(thingLabel)
                .withProperties(properties).build();

        thingDiscovered(discoveryResult);
    }

    @Override
    protected void startScan() {
        // Can be ignored here as discovery is via the bridge
    }
}
