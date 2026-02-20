package com.carview.server.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface DashboardMapper {

    // ========== 车辆监控大屏 ==========

    @Select("SELECT online_status AS status, COUNT(*) AS count FROM vehicle_realtime_state GROUP BY online_status")
    List<Map<String, Object>> getOnlineOfflineCount();

    @Select("SELECT brand AS name, COUNT(*) AS value FROM vehicle_info GROUP BY brand ORDER BY value DESC LIMIT 10")
    List<Map<String, Object>> getVehicleBrandDistribution();

    @Select("SELECT DATE_FORMAT(stat_date, '%Y-%m') AS month, COUNT(DISTINCT vehicle_id) AS count FROM offline_daily_stats GROUP BY month ORDER BY month DESC LIMIT 12")
    List<Map<String, Object>> getMonthlyActiveVehicles();

    @Select("SELECT DATE_FORMAT(stat_date, '%Y-%m') AS month, ROUND(SUM(daily_mileage), 1) AS totalMileage FROM offline_daily_stats GROUP BY month ORDER BY month DESC LIMIT 12")
    List<Map<String, Object>> getMonthlyMileageTrend();

    @Select("SELECT COALESCE(SUM(driving_duration), 0) AS totalDuration, " +
            "COALESCE(ROUND(SUM(daily_mileage), 0), 0) AS totalMileage, " +
            "COALESCE(ROUND(AVG(daily_mileage), 0), 0) AS avgMileage, " +
            "COALESCE(ROUND(AVG(driving_duration), 0), 0) AS avgDuration " +
            "FROM offline_daily_stats")
    Map<String, Object> getDrivingSummary();

    @Select("SELECT COUNT(*) FROM vehicle_info")
    int getTotalVehicleCount();

    @Select("SELECT COUNT(*) FROM vehicle_realtime_state WHERE online_status = 1")
    int getOnlineVehicleCount();

    @Select("SELECT COUNT(*) FROM vehicle_realtime_state WHERE online_status = 1 AND speed > 0")
    int getDrivingVehicleCount();

    @Select("SELECT alarm_type AS type, COUNT(*) AS count FROM alarm_event " +
            "WHERE event_time >= DATE_SUB(NOW(), INTERVAL 30 DAY) " +
            "GROUP BY alarm_type ORDER BY count DESC")
    List<Map<String, Object>> getAlarmTypeStats();

    @Select("SELECT COUNT(*) FROM alarm_event WHERE DATE(event_time) = CURDATE()")
    int getTodayAlarmCount();

    // ========== 故障诊断大屏 ==========

    @Select("SELECT alarm_level AS level, COUNT(*) AS count FROM alarm_event " +
            "WHERE event_time >= DATE_SUB(NOW(), INTERVAL 30 DAY) GROUP BY alarm_level")
    List<Map<String, Object>> getAlarmLevelDistribution();

    @Select("SELECT DATE(event_time) AS date, COUNT(*) AS count FROM alarm_event " +
            "WHERE event_time >= DATE_SUB(NOW(), INTERVAL 30 DAY) " +
            "GROUP BY DATE(event_time) ORDER BY date")
    List<Map<String, Object>> getDailyAlarmTrend();

    @Select("SELECT " +
            "(SELECT COUNT(*) FROM alarm_event WHERE DATE(event_time) = CURDATE()) AS todayCount, " +
            "(SELECT COUNT(*) FROM alarm_event WHERE DATE_FORMAT(event_time, '%Y-%m') = DATE_FORMAT(NOW(), '%Y-%m')) AS monthCount, " +
            "(SELECT COUNT(*) FROM alarm_event WHERE YEAR(event_time) = YEAR(NOW())) AS yearCount")
    Map<String, Object> getAlarmSummary();

    @Select("SELECT " +
            "(SELECT COUNT(DISTINCT vehicle_id) FROM alarm_event WHERE DATE(event_time) = CURDATE()) AS todayFaultVehicles, " +
            "(SELECT COUNT(DISTINCT vehicle_id) FROM alarm_event WHERE DATE_FORMAT(event_time, '%Y-%m') = DATE_FORMAT(NOW(), '%Y-%m')) AS monthFaultVehicles, " +
            "(SELECT COUNT(DISTINCT vehicle_id) FROM alarm_event WHERE YEAR(event_time) = YEAR(NOW())) AS yearFaultVehicles")
    Map<String, Object> getFaultVehicleSummary();

    @Select("SELECT alarm_type AS faultName, COUNT(*) AS faultCount FROM alarm_event " +
            "WHERE event_time >= DATE_SUB(NOW(), INTERVAL 30 DAY) " +
            "GROUP BY alarm_type ORDER BY faultCount DESC LIMIT 10")
    List<Map<String, Object>> getFaultTypeRanking();

    @Select("SELECT a.vehicle_id AS vehicleId, v.vin, v.model AS vehicleModel, " +
            "a.event_time AS eventTime, a.alarm_type AS faultName, " +
            "a.alarm_level AS level, a.alarm_message AS location " +
            "FROM alarm_event a LEFT JOIN vehicle_info v ON a.vehicle_id = v.vehicle_id " +
            "ORDER BY a.event_time DESC LIMIT 10")
    List<Map<String, Object>> getRecentFaultVehicles();

    @Select("SELECT v.brand, COUNT(*) AS count FROM alarm_event a " +
            "LEFT JOIN vehicle_info v ON a.vehicle_id = v.vehicle_id " +
            "WHERE a.event_time >= DATE_SUB(NOW(), INTERVAL 30 DAY) " +
            "GROUP BY v.brand ORDER BY count DESC LIMIT 8")
    List<Map<String, Object>> getFaultByBrand();

    @Select("SELECT " +
            "SUM(CASE WHEN speed BETWEEN 0 AND 30 THEN 1 ELSE 0 END) AS 'range_0_30', " +
            "SUM(CASE WHEN speed BETWEEN 31 AND 60 THEN 1 ELSE 0 END) AS 'range_31_60', " +
            "SUM(CASE WHEN speed BETWEEN 61 AND 90 THEN 1 ELSE 0 END) AS 'range_61_90', " +
            "SUM(CASE WHEN speed BETWEEN 91 AND 120 THEN 1 ELSE 0 END) AS 'range_91_120', " +
            "SUM(CASE WHEN speed > 120 THEN 1 ELSE 0 END) AS 'range_120_plus' " +
            "FROM vehicle_realtime_state WHERE online_status = 1")
    Map<String, Object> getSpeedDistribution();

    @Select("SELECT vehicle_id AS vehicleId, lng, lat, speed, online_status AS onlineStatus " +
            "FROM vehicle_realtime_state WHERE lng IS NOT NULL AND lat IS NOT NULL")
    List<Map<String, Object>> getVehiclePositions();

    // ========== Kafka 消费者写入方法 ==========

    @Insert("INSERT INTO vehicle_realtime_state (vehicle_id, last_time, lng, lat, speed, rpm, " +
            "fuel_consumption, mileage, engine_temp, battery_voltage, fault_code, driver_behavior, online_status) " +
            "VALUES (#{vehicleId}, #{lastTime}, #{lng}, #{lat}, #{speed}, #{rpm}, " +
            "#{fuelConsumption}, #{mileage}, #{engineTemp}, #{batteryVoltage}, #{faultCode}, #{driverBehavior}, 1) " +
            "ON DUPLICATE KEY UPDATE last_time=#{lastTime}, lng=#{lng}, lat=#{lat}, speed=#{speed}, " +
            "rpm=#{rpm}, fuel_consumption=#{fuelConsumption}, mileage=#{mileage}, engine_temp=#{engineTemp}, " +
            "battery_voltage=#{batteryVoltage}, fault_code=#{faultCode}, driver_behavior=#{driverBehavior}, online_status=1")
    void upsertRealtimeState(@Param("vehicleId") String vehicleId,
                             @Param("lastTime") LocalDateTime lastTime,
                             @Param("lng") double lng,
                             @Param("lat") double lat,
                             @Param("speed") double speed,
                             @Param("rpm") int rpm,
                             @Param("fuelConsumption") double fuelConsumption,
                             @Param("mileage") double mileage,
                             @Param("engineTemp") int engineTemp,
                             @Param("batteryVoltage") double batteryVoltage,
                             @Param("faultCode") String faultCode,
                             @Param("driverBehavior") String driverBehavior);

    @Insert("INSERT INTO vehicle_track (vehicle_id, event_time, lng, lat, speed, rpm, " +
            "fuel_consumption, mileage, engine_temp, battery_voltage, fault_code, driver_behavior) " +
            "VALUES (#{vehicleId}, #{eventTime}, #{lng}, #{lat}, #{speed}, #{rpm}, " +
            "#{fuelConsumption}, #{mileage}, #{engineTemp}, #{batteryVoltage}, #{faultCode}, #{driverBehavior})")
    void insertTrack(@Param("vehicleId") String vehicleId,
                     @Param("eventTime") LocalDateTime eventTime,
                     @Param("lng") double lng,
                     @Param("lat") double lat,
                     @Param("speed") double speed,
                     @Param("rpm") int rpm,
                     @Param("fuelConsumption") double fuelConsumption,
                     @Param("mileage") double mileage,
                     @Param("engineTemp") int engineTemp,
                     @Param("batteryVoltage") double batteryVoltage,
                     @Param("faultCode") String faultCode,
                     @Param("driverBehavior") String driverBehavior);

    @Insert("INSERT INTO alarm_event (vehicle_id, alarm_type, alarm_level, alarm_status, " +
            "alarm_message, alarm_value, lng, lat, event_time) " +
            "VALUES (#{vehicleId}, #{alarmType}, #{alarmLevel}, 1, " +
            "#{alarmMessage}, #{alarmValue}, #{lng}, #{lat}, #{eventTime})")
    void insertAlarm(@Param("vehicleId") String vehicleId,
                     @Param("alarmType") String alarmType,
                     @Param("alarmLevel") int alarmLevel,
                     @Param("alarmMessage") String alarmMessage,
                     @Param("alarmValue") String alarmValue,
                     @Param("lng") Double lng,
                     @Param("lat") Double lat,
                     @Param("eventTime") LocalDateTime eventTime);
}
