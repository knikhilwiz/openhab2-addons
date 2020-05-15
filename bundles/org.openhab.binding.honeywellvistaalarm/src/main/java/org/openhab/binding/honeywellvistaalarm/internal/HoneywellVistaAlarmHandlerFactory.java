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

import static org.openhab.binding.honeywellvistaalarm.internal.HoneywellVistaAlarmBindingConstants.SUPPORTED_THING_TYPES_UIDS;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.config.discovery.DiscoveryService;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandlerFactory;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandlerFactory;
import org.eclipse.smarthome.io.transport.serial.SerialPortManager;
import org.openhab.binding.honeywellvistaalarm.internal.config.KeypadConfiguration;
import org.openhab.binding.honeywellvistaalarm.internal.config.PanelConfiguration;
import org.openhab.binding.honeywellvistaalarm.internal.config.ZoneConfiguration;
import org.openhab.binding.honeywellvistaalarm.internal.handler.KeypadThingHandler;
import org.openhab.binding.honeywellvistaalarm.internal.handler.PanelBridgeHandler;
import org.openhab.binding.honeywellvistaalarm.internal.handler.ZoneThingHandler;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link HoneywellVistaAlarmHandlerFactory} is responsible for creating things and thing
 * handlers.
 *
 * @author Nick Hill - Initial contribution
 */
@Component(configurationPid = "binding.honeywellvistaalarm", service = ThingHandlerFactory.class)
public class HoneywellVistaAlarmHandlerFactory extends BaseThingHandlerFactory {

    private final Logger logger = LoggerFactory.getLogger(HoneywellVistaAlarmHandlerFactory.class);
    private final Map<ThingUID, ServiceRegistration<?>> discoveryServiceRegistrations = new HashMap<>();

    @Reference
    SerialPortManager serialPortManager;

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID);
    }

    @Override
    public Thing createThing(ThingTypeUID thingTypeUID, Configuration configuration, ThingUID thingUID,
            ThingUID bridgeUID) {
        if (HoneywellVistaAlarmBindingConstants.THING_TYPE_PANEL.equals(thingTypeUID)) {
            ThingUID panelThingUID = getHoneywellVistaAlarmPanelUID(thingTypeUID, thingUID, configuration);
            logger.debug("createThing(): PANEL: Creating '{}' type Thing - {}", thingTypeUID, panelThingUID.getId());
            return super.createThing(thingTypeUID, configuration, panelThingUID, bridgeUID);
        } else if (HoneywellVistaAlarmBindingConstants.THING_TYPE_KEYPAD.equals(thingTypeUID)) {
            ThingUID keypadThingUID = getHoneywellVistaAlarmKeypadUID(thingTypeUID, thingUID, configuration, bridgeUID);
            logger.debug("createThing(): KEYPAD: Creating '{}' type Thing - {}", thingTypeUID, keypadThingUID.getId());
            return super.createThing(thingTypeUID, configuration, keypadThingUID, bridgeUID);
        } else if (HoneywellVistaAlarmBindingConstants.THING_TYPE_ZONE.equals(thingTypeUID)) {
            ThingUID zoneThingUID = getHoneywellVistaAlarmZoneUID(thingTypeUID, thingUID, configuration, bridgeUID);
            logger.debug("createThing(): ZONE: Creating '{}' type Thing - {}", thingTypeUID, zoneThingUID.getId());
            return super.createThing(thingTypeUID, configuration, zoneThingUID, bridgeUID);
        }

        throw new IllegalArgumentException("createThing(): The thing type " + thingTypeUID
                + " is not supported by the HoneywellVistaAlarm binding.");
    }

    private ThingUID getHoneywellVistaAlarmPanelUID(ThingTypeUID thingTypeUID, ThingUID thingUID,
            Configuration configuration) {
        ThingUID thingUID2 = thingUID;
        if (thingUID2 == null) {
            String serialPort = (String) configuration.get(PanelConfiguration.SERIAL_PORT);
            String bridgeID = serialPort.replace('.', '_');
            thingUID2 = new ThingUID(thingTypeUID, bridgeID);
        }
        return thingUID2;
    }

    private ThingUID getHoneywellVistaAlarmKeypadUID(ThingTypeUID thingTypeUID, ThingUID thingUID,
            Configuration configuration, ThingUID bridgeUID) {
        ThingUID thingUID2 = thingUID;
        if (thingUID2 == null) {
            String keypadId = "keypad" + (String) configuration.get(KeypadConfiguration.PARTITION_NUMBER);
            thingUID2 = new ThingUID(thingTypeUID, keypadId, bridgeUID.getId());
        }
        return thingUID2;
    }

    private ThingUID getHoneywellVistaAlarmZoneUID(ThingTypeUID thingTypeUID, ThingUID thingUID,
            Configuration configuration, ThingUID bridgeUID) {
        ThingUID thingUID2 = thingUID;
        if (thingUID2 == null) {
            String zoneId = "zone" + (String) configuration.get(ZoneConfiguration.ZONE_NUMBER);
            thingUID2 = new ThingUID(thingTypeUID, zoneId, bridgeUID.getId());
        }
        return thingUID2;
    }

    @Override
    protected ThingHandler createHandler(Thing thing) {
        ThingTypeUID thingTypeUID = thing.getThingTypeUID();

        if (thingTypeUID.equals(HoneywellVistaAlarmBindingConstants.THING_TYPE_PANEL)) {
            PanelBridgeHandler handler = new PanelBridgeHandler((Bridge) thing, serialPortManager);
            HoneywellVistaAlarmDiscoveryService discoveryService = new HoneywellVistaAlarmDiscoveryService(handler);
            discoveryService.activate();

            ServiceRegistration<?> discoveryServiceRegistration = bundleContext
                    .registerService(DiscoveryService.class.getName(), discoveryService, new Hashtable<>());
            discoveryServiceRegistrations.put(handler.getThing().getUID(), discoveryServiceRegistration);

            logger.debug(
                    "registerHoneywellAlarmDiscoveryService(): Bridge Handler - {}, Class Name - {}, Discovery Service - {}",
                    handler, DiscoveryService.class.getName(), discoveryService);
            logger.debug("createHandler(): THING_TYPE_PANEL: ThingHandler created for {}", thingTypeUID);
            return handler;
        } else if (thingTypeUID.equals(HoneywellVistaAlarmBindingConstants.THING_TYPE_ZONE)) {
            logger.debug("createHandler(): THING_TYPE_ZONE: ThingHandler created for {}", thingTypeUID);
            return new ZoneThingHandler(thing);
        } else if (thingTypeUID.equals(HoneywellVistaAlarmBindingConstants.THING_TYPE_KEYPAD)) {
            logger.debug("createHandler(): THING_TYPE_KEYPAD: ThingHandler created for {}", thingTypeUID);
            return new KeypadThingHandler(thing);
        } else {
            logger.debug("createHandler(): ThingHandler not found for {}", thingTypeUID);
            return null;
        }
    }
}
