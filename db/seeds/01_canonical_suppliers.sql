-- Sample seed data matching Main.seedData()

USE supify;

-- 1. Suppliers
INSERT INTO supplier (id, name, city, category, capacity_per_month, moq, lead_time, trust_band, trust_score)
VALUES
    ('SUP-1', 'Aravind Fasteners Pvt Ltd',     'Coimbatore', 'Fasteners',        50000, 1000, '14 days', 'AUDITED',    0.90),
    ('SUP-2', 'Monarch Industrial Assemblies', 'Pune',       'Sheet Metal',      12000,  200, '21 days', 'AUDITED',    0.94),
    ('SUP-3', 'Kavach Safety Systems',         'Ahmedabad',  'Safety Equipment', 25000,  500, '10 days', 'VERIFIED',   0.78),
    ('SUP-4', 'Suryodaya Solar Tech',          'Jaipur',     'Solar Structures', 80000, 2500, '18 days', 'BASIC',      0.65),
    ('SUP-5', 'Bharat Forgings Co',            'Rajkot',     'Forgings',         15000,  300, '15 days', 'UNVERIFIED', 0.00);

-- 2. Claims (10 VERIFIED + 2 SUBMITTED)
INSERT INTO claim (id, supplier_id, claim_type_key, label, pillar, asserted_value, status)
VALUES
    ('CLM-1',  'SUP-1', 'gst',           'GSTIN Active',                 'IDENTITY',   '33AABCA1234F1Z8', 'VERIFIED'),
    ('CLM-2',  'SUP-1', 'incorp',        'MCA Corporate Filing',         'LEGAL',      'CIN-U2899TN',     'VERIFIED'),
    ('CLM-3',  'SUP-1', 'iso_9001',      'ISO 9001 Quality',             'QUALITY',    'QA-98421',        'VERIFIED'),
    ('CLM-4',  'SUP-1', 'capacity_cap',  '50T/mo Cold Heading Capacity', 'CAPABILITY', '50000 kg/mo',     'VERIFIED'),
    ('CLM-5',  'SUP-2', 'gst',           'GSTIN Active',                 'IDENTITY',   '27AABCM9911K1Z1', 'VERIFIED'),
    ('CLM-6',  'SUP-2', 'incorp',        'Factory License',              'LEGAL',      'MH-FAC-441',      'VERIFIED'),
    ('CLM-7',  'SUP-2', 'quality_audit', 'AS9100 Audit',                 'QUALITY',    'AS9100D',         'VERIFIED'),
    ('CLM-8',  'SUP-2', 'iso_14001',     'ISO 14001 Environmental',      'COMPLIANCE', 'ENV-2026',        'SUBMITTED'),
    ('CLM-9',  'SUP-3', 'gst',           'GSTIN Active',                 'IDENTITY',   '24AABCK1102M1Z3', 'VERIFIED'),
    ('CLM-10', 'SUP-3', 'legal_reg',     'Trade License',                'LEGAL',      'GJ-2026-88',      'VERIFIED'),
    ('CLM-11', 'SUP-4', 'gst',           'GSTIN Active',                 'IDENTITY',   '08AABCS8821D1Z9', 'VERIFIED'),
    ('CLM-12', 'SUP-5', 'self_cap',      '15T Forging Claim',            'QUALITY',    'Self Declared',   'SUBMITTED');

-- 3. Evidence documents
INSERT INTO evidence (id, claim_id, title, kind, storage_ref)
VALUES
    ('EVD-1',  'CLM-1',  'GST Cert',       'PDF',         'gst.pdf'),
    ('EVD-2',  'CLM-2',  'COI Cert',       'PDF',         'coi.pdf'),
    ('EVD-3',  'CLM-3',  'ISO PDF',        'PDF',         'iso.pdf'),
    ('EVD-4',  'CLM-4',  'Factory Report', 'SITE_REPORT', 'site.pdf'),
    ('EVD-5',  'CLM-5',  'GST Cert',       'PDF',         'm_gst.pdf'),
    ('EVD-6',  'CLM-6',  'License PDF',    'PDF',         'm_lic.pdf'),
    ('EVD-7',  'CLM-7',  'Audit Report',   'SITE_REPORT', 'm_as.pdf'),
    ('EVD-8',  'CLM-8',  'ISO 14001 PDF',  'PDF',         'iso14001.pdf'),
    ('EVD-9',  'CLM-9',  'GST PDF',        'PDF',         'k_gst.pdf'),
    ('EVD-10', 'CLM-10', 'License PDF',    'PDF',         'k_lic.pdf'),
    ('EVD-11', 'CLM-11', 'GST PDF',        'PDF',         's_gst.pdf'),
    ('EVD-12', 'CLM-12', 'Brochure',       'PDF',         'b_brochure.pdf');

