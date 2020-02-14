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

import static org.openhab.binding.jvcprojector.internal.JvcProjectorBindingConstants.THING_TYPE_PROJECTOR;

import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandlerFactory;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandlerFactory;
import org.openhab.binding.jvcprojector.internal.handler.JvcProjectorHandler;
import org.osgi.service.component.annotations.Component;

/**
 * The {@link JvcProjectorHandlerFactory} is responsible for creating thing handlers.
 *
 * @author Mark Hilbush - Initial contribution
 */
@Component(service = ThingHandlerFactory.class, configurationPid = "binding.jvcprojector")
public class JvcProjectorHandlerFactory extends BaseThingHandlerFactory {
    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return thingTypeUID.equals(THING_TYPE_PROJECTOR);
    }

    @Override
    protected ThingHandler createHandler(Thing thing) {

        ThingTypeUID thingTypeUID = thing.getThingTypeUID();

        if (thingTypeUID.equals(THING_TYPE_PROJECTOR)) {
            return new JvcProjectorHandler(thing);
        }

        return null;
    }
}
