package org.openhab.binding.honeywellvistaalarm.internal.handler;

import java.util.HashMap;
import java.util.Map;

public enum SystemEvent {
    INVALID(0, "", ""),
    FIRE_ALARM(0x01, "alarm", "Fire Alarm"),
    FIRE_ALARM_RESTORE(0x02, "alarm", "Fire Alarm Restore", 0x01),
    TROUBLE(0x03, "trouble", "Trouble"),
    TROUBLE_RESTORE(0x04, "trouble", "Trouble Restore", 0x03),
    BYPASS(0x05, "bypass", "Bypass"),
    BYPASS_RESTORE(0x06, "bypass", "Bypass Restore", 0x05),
    CLOSE_ARM(0x07, "close", "Close (arm)"),
    OPEN_DISARM(0x08, "open", "Open (disarm)"),
    MAN_TRIGGER_TEST_REPORT(0x0D, "system", "Man. Trigger Test Report"),
    SEND_A_POWERUP_REPORT1(0x0E, "system", "Send a Power-up Report"),
    EXIT_ERROR_BY_USER(0x0F, "alarm", "Exit Error By User"),

    DURESS(0x11, "alarm", "Duress"),
    DURESS_RESTORE(0x12, "alarm", "Duress Restore", 0x11),
    TELCO_LINE_1_TROUBLE(0x13, "trouble", "Telco Line 1 Trouble"),
    TELCO_LINE_1_TROUBLE_RESTORE(0x14, "trouble", "Telco Line 1 Trouble Restore", 0x13),
    BELL_1_DISABLE_BYPASS(0x15, "bypass", "Bell 1 Disable (bypass)"),
    BELL_1_BYPASS_RESTORE(0x16, "bypass", "Bell 1 Bypass Restore", 0x15),
    REMOTE_CLOSE_ARM(0x17, "close", "Remote Close (arm)"),
    REMOTE_OPEN_DISARM(0x18, "open", "Remote Open (disarm)"),
    PAGER_FAILED(0x19, "system", "Pager Failed"),
    PAGER_RESTORE(0x1A, "system", "Pager Restore", 0x19),
    AC_LOSS(0x1B, "system", "AC Loss"),
    AC_RESTORE(0x1C, "system", "AC Restore", 0x1B),
    PERIODIC_TEST_REPORT(0x1D, "system", "Periodic Test Report"),
    EXCEPT_SKD_CHG(0x1E, "system", "Except Skd Chg"),
    EXIT_ERROR_BY_ZONE(0x1F, "alarm", "Exit Error By Zone"),

    SILENT_ALARM(0x21, "alarm", "Silent Alarm"),
    SILENT_ALARM_RESTORE(0x22, "alarm", "Silent Alarm Restore", 0x21),
    TELCO_LINE_2_TROUBLE(0x23, "trouble", "Telco Line 2 Trouble"),
    TELCO_LINE_2_TROUBLE_RESTORE(0x24, "trouble", "Telco Line 2 Trouble Restore", 0x23),
    BELL_2_DISABLE_BYPASS(0x25, "bypass", "Bell 2 Disable (bypass)"),
    BELL_2_BYPASS_RESTORE(0x26, "bypass", "Bell 2 Bypass Restore", 0x25),
    QUICK_ARM_CLOSE(0x27, "close", "Quick Arm (close)"),
    LOW_BATTERY(0x29, "system", "Low Battery"),
    LOW_BATTERY_RESTORE(0x2A, "system", "Low Battery Restore", 0x29),
    ACCESS_DENIAL_GENERAL(0x2B, "open", "Access Denial (general)"),
    AC_LOSS_AT_ACS_MODULE(0x2C, "trouble", "AC Loss at ACS Module"),
    WALK_TEST(0x2D, "system", "Walk Test"),
    ACCESS_SKD_CHG(0x2E, "system", "Access Skd Chg"),
    FIRE_WALK_TEST(0x2F, "system", "Fire Walk Test"),

