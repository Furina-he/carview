package com.carview.flink.function;

import com.carview.common.model.AlarmEvent;
import com.carview.common.model.VehicleTelemetry;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.apache.flink.util.Collector;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * 故障检测：faultCode!=null, engineTemp>110, batteryVoltage<11
 */
public class FaultDetector extends ProcessFunction<VehicleTelemetry, AlarmEvent> {

    @Override
    public void processElement(VehicleTelemetry telemetry,
                               ProcessFunction<VehicleTelemetry, AlarmEvent>.Context ctx,
                               Collector<AlarmEvent> out) {

        LocalDateTime eventTime = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(telemetry.getTimestamp()), ZoneOffset.UTC);

        // 故障码检测
        if (telemetry.getFaultCode() != null && !telemetry.getFaultCode().isEmpty()) {
            out.collect(AlarmEvent.builder()
                    .vehicleId(telemetry.getVehicleId())
                    .alarmType("FAULT")
                    .alarmLevel(1) // CRITICAL
                    .alarmStatus(1)
                    .alarmMessage(String.format("车辆 %s 检测到故障码: %s",
                            telemetry.getVehicleId(), telemetry.getFaultCode()))
                    .alarmValue(telemetry.getFaultCode())
                    .lng(telemetry.getLng())
                    .lat(telemetry.getLat())
                    .eventTime(eventTime)
                    .build());
        }

        // 发动机过热检测
        if (telemetry.getEngineTemp() != null && telemetry.getEngineTemp() > 110) {
            out.collect(AlarmEvent.builder()
                    .vehicleId(telemetry.getVehicleId())
                    .alarmType("ENGINE_OVERHEAT")
                    .alarmLevel(1) // CRITICAL
                    .alarmStatus(1)
                    .alarmMessage(String.format("车辆 %s 发动机过热，温度: %d℃",
                            telemetry.getVehicleId(), telemetry.getEngineTemp()))
                    .alarmValue(String.valueOf(telemetry.getEngineTemp()))
                    .lng(telemetry.getLng())
                    .lat(telemetry.getLat())
                    .eventTime(eventTime)
                    .build());
        }

        // 低电压检测
        if (telemetry.getBatteryVoltage() != null && telemetry.getBatteryVoltage() < 11.0) {
            out.collect(AlarmEvent.builder()
                    .vehicleId(telemetry.getVehicleId())
                    .alarmType("LOW_BATTERY")
                    .alarmLevel(2) // WARNING
                    .alarmStatus(1)
                    .alarmMessage(String.format("车辆 %s 电池电压过低: %.1fV",
                            telemetry.getVehicleId(), telemetry.getBatteryVoltage()))
                    .alarmValue(String.format("%.1f", telemetry.getBatteryVoltage()))
                    .lng(telemetry.getLng())
                    .lat(telemetry.getLat())
                    .eventTime(eventTime)
                    .build());
        }
    }
}
