-- Supify MySQL 8.0 Schema (4-Layer Verification Spine)

CREATE DATABASE IF NOT EXISTS supify
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE supify;

-- 1. Root identity: an organisation can be a buyer, supplier, or both
CREATE TABLE IF NOT EXISTS organisation (
    id               CHAR(36)     NOT NULL,
    legal_name       VARCHAR(255) NOT NULL,
    trade_name       VARCHAR(255) NULL,
    entity_type      VARCHAR(50)  NOT NULL,
    country_code     CHAR(2)      NOT NULL DEFAULT 'IN',
    city             VARCHAR(100) NULL,
    state            VARCHAR(100) NULL,
    year_established SMALLINT     NULL,
    created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_org_legal_name (legal_name),
    INDEX idx_org_location (country_code, state, city)
) ENGINE=InnoDB;

-- 2. Supplier capability & trust standing summary (derived from active attestations)
CREATE TABLE IF NOT EXISTS supplier_profile (
    id                  CHAR(36)     NOT NULL,
    organisation_id     CHAR(36)     NOT NULL,
    headline            VARCHAR(500) NOT NULL,
    category            VARCHAR(150) NOT NULL,
    capacity_per_month  INT          NOT NULL DEFAULT 0,
    capacity_unit       VARCHAR(50)  NOT NULL DEFAULT 'units',
    moq                 INT          NOT NULL DEFAULT 1,
    lead_time           VARCHAR(80)  NOT NULL DEFAULT '14 days',
    listing_state       VARCHAR(32)  NOT NULL DEFAULT 'LISTED',
    trust_band          VARCHAR(32)  NOT NULL DEFAULT 'UNVERIFIED',
    overall_trust_score DOUBLE       NOT NULL DEFAULT 0.0,
    created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_profile_org (organisation_id),
    INDEX idx_profile_cat_band (category, trust_band),
    INDEX idx_profile_score (overall_trust_score),
    CONSTRAINT fk_profile_org
        FOREIGN KEY (organisation_id) REFERENCES organisation (id)
        ON DELETE CASCADE
) ENGINE=InnoDB;

-- 3. Layer 1: Claim assertions submitted by suppliers (type-driven via claim_type_key)
CREATE TABLE IF NOT EXISTS claim (
    id              CHAR(36)     NOT NULL,
    organisation_id CHAR(36)     NOT NULL,
    claim_type_key  VARCHAR(100) NOT NULL,
    label           VARCHAR(255) NOT NULL,
    pillar          VARCHAR(40)  NOT NULL,
    asserted_value  VARCHAR(500) NULL,
    status          VARCHAR(32)  NOT NULL DEFAULT 'DRAFT',
    submitted_at    TIMESTAMP    NULL DEFAULT NULL,
    superseded_by   CHAR(36)     NULL DEFAULT NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_claim_org_status (organisation_id, status),
    INDEX idx_claim_type_pillar (claim_type_key, pillar),
    CONSTRAINT fk_claim_org
        FOREIGN KEY (organisation_id) REFERENCES organisation (id)
        ON DELETE CASCADE
) ENGINE=InnoDB;

-- 4. Layer 2: Evidentiary artefacts backing a claim
CREATE TABLE IF NOT EXISTS evidence (
    id          CHAR(36)     NOT NULL,
    claim_id    CHAR(36)     NOT NULL,
    storage_ref VARCHAR(500) NOT NULL,
    kind        VARCHAR(40)  NOT NULL,
    title       VARCHAR(255) NOT NULL,
    issued_at   TIMESTAMP    NULL DEFAULT NULL,
    expires_at  TIMESTAMP    NULL DEFAULT NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_evidence_claim (claim_id),
    CONSTRAINT fk_evidence_claim
        FOREIGN KEY (claim_id) REFERENCES claim (id)
        ON DELETE CASCADE
) ENGINE=InnoDB;