    AUDIBLE(0x31, "alarm", "Audible"),
    AUDIBLE_RESTORE(0x32, "alarm", "Audible Restore", 0x31),
    EARTH_GROUND_TROUBLE(0x33, "trouble", "Earth Ground Trouble"),
    EARTH_GROUND_TROUBLE_RESTORE(0x34, "trouble", "Earth Ground Trouble Restore"),
    AUX_RELAY_DISABLE_BYPASS(0x35, "bypass", "Aux Relay Disable (bypass)"),
    AUX_RELAY_DISABLE_RESTORE(0x36, "bypass", "Aux Relay Disable Restore", 0x35),
    KEYSWITCH_CLOSE_ARM(0x37, "close", "Keyswitch Close (arm)"),
    KEYSWITCH_OPEN_DISARM(0x38, "open", "Keyswitch Open (disarm)", 0x37),
    BATTERY_CHARGER_FAILED(0x39, "system", "Battery Charger Failed"),
    BATTERY_CHARGER_RESTORE(0x3A, "system", "Battery Charger Restore", 0x39),
    DOOR_PROP_OPEN(0x3B, "trouble", "Door Prop Open"),
    LOW_BATTERY_AT_ACS_MODULE(0x3C, "system", "Low Battery at ACS Module"),
    WALK_TEST_EXIT(0x3D, "system", "Walk Test Exit"),
    SEND_A_POWERUP_REPORT2(0x3E, "system", "Send a Power-up Report"),
    FIRE_WALK_TEST_EXIT(0x3F, "test", "Fire Walk Test Exit"),

    PERIMETER_ALARM(0x41, "alarm", "Perimeter Alarm"),
    PERIMETER_ALARM_RESTORE(0x42, "alarm", "Perimeter Alarm Restore"),
    ZONE_SENSOR_SUPERVISOR_ALARM_TROUBLE(0x43, "trouble", "Zone/Sensor Supervisor Alarm"),
    ZONE_SENSOR_SUPERVISOR_ALARM_TROUBLE_RESTORE(0x44, "trouble", "Zone/Sensor Supervisor Alarm Restore", 0x43),
    DIALER_DISABLE_BYPASS(0x45, "bypass", "Dialer Disable (bypass)"),
    DIALER_BYPASS_RESTORE(0x46, "bypass", "Dialer Bypass Restore", 0x45),
    PARTIAL_ARM_CLOSE(0x47, "close", "Partial Arm (close)"),
    CALLBACK_REQUESTED(0x48, "system", "Callback Requested"),
    DOOR_PROP_OPEN_RESTORE(0x4B, "trouble", "Door Prop Open Restore", 0x3B),
    ACCESS_POINT_BYPASS(0x4C, "bypass", "Access Point Bypass"),
    EVENT_LOG_50PCT_FULL(0x4D, "all", "Event Log 50% Full"),
    PROGRAM_CHANGED(0x4E, "all", "Program Changed"),

    INTERIOR_ALARM(0x51, "alarm", "Interior Alarm"),
    INTERIOR_ALARM_RESTORE(0x52, "alarm", "Interior Alarm Restore", 0x51),
    EXP_MODULE_TAMPER(0x53, "trouble", "Exp. Module Tamper"),
    EXP_MODULE_TAMPER_RESTORE(0x54, "trouble", "Exp. Module Tamper Restore", 0x53),
    VENT_ZONE_BYPASS(0x55, "bypass", "Vent Zone (bypass)"),
    VENT_ZONE_BYPASS_RESTORE(0x56, "bypass", "Vent Zone Bypass Restore", 0x55),
    BATTERY_TEST_FAIL(0x59, "system", "Battery Test Fail"),
    BATTERY_TEST_RESTORE(0x5A, "system", "Battery Test Restore", 0x59),
    ACCESS_GRANTED(0x5B, "system", "Access Granted"),
    RESET_AT_MODULE(0x5C, "all", "Reset At Module"),
    EVENT_LOG_90PCT_FULL(0x5D, "all", "Event Log 90% Full"),
    AUTO_ARM_FAIL(0x5E, "open", "Auto-arm Fail"),
    ALARM_CANCEL(0x5F, "alarm", "Cancel Alarm"),

    EMERGENCY_24HOUR(0x61, "alarm", "24 Hour Zone"),
    EMERGENCY_24HOUR_RESTORE(0x62, "alarm", "24 Hour Restore", 0x61),
    RF_SENSOR_SUPERVISOR(0x63, "trouble", "RF Sensor Supervisor"),
    RF_SENSOR_SUPERVISOR_RESTORE(0x64, "trouble", "RF Sensor Supervisor Restore", 0x63),
    ACS_TEST_ENTRY(0x65, "system", "ACS Test Entry"),
    ACS_TEST_EXIT(0x66, "system", "ACS Test Exit", 0x65),
    AUTO_CLOSE_ARM(0x67, "close", "Auto Close (arm)"),
    AUTO_OPEN_DISARM(0x68, "open", "Auto Open (disarm)", 0x67),
    EXPANDER_MODULE_FAIL_TROUBLE(0x69, "trouble", "Expander Module Fail"),
    EXPANDER_MODULE_FAIL(0x6A, "open", "Expander Module Fail"),
    EGRESS_DENIED_GENERAL(0x6B, "trouble", "Egress Denied (General)"),
    ACCESS_POINT_RELAY_SUPERVISION_FAIL(0x6C, "system", "Access Point Relay Supervision Fail"),
    EVENT_LOG_OVERWRITE(0x6D, "system", "Event Log Overwrite"),
    OFF_NORMAL_REPORT(0x6E, "system", "Off Normal Report"),
    BEGIN_DRILL_FIRE(0x6F, "test", "Begin Drill (Fire)"),

