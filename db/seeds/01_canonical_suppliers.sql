-- Canonical idempotent seed fixtures matching SeedDataFactory

USE supify;

-- 1. Organisations (5 Canonical Suppliers)
INSERT INTO organisation (id, legal_name, trade_name, entity_type, country_code, city, state, year_established)
VALUES
    ('11111111-1111-4111-8111-000000000001', 'Aravind Fasteners Private Limited', 'Aravind Fasteners', 'pvt_ltd', 'IN', 'Coimbatore', 'Tamil Nadu', 2008),
    ('11111111-1111-4111-8111-000000000002', 'Monarch Industrial Assemblies', 'Monarch Industries', 'pvt_ltd', 'IN', 'Pune', 'Maharashtra', 2012),
    ('11111111-1111-4111-8111-000000000003', 'Kavach Industrial Safety Systems', 'Kavach Safety', 'pvt_ltd', 'IN', 'Ahmedabad', 'Gujarat', 2016),
    ('11111111-1111-4111-8111-000000000004', 'Zenith Aerospace Electronics', 'Zenith Aero', 'public_ltd', 'IN', 'Bengaluru', 'Karnataka', 2004),
    ('11111111-1111-4111-8111-000000000005', 'Suryodaya Solar Tech LLP', 'Suryodaya Solar', 'llp', 'IN', 'Jaipur', 'Rajasthan', 2019)
ON DUPLICATE KEY UPDATE
    legal_name = VALUES(legal_name),
    trade_name = VALUES(trade_name),
    city = VALUES(city),
    state = VALUES(state);

-- 2. Supplier Profiles
INSERT INTO supplier_profile (id, organisation_id, headline, category, capacity_per_month, capacity_unit, moq, lead_time, listing_state, trust_band, overall_trust_score)
VALUES
    ('22222222-2222-4222-8222-000000000001', '11111111-1111-4111-8111-000000000001', 'Precision cold-forged fasteners, automotive bolts and aerospace-grade hex screws', 'Fasteners & Cold Heading', 50000, 'kg', 1000, '14 days', 'LISTED', 'VERIFIED', 0.840),
    ('22222222-2222-4222-8222-000000000002', '11111111-1111-4111-8111-000000000002', 'Custom precision sheet metal fabrication, laser cutting, and robotic CNC bending', 'Custom Sheet Metal', 12000, 'units', 200, '21 days', 'LISTED', 'AUDITED', 0.890),
    ('22222222-2222-4222-8222-000000000003', '11111111-1111-4111-8111-000000000003', 'Certified industrial safety harnesses, fall arresters, and PPE systems for heavy industry', 'Industrial Safety Equipment', 25000, 'units', 500, '10 days', 'LISTED', 'VERIFIED', 0.760),
    ('22222222-2222-4222-8222-000000000004', '11111111-1111-4111-8111-000000000004', 'MIL-spec avionics wiring harnesses, high-density multilayer PCBs and flight controller assemblies', 'Aerospace Harnesses & PCBs', 5000, 'boards', 50, '30 days', 'LISTED', 'AUDITED', 0.940),
    ('22222222-2222-4222-8222-000000000005', '11111111-1111-4111-8111-000000000005', 'Galvanized solar module mounting structures, ground-mount tracker pylons and rooftop rails', 'Solar Mounting Structures', 80000, 'brackets', 2500, '18 days', 'LISTED', 'BASIC', 0.650)
ON DUPLICATE KEY UPDATE
    headline = VALUES(headline),
    category = VALUES(category),
    capacity_per_month = VALUES(capacity_per_month),
    moq = VALUES(moq),
    lead_time = VALUES(lead_time),
    trust_band = VALUES(trust_band),
    overall_trust_score = VALUES(overall_trust_score);

