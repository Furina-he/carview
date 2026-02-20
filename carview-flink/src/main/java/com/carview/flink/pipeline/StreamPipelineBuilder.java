package com.carview.flink.pipeline;

import com.carview.common.constants.KafkaConstants;
import com.carview.common.model.VehicleTelemetry;
import com.carview.flink.function.FaultDetector;
import com.carview.flink.function.FenceChecker;
import com.carview.flink.function.OverspeedDetector;
import com.carview.flink.function.RealtimeAggregator;
import com.carview.flink.schema.TelemetryDeserializationSchema;
import com.carview.flink.sink.MysqlAggSink;
import com.carview.flink.sink.MysqlAlarmSink;
import com.carview.flink.sink.MysqlTrackSink;
import com.carview.common.model.AlarmEvent;
import com.carview.common.model.RealtimeAgg;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.assigners.TumblingProcessingTimeWindows;

import java.time.Duration;

/**
 * Flink 流处理 Pipeline 构建器
 */
public class StreamPipelineBuilder {

    private final StreamExecutionEnvironment env;
    private final String kafkaBootstrapServers;
    private final String mysqlUrl;
    private final String mysqlUser;
    private final String mysqlPassword;

    public StreamPipelineBuilder(StreamExecutionEnvironment env,
                                  String kafkaBootstrapServers,
                                  String mysqlUrl,
                                  String mysqlUser,
                                  String mysqlPassword) {
        this.env = env;
        this.kafkaBootstrapServers = kafkaBootstrapServers;
        this.mysqlUrl = mysqlUrl;
        this.mysqlUser = mysqlUser;
        this.mysqlPassword = mysqlPassword;
    }

    public void build() {
        // 1. Kafka Source
        KafkaSource<VehicleTelemetry> kafkaSource = KafkaSource.<VehicleTelemetry>builder()
                .setBootstrapServers(kafkaBootstrapServers)
                .setTopics(KafkaConstants.TOPIC_VEHICLE_DATA)
                .setGroupId(KafkaConstants.GROUP_FLINK_REALTIME)
                .setStartingOffsets(OffsetsInitializer.latest())
                .setValueOnlyDeserializer(new TelemetryDeserializationSchema())
                .build();

        DataStream<VehicleTelemetry> sourceStream = env.fromSource(
                kafkaSource, WatermarkStrategy.noWatermarks(), "Kafka Vehicle Data");

        // 2. 轨迹数据写入 MySQL
        sourceStream.addSink(new MysqlTrackSink(mysqlUrl, mysqlUser, mysqlPassword))
                .name("MySQL Track Sink");

        // 3. 超速检测
        DataStream<AlarmEvent> overspeedAlarms = sourceStream
                .process(new OverspeedDetector())
                .name("Overspeed Detector");

        // 4. 围栏越界检测
        DataStream<AlarmEvent> fenceAlarms = sourceStream
                .process(new FenceChecker(mysqlUrl, mysqlUser, mysqlPassword))
                .name("Fence Checker");

        // 5. 故障检测
        DataStream<AlarmEvent> faultAlarms = sourceStream
                .process(new FaultDetector())
                .name("Fault Detector");

        // 6. 合并所有告警并写入 MySQL
        overspeedAlarms.union(fenceAlarms, faultAlarms)
                .addSink(new MysqlAlarmSink(mysqlUrl, mysqlUser, mysqlPassword))
                .name("MySQL Alarm Sink");

        // 7. 实时聚合（10秒窗口）
        sourceStream
                .windowAll(TumblingProcessingTimeWindows.of(Duration.ofSeconds(10)))
                .process(new RealtimeAggregator())
                .addSink(new MysqlAggSink(mysqlUrl, mysqlUser, mysqlPassword))
                .name("MySQL Agg Sink");
    }
}
