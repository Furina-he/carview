package com.carview.spark.job;

import com.carview.spark.io.JdbcWriter;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

import static org.apache.spark.sql.functions.*;

/**
 * 月度油耗趋势分析
 */
public class MonthlyFuelTrendJob {

    private final SparkSession spark;
    private final JdbcWriter writer;

    public MonthlyFuelTrendJob(SparkSession spark, String url, String user, String password) {
        this.spark = spark;
        this.writer = new JdbcWriter(url, user, password);
    }

    public void run() {
        Dataset<Row> tracks = writer.read(spark, "vehicle_track");

        Dataset<Row> monthlyFuel = tracks
                .withColumn("stat_month", date_format(col("event_time"), "yyyy-MM"))
                .groupBy("vehicle_id", "stat_month")
                .agg(
                        avg("fuel_consumption").alias("avg_fuel_consumption"),
                        sum("fuel_consumption").alias("total_fuel"),
                        max("mileage").minus(min("mileage")).alias("total_mileage")
                )
                .withColumn("created_at", current_timestamp());

        writer.save(monthlyFuel.select(
                "vehicle_id", "stat_month", "avg_fuel_consumption",
                "total_fuel", "total_mileage", "created_at"
        ), "offline_monthly_fuel");
    }
}
