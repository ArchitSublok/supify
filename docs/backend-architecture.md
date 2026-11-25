# Supify — Java Backend Architecture

Architecture summary for the 15-file pure-Java backend under `backend/src/com/supify/` (**4 packages**: `model`, `dao`, `service`, `app`).

---

## 1. 4-Package Architecture

```mermaid
flowchart TD
    APP["app (Main, TestApp)"] --> SVC["service (SupifyPlatform, DecayMonitorThread, SupifyException)"]
    APP --> DAO["dao (Repository, FileHelper, JdbcSupplierDao)"]
    APP --> MODEL["model (7 Spine Entities & Enums)"]

    SVC --> DAO
    SVC --> MODEL
    DAO --> MODEL
```

| Package | Files | Role |
| :--- | :--- | :--- |
| `com.supify.model` | `TrustBand`, `VerificationMethod`, `Supplier`, `Claim`, `Evidence`, `VerificationCheck`, `Attestation` | 4-layer verification spine & enums |
| `com.supify.dao` | `Repository<T, ID>`, `FileHelper`, `JdbcSupplierDao` | Generic CRUD contract, CSV/log File I/O, MySQL JDBC CRUD |
| `com.supify.service` | `SupifyException`, `SupifyPlatform`, `DecayMonitorThread` | Custom checked exception, shared synchronized platform (storage, Stream search, trust rules), daemon thread |
| `com.supify.app` | `Main`, `TestApp` | Interactive console menus and 8 automated checks |

---

## 2. Complete UML Class Diagram (15 Files)

```mermaid
classDiagram
    class TrustBand {
        <<enumeration>>
        UNVERIFIED(0)
        BASIC(1)
        VERIFIED(2)
        AUDITED(3)
        -int level
        +getLevel() int
    }

    class VerificationMethod {
        <<enumeration>>
        SELF_DECLARED(0.25)
        DOCUMENT_REVIEW(0.60)
        THIRD_PARTY_API(0.85)
        HUMAN_AUDIT(0.95)
        SITE_VISIT(1.00)
        -double score
        +getScore() double
    }

    class Supplier {
        -String id
        -String name
        -String city
        -String category
        -int capacityPerMonth
        -int moq
        -String leadTime
        -TrustBand trustBand
        -double trustScore
        +setTrustBand(TrustBand) void
        +setTrustScore(double) void
    }

    class Claim {
        -String id
        -String supplierId
        -String claimTypeKey
        -String label
        -String pillar
        -String value
        -String status
        +getStatus() String
        +setStatus(String) void
    }

    class Evidence {
        -String id
        -String claimId
        -String title
        -String docType
        -String filePath
    }

    class VerificationCheck {
        <<final>>
        -String id
        -String claimId
        -VerificationMethod method
        -String outcome
        -String verifierName
        -LocalDateTime checkedAt
        -String note
    }

    class Attestation {
        -String id
        -String claimId
        -String checkId
        -VerificationMethod method
        -LocalDateTime verifiedAt
        -LocalDateTime validUntil
        -String state
        +isExpired() boolean
        +expireIfNeeded() boolean
        +revoke() void
        +secondsLeft() long
    }

    class Repository~T,ID~ {
        <<interface>>
        +save(T item) T
        +findById(ID id) T
        +findAll() List~T~
        +deleteById(ID id) boolean
        +count() long
    }

    class SupifyPlatform {
        -Map suppliers
        -Map claims
        -List evidences
        -List checks
        -Map attestations
        -List alerts
        -int idCounter
        +nextId(String prefix) String
        +loadSampleData() void
        +search(String keyword, TrustBand minBand, String sortBy) List~Supplier~
        +getAllCategories() List~String~
        +submitClaim(...) Claim
        +verifyClaim(...) VerificationCheck
        +recalculateTrust(String supplierId) void
        +checkExpiredAttestations() void
        +getAndClearAlerts() List~String~
    }

    class JdbcSupplierDao {
        -String DRIVER$
        -String URL$
        +isDriverLoaded()$ boolean
        +getConnection()$ Connection
        +testConnection()$ boolean
        +save(Supplier s) Supplier
        +findById(String id) Supplier
        +findAll() List~Supplier~
        +updateTrust(String id, TrustBand band, double score) boolean
        +deleteById(String id) boolean
        +count() long
    }

    class FileHelper {
        +exportToCsv(File, List)$ void
        +importFromCsv(File)$ List~Supplier~
        +appendAuditLog(File, String)$ void
        +readAuditLog(File)$ List~String~
    }

    class DecayMonitorThread {
        -SupifyPlatform platform
        -int delayMs
        -volatile boolean running
        +run() void
        +stopMonitor() void
        +printThreadInfo() void
    }

    class SupifyException {
        +SupifyException(String message)
        +SupifyException(String message, Throwable cause)
    }

    class Main {
        +main(String[] args)$ void
    }

    class TestApp {
        +main(String[] args)$ void
    }

    Supplier --> TrustBand
    VerificationCheck --> VerificationMethod
    Attestation --> VerificationMethod
    Supplier "1" o-- "0..*" Claim : submits
    Claim "1" o-- "0..*" Evidence : backed by
    Claim "1" o-- "0..*" VerificationCheck : audited by
    VerificationCheck "1" --> "0..1" Attestation : issues on PASS
    Repository <|.. SupifyPlatform
    Repository <|.. JdbcSupplierDao
    Thread <|-- DecayMonitorThread
    Exception <|-- SupifyException
    SupifyPlatform --> Supplier
    SupifyPlatform --> Claim
    SupifyPlatform --> Evidence
    SupifyPlatform --> VerificationCheck
    SupifyPlatform --> Attestation
    SupifyPlatform ..> SupifyException
    DecayMonitorThread --> SupifyPlatform
    Main ..> SupifyPlatform
    Main ..> DecayMonitorThread
    Main ..> FileHelper
    Main ..> JdbcSupplierDao
    TestApp ..> SupifyPlatform
    TestApp ..> FileHelper
    TestApp ..> JdbcSupplierDao
```