    DAY_NIGHT_ALARM(0x71, "alarm", "Day/Night Alarm"),
    DAY_NIGHT_ALARM_RESTORE(0x72, "alarm", "Day/Night Alarm Restore"),
    RPM_SENSOR_SUPER(0x73, "trouble", "RPM Sensor Super"),
    RPM_SENSOR_SUPER_RESTORE(0x74, "trouble", "RPM Sensor Super Restore"),
    ENGINEER_RESET(0x76, "all", "Engineer Reset"),
    LOG_DIALER_SHUTDOWN_RESTORE(0x77, "system", "Log Dialer Shutdown Restore", 0x78),
    LOG_DIALER_SHUTDOWN(0x78, "system", "Log Dialer Shutdown"),
    SYSTEM_SHUTDOWN(0x79, "all", "System Shutdown"),
    SYSTEM_SHUTDOWN_RESTORE(0x7A, "all", "System Shutdown Restore"),
    DOOR_FORCED_OPEN(0x7B, "alarm", "Door Forced Open"),
    SELF_TEST_FAIL_AT_MODULE(0x7C, "trouble", "Self Test Fail At Module"),
    EVENT_LOG_RESET(0x7D, "all", "Event Log Reset"),
    POINT_TESTED_OK(0x7E, "fire", "Point Tested Ok"),
    END_DRILL_FIRE(0x7F, "test", "End Drill (Fire)", 0x6F),

    ENTRY_EXIT_ALARM(0x81, "alarm", "Entry/Exit Alarm"),
    ENTRY_EXIT_ALARM_RESTORE(0x82, "alarm", "Entry/Exit Alarm Restore", 0x81),
    ECP_RELAY_TROUBLE(0x83, "trouble", "ECP Relay Trouble"),
    ECP_RELAY_TROUBLE_RESTORE(0x84, "trouble", "ECP Relay Trouble Restore", 0x83),
    LOG_SYSTEM_SHUTDOWN_RESTORE(0x87, "system", "Log System Shutdown Restore", 0x88),
    LOG_SYSTEM_SHUTDOWN(0x88, "system", "Log System Shutdown"),
    RF_LOW_BATTERY(0x89, "trouble", "RF Low Battery"),
    RF_LOW_BATTERY_RESTORE(0x8A, "trouble", "RF Low Battery Restore", 0x89),
    DOOR_FORCED_OPEN_RESTORE(0x8B, "alarm", "Door Forced Open Restore", 0x7B),
    ACCESS_POINT_DSM_SHUNT(0x8C, "bypass", "Access Point Dsm Shunt"),
    TIME_CLOCK_RESET(0x8D, "all", "Time Clock Reset"),
    POINT_NOT_TESTED_BURGLARY(0x8E, "system", "Point Not Tested (Burglary)"),
    POINT_NOT_TESTED_FIRE(0x8F, "test", "Point Not Tested (Fire)"),

    POLL_LOOP_SHORT(0x91, "alarm", "Poll Loop Short"),
    POLL_LOOP_SHORT_RESTORE(0x92, "alarm", "Poll Loop Short Restore", 0x91),
    POLLING_LOOP_SHORT(0x93, "trouble", "Polling Loop Short"),
    POLLING_LOOP_SHORT_RESTORE(0x94, "trouble", "Polling Loop Short Restore", 0x93),
    ACS_RELAY_TRIGGER_DISABLE(0x95, "bypass", "ACS Relay/Trigger Disable"),
    ACS_RELAY_TRIGGER_ENABLE(0x96, "bypass", "ACS Relay/Trigger Enable", 0x95),
    ACS_READER_DISABLE(0x97, "bypass", "ACS Reader Disable"),
    ACS_READER_ENABLE(0x98, "bypass", "ACS Reader Enable", 0x97),
    ACS_ZONE_ALARM(0x99, "alarm", "ACS Zone Alarm"),
    ACS_ZONE_ALARM_RESTORE(0x9A, "alarm", "ACS Zone Alarm Restore", 0x99),
    EGRESS_GRANTED(0x9B, "open", "Egress Granted"),
    ACCESS_POINT_DSM_UNSHUNT(0x9C, "bypass", "Access Point Dsm Unshunt", 0x8C),
    TIME_CLOCK_WRONG(0x9D, "all", "Time Clock Wrong"),
    RECENT_CLOSE_BY_USER(0x9E, "alarm", "Recent Close By User"),
    REMOTE_PS_LOSS_RESTORE(0x9F, "trouble", "Remote PS Loss Restore"),

