# Supify — Database (MySQL 8.0)

MySQL relational schema, canonical seed fixtures, projection views, stored procedures, and append-only ledger triggers for the Supify verification platform.

---

## 1. Folder Structure

```text
db/
├── schema/
│   └── 01_supify_schema.sql           # Database creation, 9 InnoDB tables, foreign keys, indexes
├── seeds/
│   └── 01_canonical_suppliers.sql     # Idempotent canonical suppliers, claims, evidence, checks, attestations
├── procedures/
│   └── 01_views_and_procedures.sql    # Views, append-only verification_check triggers, decay procedure
├── migrations/                        # Future incremental ALTER TABLE scripts
├── setup_mysql.bat                    # One-click Windows initializer prompting cleanly for password
└── README.md
```

---

## 2. Schema Overview (9 Normalized Tables)

| Table | Role | Key Integrity Rule |
| :--- | :--- | :--- |
| `organisation` | Root legal entity (`legal_name`, `entity_type`, location) | Shared identity for buyers and suppliers |
| `supplier_profile` | Capability metrics (`category`, capacity, MOQ, lead time) & trust standing | Unique 1:1 extension of `organisation` |
| `claim` | Layer 1 supplier assertions keyed by data-driven `claim_type_key` | Never carries boolean verification state |
| `evidence` | Layer 2 evidentiary proof (`storage_ref`, `kind`, `title`, expiry) | Linked via `claim_id` FK |
| `verification_check` | Layer 3 immutable verification events (`method`, `outcome`, `rationale`) | Protected by `BEFORE UPDATE` & `BEFORE DELETE` triggers |
| `attestation` | Layer 4 time-bounded standing position (`method_achieved`, `valid_until`, `state`) | Decays to `EXPIRED` when `valid_until < CURRENT_TIMESTAMP` |
| `verification_task` | Operational verifier queue ordered by `priority DESC`, `sla_due_at ASC` | Tracks single vs `requires_dual` review SLA |
| `trust_snapshot` | Derived multi-pillar trust summary (`trust_band`, `overall_score`, `pillar_scores_json`) | Rebuildable from `attestation` records |
| `audit_event` | Strictly increasing audit ledger (`sequence_number AUTO_INCREMENT`) | Records consequential platform actions |

---

## 3. Views, Stored Procedures & Triggers

- **`vw_supplier_catalog`**: Joins `organisation`, `supplier_profile`, and active non-expired attestation counts.
- **`vw_verifier_queue`**: Joins pending `verification_task` rows with `claim` and attached evidence counts, sorted by priority and SLA deadline.
- **`trg_verification_check_no_update` / `trg_verification_check_no_delete`**: Enforces append-only immutability on `verification_check` (Axiom A5) via `SQLSTATE '45000'`.
- **`sp_expire_stale_attestations()`**: Batch-expires `ACTIVE` attestations past `valid_until` and logs a summary row into `audit_event`.

---

## 4. Setup & Backend JDBC Connection

### Initialize Local MySQL 8.0 Database
Run `setup_mysql.bat` from `db\` (or double-click in Explorer). It locates `mysql.exe`, prompts for your MySQL username (default `root`) and password once, and loads `schema/`, `seeds/`, and `procedures/` cleanly.

### Configure Backend Credentials
`DatabaseConfig` reads JVM properties or environment variables before falling back to local defaults:

| Setting | JVM Property | Environment Variable | Default |
| :--- | :--- | :--- | :--- |
| JDBC URL | `-Dsupify.db.url=...` | `SUPIFY_DB_URL` | `jdbc:mysql://localhost:3306/supify?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC` |
| DB User | `-Dsupify.db.user=...` | `SUPIFY_DB_USER` | `root` |
| DB Password | `-Dsupify.db.pass=...` | `SUPIFY_DB_PASS` | `root` |

Place `mysql-connector-j-*.jar` in `backend/lib/` (ignored by git via `.gitignore`). Both `backend/run.bat` and `backend/test.bat` load `-cp "bin;lib\*"` automatically, and **System & I/O → `[6] Database (JDBC) Status`** reports driver availability and live connectivity.

See [`docs/database-architecture.md`](../docs/database-architecture.md) for the full 9-table ER diagram and JDBC execution flow, [`docs/setup.md`](../docs/setup.md) for step-by-step installation, and [`docs/understanding.md`](../docs/understanding.md) §3 for the conceptual domain model.
