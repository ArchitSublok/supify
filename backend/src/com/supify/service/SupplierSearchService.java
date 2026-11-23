package com.supify.service;

import com.supify.dao.SupplierRepository;
import com.supify.model.Organisation;
import com.supify.model.PaginatedResult;
import com.supify.model.SupplierProfile;
import com.supify.model.enums.TrustBand;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Demonstrates:
 * - Lambda expressions & functional interfaces (Predicate, Function, Comparator)
 * - Method references (Class::method, instance::method)
 * - Stream fundamentals and intermediate operations: filter(), map(), sorted(), distinct(), skip(), limit()
 * - Terminal operations and Collectors
 */
public class SupplierSearchService {
    private final SupplierRepository supplierRepo;

    public record SupplierSearchResult(
            UUID organisationId,
            String legalName,
            String tradeName,
            String category,
            String location,
            int capacityPerMonth,
            String capacityUnit,
            int moq,
            String leadTime,
            TrustBand trustBand,
            double overallTrustScore
    ) {
        public String getCapacityString() {
            return String.format("%,d %s/mo", capacityPerMonth, capacityUnit);
        }
    }

    public SupplierSearchService(SupplierRepository supplierRepo) {
        this.supplierRepo = Objects.requireNonNull(supplierRepo, "supplierRepo must not be null");
    }

    /**
     * Multi-faceted supplier search utilizing functional Stream pipelines.
     */
    public PaginatedResult<SupplierSearchResult> search(
            String keywordQuery,
            String categoryFilter,
            TrustBand minimumBand,
            Integer minCapacity,
            String sortBy,
            boolean ascending,
            int page,
            int pageSize) {

        // Build composite predicates using lambda syntax
        Predicate<SupplierProfile> profilePredicate = p -> true;

        if (categoryFilter != null && !categoryFilter.trim().isEmpty()) {
            String catLower = categoryFilter.trim().toLowerCase();
            profilePredicate = profilePredicate.and(p -> p.getCategory().toLowerCase().contains(catLower));
        }

        if (minimumBand != null) {
            profilePredicate = profilePredicate.and(p -> p.getTrustBand().getTierLevel() >= minimumBand.getTierLevel());
        }

        if (minCapacity != null && minCapacity > 0) {
            profilePredicate = profilePredicate.and(p -> p.getCapacityPerMonth() >= minCapacity);
        }

        // Preload organisations map for joining
        Map<UUID, Organisation> orgMap = supplierRepo.findAllOrganisations().stream()
                .collect(Collectors.toMap(Organisation::getId, Function.identity()));

        // Stream pipeline
        List<SupplierSearchResult> fullMatched = supplierRepo.findAllProfiles().stream()
                .filter(profilePredicate)
                .map(p -> {
                    Organisation org = orgMap.get(p.getOrganisationId());
                    String legalName = org != null ? org.getLegalName() : "Unknown";
                    String tradeName = org != null ? org.getTradeName() : "";
                    String location = org != null ? org.getLocation() : "India";
                    return new SupplierSearchResult(
                            p.getOrganisationId(),
                            legalName,
                            tradeName,
                            p.getCategory(),
                            location,
                            p.getCapacityPerMonth(),
                            p.getCapacityUnit(),
                            p.getMinimumOrderQuantity(),
                            p.getLeadTime(),
                            p.getTrustBand(),
                            p.getOverallTrustScore()
                    );
                })
                .filter(r -> {
                    if (keywordQuery == null || keywordQuery.trim().isEmpty()) return true;
                    String q = keywordQuery.trim().toLowerCase();
                    return r.legalName().toLowerCase().contains(q)
                            || r.tradeName().toLowerCase().contains(q)
                            || r.category().toLowerCase().contains(q)
                            || r.location().toLowerCase().contains(q);
                })
                .distinct() // distinct operations
                .sorted(getComparator(sortBy, ascending)) // sorted operation
                .collect(Collectors.toList());

        // Apply pagination intermediate operations: skip() and limit()
        int total = fullMatched.size();
        int offset = Math.max(0, (page - 1) * pageSize);

        List<SupplierSearchResult> pageItems = fullMatched.stream()
                .skip(offset)
                .limit(pageSize)
                .collect(Collectors.toList());

        return new PaginatedResult<>(pageItems, total, page, pageSize);
    }

    private Comparator<SupplierSearchResult> getComparator(String sortBy, boolean ascending) {
        Comparator<SupplierSearchResult> comparator;
        if ("score".equalsIgnoreCase(sortBy)) {
            comparator = Comparator.comparingDouble(SupplierSearchResult::overallTrustScore);
        } else if ("capacity".equalsIgnoreCase(sortBy)) {
            comparator = Comparator.comparingInt(SupplierSearchResult::capacityPerMonth);
        } else if ("moq".equalsIgnoreCase(sortBy)) {
            comparator = Comparator.comparingInt(SupplierSearchResult::moq);
        } else {
            // Default sort by legal name
            comparator = Comparator.comparing(SupplierSearchResult::legalName, String.CASE_INSENSITIVE_ORDER);
        }
        return ascending ? comparator : comparator.reversed();
    }
}
