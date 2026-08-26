const methodLabels = {
  self_declared: 'Self-declared',
  document_review: 'Document review',
  third_party_api: 'Registry check',
  human_audit: 'Human audit',
  site_audit: 'Site audit',
  site_visit: 'Site visit',
  registry_and_audit: 'Registry check & audit',
  comprehensive_audit: 'Comprehensive audit & API sync',
}

/** Maps API/database wire data to stable UI domain data. */
export function toSupplier(wire) {
  if (!wire) return null
  return {
    id: wire.id,
    legalName: wire.legal_name,
    tradeName: wire.trade_name || wire.legal_name,
    location: wire.location,
    region: wire.region || 'Global',
    category: wire.category,
    description: wire.description || 'Verified industrial manufacturer.',
    capacity: wire.capacity,
    moq: wire.moq,
    leadTime: wire.lead_time,
    rating: wire.rating || 4.5,
    reviewsCount: wire.reviews_count || 0,
    themeColor: wire.theme_color || 'brand-teal',
    avatarInitial: wire.avatar_initial || wire.trade_name?.[0] || 'S',
    listingState: wire.listing_state,
    trust: {
      band: wire.trust?.band || 'unverified',
      method: wire.trust?.method || 'self_declared',
      methodLabel: wire.trust?.method_label || methodLabels[wire.trust?.method] || 'Self-declared',
      verifiedAt: wire.trust?.verified_at || null,
      validUntil: wire.trust?.valid_until || null,
      lastChecked: wire.trust?.last_checked || 'Pending check',
      overallScore: wire.trust?.overall_score || 0,
      pillars: (wire.trust?.pillars || []).map(([name, score]) => ({ name, score })),
      gatesApplied: wire.trust?.gates_applied || [],
    },
    claims: (wire.claims || []).map((claim) => ({
      key: claim.key,
      label: claim.label,
      value: claim.value,
      state: claim.state,
      method: claim.method,
      methodLabel: claim.method_label || methodLabels[claim.method] || 'Self-declared',
      verifiedAt: claim.verified_at,
      validUntil: claim.valid_until,
      evidenceCount: claim.evidence_count || 0,
    })),
  }
}
