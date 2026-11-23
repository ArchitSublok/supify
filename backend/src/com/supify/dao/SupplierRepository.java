package com.supify.dao;

import com.supify.model.Organisation;
import com.supify.model.SupplierProfile;
import com.supify.model.TrustSnapshot;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

public interface SupplierRepository {
    Organisation saveOrganisation(Organisation organisation);
    Optional<Organisation> findOrganisationById(UUID id);
    List<Organisation> findAllOrganisations();

    SupplierProfile saveProfile(SupplierProfile profile);
    Optional<SupplierProfile> findProfileByOrganisationId(UUID organisationId);
    List<SupplierProfile> findAllProfiles();
    List<SupplierProfile> findProfilesMatching(Predicate<? super SupplierProfile> predicate);

    TrustSnapshot saveSnapshot(TrustSnapshot snapshot);
    Optional<TrustSnapshot> findSnapshotByOrganisationId(UUID organisationId);
}