-- 5. Layer 3: Append-only verification checks performed by the platform/verifier
CREATE TABLE IF NOT EXISTS verification_check (
    id           CHAR(36)     NOT NULL,
    claim_id     CHAR(36)     NOT NULL,
    method       VARCHAR(40)  NOT NULL,
    outcome      VARCHAR(40)  NOT NULL,
    confidence   DOUBLE       NOT NULL DEFAULT 0.0,
    performed_by VARCHAR(150) NOT NULL,
    performed_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    rationale    TEXT         NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_check_claim (claim_id),
    CONSTRAINT fk_check_claim
        FOREIGN KEY (claim_id) REFERENCES claim (id)
        ON DELETE CASCADE
) ENGINE=InnoDB;

-- 6. Layer 4: Time-bounded platform attestation derived from a check
CREATE TABLE IF NOT EXISTS attestation (
    id                CHAR(36)     NOT NULL,
    claim_id          CHAR(36)     NOT NULL,
    source_check_id   CHAR(36)     NOT NULL,
    method_achieved   VARCHAR(40)  NOT NULL,
    verified_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    valid_until       TIMESTAMP    NULL DEFAULT NULL,
    state             VARCHAR(32)  NOT NULL DEFAULT 'ACTIVE',
    revocation_reason VARCHAR(500) NULL DEFAULT NULL,
    issued_by         VARCHAR(150) NOT NULL,
    created_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_att_claim_state_valid (claim_id, state, valid_until),
    CONSTRAINT fk_att_claim
        FOREIGN KEY (claim_id) REFERENCES claim (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_att_check
        FOREIGN KEY (source_check_id) REFERENCES verification_check (id)
        ON DELETE RESTRICT
) ENGINE=InnoDB;

-- 7. Operational verifier queue ordered by priority and SLA deadline
CREATE TABLE IF NOT EXISTS verification_task (
    id              CHAR(36)     NOT NULL,
    claim_id        CHAR(36)     NOT NULL,
    organisation_id CHAR(36)     NOT NULL,
    supplier_name   VARCHAR(255) NOT NULL,
    claim_label     VARCHAR(255) NOT NULL,
    state           VARCHAR(32)  NOT NULL DEFAULT 'QUEUED',
    assigned_to     VARCHAR(150) NULL DEFAULT NULL,
    priority        INT          NOT NULL DEFAULT 50,
    requires_dual   BOOLEAN      NOT NULL DEFAULT FALSE,
    sla_due_at      TIMESTAMP    NOT NULL,
    claimed_at      TIMESTAMP    NULL DEFAULT NULL,
    decided_at      TIMESTAMP    NULL DEFAULT NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_task_queue (state, priority, sla_due_at),
    CONSTRAINT fk_task_claim
        FOREIGN KEY (claim_id) REFERENCES claim (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_task_org
        FOREIGN KEY (organisation_id) REFERENCES organisation (id)
        ON DELETE CASCADE
) ENGINE=InnoDB;

-- 8. Computed multi-pillar trust snapshot per supplier organisation
CREATE TABLE IF NOT EXISTS trust_snapshot (
    organisation_id    CHAR(36)    NOT NULL,
    trust_band         VARCHAR(32) NOT NULL,
    overall_score      DOUBLE      NOT NULL,
    pillar_scores_json TEXT        NOT NULL,
    computed_at        TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (organisation_id),
    CONSTRAINT fk_snapshot_org
        FOREIGN KEY (organisation_id) REFERENCES organisation (id)
        ON DELETE CASCADE
) ENGINE=InnoDB;

-- 9. Monotonic append-only audit log
CREATE TABLE IF NOT EXISTS audit_event (
    sequence_number BIGINT       NOT NULL AUTO_INCREMENT,
    event_timestamp TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actor           VARCHAR(150) NOT NULL,
    action          VARCHAR(100) NOT NULL,
    subject_type    VARCHAR(100) NOT NULL,
    subject_id      VARCHAR(100) NOT NULL,
    details         TEXT         NULL,
    PRIMARY KEY (sequence_number),
    INDEX idx_audit_subject (subject_type, subject_id),
    INDEX idx_audit_time (event_timestamp)
) ENGINE=InnoDB AUTO_INCREMENT=1000;
