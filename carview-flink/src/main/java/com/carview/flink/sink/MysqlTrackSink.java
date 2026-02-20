package com.carview.flink.sink;

import com.carview.common.model.VehicleTelemetry;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * 轨迹数据写入 MySQL
 */
public class MysqlTrackSink extends RichSinkFunction<VehicleTelemetry> {

    private static final Logger log = LoggerFactory.getLogger(MysqlTrackSink.class);
    private final String url;
    private final String user;
    private final String password;
    private Connection connection;
    private PreparedStatement ps;

    private static final String INSERT_SQL =
            "INSERT INTO vehicle_track (vehicle_id, event_time, lng, lat, speed, rpm, " +
            "fuel_consumption, mileage, engine_temp, battery_voltage, fault_code, driver_behavior) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    public MysqlTrackSink(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
    }

    @Override
    public void open(Configuration parameters) throws Exception {
        connection = DriverManager.getConnection(url, user, password);
        ps = connection.prepareStatement(INSERT_SQL);
    }

    @Override
    public void invoke(VehicleTelemetry t, Context context) {
        try {
            ps.setString(1, t.getVehicleId());
            ps.setTimestamp(2, Timestamp.valueOf(
                    LocalDateTime.ofInstant(Instant.ofEpochMilli(t.getTimestamp()), ZoneOffset.UTC)));
            ps.setDouble(3, t.getLng() != null ? t.getLng() : 0);
            ps.setDouble(4, t.getLat() != null ? t.getLat() : 0);
            ps.setDouble(5, t.getSpeed() != null ? t.getSpeed() : 0);
            ps.setInt(6, t.getRpm() != null ? t.getRpm() : 0);
            ps.setDouble(7, t.getFuelConsumption() != null ? t.getFuelConsumption() : 0);
            ps.setDouble(8, t.getMileage() != null ? t.getMileage() : 0);
            ps.setInt(9, t.getEngineTemp() != null ? t.getEngineTemp() : 0);
            ps.setDouble(10, t.getBatteryVoltage() != null ? t.getBatteryVoltage() : 0);
            ps.setString(11, t.getFaultCode());
            ps.setString(12, t.getDriverBehavior());
            ps.executeUpdate();
        } catch (Exception e) {
            log.error("写入轨迹数据失败: {}", t.getVehicleId(), e);
        }
    }

    @Override
    public void close() throws Exception {
        if (ps != null) ps.close();
        if (connection != null) connection.close();
    }
}
