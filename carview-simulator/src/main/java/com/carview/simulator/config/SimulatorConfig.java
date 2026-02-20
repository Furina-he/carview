package com.carview.simulator.config;

import java.util.ArrayList;
import java.util.List;

/**
 * 模拟器配置
 */
public class SimulatorConfig {

    private final List<String> vehicleIds;
    private final String bootstrapServers;
    private final long intervalMs;

    public SimulatorConfig() {
        this.bootstrapServers = System.getProperty("kafka.bootstrap.servers", "localhost:9092");
        this.intervalMs = Long.parseLong(System.getProperty("simulator.interval.ms", "1000"));
        int vehicleCount = Integer.parseInt(System.getProperty("simulator.vehicle.count", "30"));

        this.vehicleIds = new ArrayList<>();
        String[] prefixes = {"京A", "京B", "京C", "京D", "京E", "京F"};
        int[] starts = {12345, 23456, 34567, 45678, 56789};

        for (int i = 0; i < vehicleCount && i < 30; i++) {
            String prefix = prefixes[i / 5];
            int num = starts[i % 5];
            vehicleIds.add(prefix + num);
        }
    }

    public List<String> getVehicleIds() {
        return vehicleIds;
    }

    public String getBootstrapServers() {
        return bootstrapServers;
    }

    public long getIntervalMs() {
        return intervalMs;
    }
}