    EXPANDER_MODULE_FAIL_ALARM(0xA1, "alarm", "Expander Module Fail"),
    EXPANDER_MODULE_FAIL_ALARM_RESTORE(0xA2, "alarm", "Expander Module Fail Restore", 0xA1),
    EXPANDER_MODULE_TROUBLE(0xA3, "trouble", "Expander Module Trouble"),
    EXPANDER_MODULE_TROUBLE_RESTORE(0xA4, "trouble", "Expander Module Trouble Restore", 0xA3),
    ACS_ZONE_SHUNT(0xA5, "bypass", "ACS Zone Shunt"),
    ACS_ZONE_SHUNT_RESTORE(0xA6, "bypass", "ACS Zone Shunt Restore", 0xA5),
    ACCESS_POINT_RTE_TROUBLE(0xA7, "trouble", "Access Point RTE Trouble"),
    ACCESS_POINT_RTE_TROUBLE_RESTORE(0xA8, "trouble", "Access Point RTE Trouble Restore", 0xA7),
    ACCESS_POINT_DSM_TROUBLE(0xA9, "trouble", "Access Point DSM Trouble"),
    ACCESS_POINT_DSM_TROUBLE_RESTORE(0xAA, "trouble", "Access Point DSM Trouble Restore", 0xA9),
    ACCESS_POINT_RTE_SHUNT(0xAB, "bypass", "Access Point RTE Shunt"),
    ACCESS_POINT_RTE_SHUNT_RESTORE(0xAC, "bypass", "Access Point RTE Shunt Restore", 0xAB),
    LOG_PROGRAM_MODE_ENTRY(0xAD, "system", "Log Program Mode Entry"),
    LISTEN_IN_TO_FOLLOW(0xAE, "alarm", "Listen-in To Follow"),

    NON_BURGLAR_ALARM(0xB1, "alarm", "Non-burglar Alarm"),
    NON_BURGLAR_ALARM_RESTORE(0xB2, "alarm", "Non-burglar Alarm"),
    SENSOR_TAMPER(0xB3, "trouble", "Sensor Tamper"),
    SENSOR_TAMPER_RESTORE(0xB4, "trouble", "Sensor Tamper Restore", 0xB3),
    CROSS_ZONING_TROUBLE(0xB5, "trouble", "Cross-zoning Trouble"),
    CROSS_ZONING_TROUBLE_RESTORE(0xB6, "trouble", "Cross-zoning Trouble Restore", 0xB5),
    ARM_STAY_CLOSE(0xB7, "close", "Arm Stay (close)"),
    ACS_PROGRAM_ENTRY(0xBB, "all", "ACS Program Entry"),
    AC_LOSS_RESTORED_ACS_MODULE(0xBC, "trouble", "AC Loss Restored ACS Module", 0xBB),
    PROGRAM_MODE_EXITED(0xBD, "system", "Program Mode Exited"),
    POINT_TESTED_FAILED(0xBE, "trouble", "Point Tested Failed"),

    SMOKE_ALARM(0xC1, "alarm", "Smoke Alarm"),
    SMORE_ALARM_RESTORE(0xC2, "alarm", "Smore Alarm Restore", 0xC1),
    FIRE_TROUBLE(0xC3, "trouble", "Fire Trouble"),
    FIRE_TROUBLE_RESTORE(0xC4, "trouble", "Fire Trouble Restore", 0xC3),
    CO_ALARM(0xC5, "trouble", "CO Alarm"),
    CO_ALARM_RESTORE(0xC6, "trouble", "CO Alarm Restore", 0xC5),
    FAIL_TO_CLOSE_ARM(0xC7, "close", "Fail To close (arm)"),
    FAIL_TO_OPEN_DISARM(0xC8, "open", "Fail To Open (disarm)"),
    SMOKE_DETECTOR_HIGH_SENSITIVITY(0xC9, "trouble", "Smoke Detector High Sensitivity"),
    SMOKE_DETECTOR_HIGH_SENSITIVITY_RESTORE(0xCA, "trouble", "Smoke Detector High Sensitivity Restore", 0xC9),
    ACS_PROGRAM_EXIT(0xCB, "all", "ACS Program Exit"),
    LOW_BATTERY_RESTORED_ACS_MODULE(0xCC, "trouble", "Low Battery Restored ACS Module", 0x3C),
    USER_CODE_ADDED(0xCD, "system", "User Code Added"),