-- 3. Trust Snapshots
INSERT INTO trust_snapshot (organisation_id, trust_band, overall_score, pillar_scores_json)
VALUES
    ('11111111-1111-4111-8111-000000000001', 'VERIFIED', 0.840, '{"IDENTITY":0.92,"LEGAL":0.86,"QUALITY":0.68,"CAPABILITY":0.58}'),
    ('11111111-1111-4111-8111-000000000002', 'AUDITED',  0.890, '{"IDENTITY":0.95,"LEGAL":0.90,"QUALITY":0.92,"CAPABILITY":0.81}'),
    ('11111111-1111-4111-8111-000000000003', 'VERIFIED', 0.760, '{"IDENTITY":0.90,"LEGAL":0.82,"QUALITY":0.70,"CAPABILITY":0.62}'),
    ('11111111-1111-4111-8111-000000000004', 'AUDITED',  0.940, '{"IDENTITY":0.98,"LEGAL":0.94,"QUALITY":0.96,"CAPABILITY":0.88}'),
    ('11111111-1111-4111-8111-000000000005', 'BASIC',    0.650, '{"IDENTITY":0.85,"LEGAL":0.70,"QUALITY":0.55,"CAPABILITY":0.60}')
ON DUPLICATE KEY UPDATE
    trust_band = VALUES(trust_band),
    overall_score = VALUES(overall_score),
    pillar_scores_json = VALUES(pillar_scores_json);

-- 4. Claims (Verified spine examples + pending review queue claims)
INSERT INTO claim (id, organisation_id, claim_type_key, label, pillar, asserted_value, status, submitted_at)
VALUES
    ('33333333-3333-4333-8333-000000000001', '11111111-1111-4111-8111-000000000001', 'gst_registration', 'GSTIN Active Registration', 'IDENTITY', '33AABCA1234F1Z8', 'VERIFIED', DATE_SUB(NOW(), INTERVAL 90 DAY)),
    ('33333333-3333-4333-8333-000000000002', '11111111-1111-4111-8111-000000000001', 'iso_9001', 'ISO 9001:2015 Quality Management', 'QUALITY', 'ISO-9001:2015 - Reg #QA-98421', 'VERIFIED', DATE_SUB(NOW(), INTERVAL 200 DAY)),
    ('33333333-3333-4333-8333-000000000003', '11111111-1111-4111-8111-000000000001', 'annual_capacity', 'Cold Heading Production Capacity (50T/mo)', 'CAPABILITY', '50,000 kg / month rated capacity', 'SUBMITTED', DATE_SUB(NOW(), INTERVAL 2 HOUR)),
    ('33333333-3333-4333-8333-000000000004', '11111111-1111-4111-8111-000000000002', 'iso_14001', 'ISO 14001 Environmental Management', 'COMPLIANCE', 'ISO-14001:2015 Environmental System', 'SUBMITTED', DATE_SUB(NOW(), INTERVAL 3 HOUR)),
    ('33333333-3333-4333-8333-000000000005', '11111111-1111-4111-8111-000000000003', 'pollution_control', 'State PCB Consent to Operate (CTO)', 'COMPLIANCE', 'Gujarat PCB Consent Order #GPCB/CTO/2024-29', 'SUBMITTED', DATE_SUB(NOW(), INTERVAL 1 HOUR))
ON DUPLICATE KEY UPDATE
    label = VALUES(label),
    asserted_value = VALUES(asserted_value),
    status = VALUES(status);

-- 5. Evidence Artefacts
INSERT INTO evidence (id, claim_id, storage_ref, kind, title, issued_at, expires_at)
VALUES
    ('44444444-4444-4444-8444-000000000001', '33333333-3333-4333-8333-000000000001', 'ref:gst_cert_2026.pdf', 'PDF', 'GST Registration Certificate', DATE_SUB(NOW(), INTERVAL 90 DAY), NULL),
    ('44444444-4444-4444-8444-000000000002', '33333333-3333-4333-8333-000000000002', 'ref:tuv_iso9001.pdf', 'PDF', 'TUV Rheinland Certificate', DATE_SUB(NOW(), INTERVAL 200 DAY), DATE_ADD(NOW(), INTERVAL 365 DAY)),
    ('44444444-4444-4444-8444-000000000003', '33333333-3333-4333-8333-000000000004', 'ref:iso14001_monarch.pdf', 'PDF', 'Bureau Veritas ISO 14001 Certificate', DATE_SUB(NOW(), INTERVAL 30 DAY), DATE_ADD(NOW(), INTERVAL 335 DAY))
