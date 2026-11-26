# Supify — Java Backend Architecture

Architecture specification for the pure-Java CLI backend under `backend/src/com/supify/` (**4 packages**: `model`, `dao`, `service`, `app`).

---

## 1. 4-Package Layered Architecture

```mermaid
flowchart TD
    APP["com.supify.app<br/>Main (Interactive 4-Role CLI)"]
    SVC["com.supify.service<br/>SupifyPlatform · DecayMonitorThread · SupifyException"]
    DAO["com.supify.dao<br/>Repository&lt;T, ID&gt; · FileHelper · JdbcSupplierDao"]
    MODEL["com.supify.model<br/>Supplier · Claim · Evidence · VerificationCheck<br/>Attestation · TrustBand · VerificationMethod"]
    FS[("Local File I/O<br/>data/suppliers.csv &amp; data/audit.log")]
    DB[("MySQL 8.0 (supify)<br/>5 InnoDB Tables via JDBC :3306")]

    APP --> SVC
    APP --> DAO
    APP --> MODEL
    SVC --> DAO
    SVC --> MODEL
    DAO --> MODEL
    DAO --> FS
    DAO --> DB
```

| Package | Classes | Responsibility |
| :--- | :--- | :--- |
| `com.supify.model` | `TrustBand`, `VerificationMethod`, `Supplier`, `Claim`, `Evidence`, `VerificationCheck`, `Attestation` | 4-layer verification spine domain entities & weighted enums |
| `com.supify.dao` | `Repository<T, ID>`, `FileHelper`, `JdbcSupplierDao` | Generic CRUD interface, buffered CSV/audit File I/O, MySQL 8.0 JDBC CRUD |
| `com.supify.service` | `SupifyException`, `SupifyPlatform`, `DecayMonitorThread` | Custom checked exception, shared synchronized platform (storage, Stream search, trust rules), background daemon |
| `com.supify.app` | `Main` | Interactive console entry point (`Buyer`, `Supplier`, `Verifier`, `Admin` menus with flushed prompts) |

---

## 2. Reconstructed UML Class Diagram

![Supify UML Class Diagram](../albums/diagrams/supify_uml_class_diagram.png)

```mermaid
classDiagram
    direction TB

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
        +getId() String
        +getTrustBand() TrustBand
        +setTrustBand(TrustBand) void
        +getTrustScore() double
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
        +getClaimId() String
    }

    class VerificationCheck {
        <<final / immutable>>
        -final String id
        -final String claimId
        -final VerificationMethod method
        -final String outcome
        -final String verifierName
        -final LocalDateTime checkedAt
        -final String note
        +getOutcome() String
        +getMethod() VerificationMethod
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
        -Map~String,Supplier~ suppliers
        -Map~String,Claim~ claims
        -List~Evidence~ evidences
        -List~VerificationCheck~ checks
        -Map~String,Attestation~ attestations
        -List~String~ alerts
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
        -String USER$
        -String PASS$
        +getUrl()$ String
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
        +exportToCsv(File file, List~Supplier~ suppliers)$ void
        +importFromCsv(File file)$ List~Supplier~
        +appendAuditLog(File file, String entry)$ void
        +readAuditLog(File file)$ List~String~
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
        -buyerMenu(Scanner sc, SupifyPlatform p)$ void
        -supplierMenu(Scanner sc, SupifyPlatform p)$ void
        -verifierMenu(Scanner sc, SupifyPlatform p)$ void
        -adminMenu(Scanner sc, SupifyPlatform p, DecayMonitorThread m)$ void
        -readInt(Scanner sc, String prompt, int defaultVal)$ int
    }

    Supplier --> TrustBand : has standing
    VerificationCheck --> VerificationMethod : uses
    Attestation --> VerificationMethod : weighted by
    Supplier "1" o-- "0..*" Claim : submits
    Claim "1" o-- "0..*" Evidence : backed by
    Claim "1" o-- "0..*" VerificationCheck : audited by
    VerificationCheck "1" --> "0..1" Attestation : issues on PASS

    Repository <|.. SupifyPlatform : implements
    Repository <|.. JdbcSupplierDao : implements
    Thread <|-- DecayMonitorThread : extends
    Exception <|-- SupifyException : extends

    SupifyPlatform *-- Supplier : manages
    SupifyPlatform *-- Claim : manages
    SupifyPlatform *-- Evidence : manages
    SupifyPlatform *-- VerificationCheck : manages
    SupifyPlatform *-- Attestation : manages
    SupifyPlatform ..> SupifyException : throws
    DecayMonitorThread --> SupifyPlatform : polls every 4s

    Main ..> SupifyPlatform : orchestrates
    Main ..> DecayMonitorThread : starts / stops
    Main ..> FileHelper : CSV & audit I/O
    Main ..> JdbcSupplierDao : JDBC status
```

