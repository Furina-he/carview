package com.carview.server.consumer;

import com.carview.common.model.VehicleTelemetry;
import com.carview.server.mapper.DashboardMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * Kafka 消费者：消费 vehicle-data topic，写入 MySQL（替代 Flink 的核心处理逻辑）
 */
@Component
public class TelemetryConsumer {

    private static final Logger log = LoggerFactory.getLogger(TelemetryConsumer.class);

    private final DashboardMapper dashboardMapper;
    private final ObjectMapper objectMapper;

    public TelemetryConsumer(DashboardMapper dashboardMapper, ObjectMapper objectMapper) {
        this.dashboardMapper = dashboardMapper;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "vehicle-data", groupId = "carview-server")
    public void consume(String message) {
        try {
            VehicleTelemetry t = objectMapper.readValue(message, VehicleTelemetry.class);
            LocalDateTime eventTime = LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(t.getTimestamp()), ZoneOffset.UTC);

            double lng = t.getLng() != null ? t.getLng() : 0;
            double lat = t.getLat() != null ? t.getLat() : 0;
            double speed = t.getSpeed() != null ? t.getSpeed() : 0;
            int rpm = t.getRpm() != null ? t.getRpm() : 0;
            double fuelConsumption = t.getFuelConsumption() != null ? t.getFuelConsumption() : 0;
            double mileage = t.getMileage() != null ? t.getMileage() : 0;
            int engineTemp = t.getEngineTemp() != null ? t.getEngineTemp() : 0;
            double batteryVoltage = t.getBatteryVoltage() != null ? t.getBatteryVoltage() : 0;
            String faultCode = t.getFaultCode();
            String driverBehavior = t.getDriverBehavior() != null ? t.getDriverBehavior() : "NORMAL";

            // 1. 更新实时状态（UPSERT）
            dashboardMapper.upsertRealtimeState(
                    t.getVehicleId(), eventTime, lng, lat, speed, rpm,
                    fuelConsumption, mileage, engineTemp, batteryVoltage,
                    faultCode, driverBehavior);

            // 2. 插入轨迹记录
            dashboardMapper.insertTrack(
                    t.getVehicleId(), eventTime, lng, lat, speed, rpm,
                    fuelConsumption, mileage, engineTemp, batteryVoltage,
                    faultCode, driverBehavior);

            // 3. 告警检测
            detectAlarms(t, eventTime);

        } catch (Exception e) {
            log.error("处理遥测数据失败: {}", message, e);
        }
    }

    private void detectAlarms(VehicleTelemetry t, LocalDateTime eventTime) {
        // 超速检测: speed > 120
        if (t.getSpeed() != null && t.getSpeed() > 120) {
            dashboardMapper.insertAlarm(
                    t.getVehicleId(), "OVERSPEED", 1,
                    String.format("车辆 %s 超速行驶，当前速度 %.1f km/h，超过限速 120 km/h",
                            t.getVehicleId(), t.getSpeed()),
                    String.format("%.1f", t.getSpeed()),
                    t.getLng(), t.getLat(), eventTime);
        }

        // 发动机过热: engineTemp > 110
        if (t.getEngineTemp() != null && t.getEngineTemp() > 110) {
            dashboardMapper.insertAlarm(
                    t.getVehicleId(), "ENGINE_OVERHEAT", 1,
                    String.format("车辆 %s 发动机过热，温度: %d℃",
                            t.getVehicleId(), t.getEngineTemp()),
                    String.valueOf(t.getEngineTemp()),
                    t.getLng(), t.getLat(), eventTime);
        }

        // 低电压: batteryVoltage < 11
        if (t.getBatteryVoltage() != null && t.getBatteryVoltage() < 11.0) {
            dashboardMapper.insertAlarm(
                    t.getVehicleId(), "LOW_BATTERY", 2,
                    String.format("车辆 %s 电池电压过低: %.1fV",
                            t.getVehicleId(), t.getBatteryVoltage()),
                    String.format("%.1f", t.getBatteryVoltage()),
                    t.getLng(), t.getLat(), eventTime);
        }

        // 故障码检测: faultCode 非空
        if (t.getFaultCode() != null && !t.getFaultCode().isEmpty()) {
            dashboardMapper.insertAlarm(
                    t.getVehicleId(), "FAULT", 1,
                    String.format("车辆 %s 检测到故障码: %s",
                            t.getVehicleId(), t.getFaultCode()),
                    t.getFaultCode(),
                    t.getLng(), t.getLat(), eventTime);
        }
    }
}
