package com.carview.flink.sink;

import com.carview.common.model.AlarmEvent;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;

/**
 * 告警事件写入 MySQL
 */
public class MysqlAlarmSink extends RichSinkFunction<AlarmEvent> {

    private static final Logger log = LoggerFactory.getLogger(MysqlAlarmSink.class);
    private final String url;
    private final String user;
    private final String password;
    private Connection connection;
    private PreparedStatement ps;

    private static final String INSERT_SQL =
            "INSERT INTO alarm_event (vehicle_id, alarm_type, alarm_level, alarm_status, " +
            "alarm_message, alarm_value, lng, lat, event_time) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

    public MysqlAlarmSink(String url, String user, String password) {
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
    public void invoke(AlarmEvent alarm, Context context) {
        try {
            ps.setString(1, alarm.getVehicleId());
            ps.setString(2, alarm.getAlarmType());
            ps.setInt(3, alarm.getAlarmLevel());
            ps.setInt(4, alarm.getAlarmStatus());
            ps.setString(5, alarm.getAlarmMessage());
            ps.setString(6, alarm.getAlarmValue());
            ps.setDouble(7, alarm.getLng() != null ? alarm.getLng() : 0);
            ps.setDouble(8, alarm.getLat() != null ? alarm.getLat() : 0);
            ps.setTimestamp(9, Timestamp.valueOf(alarm.getEventTime()));
            ps.executeUpdate();
        } catch (Exception e) {
            log.error("写入告警失败: {}", alarm.getVehicleId(), e);
        }
    }

    @Override
    public void close() throws Exception {
        if (ps != null) ps.close();
        if (connection != null) connection.close();
    }
}
