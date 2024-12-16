package org.example.wordgame.utils;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseConnectionPool {

    private static HikariDataSource dataSource;

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:postgresql://localhost:5432/postgres");
        config.setUsername("postgres");
        config.setPassword("new_password");
        config.setMaximumPoolSize(10);

        dataSource = new HikariDataSource(config);
    }

    // Lấy kết nối từ pool
    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }
}