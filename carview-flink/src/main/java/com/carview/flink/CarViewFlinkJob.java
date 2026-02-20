package com.carview.flink;

import com.carview.flink.pipeline.StreamPipelineBuilder;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CarView Flink 实时流处理任务入口
 */
public class CarViewFlinkJob {

    private static final Logger log = LoggerFactory.getLogger(CarViewFlinkJob.class);

    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(2);
        env.enableCheckpointing(60000);

        String kafkaBootstrapServers = System.getProperty("kafka.bootstrap.servers", "localhost:9092");
        String mysqlUrl = System.getProperty("mysql.url", "jdbc:mysql://localhost:3306/carview?useSSL=false&serverTimezone=UTC");
        String mysqlUser = System.getProperty("mysql.user", "carview");
        String mysqlPassword = System.getProperty("mysql.password", "carview123");

        StreamPipelineBuilder builder = new StreamPipelineBuilder(
                env, kafkaBootstrapServers, mysqlUrl, mysqlUser, mysqlPassword);

        builder.build();

        log.info("CarView Flink Job 启动...");
        env.execute("CarView Realtime Processing");
    }
}
