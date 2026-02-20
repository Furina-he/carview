package com.carview.spark.job;

import com.carview.spark.io.JdbcWriter;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

import static org.apache.spark.sql.functions.*;

/**
 * 每日里程统计
 */
public class DailyMileageJob {

    private final SparkSession spark;
    private final JdbcWriter writer;

    public DailyMileageJob(SparkSession spark, String url, String user, String password) {
        this.spark = spark;
        this.writer = new JdbcWriter(url, user, password);
    }

    public void run() {
        // 从 MySQL vehicle_track 表读取数据
        Dataset<Row> tracks = writer.read(spark, "vehicle_track");

        Dataset<Row> dailyStats = tracks
                .withColumn("stat_date", to_date(col("event_time")))
                .groupBy("vehicle_id", "stat_date")
                .agg(
                        max("mileage").minus(min("mileage")).alias("daily_mileage"),
                        avg("fuel_consumption").alias("daily_fuel"),
                        avg("speed").alias("avg_speed"),
                        max("speed").alias("max_speed"),
                        count("*").alias("driving_duration")
                )
                .withColumn("alarm_count", lit(0))
                .withColumn("created_at", current_timestamp());

        writer.save(dailyStats.select(
                "vehicle_id", "stat_date", "daily_mileage", "daily_fuel",
                "avg_speed", "max_speed", "driving_duration", "alarm_count", "created_at"
        ), "offline_daily_stats");
    }
}
