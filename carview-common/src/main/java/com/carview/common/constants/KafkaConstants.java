package com.carview.common.constants;

/**
 * Kafka 相关常量
 */
public final class KafkaConstants {

    private KafkaConstants() {}

    /** 车辆遥测数据 Topic */
    public static final String TOPIC_VEHICLE_DATA = "vehicle-data";

    /** 消费者组：Flink 实时处理 */
    public static final String GROUP_FLINK_REALTIME = "carview-flink-realtime";

    /** 消费者组：HDFS 落盘 */
    public static final String GROUP_HDFS_SINK = "carview-hdfs-sink";

    /** 默认 Kafka Broker 地址 */
    public static final String DEFAULT_BOOTSTRAP_SERVERS = "localhost:9092";
}