-- 4. Verification checks
INSERT INTO verification_check (id, claim_id, method, outcome, performed_by, rationale)
VALUES
    ('CHK-1',  'CLM-1',  'THIRD_PARTY_API', 'PASS', 'API-Adapter',     'GST portal active'),
    ('CHK-2',  'CLM-2',  'DOCUMENT_REVIEW', 'PASS', 'Verifier-01',     'MCA verified'),
    ('CHK-3',  'CLM-3',  'HUMAN_AUDIT',     'PASS', 'Auditor-01',      'ISO audit passed'),
    ('CHK-4',  'CLM-4',  'SITE_VISIT',      'PASS', 'Auditor-Chennai', 'Inspected 4 headers'),
    ('CHK-5',  'CLM-5',  'THIRD_PARTY_API', 'PASS', 'API-Adapter',     'GST active'),
    ('CHK-6',  'CLM-6',  'HUMAN_AUDIT',     'PASS', 'Auditor-02',      'Factory license valid'),
    ('CHK-7',  'CLM-7',  'SITE_VISIT',      'PASS', 'Auditor-Pune',    'On-site audit passed'),
    ('CHK-8',  'CLM-9',  'DOCUMENT_REVIEW', 'PASS', 'Verifier-01',     'GST verified'),
    ('CHK-9',  'CLM-10', 'THIRD_PARTY_API', 'PASS', 'Verifier-01',     'Trade registry matched'),
    ('CHK-10', 'CLM-11', 'DOCUMENT_REVIEW', 'PASS', 'Verifier-01',     'GST valid');

-- 5. Attestations
INSERT INTO attestation (id, claim_id, source_check_id, method, verified_at, valid_until, state, issued_by)
VALUES
    ('ATT-1',  'CLM-1',  'CHK-1',  'THIRD_PARTY_API', NOW(), DATE_ADD(NOW(), INTERVAL 180 DAY),   'ACTIVE', 'API-Adapter'),
    ('ATT-2',  'CLM-2',  'CHK-2',  'DOCUMENT_REVIEW', NOW(), DATE_ADD(NOW(), INTERVAL 180 DAY),   'ACTIVE', 'Verifier-01'),
    ('ATT-3',  'CLM-3',  'CHK-3',  'HUMAN_AUDIT',     NOW(), DATE_ADD(NOW(), INTERVAL 180 DAY),   'ACTIVE', 'Auditor-01'),
    ('ATT-4',  'CLM-4',  'CHK-4',  'SITE_VISIT',      NOW(), DATE_ADD(NOW(), INTERVAL 35 SECOND), 'ACTIVE', 'Auditor-Chennai'),
    ('ATT-5',  'CLM-5',  'CHK-5',  'THIRD_PARTY_API', NOW(), DATE_ADD(NOW(), INTERVAL 180 DAY),   'ACTIVE', 'API-Adapter'),
    ('ATT-6',  'CLM-6',  'CHK-6',  'HUMAN_AUDIT',     NOW(), DATE_ADD(NOW(), INTERVAL 180 DAY),   'ACTIVE', 'Auditor-02'),
    ('ATT-7',  'CLM-7',  'CHK-7',  'SITE_VISIT',      NOW(), DATE_ADD(NOW(), INTERVAL 180 DAY),   'ACTIVE', 'Auditor-Pune'),
    ('ATT-8',  'CLM-9',  'CHK-8',  'DOCUMENT_REVIEW', NOW(), DATE_ADD(NOW(), INTERVAL 180 DAY),   'ACTIVE', 'Verifier-01'),
    ('ATT-9',  'CLM-10', 'CHK-9',  'THIRD_PARTY_API', NOW(), DATE_ADD(NOW(), INTERVAL 180 DAY),   'ACTIVE', 'Verifier-01'),
    ('ATT-10', 'CLM-11', 'CHK-10', 'DOCUMENT_REVIEW', NOW(), DATE_ADD(NOW(), INTERVAL 180 DAY),   'ACTIVE', 'Verifier-01');