ON DUPLICATE KEY UPDATE
    title = VALUES(title),
    storage_ref = VALUES(storage_ref);

-- 6. Verification Checks (INSERT IGNORE preserves append-only trigger invariant on re-seed)
INSERT IGNORE INTO verification_check (id, claim_id, method, outcome, confidence, performed_by, performed_at, rationale)
VALUES
    ('55555555-5555-4555-8555-000000000001', '33333333-3333-4333-8333-000000000001', 'THIRD_PARTY_API', 'PASS', 1.00, 'gst_adapter@v1', DATE_SUB(NOW(), INTERVAL 89 DAY), 'Registry verified ACTIVE on GST portal.'),
    ('55555555-5555-4555-8555-000000000002', '33333333-3333-4333-8333-000000000002', 'DOCUMENT_REVIEW', 'PASS', 0.95, 'senior_auditor_01', DATE_SUB(NOW(), INTERVAL 180 DAY), 'Accredited certificate matches Registrar QR code.'),
    ('55555555-5555-4555-8555-000000000003', '33333333-3333-4333-8333-000000000003', 'SITE_VISIT', 'PASS', 1.00, 'auditor_chennai', NOW(), 'On-site machine audit verified 4 high-speed forging headers.');

-- 7. Attestations (includes one short-lived attestation for sp_expire_stale_attestations demo)
INSERT INTO attestation (id, claim_id, source_check_id, method_achieved, verified_at, valid_until, state, issued_by)
VALUES
    ('66666666-6666-4666-8666-000000000001', '33333333-3333-4333-8333-000000000001', '55555555-5555-4555-8555-000000000001', 'THIRD_PARTY_API', DATE_SUB(NOW(), INTERVAL 89 DAY), DATE_ADD(NOW(), INTERVAL 180 DAY), 'ACTIVE', 'SYSTEM_ADAPTER'),
    ('66666666-6666-4666-8666-000000000002', '33333333-3333-4333-8333-000000000002', '55555555-5555-4555-8555-000000000002', 'DOCUMENT_REVIEW', DATE_SUB(NOW(), INTERVAL 180 DAY), DATE_ADD(NOW(), INTERVAL 165 DAY), 'ACTIVE', 'senior_auditor_01'),
    ('66666666-6666-4666-8666-000000000003', '33333333-3333-4333-8333-000000000003', '55555555-5555-4555-8555-000000000003', 'SITE_VISIT', NOW(), DATE_ADD(NOW(), INTERVAL 35 SECOND), 'ACTIVE', 'auditor_chennai')
ON DUPLICATE KEY UPDATE
    valid_until = VALUES(valid_until),
    state = VALUES(state);

-- 8. Pending Verification Tasks (SLA warning + dual-control tasks)
INSERT INTO verification_task (id, claim_id, organisation_id, supplier_name, claim_label, state, priority, requires_dual, sla_due_at)
VALUES
    ('77777777-7777-4777-8777-000000000001', '33333333-3333-4333-8333-000000000003', '11111111-1111-4111-8111-000000000001', 'Aravind Fasteners Private Limited', 'Cold Heading Production Capacity (50T/mo)', 'QUEUED', 95, FALSE, DATE_ADD(NOW(), INTERVAL 1 HOUR)),
    ('77777777-7777-4777-8777-000000000002', '33333333-3333-4333-8333-000000000004', '11111111-1111-4111-8111-000000000002', 'Monarch Industrial Assemblies', 'ISO 14001 Environmental Management', 'QUEUED', 90, TRUE, DATE_ADD(NOW(), INTERVAL 4 HOUR)),
    ('77777777-7777-4777-8777-000000000003', '33333333-3333-4333-8333-000000000005', '11111111-1111-4111-8111-000000000003', 'Kavach Industrial Safety Systems', 'State PCB Consent to Operate (CTO)', 'QUEUED', 75, FALSE, DATE_ADD(NOW(), INTERVAL 18 HOUR))
ON DUPLICATE KEY UPDATE
    priority = VALUES(priority),
    sla_due_at = VALUES(sla_due_at);
