package com.carview.flink.function;

import com.carview.common.model.AlarmEvent;
import com.carview.common.model.FenceRule;
import com.carview.common.model.VehicleTelemetry;
import com.carview.common.util.GeoUtils;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.apache.flink.util.Collector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

/**
 * 电子围栏检测：判断 GPS 是否在围栏外，越界则告警
 */
public class FenceChecker extends ProcessFunction<VehicleTelemetry, AlarmEvent> {

    private static final Logger log = LoggerFactory.getLogger(FenceChecker.class);
    private final String mysqlUrl;
    private final String mysqlUser;
    private final String mysqlPassword;
    private List<FenceRule> fenceRules;
    private long lastRefreshTime;
    private static final long REFRESH_INTERVAL = 60000; // 每分钟刷新围栏规则

    public FenceChecker(String mysqlUrl, String mysqlUser, String mysqlPassword) {
        this.mysqlUrl = mysqlUrl;
        this.mysqlUser = mysqlUser;
        this.mysqlPassword = mysqlPassword;
    }

    @Override
    public void open(Configuration parameters) {
        fenceRules = new ArrayList<>();
        loadFenceRules();
    }

    private void loadFenceRules() {
        fenceRules.clear();
        try (Connection conn = DriverManager.getConnection(mysqlUrl, mysqlUser, mysqlPassword);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM fence_rule WHERE enabled = 1")) {

            while (rs.next()) {
                FenceRule rule = FenceRule.builder()
                        .id(rs.getLong("id"))
                        .fenceName(rs.getString("fence_name"))
                        .fenceType(rs.getString("fence_type"))
                        .minLng(rs.getObject("min_lng") != null ? rs.getDouble("min_lng") : null)
                        .minLat(rs.getObject("min_lat") != null ? rs.getDouble("min_lat") : null)
                        .maxLng(rs.getObject("max_lng") != null ? rs.getDouble("max_lng") : null)
                        .maxLat(rs.getObject("max_lat") != null ? rs.getDouble("max_lat") : null)
                        .centerLng(rs.getObject("center_lng") != null ? rs.getDouble("center_lng") : null)
                        .centerLat(rs.getObject("center_lat") != null ? rs.getDouble("center_lat") : null)
                        .radius(rs.getObject("radius") != null ? rs.getDouble("radius") : null)
                        .build();
                fenceRules.add(rule);
            }
            lastRefreshTime = System.currentTimeMillis();
            log.info("已加载 {} 条围栏规则", fenceRules.size());
        } catch (Exception e) {
            log.error("加载围栏规则失败", e);
        }
    }

    @Override
    public void processElement(VehicleTelemetry telemetry,
                               ProcessFunction<VehicleTelemetry, AlarmEvent>.Context ctx,
                               Collector<AlarmEvent> out) {
        // 定期刷新围栏规则
        if (System.currentTimeMillis() - lastRefreshTime > REFRESH_INTERVAL) {
            loadFenceRules();
        }

        if (telemetry.getLng() == null || telemetry.getLat() == null) {
            return;
        }

        for (FenceRule rule : fenceRules) {
            boolean inside;
            if ("RECTANGLE".equals(rule.getFenceType())) {
                inside = GeoUtils.isInRectangle(telemetry.getLng(), telemetry.getLat(),
                        rule.getMinLng(), rule.getMinLat(), rule.getMaxLng(), rule.getMaxLat());
            } else if ("CIRCLE".equals(rule.getFenceType())) {
                inside = GeoUtils.isInCircle(telemetry.getLng(), telemetry.getLat(),
                        rule.getCenterLng(), rule.getCenterLat(), rule.getRadius());
            } else {
                continue;
            }

            if (!inside) {
                AlarmEvent alarm = AlarmEvent.builder()
                        .vehicleId(telemetry.getVehicleId())
                        .alarmType("FENCE_OUT")
                        .alarmLevel(2) // WARNING
                        .alarmStatus(1) // ACTIVE
                        .alarmMessage(String.format("车辆 %s 驶出围栏 [%s]",
                                telemetry.getVehicleId(), rule.getFenceName()))
                        .alarmValue(rule.getFenceName())
                        .lng(telemetry.getLng())
                        .lat(telemetry.getLat())
                        .eventTime(LocalDateTime.ofInstant(
                                Instant.ofEpochMilli(telemetry.getTimestamp()), ZoneOffset.UTC))
                        .build();
                out.collect(alarm);
            }
        }
    }
}
