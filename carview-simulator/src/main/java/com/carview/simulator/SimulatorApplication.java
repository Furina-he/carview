package com.carview.simulator;

import com.carview.simulator.config.SimulatorConfig;
import com.carview.simulator.generator.TelemetryGenerator;
import com.carview.simulator.producer.KafkaTelemetryProducer;
import com.carview.common.model.VehicleTelemetry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 车辆数据模拟器启动类
 */
public class SimulatorApplication {

    private static final Logger log = LoggerFactory.getLogger(SimulatorApplication.class);

    public static void main(String[] args) {
        SimulatorConfig config = new SimulatorConfig();
        TelemetryGenerator generator = new TelemetryGenerator(config.getVehicleIds());
        KafkaTelemetryProducer producer = new KafkaTelemetryProducer(config.getBootstrapServers());

        AtomicLong totalSent = new AtomicLong(0);
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "simulator-sender");
            t.setDaemon(true);
            return t;
        });

        log.info("CarView 数据模拟器启动，车辆数: {}, Kafka: {}, 发送间隔: {}ms",
                config.getVehicleIds().size(), config.getBootstrapServers(), config.getIntervalMs());

        scheduler.scheduleAtFixedRate(() -> {
            try {
                List<VehicleTelemetry> batch = generator.generateBatch();
                for (VehicleTelemetry telemetry : batch) {
                    producer.send(telemetry);
                }
                long count = totalSent.addAndGet(batch.size());
                // 每 10 秒打印一次状态
                if (count % (config.getVehicleIds().size() * 10) < config.getVehicleIds().size()) {
                    log.info("已发送 {} 条数据，本批 {} 条", count, batch.size());
                }
            } catch (Exception e) {
                log.error("发送数据异常", e);
            }
        }, 0, config.getIntervalMs(), TimeUnit.MILLISECONDS);

        // 保持主线程存活
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("模拟器关闭中... 共发送 {} 条数据", totalSent.get());
            scheduler.shutdown();
            producer.close();
        }));

        // 阻塞主线程，防止程序退出
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            log.info("主线程中断，退出");
        }
    }
}
