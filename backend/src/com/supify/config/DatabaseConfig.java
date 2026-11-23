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

    private static String driver = DEFAULT_DRIVER;
    private static String url = DEFAULT_URL;
    private static String user = DEFAULT_USER;
    private static String password = DEFAULT_PASS;

    public static void configure(String dbUrl, String dbUser, String dbPass) {
        url = dbUrl;
        user = dbUser;
        password = dbPass;
    }

    public static Connection getConnection() throws SQLException {
        try {
            Class.forName(driver);
        } catch (ClassNotFoundException e) {
            // Driver class may not be on classpath if running pure in-memory demo
            throw new SQLException("MySQL JDBC Driver not found on classpath: " + driver, e);
        }
        return DriverManager.getConnection(url, user, password);
    }

    public static boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (Exception e) {
            return false;
        }
    }
}
