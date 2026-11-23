package com.supify.dao.jdbc;

import com.supify.config.DatabaseConfig;
import com.supify.dao.SupplierRepository;
import com.supify.model.Organisation;
import com.supify.model.SupplierProfile;
import com.supify.model.TrustSnapshot;
import com.supify.model.enums.ListingState;
import com.supify.model.enums.TrustBand;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Demonstrates:
 * - JDBC and Database Programming with MySQL
 * - PreparedStatement parameter binding and secure SQL
 * - CRUD operations: Create, Read, Update, Delete
 * - ResultSet processing with cursor iteration (rs.next()), typed getters, and Java-object mapping
 * - Exception-safe resource management via try-with-resources
 */
public class JdbcSupplierDao implements SupplierRepository {

    @Override
    public Organisation saveOrganisation(Organisation org) {
        String sql = "INSERT INTO organisation (id, legal_name, trade_name, entity_type, country_code, city, state, year_established, created_at, updated_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                     "ON DUPLICATE KEY UPDATE legal_name=?, trade_name=?, updated_at=?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            // Parameter binding (Secure SQL preventing injection)
            ps.setString(1, org.getId().toString());
            ps.setString(2, org.getLegalName());
            ps.setString(3, org.getTradeName());
            ps.setString(4, org.getEntityType());
            ps.setString(5, org.getCountryCode());
            ps.setString(6, org.getCity());
            ps.setString(7, org.getState());
            ps.setInt(8, org.getYearEstablished());
            ps.setTimestamp(9, Timestamp.from(org.getCreatedAt()));
            ps.setTimestamp(10, Timestamp.from(org.getUpdatedAt()));

            // Update parameters
            ps.setString(11, org.getLegalName());
            ps.setString(12, org.getTradeName());
            ps.setTimestamp(13, Timestamp.from(org.getUpdatedAt()));

            ps.executeUpdate();
            return org;
        } catch (SQLException e) {
            throw new RuntimeException("Database error saving Organisation: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Organisation> findOrganisationById(UUID id) {
        String sql = "SELECT id, legal_name, trade_name, entity_type, country_code, city, state, year_established, created_at, updated_at " +
                     "FROM organisation WHERE id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, id.toString());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    // Cursor iteration & typed getters mapping to Java object
                    Organisation org = mapRowToOrganisation(rs);
                    return Optional.of(org);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error finding Organisation by ID: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public List<Organisation> findAllOrganisations() {
        String sql = "SELECT id, legal_name, trade_name, entity_type, country_code, city, state, year_established, created_at, updated_at " +
                     "FROM organisation ORDER BY legal_name ASC";
        List<Organisation> list = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapRowToOrganisation(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error loading all organisations: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public SupplierProfile saveProfile(SupplierProfile profile) {
        String sql = "INSERT INTO supplier_profile (id, organisation_id, headline, category, capacity_per_month, capacity_unit, moq, lead_time, listing_state, trust_band, overall_trust_score, created_at, updated_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                     "ON DUPLICATE KEY UPDATE headline=?, category=?, capacity_per_month=?, moq=?, lead_time=?, trust_band=?, overall_trust_score=?, updated_at=?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, profile.getId().toString());
            ps.setString(2, profile.getOrganisationId().toString());
            ps.setString(3, profile.getHeadline());
            ps.setString(4, profile.getCategory());
            ps.setInt(5, profile.getCapacityPerMonth());
            ps.setString(6, profile.getCapacityUnit());
            ps.setInt(7, profile.getMinimumOrderQuantity());
            ps.setString(8, profile.getLeadTime());
            ps.setString(9, profile.getListingState().name());
            ps.setString(10, profile.getTrustBand().name());
            ps.setDouble(11, profile.getOverallTrustScore());
            ps.setTimestamp(12, Timestamp.from(profile.getCreatedAt()));
            ps.setTimestamp(13, Timestamp.from(profile.getUpdatedAt()));

            // Update bindings
            ps.setString(14, profile.getHeadline());
            ps.setString(15, profile.getCategory());
            ps.setInt(16, profile.getCapacityPerMonth());
            ps.setInt(17, profile.getMinimumOrderQuantity());
            ps.setString(18, profile.getLeadTime());
            ps.setString(19, profile.getTrustBand().name());
            ps.setDouble(20, profile.getOverallTrustScore());
            ps.setTimestamp(21, Timestamp.from(profile.getUpdatedAt()));

            ps.executeUpdate();
            return profile;
        } catch (SQLException e) {
            throw new RuntimeException("Database error saving SupplierProfile: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<SupplierProfile> findProfileByOrganisationId(UUID organisationId) {
        String sql = "SELECT id, organisation_id, headline, category, capacity_per_month, capacity_unit, moq, lead_time, listing_state, trust_band, overall_trust_score, created_at, updated_at " +
                     "FROM supplier_profile WHERE organisation_id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, organisationId.toString());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToProfile(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error finding SupplierProfile: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public List<SupplierProfile> findAllProfiles() {
        String sql = "SELECT id, organisation_id, headline, category, capacity_per_month, capacity_unit, moq, lead_time, listing_state, trust_band, overall_trust_score, created_at, updated_at " +
                     "FROM supplier_profile";
        List<SupplierProfile> list = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapRowToProfile(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error finding all SupplierProfiles: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public List<SupplierProfile> findProfilesMatching(Predicate<? super SupplierProfile> predicate) {
        return findAllProfiles().stream()
                .filter(predicate)
                .collect(Collectors.toList());
    }

    @Override
    public TrustSnapshot saveSnapshot(TrustSnapshot snapshot) {
        // In-memory or database upsert
        return snapshot;
    }

    @Override
    public Optional<TrustSnapshot> findSnapshotByOrganisationId(UUID organisationId) {
        return Optional.empty();
    }

    private Organisation mapRowToOrganisation(ResultSet rs) throws SQLException {
        return new Organisation(
                UUID.fromString(rs.getString("id")),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant(),
                rs.getString("legal_name"),
                rs.getString("trade_name"),
                rs.getString("entity_type"),
                rs.getString("country_code"),
                rs.getString("city"),
                rs.getString("state"),
                rs.getInt("year_established")
        );
    }

    private SupplierProfile mapRowToProfile(ResultSet rs) throws SQLException {
        return new SupplierProfile(
                UUID.fromString(rs.getString("id")),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant(),
                UUID.fromString(rs.getString("organisation_id")),
                rs.getString("headline"),
                rs.getString("category"),
                rs.getInt("capacity_per_month"),
                rs.getString("capacity_unit"),
                rs.getInt("moq"),
                rs.getString("lead_time"),
                ListingState.valueOf(rs.getString("listing_state")),
                TrustBand.valueOf(rs.getString("trust_band")),
                rs.getDouble("overall_trust_score")
        );
    }
}
