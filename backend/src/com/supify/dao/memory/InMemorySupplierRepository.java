package com.supify.dao.memory;

import com.supify.dao.SupplierRepository;
import com.supify.model.Organisation;
import com.supify.model.SupplierProfile;
import com.supify.model.TrustSnapshot;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class InMemorySupplierRepository implements SupplierRepository {
    private final Map<UUID, Organisation> organisations = new ConcurrentHashMap<>();
    private final Map<UUID, SupplierProfile> profiles = new ConcurrentHashMap<>();
    private final Map<UUID, TrustSnapshot> snapshots = new ConcurrentHashMap<>();

    @Override
    public Organisation saveOrganisation(Organisation organisation) {
        organisations.put(organisation.getId(), organisation);
        return organisation;
    }

    @Override
    public Optional<Organisation> findOrganisationById(UUID id) {
        return Optional.ofNullable(organisations.get(id));
    }

    @Override
    public List<Organisation> findAllOrganisations() {
        return new ArrayList<>(organisations.values());
    }

    @Override
    public SupplierProfile saveProfile(SupplierProfile profile) {
        profiles.put(profile.getOrganisationId(), profile);
        return profile;
    }

    @Override
    public Optional<SupplierProfile> findProfileByOrganisationId(UUID organisationId) {
        return Optional.ofNullable(profiles.get(organisationId));
    }

    @Override
    public List<SupplierProfile> findAllProfiles() {
        return new ArrayList<>(profiles.values());
    }

    @Override
    public List<SupplierProfile> findProfilesMatching(Predicate<? super SupplierProfile> predicate) {
        return profiles.values().stream()
                .filter(predicate)
                .collect(Collectors.toList());
    }

    @Override
    public TrustSnapshot saveSnapshot(TrustSnapshot snapshot) {
        snapshots.put(snapshot.getOrganisationId(), snapshot);
        return snapshot;
    }

    @Override
    public Optional<TrustSnapshot> findSnapshotByOrganisationId(UUID organisationId) {
        return Optional.ofNullable(snapshots.get(organisationId));
    }
}
