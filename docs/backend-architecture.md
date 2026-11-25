# Supify — Java Backend Architecture, UML Class Diagrams & Flowcharts

Technical architecture reference for the 20-file pure-Java backend under `backend/src/com/supify/`.  
See [`setup.md`](setup.md) to run locally, [`understanding.md`](understanding.md) for domain axioms, [`database-architecture.md`](database-architecture.md) for the MySQL 8.0 ER diagram, and [`backend/README.md`](../backend/README.md) for setup and execution instructions.

---

## 1. System Overview & Layered Package Architecture

The backend organizes terminal interaction (`app`), business workflows (`service`), data persistence (`dao`), background concurrency (`concurrency`), file I/O (`io`), and domain errors (`exception`) into 8 focused packages. Services and repositories expose clear contracts (`Repository<T, ID>`), allowing `InMemoryStore` and `JdbcSupplierDao` to share the same `Supplier` domain model.

```mermaid
flowchart TD
    APP["app (Main)"] --> SVC["service (SupplierService, VerificationService)"]
    APP --> DAO["dao (InMemoryStore, Repository, DatabaseConfig)"]
    APP --> CONC["concurrency (TaskBuffer, DecayMonitorThread)"]
    APP --> IO["io (CsvSupplierIO, AuditFileIO)"]
    APP --> EXC["exception (SupifyException)"]
    APP --> MODEL["model (Entities & Enums)"]

    SVC --> DAO
    SVC --> CONC
    SVC --> EXC
    SVC --> MODEL

    CONC --> DAO
    CONC --> SVC
    CONC --> MODEL

    IO --> MODEL
    DAO --> MODEL

    TEST["test (SupifyTestRunner)"] --> APP
    TEST --> SVC
    TEST --> DAO
    TEST --> CONC
    TEST --> IO
    TEST --> EXC
    TEST --> MODEL
```

### Package Inventory (`backend/src/com/supify/`)

| Package | Role | Classes |
| :--- | :--- | :--- |
| `app` | Interactive CLI menus (Buyer, Supplier, Verifier, System & Admin), seed data, shutdown hook | `Main` |
| `model` | 4-layer verification spine entities & enums | `Supplier`, `Claim`, `Evidence`, `VerificationCheck`, `Attestation`, `TrustBand`, `VerificationMethod` |
| `dao` | Generic repository contract, in-memory store, MySQL JDBC config & CRUD DAO | `Repository`, `InMemoryStore`, `DatabaseConfig`, `JdbcSupplierDao` |
| `service` | Stream search, trust calculation & capping, claim intake & decision orchestration | `SupplierService`, `VerificationService` |
| `concurrency` | Wait/notify task queue & background expiry monitor daemon | `TaskBuffer`, `DecayMonitorThread` |
| `io` | CSV character streams, byte-stream audit log & rule tokenizer | `CsvSupplierIO`, `AuditFileIO` |
| `exception` | Checked domain exception | `SupifyException` |
| `test` | Self-contained 10-group verification test suite | `SupifyTestRunner` |

---

## 2. Core Domain Model & 4-Layer Spine — UML Class Diagram

Supify models trust as a **4-layer evidence spine** (`Claim → Evidence → VerificationCheck → Attestation`) rather than a single boolean flag. `VerificationCheck` is a `final` immutable record with no setters, while `Attestation` carries explicit `validUntil` bounds that expire automatically over time.

```mermaid
classDiagram
    class TrustBand {
        <<enumeration>>
        UNVERIFIED
        BASIC
        VERIFIED
        AUDITED
        -int level
        +getLevel() int
    }

    class VerificationMethod {
        <<enumeration>>
        SELF_DECLARED
        DOCUMENT_REVIEW
        THIRD_PARTY_API
        HUMAN_AUDIT
        SITE_VISIT
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
        +setTrustBand(TrustBand trustBand) void
        +setTrustScore(double trustScore) void
        +toString() String
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
        +setStatus(String status) void
        +toString() String
    }

    class Evidence {
        -String id
        -String claimId
        -String title
        -String docType
        -String filePath
        +toString() String
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
        +toString() String
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

    Supplier "1" --> "0..*" Claim : asserts
    Claim "1" --> "0..*" Evidence : backed by
    Claim "1" --> "0..*" VerificationCheck : evaluated by
    Claim "1" --> "0..*" Attestation : yields
    VerificationCheck "1" --> "0..1" Attestation : justifies

    Supplier --> TrustBand
    VerificationCheck --> VerificationMethod
    Attestation --> VerificationMethod
```

