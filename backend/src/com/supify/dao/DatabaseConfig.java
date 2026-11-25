package com.supify.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/** Manages MySQL JDBC driver loading, connection URL resolution, and Connection factory calls. */
public class DatabaseConfig {
    private static final String DRIVER = "com.mysql.cj.jdbc.Driver";
    private static final String DEFAULT_URL =
            "jdbc:mysql://localhost:3306/supify?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";

    private static String lastError = "";

    public static String getUrl() {
        return readSetting("supify.db.url", "SUPIFY_DB_URL", DEFAULT_URL);
    }

    public static String getUser() {
        return readSetting("supify.db.user", "SUPIFY_DB_USER", "root");
    }

    private static String getPassword() {
        return readSetting("supify.db.pass", "SUPIFY_DB_PASS", "root");
    }

    private static String readSetting(String propKey, String envKey, String fallback) {
        String prop = System.getProperty(propKey);
        if (prop != null && !prop.isBlank()) return prop.trim();
        String env = System.getenv(envKey);
        if (env != null && !env.isBlank()) return env.trim();
        return fallback;
    }

    public static String getLastError() {
        return lastError;
    }

    public static boolean isDriverAvailable() {
        try {
            Class.forName(DRIVER);
            return true;
        } catch (ClassNotFoundException e) {
            lastError = "Driver not found: " + DRIVER;
            return false;
        }
    }

    public static Connection getConnection() throws SQLException {
        try {
            Class.forName(DRIVER);
        } catch (ClassNotFoundException e) {
            lastError = "MySQL driver missing from classpath: " + DRIVER;
            throw new SQLException(lastError, e);
        }
        try {
            Connection conn = DriverManager.getConnection(getUrl(), getUser(), getPassword());
            lastError = "";
            return conn;
        } catch (SQLException e) {
            lastError = e.getMessage();
            throw e;
        }
    }

    public static boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (Exception e) {
            lastError = e.getMessage();
            return false;
        }
    }
}