---

## 3. Verification Spine, Authority Split & Background Decay Sequence

```mermaid
sequenceDiagram
    participant S as Supplier / Main
    participant V as Verifier / Main
    participant SP as SupifyPlatform (synchronized)
    participant DM as DecayMonitorThread (daemon)

    Note over S,SP: Write Authority Split - Suppliers never write checks or attestations
    S->>SP: submitClaim(SUP-1, label, pillar, val, proof)
    SP-->>S: Claim(SUBMITTED) + Evidence saved

    V->>SP: verifyClaim(claimId, Verifier-01, SITE_VISIT, true, note, 30s)
    SP->>SP: Save immutable final VerificationCheck(PASS)
    SP->>SP: Issue Attestation(ACTIVE, validUntil = now + 30s)
    SP->>SP: Claim.setStatus(VERIFIED) and recalculateTrust(SUP-1)
    SP-->>V: Return VerificationCheck

    loop Every 4000ms while running
        DM->>SP: checkExpiredAttestations()
        alt validUntil passed on ACTIVE attestation
            SP->>SP: att.expireIfNeeded() -> EXPIRED
            SP->>SP: claim.setStatus(EXPIRED) and recalculateTrust(supplierId)
            SP->>SP: Queue alert in alerts list
        end
    end

    S->>SP: getAndClearAlerts()
    SP-->>S: Print expiry [ALERT] at top of next menu loop
```

---

## 4. Concurrency, File I/O & Trust Calculation Rules

### Concurrency & Synchronization
- **Shared Monitor (`SupifyPlatform`):** All state mutations and snapshots (`nextId`, `save`, `findAll`, `submitClaim`, `verifyClaim`, `recalculateTrust`, `checkExpiredAttestations`, `getAndClearAlerts`) are `synchronized` on the shared `SupifyPlatform` instance, preventing race conditions between the `main` interactive thread and the `DecayMonitor` thread.
- **Model-Level Synchronization & Volatile Signal:** `DecayMonitorThread` uses `setDaemon(true)` and `volatile boolean running = true` for immediate cross-thread stop visibility (`stopMonitor()` sets `running = false` and calls `interrupt()`). Mutable status fields on `Attestation`, `Claim`, and `Supplier` also synchronize their state transitions (`expireIfNeeded()`, `revoke()`, `setStatus()`, `setTrustBand()`).

### Trust Calculation & Capping (`SupifyPlatform.recalculateTrust`)
Only **`ACTIVE`** attestations contribute to trust:

```text
avgScore = min(1.0, sum(method.getScore()) / max(3, activeCount))
```

| Active Pillar / Audit Condition | Resulting `TrustBand` | Score Rule |
| :--- | :--- | :--- |
| Missing active `IDENTITY` attestation | `UNVERIFIED` | Capped at `<= 0.30` |
| Active `IDENTITY`, missing active `LEGAL` | `BASIC` | Capped at `<= 0.65` |
| Active `IDENTITY` + `LEGAL`, plus `HUMAN_AUDIT`/`SITE_VISIT` and `avgScore >= 0.85` | `AUDITED` | Computed `avgScore` |
| Active `IDENTITY` + `LEGAL` (otherwise) | `VERIFIED` | Computed `avgScore` |

### Character File I/O (`FileHelper`)
- **CSV Export (`exportToCsv`):** Writes `List<Supplier>` via `BufferedWriter(new FileWriter(file))` inside `try-with-resources`, sanitizing commas (`clean()`) so fields stay column-aligned.
- **CSV Import (`importFromCsv`):** Reads `data/suppliers.csv` via `BufferedReader(new FileReader(file))`, skips the header and blank lines, splits fields, and wraps `Integer.parseInt` / `Double.parseDouble` / `TrustBand.valueOf` in a row-level `catch (IllegalArgumentException)` so bad lines are skipped gracefully.
- **Append Audit Log (`appendAuditLog` / `readAuditLog`):** Uses `new FileWriter(file, true)` inside `BufferedWriter` to append timestamped audit lines to `data/audit.log` without overwriting earlier history.