---

## 3. Data Access, Services, Concurrency, I/O & Exceptions — UML Class Diagram

Generic interfaces (`Repository<T, ID>`), storage implementations (`InMemoryStore`, `JdbcSupplierDao`), business services (`SupplierService`, `VerificationService`), concurrency primitives (`TaskBuffer<T>`, `DecayMonitorThread`), file I/O (`CsvSupplierIO`, `AuditFileIO`), and checked exception (`SupifyException`).

```mermaid
classDiagram
    class Repository~T,ID~ {
        <<interface>>
        +save(T item) T
        +findById(ID id) Optional~T~
        +findAll() List~T~
        +findMatching(Predicate filter) List~T~
        +deleteById(ID id) boolean
        +count() long
        +mapList(List items, Function mapper)$ List
    }

    class InMemoryStore {
        -Map suppliers
        -Map claims
        -List evidences
        -List checks
        -Map attestations
        -int idCounter
        +nextId(String prefix) String
        +save(Supplier supplier) Supplier
        +findById(String id) Optional~Supplier~
        +findAll() List~Supplier~
        +saveClaim(Claim claim) void
        +saveEvidence(Evidence evidence) void
        +recordDecision(Claim claim, VerificationCheck check, Attestation attestation) void
        +getActiveAttestations() List~Attestation~
    }

    class JdbcSupplierDao {
        +save(Supplier s) Supplier
        +findById(String id) Optional~Supplier~
        +findAll() List~Supplier~
        +findMatching(Predicate filter) List~Supplier~
        +updateTrust(String supplierId, TrustBand band, double score) boolean
        +deleteById(String id) boolean
        +count() long
    }

    class DatabaseConfig {
        +getUrl()$ String
        +getUser()$ String
        +getLastError()$ String
        +isDriverAvailable()$ boolean
        +getConnection()$ Connection
        +testConnection()$ boolean
    }

    class SupplierService {
        -InMemoryStore store
        +search(String keyword, String category, TrustBand minBand, Integer minCapacity, String sortBy, int page, int pageSize) List
        +getAllCategories() List~String~
        +recalculateTrust(String supplierId) void
    }

    class VerificationService {
        -InMemoryStore store
        -SupplierService supplierService
        -TaskBuffer~Claim~ reviewQueue
        +submitClaim(String supplierId, String claimTypeKey, String label, String pillar, String value, String docTitle, String docType, String filePath) Claim
        +verifyClaim(String claimId, String verifierName, VerificationMethod method, boolean approved, String note, int validSeconds) VerificationCheck
    }

    class TaskBuffer~T~ {
        -Queue~T~ queue
        -int capacity
        -boolean open
        +produce(T item) void
        +consume() T
        +size() int
        +shutdown() void
    }

    class Thread {
        <<JDK>>
    }

    class DecayMonitorThread {
        -InMemoryStore store
        -SupplierService supplierService
        -long pollMillis
        -volatile boolean running
        -List~String~ alerts
        +run() void
        +checkExpiredAttestations() void
        +drainAlerts() List~String~
        +stopMonitor() void
        +printThreadStates()$ void
    }

    class CsvSupplierIO {
        +exportToCsv(File file, List suppliers)$ void
        +importFromCsv(File file)$ List
    }

    class AuditFileIO {
        +appendAuditByteLog(File file, String eventText)$ void
        +readAuditByteLog(File file)$ String
        +tokenizeRule(String ruleExpression)$ List
    }

    class Exception {
        <<JDK>>
    }

    class SupifyException {
        +SupifyException(String message)
        +SupifyException(String message, Throwable cause)
    }

    class Main {
        +main(List args)$ void
        +seedSuppliers(InMemoryStore store, VerificationService service)$ void
    }

    class SupifyTestRunner {
        +main(List args)$ void
    }

    Repository <|.. InMemoryStore
    Repository <|.. JdbcSupplierDao
    JdbcSupplierDao ..> DatabaseConfig : gets connections
    Thread <|-- DecayMonitorThread
    Exception <|-- SupifyException

    SupplierService --> InMemoryStore
    VerificationService --> InMemoryStore
    VerificationService --> SupplierService
    VerificationService --> TaskBuffer
    VerificationService ..> SupifyException : throws
    DecayMonitorThread --> InMemoryStore
    DecayMonitorThread --> SupplierService

    Main --> InMemoryStore
    Main --> SupplierService
    Main --> VerificationService
    Main --> DecayMonitorThread
    Main --> TaskBuffer
    Main --> CsvSupplierIO
    Main --> AuditFileIO
    Main --> DatabaseConfig
    SupifyTestRunner ..> Main : seeds fixtures
```

