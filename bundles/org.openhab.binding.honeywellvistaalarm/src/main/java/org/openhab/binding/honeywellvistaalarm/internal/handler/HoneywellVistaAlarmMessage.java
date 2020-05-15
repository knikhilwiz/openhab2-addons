package org.openhab.binding.honeywellvistaalarm.internal.handler;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HoneywellVistaAlarmMessage {

    private final Logger logger = LoggerFactory.getLogger(HoneywellVistaAlarmMessage.class);
    private MessageType messageType;
    private String data;
    private String message;

    public HoneywellVistaAlarmMessage(String incomingMessage) {
        parseMessage(incomingMessage);
    }

    public HoneywellVistaAlarmMessage(String cmd, String data) {
        SendMessageType messageType = SendMessageType.getMessageType(cmd);
        this.data = data;
        if (messageType.needParams() && data == null) {
            throw new IllegalArgumentException("Need parameters to be passed in for " + messageType.description);
        }

        this.messageType = messageType;
    }

    public HoneywellVistaAlarmMessage(SendMessageType cmdType, String data) {
        this.messageType = cmdType;
        if (cmdType.needParams() && data == null) {
            throw new IllegalArgumentException("Need parameters to be passed in for " + cmdType.description);
        }

        this.data = data;

        String message = cmdType.cmd + data + "00";
        // include 2 bytes for the length header and another 2 bytes for the checksum at the end
        int length = 2 + message.length() + 2;
        message = String.format("%1$02X", length) + message;
        this.message = message + String.format("%1$02X", calculateChecksum(message));
    }

    public void parseMessage(String message) {
        int length = 0;
        try {
            length = Integer.parseInt(message.substring(0, 2), 16);
        } catch (NumberFormatException e) {
            logger.error("Unable to parse message size from message: {}", message);
            throw new UnsupportedOperationException(e);
        }
        if (length != message.length()) {
            logger.debug("The received message length did not match the size in the header!");
            throw new UnsupportedOperationException();
        }
        int calculatedChecksum = calculateChecksum(message.substring(0, message.length() - 2));
        int foundChecksum = getChecksum(message);
        if (foundChecksum != calculatedChecksum) {
            logger.debug("The checksum of the message did not match. calculated: {} found: {}", calculatedChecksum,
                    foundChecksum);
            throw new UnsupportedOperationException();
        }

        this.data = message.substring(4, message.length() - 4);
        String cmd = message.substring(2, 4);

        this.messageType = ReceivedMessageType.getMessageType(cmd);
    }

    public int getChecksum(String message) {
        return Integer.parseInt(message.substring(message.length() - 2, message.length()), 16);
    }

    public int calculateChecksum(String message) {
        byte checksum = 0;
        for (int i = 0; i < message.length(); ++i) {
            checksum += message.charAt(i);
        }
        return -checksum & 0xff;
    }

    public String getMessage() {
        return this.message;
    }

    public String getData() {
        return this.data;
    }

    public MessageType messageType() {
        return messageType;
    }

    public String getDescription() {
        return this.messageType.getDescription();
    }

    private interface MessageType {
        public String getDescription();
    }

    public enum ReceivedMessageType implements MessageType {
        COMMUNICATIONS_OFF("XF", "Communications Off"),
        COMMUNICATIONS_ON("XN", "Communications On"),
        ACK("FV", "ACK"),
        READY_FOR_NEXT("OK", "Ready for Next"),
        SYSTEM_EVENT("nq", "System Event"),
        EVENT_LOG_ENTRY("ld", "Event Log Entry"),
        EVENT_LOG_COMPLETE("lc", "Event Log Complete"),
        ARMING_STATUS_REPORT("AS", "Arming Status Report"),
        ZONE_STATUS_REPORT("ZS", "Zone Status Report"),
        ZONE_PARTITION_REPORT("ZP", "Zone Partion Report"),
        ZONE_DESCRIPTOR_REPORT("zd", "Zone Descriptor Report"),
        KEYPAD_DISPLAY_RESPONSE("kd", "Keypad Display Response");

        private String cmd;
        private String description;

        static Map<String, ReceivedMessageType> messageTypeLookup;

        static {
            messageTypeLookup = new HashMap<>();
            for (ReceivedMessageType r : values()) {
                messageTypeLookup.put(r.cmd, r);
            }
        }

        public static ReceivedMessageType getMessageType(String type) {
            return messageTypeLookup.get(type);
        }

        private ReceivedMessageType(String cmd, String description) {
            this.cmd = cmd;
            this.description = description;
        }

        @Override
        public String getDescription() {
            return description;
        }
    }

    public enum SendMessageType implements MessageType {
        DISARM("AD00", "Arm Away", 16, true),
        ARM_AWAY("AA00", "Arm Away", 16, true),
        ARM_STAY("AH00", "Arm Home (Stay)", 16, true),
        ARM_INSTANT("AI00", "Arm Instant", 16, true),
        ARM_MAX("AM00", "Arm Max", 16, true),
        ARM_FORCE_AWAY("FA00", "Force Arm Away", 16, true),
        ARM_FORCE_STAY("FH00", "Force Arm Home (Stay)", 16, true),
        ARMING_STATUS_REQUEST("AS", "Arming Status Request", 8, false),
        ZONE_STATUS_REQUEST("ZS", "Zone Status Request", 8, false),
        ZONE_PARTITION_REQUEST("ZP", "Zone Partition Request", 8, false),
        ZONE_DESCRIPTOR_REQUEST("ZD", "Zone Descriptor Request", 8, false),
        EVENT_LOG_DUMP_REQUEST("LD", "Event Log Dump Request", 8, false),
        KEYPAD_DISPLAY_REQUEST("KD", "Keypad Display Request", 9, true),
        KEYSTROKE_COMMAND("KS", "Keystroke Command", -1, true);

        private String cmd;
        private String description;
        static Map<String, SendMessageType> messageTypeLookup;
        private boolean needParams = false;
        private int requestSize;

        static {
            messageTypeLookup = new HashMap<>();
            for (SendMessageType r : values()) {
                messageTypeLookup.put(r.cmd, r);
            }
        }

        public boolean needParams() {
            return needParams;
        }

        public static SendMessageType getMessageType(String type) {
            return messageTypeLookup.get(type);
        }

        private SendMessageType(String cmd, String description, int requestSize, boolean needParams) {
            this.cmd = cmd;
            this.description = description;
            this.needParams = needParams;
            this.requestSize = requestSize;
        }

        @Override
        public String getDescription() {
            return description;
        }

        public int requestSize() {
            return this.requestSize;
        }
    }
}
