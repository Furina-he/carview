package com.carview.server.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface TrackMapper {

    @Select("SELECT vehicle_id AS vehicleId, event_time AS eventTime, lng, lat, speed, " +
            "rpm, fuel_consumption AS fuelConsumption, mileage, engine_temp AS engineTemp, " +
            "battery_voltage AS batteryVoltage, fault_code AS faultCode, driver_behavior AS driverBehavior " +
            "FROM vehicle_track " +
            "WHERE vehicle_id = #{vehicleId} " +
            "AND event_time BETWEEN #{startTime} AND #{endTime} " +
            "ORDER BY event_time ASC")
    List<Map<String, Object>> queryTrack(@Param("vehicleId") String vehicleId,
                                         @Param("startTime") String startTime,
                                         @Param("endTime") String endTime);
}
