package com.carview.simulator.generator;

import com.carview.common.enums.DriverBehavior;
import com.carview.common.model.VehicleTelemetry;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 车辆遥测数据生成器 — 全国各大城市分布
 */
public class TelemetryGenerator {

    private final List<String> vehicleIds;
    private final Map<String, VehicleState> vehicleStates;

    /** 全国主要城市中心坐标 [经度, 纬度, 漫游范围(经度), 漫游范围(纬度)] */
    private static final double[][] CITY_CENTERS = {
            {116.40, 39.90, 0.25, 0.18},  // 北京
            {121.47, 31.23, 0.22, 0.15},  // 上海
            {113.26, 23.13, 0.20, 0.15},  // 广州
            {114.07, 22.55, 0.18, 0.12},  // 深圳
            {104.07, 30.67, 0.22, 0.16},  // 成都
            {114.30, 30.59, 0.22, 0.15},  // 武汉
            {120.15, 30.28, 0.20, 0.15},  // 杭州
            {118.80, 32.06, 0.20, 0.15},  // 南京
            {108.94, 34.26, 0.20, 0.15},  // 西安
            {106.55, 29.56, 0.22, 0.16},  // 重庆
    };

    private static final String[] FAULT_CODES = {
            "P0300", "P0171", "P0420", "P0442", "P0455",
            "P0128", "P0507", "P0113", "P0102", "P0341"
    };

    public TelemetryGenerator(List<String> vehicleIds) {
        this.vehicleIds = vehicleIds;
        this.vehicleStates = new HashMap<>();
        for (int i = 0; i < vehicleIds.size(); i++) {
            // 按城市均匀分配车辆
            int cityIndex = i % CITY_CENTERS.length;
            vehicleStates.put(vehicleIds.get(i), new VehicleState(cityIndex));
        }
    }

    public List<VehicleTelemetry> generateBatch() {
        List<VehicleTelemetry> batch = new ArrayList<>();
        for (String vehicleId : vehicleIds) {
            batch.add(generateForVehicle(vehicleId));
        }
        return batch;
    }

    public VehicleTelemetry generateForVehicle(String vehicleId) {
        VehicleState state = vehicleStates.get(vehicleId);
        state.update();

        ThreadLocalRandom random = ThreadLocalRandom.current();

        // 故障码：2% 概率触发
        String faultCode = null;
        if (random.nextDouble() < 0.02) {
            faultCode = FAULT_CODES[random.nextInt(FAULT_CODES.length)];
        }

        // 驾驶行为
        String behavior = DriverBehavior.NORMAL.name();
        double behaviorRoll = random.nextDouble();
        if (behaviorRoll < 0.03) {
            behavior = DriverBehavior.RAPID_ACCEL.name();
        } else if (behaviorRoll < 0.06) {
            behavior = DriverBehavior.HARD_BRAKE.name();
        } else if (behaviorRoll < 0.08) {
            behavior = DriverBehavior.SHARP_TURN.name();
        }

        return VehicleTelemetry.builder()
                .vehicleId(vehicleId)
                .vin("LSVAU2A37N2183" + String.format("%03d", vehicleIds.indexOf(vehicleId) + 1))
                .timestamp(System.currentTimeMillis())
                .lng(state.lng)
                .lat(state.lat)
                .speed(state.speed)
                .rpm(state.rpm)
                .fuelConsumption(state.fuelConsumption)
                .mileage(state.mileage)
                .engineTemp(state.engineTemp)
                .batteryVoltage(state.batteryVoltage)
                .faultCode(faultCode)
                .driverBehavior(behavior)
                .build();
    }

    /**
     * 内部类：维护每辆车的当前状态，绑定到某个城市
     */
    private static class VehicleState {
        final double centerLng;
        final double centerLat;
        final double lngRange;
        final double latRange;

        double lng;
        double lat;
        double speed;
        int rpm;
        double fuelConsumption;
        double mileage;
        int engineTemp;
        double batteryVoltage;
        double direction;

        VehicleState(int cityIndex) {
            double[] city = CITY_CENTERS[cityIndex];
            this.centerLng = city[0];
            this.centerLat = city[1];
            this.lngRange = city[2];
            this.latRange = city[3];

            ThreadLocalRandom random = ThreadLocalRandom.current();
            // 在城市范围内随机初始位置
            this.lng = centerLng - lngRange / 2 + random.nextDouble() * lngRange;
            this.lat = centerLat - latRange / 2 + random.nextDouble() * latRange;
            this.speed = 40 + random.nextDouble() * 60;
            this.rpm = 2000 + random.nextInt(1500);
            this.fuelConsumption = 6.0 + random.nextDouble() * 4.0;
            this.mileage = 10000 + random.nextDouble() * 50000;
            this.engineTemp = 85 + random.nextInt(10);
            this.batteryVoltage = 12.2 + random.nextDouble() * 0.8;
            this.direction = random.nextDouble() * 360;
        }

        void update() {
            ThreadLocalRandom random = ThreadLocalRandom.current();

            // 方向微调
            direction += (random.nextDouble() - 0.5) * 30;
            direction = direction % 360;

            // 速度变化
            speed += (random.nextGaussian() * 5);
            if (random.nextDouble() < 0.05) {
                speed = 120 + random.nextDouble() * 30; // 5% 概率超速
            }
            speed = Math.max(0, Math.min(180, speed));

            // 位置更新（基于速度和方向）
            double distanceKm = speed / 3600.0;
            double dLng = distanceKm * Math.cos(Math.toRadians(direction)) / 111.32;
            double dLat = distanceKm * Math.sin(Math.toRadians(direction)) / 110.57;
            lng += dLng;
            lat += dLat;

            // 确保在城市范围内（碰到边界就反弹）
            double minLng = centerLng - lngRange / 2;
            double maxLng = centerLng + lngRange / 2;
            double minLat = centerLat - latRange / 2;
            double maxLat = centerLat + latRange / 2;

            if (lng < minLng || lng > maxLng) {
                direction = 180 - direction;
                lng = Math.max(minLng, Math.min(maxLng, lng));
            }
            if (lat < minLat || lat > maxLat) {
                direction = -direction;
                lat = Math.max(minLat, Math.min(maxLat, lat));
            }

            // 转速与速度关联
            rpm = (int) (speed * 30 + 800 + random.nextGaussian() * 200);
            rpm = Math.max(800, Math.min(6000, rpm));

            // 油耗与速度关联
            fuelConsumption = 5.0 + speed * 0.05 + random.nextGaussian() * 0.5;
            fuelConsumption = Math.max(3.0, Math.min(20.0, fuelConsumption));

            // 里程累加
            mileage += distanceKm;

            // 发动机温度
            engineTemp += (int) (random.nextGaussian() * 2);
            if (random.nextDouble() < 0.01) {
                engineTemp = 110 + random.nextInt(10);
            }
            engineTemp = Math.max(70, Math.min(130, engineTemp));

            // 电池电压
            batteryVoltage += random.nextGaussian() * 0.1;
            if (random.nextDouble() < 0.01) {
                batteryVoltage = 10.5 + random.nextDouble();
            }
            batteryVoltage = Math.max(10.0, Math.min(14.5, batteryVoltage));
        }
    }
}