    WATER_FLOW_ALARM(0xD1, "alarm", "Water Flow Alarm"),
    WATER_FLOW_ALARM_RESTORE(0xD2, "alarm", "Water Flow Alarm Restore", 0xD1),
    FAIL_TO_COMMUNICATE(0xD3, "all", "Fail To Communicate"),
    COMMUNICATIONS_RESTORE(0xD4, "all", "Communications Restore", 0xD3),
    LATE_CLOSE_ARM(0xD7, "close", "Late Close (arm)"),
    LATE_OPEN(0xD8, "open", "Late Open"),
    SMOKE_DETECTOR_LOW_SENSITIVITY(0xD9, "trouble", "Smoke Detector Low Sensitivity"),
    SMOKE_DETECTOR_LOW_SENSITIVITY_RESTORE(0xDA, "trouble", "Smoke Detector Low Sensitivity Restore", 0xD9),
    ACS_THREAT_CHANGE(0xDB, "all", "ACS Threat Change"),
    ACCESS_POINT_UNBYPASS(0xDC, "bypass", "Access Point Unbypass", 0x4C),
    USER_CODE_DELETED(0xDD, "system", "User Code Deleted"),

    ZONE_SENSOR_SUPERVISOR_ALARM(0xE1, "alarm", "Zone/Sensor Supervisor Alarm"),
    ZONE_SENSOR_SUPERVISOR_ALARM_RESTORE(0xE2, "alarm", "Zone/Sensor Supervisor Alarm Restore", 0xE1),
    BELL_1_TROUBLE(0xE3, "trouble", "Bell 1 Trouble"),
    BELL_1_TROUBLE_RESTORE(0xE4, "trouble", "Bell 1 Trouble Restore", 0xE3),
    EARLY_CLOSE_ARM(0xE7, "close", "Early Close (arm)"),
    EARLY_OPEN(0xE8, "open", "Early Open"),
    INTRUSION_DETECTION_HIGH_SENSITIVITY(0xE9, "trouble", "Intrusion Detection High Sensitivity"),
    INTRUSION_DETECTION_HIGH_SENSITIVITY_RESTORE(0xEA, "trouble", "Intrusion Detection High Sensitivity Restore", 0xE9),
    DURESS_ACCESS_GRANT(0xEB, "alarm", "Duress Access Grant"),
    ACCESS_POINT_RELAY_SUPERVISOR_RESTORE(0xEC, "trouble", "Access Point Relay Supervisor Restore", 0x6C),
    USER_CODE_CHANGED(0xED, "system", "User Code Changed"),

    BELL_2_TROUBLE(0xF3, "trouble", "Bell 2 Trouble"),
    BELL_2_TROUBLE_RESTORE(0xF4, "trouble", "Bell 2 Trouble Restore", 0xF3),
    FAULTS(0xF5, "trouble", "Faults"),
    FAULT_RESTORED(0xF6, "trouble", "Fault Restored", 0xF6),
    DIALER_QUEUE_FULL(0xF8, "trouble", "Dialer Queue Full"),
    PIR_DETECTOR_LO_SENSITIVITY(0xF9, "trouble", "PIR Detector Lo Sensitivity"),
    INTRUSION_DETECTION_LOW_SENSITIVITY(0xFA, "alarm", "Intrusion Detection Low Sensitivity"),
    DURESS_EGRESS_GRANT(0xFB, "trouble", "Duress Egress Grant"),
    SELFTEST_RESTORED_ACS_MODULE(0xFC, "trouble", "Selftest Restored ACS Module", 0xFB),
    FAIL_TO_PRINT(0xFD, "system", "Fail to Print"),
    FAIL_TO_PRINT_RESTORE(0xFE, "system", "Fail to Print Restore", 0xFD);

    private int code;
    private String description;
    private String type;
    private int restoreCode;

    static Map<Integer, SystemEvent> lookup = new HashMap<Integer, SystemEvent>();
    static {
        for (SystemEvent e : values()) {
            lookup.put(e.code, e);
        }
    }

    static SystemEvent lookupEventCode(int code) {
        return lookup.get(code);
    }

    SystemEvent(int code, String type, String description, int restoreCode) {
        this.code = code;
        this.type = type;
        this.description = description;
        this.restoreCode = restoreCode;
    }

    SystemEvent(int code, String type, String description) {
        this(code, type, description, -1);
    }

    public String description() {
        return description;
    }

    public String type() {
        return type;
    }

    public int restoreCode() {
        return restoreCode;
    }
}
