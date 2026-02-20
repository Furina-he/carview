package com.carview.spark.job;

import com.carview.spark.io.JdbcWriter;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.expressions.Window;

import static org.apache.spark.sql.functions.*;

/**
 * 故障率排名统计
 */
public class FaultRankingJob {

    private final SparkSession spark;
    private final JdbcWriter writer;

    public FaultRankingJob(SparkSession spark, String url, String user, String password) {
        this.spark = spark;
        this.writer = new JdbcWriter(url, user, password);
    }

    public void run() {
        Dataset<Row> tracks = writer.read(spark, "vehicle_track");

        // 统计每辆车的故障次数
        Dataset<Row> faultStats = tracks
                .filter(col("fault_code").isNotNull())
                .groupBy("vehicle_id")
                .agg(
                        count("*").alias("fault_count"),
                        first("fault_code").alias("common_fault_code")
                );

        // 计算总数据量用于计算故障率
        long totalRecords = tracks.count();

        Dataset<Row> withRate = faultStats
                .withColumn("fault_rate", col("fault_count").divide(lit(totalRecords)).multiply(100))
                .withColumn("stat_period", lit("ALL"))
                .withColumn("ranking", row_number().over(
                        Window.partitionBy("stat_period").orderBy(col("fault_count").desc())))
                .withColumn("created_at", current_timestamp());

        writer.save(withRate.select(
                "vehicle_id", "stat_period", "fault_count", "fault_rate",
                "common_fault_code", "ranking", "created_at"
        ), "offline_fault_rank");
    }
}
