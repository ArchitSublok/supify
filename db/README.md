# Supify — Database (MySQL)

MySQL relational schema for the Supify trust ledger and verification platform.

## Structure

```
db/
├── schema/       # CREATE TABLE statements and type definitions
├── migrations/   # Versioned ALTER TABLE / schema change scripts
├── seeds/        # Sample INSERT data for development and testing
├── procedures/   # Stored procedures, triggers, and views
└── README.md
```

## Engine

- **Database:** MySQL 8.0+
- **Schema:** 15 tables, 3NF normalized with append-only verification ledger
- **Key entities:** organisation, user, supplier_profile, claim, evidence, verification_check, attestation, trust_snapshot

See `docs/understanding.md` §3 for the full domain model and ER diagram.
