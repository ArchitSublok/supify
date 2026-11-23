-- Supify Views, Stored Procedures, and Append-Only Integrity Triggers

USE supify;

-- 1. Catalog projection view: joins organisation, profile, and active attestation counts
CREATE OR REPLACE VIEW vw_supplier_catalog AS
SELECT
    o.id                    AS organisation_id,
    o.legal_name,
    o.trade_name,
    o.city,
    o.state,
    o.country_code,
    sp.category,
    sp.capacity_per_month,
    sp.capacity_unit,
    sp.moq,
    sp.lead_time,
    sp.listing_state,
    sp.trust_band,
    sp.overall_trust_score,
    COUNT(a.id)             AS active_attestations
FROM organisation o
JOIN supplier_profile sp
    ON sp.organisation_id = o.id
LEFT JOIN claim c
    ON c.organisation_id = o.id
LEFT JOIN attestation a
    ON a.claim_id = c.id
   AND a.state = 'ACTIVE'
   AND (a.valid_until IS NULL OR a.valid_until >= CURRENT_TIMESTAMP)
GROUP BY
    o.id,
    o.legal_name,
    o.trade_name,
    o.city,
    o.state,
    o.country_code,
    sp.category,
    sp.capacity_per_month,
    sp.capacity_unit,
    sp.moq,
    sp.lead_time,
    sp.listing_state,
    sp.trust_band,
    sp.overall_trust_score;

-- 2. Pending verifier queue view joined with claim and evidence counts
CREATE OR REPLACE VIEW vw_verifier_queue AS
SELECT
    t.id               AS task_id,
    t.supplier_name,
    t.claim_label,
    t.state            AS task_state,
    t.priority,
    t.requires_dual,
    t.sla_due_at,
    c.pillar,
    c.asserted_value,
    c.status           AS claim_status,
    COUNT(e.id)        AS evidence_count
FROM verification_task t
JOIN claim c
    ON c.id = t.claim_id
LEFT JOIN evidence e
    ON e.claim_id = c.id
WHERE t.state IN ('QUEUED', 'CLAIMED', 'ESCALATED')
GROUP BY
    t.id,
    t.supplier_name,
    t.claim_label,
    t.state,
    t.priority,
    t.requires_dual,
    t.sla_due_at,
    c.pillar,
    c.asserted_value,
    c.status
ORDER BY t.priority DESC, t.sla_due_at ASC;

-- 3. Append-only immutability triggers on verification_check (Axiom A5)
DROP TRIGGER IF EXISTS trg_verification_check_no_update;
DROP TRIGGER IF EXISTS trg_verification_check_no_delete;
DROP PROCEDURE IF EXISTS sp_expire_stale_attestations;

DELIMITER //

CREATE TRIGGER trg_verification_check_no_update
BEFORE UPDATE ON verification_check
FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'verification_check is append-only and cannot be modified';
END//

CREATE TRIGGER trg_verification_check_no_delete
BEFORE DELETE ON verification_check
FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'verification_check is append-only and cannot be deleted';
END//

-- 4. Expire aged ACTIVE attestations and log summary in audit_event
CREATE PROCEDURE sp_expire_stale_attestations()
BEGIN
    DECLARE v_expired INT DEFAULT 0;

    UPDATE attestation
       SET state = 'EXPIRED',
           updated_at = CURRENT_TIMESTAMP
     WHERE state = 'ACTIVE'
       AND valid_until IS NOT NULL
       AND valid_until < CURRENT_TIMESTAMP;

    SET v_expired = ROW_COUNT();

    IF v_expired > 0 THEN
        INSERT INTO audit_event (actor, action, subject_type, subject_id, details)
        VALUES (
            'SYSTEM_DECAY_PROC',
            'attestation.batch_expired',
            'Attestation',
            'BATCH',
            CONCAT('Marked ', v_expired, ' expired attestation(s) as EXPIRED.')
        );
    END IF;
END//

DELIMITER ;
