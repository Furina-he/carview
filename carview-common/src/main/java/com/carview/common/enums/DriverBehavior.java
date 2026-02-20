package com.carview.common.enums;

/**
 * 驾驶行为枚举
 */
public enum DriverBehavior {

    NORMAL("正常驾驶"),
    RAPID_ACCEL("急加速"),
    HARD_BRAKE("急刹车"),
    SHARP_TURN("急转弯");

    private final String description;

    DriverBehavior(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
