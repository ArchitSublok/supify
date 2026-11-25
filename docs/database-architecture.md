# Supify — MySQL 8.0 ER Diagram & JDBC Data-Flow Architecture

Relational schema specification and JDBC connectivity guide for [`db/`](../db/README.md) and [`backend/`](../backend/README.md).  
For domain axioms and conceptual modeling, see [`understanding.md`](understanding.md); for local setup commands, see [`setup.md`](setup.md).

---

## 1. Entity–Relationship Diagram (9 Normalized InnoDB Tables)

Matches [`db/schema/01_supify_schema.sql`](../db/schema/01_supify_schema.sql) verbatim. Write authority crosses once along the verification spine: **Supplier** writes `claim` + `evidence`; **Platform / Verifier** writes immutable `verification_check` + time-bounded `attestation`.

```mermaid
erDiagram
    organisation ||--o| supplier_profile : "extends"
    organisation ||--o{ claim : "asserts"
    organisation ||--o| trust_snapshot : "cached score"
    organisation ||--o{ verification_task : "queued for"

    claim ||--o{ evidence : "supported by"
    claim ||--o{ verification_check : "audited by"
    claim ||--o{ attestation : "attested by"
    claim ||--o{ verification_task : "scheduled as"

    verification_check ||--o{ attestation : "justifies"

    organisation {
        char id PK
        varchar legal_name
        varchar trade_name
        varchar entity_type
        char country_code
        varchar city
        varchar state
        smallint year_established
        timestamp created_at
        timestamp updated_at
    }

    supplier_profile {
        char id PK
        char organisation_id UK,FK
        varchar headline
        varchar category
        int capacity_per_month
        varchar capacity_unit
        int moq
        varchar lead_time
        varchar listing_state
        varchar trust_band
        double overall_trust_score
        timestamp created_at
        timestamp updated_at
    }

    claim {
        char id PK
        char organisation_id FK
        varchar claim_type_key
        varchar label
        varchar pillar
        varchar asserted_value
        varchar status
        timestamp submitted_at
        char superseded_by
        timestamp created_at
        timestamp updated_at
    }

    evidence {
        char id PK
        char claim_id FK
        varchar storage_ref
        varchar kind
        varchar title
        timestamp issued_at
        timestamp expires_at
        timestamp created_at
        timestamp updated_at
    }

    verification_check {
        char id PK
        char claim_id FK
        varchar method
        varchar outcome
        double confidence
        varchar performed_by
        timestamp performed_at
        text rationale
    }

    attestation {
        char id PK
        char claim_id FK
        char source_check_id FK
        varchar method_achieved
        timestamp verified_at
        timestamp valid_until
        varchar state
        varchar revocation_reason
        varchar issued_by
        timestamp created_at
        timestamp updated_at
    }

    verification_task {
        char id PK
        char claim_id FK
        char organisation_id FK
        varchar supplier_name
        varchar claim_label
        varchar state
        varchar assigned_to
        int priority
        boolean requires_dual
        timestamp sla_due_at
        timestamp claimed_at
        timestamp decided_at
        timestamp created_at
        timestamp updated_at
    }

    trust_snapshot {
        char organisation_id PK,FK
        varchar trust_band
        double overall_score
        text pillar_scores_json
        timestamp computed_at
    }

    audit_event {
        bigint sequence_number PK
        timestamp event_timestamp
        varchar actor
        varchar action
        varchar subject_type
        varchar subject_id
        text details
    }
```

---

## 2. JDBC Execution & MySQL Enforcement Flow

```mermaid
flowchart LR
    CLI["Main (Database Connection Status)"] --> CFG["DatabaseConfig (SUPIFY_DB_* / -Dsupify.db.*)"]
    DAO["JdbcSupplierDao (PreparedStatement + ResultSet)"] --> CFG
    CFG -->|"Class.forName + DriverManager"| DRV["mysql-connector-j (*.jar in backend/lib)"]

    DRV -->|"JDBC over TCP 3306"| DB["MySQL 8.0 Database (supify)"]

    subgraph DBObjects["MySQL Tables, Views, Procedures & Triggers"]
        direction TB
        TBL["9 InnoDB Tables (organisation .. audit_event)"]
        VW["Views (vw_supplier_catalog, vw_verifier_queue)"]
        TRG["Triggers (trg_verification_check_no_update / no_delete)"]
        SP["Procedure (sp_expire_stale_attestations)"]
        TBL --- VW
        TBL --- TRG
        TBL --- SP
    end

    DB --> DBObjects
```

### Active JDBC Capabilities (`backend/src/com/supify/dao/`)

- **`DatabaseConfig`**: Resolves `-Dsupify.db.*` JVM properties, then `SUPIFY_DB_*` environment variables, then local defaults (`jdbc:mysql://localhost:3306/supify?...`). Exposes `isDriverAvailable()`, `getConnection()`, `testConnection()`, `getUrl()`, `getUser()`, and `getLastError()`.
- **`JdbcSupplierDao`**: Implements `Repository<Supplier, String>` using parameterized `PreparedStatement` queries (`executeQuery()` / `executeUpdate()`) and `ResultSet` cursor row mapping (`mapRow`) across `organisation` and `supplier_profile` (`save`, `findById`, `findAll`, `findMatching`, `updateTrust`, `deleteById`, `count`).

---

## 3. Axiom-to-Schema Traceability

Every SQL table and constraint in [`db/`](../db/README.md) enforces the trust axioms from [`understanding.md`](understanding.md) §2:

| Axiom | Core Rule | MySQL Enforcement in `db/` |
| :--- | :--- | :--- |
| **A1** (Explainable Trust) | Scores derive from inspectable records | `trust_snapshot.pillar_scores_json` summarizes `attestation` + `verification_check.rationale`; no opaque score without backing rows |
| **A2** (Method Matters) | Verification carries method & rigor | `verification_check.method` and `attestation.method_achieved` distinguish `SELF_DECLARED`, `DOCUMENT_REVIEW`, `THIRD_PARTY_API`, `HUMAN_AUDIT`, `SITE_VISIT` |
| **A3** (Time-Bounded Validity) | Trust decays on expiration | `attestation.valid_until` indexed with `state`; `sp_expire_stale_attestations()` marks aged rows `EXPIRED` and logs in `audit_event`; `vw_supplier_catalog` counts only non-expired `ACTIVE` rows |
| **A5** (Append-Only Ledger) | Checks are never mutated or deleted | `trg_verification_check_no_update` and `trg_verification_check_no_delete` raise `SQLSTATE '45000'`; `claim.superseded_by` records claim replacement history |
| **A6** (Claim vs Attestation Split) | Suppliers assert; platform attests | `claim` + `evidence` are separate tables from `verification_check` + `attestation`; `fk_att_check` (`ON DELETE RESTRICT`) prevents dangling attestations without a recorded check |
| **A10** (Claims as Data) | Claim types live in data, not code | `claim.claim_type_key` (`VARCHAR(100)`) and `claim.pillar` avoid hardcoded SQL `ENUM` switches per certification |

---

## 4. Planned Next Increment

1. Add a JDBC DAO covering the remaining verification spine tables (`claim`, `evidence`, `verification_check`, `attestation`) with atomic SQL transactions (`conn.setAutoCommit(false)`).
2. Wire an optional `--db` mode (or auto-switch when `DatabaseConfig.testConnection()` returns `true`) in `com.supify.app.Main`.
