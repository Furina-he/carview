package com.carview.common.enums;

/**
 * 告警级别枚举
 */
public enum AlarmLevel {

    CRITICAL(1, "严重"),
    WARNING(2, "警告"),
    INFO(3, "提示");

    private final int code;
    private final String description;

    AlarmLevel(int code, String description) {
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
