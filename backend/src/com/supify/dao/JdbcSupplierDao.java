package com.supify.dao;

import com.supify.model.Supplier;
import com.supify.model.TrustBand;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/** MySQL JDBC CRUD implementation using PreparedStatement, executeQuery/Update, and ResultSet mapping. */
public class JdbcSupplierDao implements Repository<Supplier, String> {

    @Override
    public Supplier save(Supplier s) {
        String orgSql = "INSERT INTO organisation (id, legal_name, trade_name, entity_type, country_code, city) "
                + "VALUES (?, ?, ?, 'pvt_ltd', 'IN', ?) "
                + "ON DUPLICATE KEY UPDATE legal_name = ?, trade_name = ?, city = ?";

        String profileSql = "INSERT INTO supplier_profile "
                + "(id, organisation_id, headline, category, capacity_per_month, moq, lead_time, trust_band, overall_trust_score) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE headline = ?, category = ?, capacity_per_month = ?, moq = ?, lead_time = ?, trust_band = ?, overall_trust_score = ?";

        String headline = s.getName() + " - " + s.getCategory();

        try (Connection conn = DatabaseConfig.getConnection()) {
            try (PreparedStatement psOrg = conn.prepareStatement(orgSql)) {
                psOrg.setString(1, s.getId());
                psOrg.setString(2, s.getName());
                psOrg.setString(3, s.getName());
                psOrg.setString(4, s.getCity());
                psOrg.setString(5, s.getName());
                psOrg.setString(6, s.getName());
                psOrg.setString(7, s.getCity());
                psOrg.executeUpdate();
            }
            try (PreparedStatement psProf = conn.prepareStatement(profileSql)) {
                psProf.setString(1, s.getId() + "-PROF");
                psProf.setString(2, s.getId());
                psProf.setString(3, headline);
                psProf.setString(4, s.getCategory());
                psProf.setInt(5, s.getCapacityPerMonth());
                psProf.setInt(6, s.getMoq());
                psProf.setString(7, s.getLeadTime());
                psProf.setString(8, s.getTrustBand().name());
                psProf.setDouble(9, s.getTrustScore());

                psProf.setString(10, headline);
                psProf.setString(11, s.getCategory());
                psProf.setInt(12, s.getCapacityPerMonth());
                psProf.setInt(13, s.getMoq());
                psProf.setString(14, s.getLeadTime());
                psProf.setString(15, s.getTrustBand().name());
                psProf.setDouble(16, s.getTrustScore());
                psProf.executeUpdate();
            }
            return s;
        } catch (SQLException e) {
            throw new RuntimeException("JDBC save failed: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Supplier> findById(String id) {
        String sql = "SELECT o.id, o.legal_name, o.city, p.category, p.capacity_per_month, "
                + "p.moq, p.lead_time, p.trust_band, p.overall_trust_score "
                + "FROM organisation o "
                + "JOIN supplier_profile p ON p.organisation_id = o.id "
                + "WHERE o.id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("JDBC findById failed: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public List<Supplier> findAll() {
        String sql = "SELECT o.id, o.legal_name, o.city, p.category, p.capacity_per_month, "
                + "p.moq, p.lead_time, p.trust_band, p.overall_trust_score "
                + "FROM organisation o "
                + "JOIN supplier_profile p ON p.organisation_id = o.id "
                + "ORDER BY p.overall_trust_score DESC";
        List<Supplier> list = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getConnection();
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

    @Override
    public List<Supplier> findMatching(Predicate<? super Supplier> filter) {
        return findAll().stream().filter(filter).collect(Collectors.toList());
    }

    public boolean updateTrust(String supplierId, TrustBand band, double score) {
        String sql = "UPDATE supplier_profile SET trust_band = ?, overall_trust_score = ? WHERE organisation_id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
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
        String sql = "DELETE FROM organisation WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("JDBC deleteById failed: " + e.getMessage(), e);
        }
    }

    @Override
    public long count() {
        String sql = "SELECT COUNT(*) FROM supplier_profile";
        try (Connection conn = DatabaseConfig.getConnection();
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
                rs.getString("legal_name"),
                rs.getString("city"),
                rs.getString("category"),
                rs.getInt("capacity_per_month"),
                rs.getInt("moq"),
                rs.getString("lead_time")
        );
        s.setTrustBand(TrustBand.valueOf(rs.getString("trust_band")));
        s.setTrustScore(rs.getDouble("overall_trust_score"));
        return s;
    }
}
