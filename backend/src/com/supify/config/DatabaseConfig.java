package com.supify.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/** JDBC connection configuration and connectivity check for MySQL. */
public class DatabaseConfig {
    private static final String DEFAULT_DRIVER = "com.mysql.cj.jdbc.Driver";
    private static final String DEFAULT_URL = "jdbc:mysql://localhost:3306/supify?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String DEFAULT_USER = "root";
    private static final String DEFAULT_PASS = "root";

    private static final String driver = DEFAULT_DRIVER;
    private static String url = resolveSetting("supify.db.url", "SUPIFY_DB_URL", DEFAULT_URL);
    private static String user = resolveSetting("supify.db.user", "SUPIFY_DB_USER", DEFAULT_USER);
    private static String password = resolveSetting("supify.db.pass", "SUPIFY_DB_PASS", DEFAULT_PASS);
    private static volatile String lastError = null;

    private static String resolveSetting(String propKey, String envKey, String fallback) {
        String prop = System.getProperty(propKey);
        if (prop != null && !prop.isBlank()) return prop.trim();
        String env = System.getenv(envKey);
        if (env != null && !env.isBlank()) return env.trim();
        return fallback;
    }

    public static synchronized void configure(String dbUrl, String dbUser, String dbPass) {
        if (dbUrl != null && !dbUrl.isBlank()) url = dbUrl.trim();
        if (dbUser != null && !dbUser.isBlank()) user = dbUser.trim();
        if (dbPass != null) password = dbPass;
        lastError = null;
    }

    public static String getUrl() {
        return url;
    }

    public static String getUser() {
        return user;
    }

    public static String getLastError() {
        return lastError;
    }

    public static boolean isDriverAvailable() {
        try {
            Class.forName(driver);
            return true;
        } catch (ClassNotFoundException e) {
            lastError = "Driver not on classpath: " + driver;
            return false;
        }
    }

    public static Connection getConnection() throws SQLException {
        try {
            Class.forName(driver);
        } catch (ClassNotFoundException e) {
            lastError = "MySQL JDBC Driver not found on classpath: " + driver;
            throw new SQLException(lastError, e);
        }
        try {
            Connection conn = DriverManager.getConnection(url, user, password);
            lastError = null;
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
            if (lastError == null || lastError.isBlank()) {
                lastError = e.getMessage();
            }
            return false;
        }
    }
}
