package com.carview.server.service;

import com.carview.server.mapper.DashboardMapper;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class DashboardService {

    private final DashboardMapper dashboardMapper;

    public DashboardService(DashboardMapper dashboardMapper) {
        this.dashboardMapper = dashboardMapper;
    }

    public Map<String, Object> getMonitorData() {
        Map<String, Object> result = new HashMap<>();
        result.put("totalVehicles", dashboardMapper.getTotalVehicleCount());
        result.put("onlineVehicles", dashboardMapper.getOnlineVehicleCount());
        result.put("drivingVehicles", dashboardMapper.getDrivingVehicleCount());
        result.put("todayAlarms", dashboardMapper.getTodayAlarmCount());
        result.put("drivingSummary", dashboardMapper.getDrivingSummary());
        result.put("brandDistribution", dashboardMapper.getVehicleBrandDistribution());
        result.put("monthlyActiveVehicles", dashboardMapper.getMonthlyActiveVehicles());
        result.put("monthlyMileageTrend", dashboardMapper.getMonthlyMileageTrend());
        result.put("alarmTypeStats", dashboardMapper.getAlarmTypeStats());
        result.put("speedDistribution", dashboardMapper.getSpeedDistribution());
        result.put("vehiclePositions", dashboardMapper.getVehiclePositions());
        return result;
    }

    public Map<String, Object> getFaultData() {
        Map<String, Object> result = new HashMap<>();
        result.put("alarmSummary", dashboardMapper.getAlarmSummary());
        result.put("faultVehicleSummary", dashboardMapper.getFaultVehicleSummary());
        result.put("alarmLevelDistribution", dashboardMapper.getAlarmLevelDistribution());
        result.put("dailyAlarmTrend", dashboardMapper.getDailyAlarmTrend());
        result.put("faultTypeRanking", dashboardMapper.getFaultTypeRanking());
        result.put("recentFaultVehicles", dashboardMapper.getRecentFaultVehicles());
        result.put("faultByBrand", dashboardMapper.getFaultByBrand());
        result.put("vehiclePositions", dashboardMapper.getVehiclePositions());
        return result;
    }
}
