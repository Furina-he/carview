package com.carview.flink.sink;

import com.carview.common.model.RealtimeAgg;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;

/**
 * 实时聚合结果写入 MySQL
 */
public class MysqlAggSink extends RichSinkFunction<RealtimeAgg> {

    private static final Logger log = LoggerFactory.getLogger(MysqlAggSink.class);
    private final String url;
    private final String user;
    private final String password;
    private Connection connection;
    private PreparedStatement ps;

    private static final String INSERT_SQL =
            "INSERT INTO realtime_agg_10s (window_start, window_end, online_count, " +
            "avg_speed, total_mileage_increment, avg_fuel_consumption, alarm_count) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?)";

    public MysqlAggSink(String url, String user, String password) {
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
    public void invoke(RealtimeAgg agg, Context context) {
        try {
            ps.setTimestamp(1, Timestamp.valueOf(agg.getWindowStart()));
            ps.setTimestamp(2, Timestamp.valueOf(agg.getWindowEnd()));
            ps.setInt(3, agg.getOnlineCount() != null ? agg.getOnlineCount() : 0);
            ps.setDouble(4, agg.getAvgSpeed() != null ? agg.getAvgSpeed() : 0);
            ps.setDouble(5, agg.getTotalMileageIncrement() != null ? agg.getTotalMileageIncrement() : 0);
            ps.setDouble(6, agg.getAvgFuelConsumption() != null ? agg.getAvgFuelConsumption() : 0);
            ps.setInt(7, agg.getAlarmCount() != null ? agg.getAlarmCount() : 0);
            ps.executeUpdate();
        } catch (Exception e) {
            log.error("写入聚合结果失败", e);
        }
    }

    @Override
    public void close() throws Exception {
        if (ps != null) ps.close();
        if (connection != null) connection.close();
    }
}
