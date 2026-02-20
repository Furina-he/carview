package com.carview.common.enums;

/**
 * 告警状态枚举
 */
public enum AlarmStatus {

    ACTIVE(1, "活跃"),
    ACKNOWLEDGED(2, "已确认"),
    RESOLVED(3, "已解决");

    private final int code;
    private final String description;

    AlarmStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}
