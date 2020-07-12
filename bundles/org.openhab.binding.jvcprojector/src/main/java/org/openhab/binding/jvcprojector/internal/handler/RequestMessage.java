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
package org.openhab.binding.jvcprojector.internal.handler;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * The {@link RequestMessage} class is responsible for storing the command to be sent to the JvcProjector
 * device and the queue over which the responses are put.
 *
 * @author Nick Hill - Initial contribution
 */
public class RequestMessage {
    private LinkedBlockingQueue<byte[]> rcvQueue;
    private byte[] commandBytes;
    private String commandName;
    private int numResponses;

    public RequestMessage(String commandName, byte[] commandBytes, LinkedBlockingQueue<byte[]> rcvQueue,
            int numResponses) {
        this.commandName = commandName;
        this.commandBytes = commandBytes;
        this.rcvQueue = rcvQueue;
        this.numResponses = numResponses;
    }

    public byte[] getCommandBytes() {
        return commandBytes;
    }

    public String getCommandName() {
        return commandName;
    }

    public LinkedBlockingQueue<byte[]> getReceiveQueue() {
        return rcvQueue;
    }

    public int getNumResponses() {
        return numResponses;
    }
}
