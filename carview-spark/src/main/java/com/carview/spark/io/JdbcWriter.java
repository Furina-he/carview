package com.carview.spark.io;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SaveMode;

import java.util.Properties;

/**
 * JDBC 写入工具
 */
public class JdbcWriter {

    private final String url;
    private final String user;
    private final String password;

    public JdbcWriter(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
    }

    public void save(Dataset<Row> df, String tableName) {
        Properties props = new Properties();
        props.setProperty("user", user);
        props.setProperty("password", password);
        props.setProperty("driver", "com.mysql.cj.jdbc.Driver");

        df.write()
                .mode(SaveMode.Append)
                .jdbc(url, tableName, props);
    }

    public Dataset<Row> read(org.apache.spark.sql.SparkSession spark, String tableName) {
        Properties props = new Properties();
        props.setProperty("user", user);
        props.setProperty("password", password);
        props.setProperty("driver", "com.mysql.cj.jdbc.Driver");

        return spark.read().jdbc(url, tableName, props);
    }
}