---

## 3. Verification Spine, Authority Split & Background Decay Sequence

```mermaid
sequenceDiagram
    participant S as Supplier (Main)
    participant V as Verifier (Main)
    participant SP as SupifyPlatform (synchronized)
    participant DM as DecayMonitorThread (daemon)

    Note over S,SP: Authority Split — Suppliers never write checks or attestations
    S->>SP: submitClaim(SUP-1, label, pillar, val, proof)
    SP-->>S: Claim(SUBMITTED) + Evidence saved

    V->>SP: verifyClaim(claimId, Verifier-01, SITE_VISIT, true, note, 30s)
    SP->>SP: Save immutable final VerificationCheck(PASS)
    SP->>SP: Issue Attestation(ACTIVE, validUntil = now + 30s)
    SP->>SP: Claim.setStatus(VERIFIED) & recalculateTrust(SUP-1)
    SP-->>V: Return VerificationCheck

    loop Every 4000ms while running
        DM->>SP: checkExpiredAttestations()
        alt validUntil passed on ACTIVE attestation
            SP->>SP: att.expireIfNeeded() -> EXPIRED
            SP->>SP: claim.setStatus(EXPIRED) & recalculateTrust(supplierId)
            SP->>SP: Queue alert in alerts list
        end
    end

    S->>SP: getAndClearAlerts()
    SP-->>S: Print expiry [ALERT] before next menu prompt
```

---

## 4. Concurrency, File I/O & Trust Calculation Rules

### Concurrency & Synchronization
- **Shared Monitor (`SupifyPlatform`):** All state mutations and snapshots (`nextId`, `save`, `findAll`, `submitClaim`, `verifyClaim`, `recalculateTrust`, `checkExpiredAttestations`, `getAndClearAlerts`) are `synchronized` on the shared `SupifyPlatform` instance, preventing race conditions between the `main` interactive thread and `DecayMonitor`.
- **Model-Level Synchronization & Volatile Signal:** `DecayMonitorThread` uses `setDaemon(true)` and `volatile boolean running = true` for immediate cross-thread shutdown (`stopMonitor()` sets `running = false` and calls `interrupt()`). Mutable status fields on `Attestation`, `Claim`, and `Supplier` also synchronize their state transitions (`expireIfNeeded()`, `revoke()`, `setStatus()`, `setTrustBand()`).
- **Non-Blocking Interactive Prompts (`Main.java`):** Menu loops print prompts and call `System.out.flush()` before reading from `Scanner`, returning `-2` on EOF so the CLI exits cleanly without blocking before showing prompts.

### Trust Calculation & Pillar Capping (`SupifyPlatform.recalculateTrust`)
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
- **CSV Import (`importFromCsv`):** Reads `data/suppliers.csv` via `BufferedReader(new FileReader(file))`, skips the header and blank lines, splits fields, and wraps parsing in a row-level `catch (IllegalArgumentException)` so malformed rows are skipped safely.
- **Append Audit Log (`appendAuditLog` / `readAuditLog`):** Uses `new FileWriter(file, true)` inside `BufferedWriter` to append timestamped audit lines to `data/audit.log` without overwriting earlier history.
