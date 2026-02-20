package com.carview.flink.function;

import com.carview.common.model.RealtimeAgg;
import com.carview.common.model.VehicleTelemetry;
import org.apache.flink.streaming.api.functions.windowing.ProcessAllWindowFunction;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.Set;

/**
 * 实时聚合：10秒窗口统计
 */
public class RealtimeAggregator extends ProcessAllWindowFunction<VehicleTelemetry, RealtimeAgg, TimeWindow> {

    @Override
    public void process(ProcessAllWindowFunction<VehicleTelemetry, RealtimeAgg, TimeWindow>.Context context,
                        Iterable<VehicleTelemetry> elements,
                        Collector<RealtimeAgg> out) {

        Set<String> onlineVehicles = new HashSet<>();
        double totalSpeed = 0;
        double totalMileageIncrement = 0;
        double totalFuel = 0;
        int count = 0;

        for (VehicleTelemetry t : elements) {
            onlineVehicles.add(t.getVehicleId());
            if (t.getSpeed() != null) {
                totalSpeed += t.getSpeed();
            }
            if (t.getFuelConsumption() != null) {
                totalFuel += t.getFuelConsumption();
            }
            // 按1秒间隔估算里程增量 (speed km/h / 3600 = km/s)
            if (t.getSpeed() != null) {
                totalMileageIncrement += t.getSpeed() / 3600.0;
            }
            count++;
        }

        TimeWindow window = context.window();
        RealtimeAgg agg = RealtimeAgg.builder()
                .windowStart(LocalDateTime.ofInstant(Instant.ofEpochMilli(window.getStart()), ZoneOffset.UTC))
                .windowEnd(LocalDateTime.ofInstant(Instant.ofEpochMilli(window.getEnd()), ZoneOffset.UTC))
                .onlineCount(onlineVehicles.size())
                .avgSpeed(count > 0 ? totalSpeed / count : 0.0)
                .totalMileageIncrement(totalMileageIncrement)
                .avgFuelConsumption(count > 0 ? totalFuel / count : 0.0)
                .alarmCount(0)
                .build();

        out.collect(agg);
    }
}
