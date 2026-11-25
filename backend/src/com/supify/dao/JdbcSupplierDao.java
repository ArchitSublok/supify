package com.supify.dao;

import com.supify.model.Supplier;
import com.supify.model.TrustBand;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

// Connects to MySQL using JDBC and runs CRUD queries on the supplier table
public class JdbcSupplierDao implements Repository<Supplier, String> {
    private static final String DRIVER = "com.mysql.cj.jdbc.Driver";
    private static final String URL =
            "jdbc:mysql://localhost:3306/supify?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String USER = "root";
    private static final String PASS = "root";
    private static String lastError = "";

    public static String getUrl() {
        return URL;
    }

    public static String getLastError() {
        return lastError;
    }

    private static String getPassword() {
        String envPass = System.getenv("SUPIFY_DB_PASS");
        if (envPass != null && !envPass.isEmpty()) {
            return envPass;
        }
        return PASS;
    }

    public static boolean isDriverLoaded() {
        try {
            Class.forName(DRIVER);
            return true;
        } catch (ClassNotFoundException e) {
            lastError = "Driver JAR not in lib/: " + DRIVER;
            return false;
        }
    }

    public static Connection getConnection() throws SQLException {
        try {
            Class.forName(DRIVER);
        } catch (ClassNotFoundException e) {
            lastError = "MySQL driver missing: " + DRIVER;
            throw new SQLException(lastError, e);
        }
        try {
            Connection conn = DriverManager.getConnection(URL, USER, getPassword());
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

    @Override
    public Supplier save(Supplier s) {
        if (findById(s.getId()) != null) {
            deleteById(s.getId());
        }

        String sql = "INSERT INTO supplier "
                + "(id, name, city, category, capacity_per_month, moq, lead_time, trust_band, trust_score) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, s.getId());
            ps.setString(2, s.getName());
            ps.setString(3, s.getCity());
            ps.setString(4, s.getCategory());
            ps.setInt(5, s.getCapacityPerMonth());
            ps.setInt(6, s.getMoq());
            ps.setString(7, s.getLeadTime());
            ps.setString(8, s.getTrustBand().name());
            ps.setDouble(9, s.getTrustScore());
            ps.executeUpdate();
            return s;
        } catch (SQLException e) {
            throw new RuntimeException("JDBC save failed: " + e.getMessage(), e);
        }
    }

    @Override
    public Supplier findById(String id) {
        String sql = "SELECT id, name, city, category, capacity_per_month, "
                + "moq, lead_time, trust_band, trust_score "
                + "FROM supplier WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("JDBC findById failed: " + e.getMessage(), e);
        }
        return null;
    }

    @Override
    public List<Supplier> findAll() {
        String sql = "SELECT id, name, city, category, capacity_per_month, "
                + "moq, lead_time, trust_band, trust_score "
                + "FROM supplier ORDER BY trust_score DESC";
        List<Supplier> list = new ArrayList<>();

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("JDBC findAll failed: " + e.getMessage(), e);
        }
        return list;
    }

    public boolean updateTrust(String supplierId, TrustBand band, double score) {
        String sql = "UPDATE supplier SET trust_band = ?, trust_score = ? WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, band.name());
            ps.setDouble(2, score);
            ps.setString(3, supplierId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("JDBC updateTrust failed: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean deleteById(String id) {
        String sql = "DELETE FROM supplier WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("JDBC deleteById failed: " + e.getMessage(), e);
        }
    }

    @Override
    public long count() {
        String sql = "SELECT COUNT(*) FROM supplier";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("JDBC count failed: " + e.getMessage(), e);
        }
        return 0;
    }

    private Supplier mapRow(ResultSet rs) throws SQLException {
        Supplier s = new Supplier(
                rs.getString("id"),
                rs.getString("name"),
                rs.getString("city"),
                rs.getString("category"),
                rs.getInt("capacity_per_month"),
                rs.getInt("moq"),
                rs.getString("lead_time")
        );
        s.setTrustBand(TrustBand.valueOf(rs.getString("trust_band")));
        s.setTrustScore(rs.getDouble("trust_score"));
        return s;
    }
}
