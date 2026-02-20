package com.carview.spark;

import com.carview.spark.job.DailyMileageJob;
import com.carview.spark.job.DriverBehaviorScoreJob;
import com.carview.spark.job.FaultRankingJob;
import com.carview.spark.job.MonthlyFuelTrendJob;
import org.apache.spark.sql.SparkSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CarView Spark 离线批处理任务入口
 */
public class CarViewSparkBatchJob {

    private static final Logger log = LoggerFactory.getLogger(CarViewSparkBatchJob.class);

    public static void main(String[] args) {
        String mysqlUrl = System.getProperty("mysql.url",
                "jdbc:mysql://localhost:3306/carview?useSSL=false&serverTimezone=UTC");
        String mysqlUser = System.getProperty("mysql.user", "carview");
        String mysqlPassword = System.getProperty("mysql.password", "carview123");
        String hdfsPath = System.getProperty("hdfs.path", "hdfs://hadoop-namenode:9000/carview/raw");

        SparkSession spark = SparkSession.builder()
                .appName("CarView Offline Analysis")
                .getOrCreate();

        try {
            log.info("开始执行离线分析任务...");

            log.info("1/4 计算每日里程统计...");
            new DailyMileageJob(spark, mysqlUrl, mysqlUser, mysqlPassword).run();

            log.info("2/4 计算月度油耗趋势...");
            new MonthlyFuelTrendJob(spark, mysqlUrl, mysqlUser, mysqlPassword).run();

            log.info("3/4 计算故障率排名...");
            new FaultRankingJob(spark, mysqlUrl, mysqlUser, mysqlPassword).run();

            log.info("4/4 计算驾驶行为评分...");
            new DriverBehaviorScoreJob(spark, mysqlUrl, mysqlUser, mysqlPassword).run();

            log.info("所有离线分析任务完成");
        } catch (Exception e) {
            log.error("离线分析任务异常", e);
        } finally {
            spark.stop();
        }
    }
}
