package com.carview.spark.job;

import com.carview.spark.io.JdbcWriter;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

import static org.apache.spark.sql.functions.*;

/**
 * 驾驶行为评分
 */
public class DriverBehaviorScoreJob {

    private final SparkSession spark;
    private final JdbcWriter writer;

    public DriverBehaviorScoreJob(SparkSession spark, String url, String user, String password) {
        this.spark = spark;
        this.writer = new JdbcWriter(url, user, password);
    }

    public void run() {
        Dataset<Row> tracks = writer.read(spark, "vehicle_track");

        // 统计异常驾驶行为频率
        Dataset<Row> behaviorStats = tracks
                .groupBy("vehicle_id")
                .agg(
                        count("*").alias("total_count"),
                        count(when(col("driver_behavior").equalTo("RAPID_ACCEL"), 1)).alias("rapid_accel_count"),
                        count(when(col("driver_behavior").equalTo("HARD_BRAKE"), 1)).alias("hard_brake_count"),
                        count(when(col("driver_behavior").equalTo("SHARP_TURN"), 1)).alias("sharp_turn_count")
                );

        // 计算综合评分（100分制，扣分制）
        Dataset<Row> withScore = behaviorStats
                .withColumn("score",
                        lit(100)
                                .minus(col("rapid_accel_count").divide(col("total_count")).multiply(100))
                                .minus(col("hard_brake_count").divide(col("total_count")).multiply(100))
                                .minus(col("sharp_turn_count").divide(col("total_count")).multiply(100))
                )
                .withColumn("score", when(col("score").lt(0), lit(0)).otherwise(col("score")));

        // 将评分写入日统计表（更新 avg_speed 字段存放评分，简化实现）
        // 实际生产应有独立评分表
        withScore.select("vehicle_id", "score").show();
    }
}
