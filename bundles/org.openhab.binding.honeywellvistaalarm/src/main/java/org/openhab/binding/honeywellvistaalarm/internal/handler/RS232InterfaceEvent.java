package org.openhab.binding.honeywellvistaalarm.internal.handler;

public interface RS232InterfaceEvent {

    public void onConnected();

    public void onDisconnected();

    public void handleIncomingMessage(String message);
}
