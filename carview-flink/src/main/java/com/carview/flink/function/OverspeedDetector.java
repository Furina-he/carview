package com.carview.flink.function;

import com.carview.common.model.AlarmEvent;
import com.carview.common.model.VehicleTelemetry;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.apache.flink.util.Collector;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * 超速检测：speed > 120km/h 生成 CRITICAL 告警
 */
public class OverspeedDetector extends ProcessFunction<VehicleTelemetry, AlarmEvent> {

    private static final double SPEED_THRESHOLD = 120.0;

    @Override
    public void processElement(VehicleTelemetry telemetry,
                               ProcessFunction<VehicleTelemetry, AlarmEvent>.Context ctx,
                               Collector<AlarmEvent> out) {
        if (telemetry.getSpeed() != null && telemetry.getSpeed() > SPEED_THRESHOLD) {
            AlarmEvent alarm = AlarmEvent.builder()
                    .vehicleId(telemetry.getVehicleId())
                    .alarmType("OVERSPEED")
                    .alarmLevel(1) // CRITICAL
                    .alarmStatus(1) // ACTIVE
                    .alarmMessage(String.format("车辆 %s 超速行驶，当前速度 %.1f km/h，超过限速 %.0f km/h",
                            telemetry.getVehicleId(), telemetry.getSpeed(), SPEED_THRESHOLD))
                    .alarmValue(String.format("%.1f", telemetry.getSpeed()))
                    .lng(telemetry.getLng())
                    .lat(telemetry.getLat())
                    .eventTime(LocalDateTime.ofInstant(
                            Instant.ofEpochMilli(telemetry.getTimestamp()), ZoneOffset.UTC))
                    .build();
            out.collect(alarm);
        }
    }
}