---

## 4. End-to-End Verification Spine & Trust Recalculation Sequence

```mermaid
sequenceDiagram
    actor Sup as "Supplier (Main Menu [2])"
    participant VS as VerificationService
    participant IS as InMemoryStore
    participant TB as TaskBuffer
    actor Ver as "Verifier (Main Menu [3])"
    participant SS as SupplierService

    Sup->>VS: submitClaim(supplierId, key, label, pillar, val, docTitle, type, path)
    VS->>IS: saveClaim(Claim SUBMITTED) and saveEvidence(Evidence linked to claimId)
    VS->>TB: produce(Claim)
    VS-->>Sup: Return submitted Claim

    Ver->>IS: getPendingClaims()
    Ver->>VS: verifyClaim(claimId, verifierName, method, approved, note, validSeconds)
    VS->>IS: recordDecision(Claim, immutable VerificationCheck, Attestation)
    Note over IS: Synchronized update saves Check, writes ACTIVE Attestation, sets Claim VERIFIED
    VS->>SS: recalculateTrust(supplierId)
    SS->>IS: Read claims and active attestations
    Note over SS: Averages active method rigor scores and enforces Identity/Legal TrustBand caps
    VS-->>Ver: Return VerificationCheck
```

### Trust Evaluation Capping Rules

`SupplierService.recalculateTrust(String supplierId)` counts only **`ACTIVE`** attestations (`Attestation.state == "ACTIVE"`) and enforces these gating rules:
1. No active `IDENTITY` attestation → capped at **`UNVERIFIED`** (`score ≤ 0.30`).
2. Active `IDENTITY`, but no active `LEGAL` attestation → capped at **`BASIC`** (`score ≤ 0.65`).
3. Both present, plus at least one high-rigor check (`HUMAN_AUDIT` or `SITE_VISIT`), and weighted average `score ≥ 0.85` → **`AUDITED`**.
4. Otherwise → **`VERIFIED`**.

---

## 5. Concurrency, Thread Lifecycle & File I/O Pipelines

```mermaid
flowchart LR
    DM["DecayMonitorThread (daemon=true, 4s sleep)"] -->|polls ACTIVE attestations| IS["InMemoryStore"]
    IS -->|isExpired validUntil| EXP["att.expireIfNeeded() -> EXPIRED"]
    EXP --> SS["SupplierService.recalculateTrust()"]
    EXP --> BUF["Buffered Alerts List (synchronized drainAlerts)"]
    BUF -->|drained at clean prompt boundary| CLI["Main Menu Prompt"]

    PROD["Producer Thread"] -->|"produce() while-guard + notifyAll()"| TB["TaskBuffer (LinkedList)"]
    TB -->|"consume() while-guard + notifyAll()"| CONS["Consumer Thread"]
```

### JVM Thread States Tracked (`DecayMonitorThread.printThreadStates`)

```mermaid
stateDiagram-v2
    [*] --> NEW : new DecayMonitorThread() / new Thread()
    NEW --> RUNNABLE : start()
    RUNNABLE --> TIMED_WAITING : Thread.sleep(pollMillis) / join(1000)
    TIMED_WAITING --> RUNNABLE : interval elapsed
    RUNNABLE --> WAITING : TaskBuffer.wait() on empty or full buffer
    WAITING --> RUNNABLE : notifyAll()
    RUNNABLE --> BLOCKED : waiting for synchronized monitor lock
    BLOCKED --> RUNNABLE : lock acquired
    RUNNABLE --> TERMINATED : stopMonitor() / run() returns
    TIMED_WAITING --> TERMINATED : InterruptedException -> interrupt() + break
    TERMINATED --> [*]
```

### File I/O & Rule Parsing Pipelines

```mermaid
flowchart LR
    SUP["List of Suppliers"] -->|"FileWriter + BufferedWriter (CsvSupplierIO)"| CSV["data/suppliers.csv"]
    CSV -->|"FileReader + BufferedReader"| IMP["Validated Supplier List"]

    EVT["Audit Log Text"] -->|"FileOutputStream (append=true)"| LOG["data/audit.log"]
    LOG -->|"FileInputStream (AuditFileIO)"| TXT["UTF-8 Audit Log Output"]

    RULE["Rule String (CAPACITY >= 50000 AND SCORE >= 0.80)"] -->|"StringReader -> StreamTokenizer"| TOK["WORD / NUM / OP Token List"]
```
