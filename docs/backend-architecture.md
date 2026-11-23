# Supify — Java Backend Architecture, UML Class Diagrams & Flowcharts

Comprehensive technical reference for the pure-Java backend under `backend/src/com/supify/`.  
For problem framing and domain axioms, see [`understanding.md`](understanding.md); for compilation and CLI usage, see [`backend/README.md`](../backend/README.md).

---

## 1. System Overview & Layered Package Architecture

The backend isolates interactive terminal I/O (`cli`), business logic (`service`), data persistence (`dao`), and concurrency workers (`concurrency`) behind repository interfaces (`SupplierRepository`, `VerificationRepository`, `AuditRepository`). Dependencies flow downward so `InMemory*` repositories and `JdbcSupplierDao` are interchangeable without changing `service` or `cli`.

```mermaid
flowchart TD
    APP["app (Main Entry Point)"] --> CLI["cli (BuyerMenu, SupplierMenu, VerifierMenu, SystemMenu)"]
    APP --> CONC["concurrency (DecayMonitorDaemon, SlaEscalationDaemon)"]
    CLI --> SVC["service (SupplierSearchService, TrustEvaluationService, VerificationService)"]
    CLI --> CONC
    CLI --> IO["io (CsvDataExporterImporter, AuditByteStreamHandler, RuleStreamTokenizer)"]
    CLI --> UTIL["util (TableFormatter, SeedDataFactory)"]

    SVC --> DAO["dao (Interfaces: SupplierRepository, VerificationRepository, AuditRepository, GenericRepository)"]
    CONC --> DAO
    UTIL --> DAO

    MEM["dao.memory (InMemorySupplierRepository, InMemoryVerificationRepository, InMemoryAuditRepository)"] --> DAO
    JDBC["dao.jdbc (JdbcSupplierDao)"] --> DAO
    JDBC --> CFG["config (DatabaseConfig)"]

    CLI --> EXC["exception (SupplierNotFoundException, VerificationException, InvalidDataException)"]
    SVC --> MODEL["model (Entities & Enums)"]
    DAO --> MODEL
    IO --> MODEL

    TEST["test (SupifyTestRunner)"] --> SVC
    TEST --> CONC
    TEST --> IO
    TEST --> MEM
    TEST --> EXC
```

### Package Inventory (`backend/src/com/supify/`)

| Package | Role | Key Classes |
| :--- | :--- | :--- |
| `app` | Application bootstrap & graceful shutdown | `Main` |
| `cli` | Interactive ANSI consoles & role session state | `ConsoleView`, `SessionContext`, `BuyerMenu`, `SupplierMenu`, `VerifierMenu`, `SystemMenu` |
| `service` | Domain pipelines & trust scoring | `SupplierSearchService`, `TrustEvaluationService`, `VerificationService` |
| `dao` | Repository contracts & implementations | `GenericRepository`, `SupplierRepository`, `VerificationRepository`, `AuditRepository`, `InMemory*`, `JdbcSupplierDao` |
| `model` | 4-layer verification spine entities & enums | `Organisation`, `SupplierProfile`, `Claim`, `Evidence`, `VerificationCheck`, `Attestation`, `VerificationTask`, `TrustSnapshot`, `AuditEvent`, `PaginatedResult` |
| `concurrency` | Background daemons, wait/notify queue & alert bridge | `DecayMonitorDaemon`, `SlaEscalationDaemon`, `CustomTaskQueue`, `AlertBuffer`, `ThreadLifecycleMonitor` |
| `io` | Character/byte file streams & rule tokenizer | `CsvDataExporterImporter`, `AuditByteStreamHandler`, `RuleStreamTokenizer` |
| `exception` | Checked domain exception hierarchy | `SupplierNotFoundException`, `VerificationException`, `InvalidDataException` |
| `config` | JDBC connection configuration | `DatabaseConfig` |
| `util` | Seed fixtures & terminal table rendering | `SeedDataFactory`, `TableFormatter` |
| `test` | Zero-dependency automated test runner | `SupifyTestRunner` |

---

## 2. Core Domain Model — UML Class Diagram

Supify models verification as a **4-layer evidence spine** (`Claim -> Evidence -> VerificationCheck -> Attestation`) rather than a single boolean flag. Mutable entities extend `AbstractAuditEntity` (which implements `Identifiable~UUID~`), while immutable platform facts (`VerificationCheck`, `AuditEvent`) are `final` value records.

```mermaid
classDiagram
    class Identifiable~ID~ {
        <<interface>>
        +getId() ID
    }

    class AbstractAuditEntity {
        <<abstract>>
        -UUID id
        -Instant createdAt
        -Instant updatedAt
        +getId() UUID
        +getCreatedAt() Instant
        +getUpdatedAt() Instant
        #touch() void
    }

    class Organisation {
        -String legalName
        -String tradeName
        -String entityType
        -String countryCode
        -String city
        -String state
        -int yearEstablished
        +getLocation() String
    }

    class SupplierProfile {
        -UUID organisationId
        -String headline
        -String category
        -int capacityPerMonth
        -String capacityUnit
        -int minimumOrderQuantity
        -String leadTime
        -ListingState listingState
        -TrustBand trustBand
        -double overallTrustScore
        +getCapacityDisplay() String
    }

    class Claim {
        -UUID organisationId
        -String claimTypeKey
        -String label
        -Pillar pillar
        -String assertedValue
        -ClaimStatus status
        -Instant submittedAt
        -UUID supersededBy
        +submit() void
        +setStatus(ClaimStatus status) void
        +markSuperseded(UUID newClaimId) void
    }

    class Evidence {
        -UUID claimId
        -String storageRef
        -EvidenceKind kind
        -String title
        -Instant issuedAt
        -Instant expiresAt
    }

    class VerificationCheck {
        <<final>>
        -UUID id
        -UUID claimId
        -VerificationMethod method
        -CheckOutcome outcome
        -double confidence
        -String performedBy
        -Instant performedAt
        -String rationale
    }

    class Attestation {
        -UUID claimId
        -UUID sourceCheckId
        -VerificationMethod methodAchieved
        -Instant verifiedAt
        -Instant validUntil
        -AttestationState state
        -String revocationReason
        -String issuedBy
        +isExpired() boolean
        +checkAndApplyDecay() boolean
        +revoke(String reason) void
        +getDecayCountdownString() String
    }

    class VerificationTask {
        -UUID claimId
        -UUID organisationId
        -String supplierName
        -String claimLabel
        -TaskState state
        -int priority
        -boolean requiresDual
        -Instant slaDueAt
        +claim(String verifierName) void
        +decide() void
        +escalate(int priorityBoost) void
        +compareTo(VerificationTask other) int
    }

    class TrustSnapshot {
        -UUID organisationId
        -TrustBand band
        -double overallScore
        -Map~Pillar, Double~ pillarScores
        -Instant computedAt
        +getPillarScore(Pillar pillar) double
    }

    class AuditEvent {
        <<final>>
        -AtomicLong SEQUENCE$
        -long sequenceNumber
        -Instant timestamp
        -String actor
        -String action
        -String subjectType
        -String subjectId
        -String details
    }

    class PaginatedResult~T~ {
        -List~T~ items
        -int totalCount
        -int page
        -int pageSize
        +getTotalPages() int
        +map(Function mapper) PaginatedResult
    }

    Identifiable <|.. AbstractAuditEntity
    Identifiable <|.. VerificationCheck
    Identifiable <|.. TrustSnapshot
    Identifiable <|.. AuditEvent

    AbstractAuditEntity <|-- Organisation
    AbstractAuditEntity <|-- SupplierProfile
    AbstractAuditEntity <|-- Claim
    AbstractAuditEntity <|-- Evidence
    AbstractAuditEntity <|-- Attestation
    AbstractAuditEntity <|-- VerificationTask

    Organisation "1" --> "1" SupplierProfile : owns profile
    Organisation "1" --> "0..*" Claim : asserts
    Organisation "1" --> "0..1" TrustSnapshot : summarized by
    Claim "1" --> "0..*" Evidence : backed by
    Claim "1" --> "0..*" VerificationCheck : evaluated by
    Claim "1" --> "0..*" Attestation : yields
    VerificationCheck "1" --> "0..1" Attestation : justifies
    Claim "1" --> "0..*" VerificationTask : schedules review
```

---

## 3. Services, Repositories & Exceptions — UML Class Diagram

Services depend strictly on repository interfaces (`SupplierRepository`, `VerificationRepository`, `AuditRepository`), isolating the in-memory store from MySQL/JDBC persistence. `GenericRepository~T, ID~` demonstrates type-safe parameterized CRUD with bounded wildcards (`Predicate~? super T~`).

```mermaid
classDiagram
    class GenericRepository~T, ID~ {
        <<interface>>
        +save(T entity) T
        +findById(ID id) Optional~T~
        +findAll() List~T~
        +findMatching(Predicate predicate) List~T~
        +deleteById(ID id) boolean
        +count() long
    }

    class SupplierRepository {
        <<interface>>
        +saveOrganisation(Organisation org) Organisation
        +findOrganisationById(UUID id) Optional~Organisation~
        +findAllOrganisations() List~Organisation~
        +saveProfile(SupplierProfile profile) SupplierProfile
        +findProfileByOrganisationId(UUID orgId) Optional~SupplierProfile~
        +findAllProfiles() List~SupplierProfile~
        +findProfilesMatching(Predicate predicate) List~SupplierProfile~
        +saveSnapshot(TrustSnapshot snapshot) TrustSnapshot
        +findSnapshotByOrganisationId(UUID orgId) Optional~TrustSnapshot~
    }

    class VerificationRepository {
        <<interface>>
        +saveClaim(Claim claim) Claim
        +findClaimById(UUID id) Optional~Claim~
        +findClaimsByOrganisation(UUID orgId) List~Claim~
        +saveEvidence(Evidence evidence) Evidence
        +findEvidenceByClaim(UUID claimId) List~Evidence~
        +recordCheck(VerificationCheck check) VerificationCheck
        +findChecksByClaim(UUID claimId) List~VerificationCheck~
        +saveAttestation(Attestation attestation) Attestation
        +findActiveAttestationByClaim(UUID claimId) Optional~Attestation~
        +findAttestationsByState(AttestationState state) List~Attestation~
        +findAllAttestations() List~Attestation~
        +enqueueTask(VerificationTask task) VerificationTask
        +findTaskById(UUID taskId) Optional~VerificationTask~
        +findPendingTasks() List~VerificationTask~
        +getTaskPriorityQueue() PriorityBlockingQueue~VerificationTask~
        +executeVerificationDecision(UUID taskId, VerificationCheck check, Attestation attestation) void
    }

    class AuditRepository {
        <<interface>>
        +recordEvent(AuditEvent event) AuditEvent
        +findRecentEvents(int limit) List~AuditEvent~
        +count() long
    }

    class InMemorySupplierRepository {
        -Map~UUID, Organisation~ organisations
        -Map~UUID, SupplierProfile~ profiles
        -Map~UUID, TrustSnapshot~ snapshots
    }

    class JdbcSupplierDao {
        +saveOrganisation(Organisation org) Organisation
        +findOrganisationById(UUID id) Optional~Organisation~
        +findAllOrganisations() List~Organisation~
        +saveProfile(SupplierProfile profile) SupplierProfile
        +findProfileByOrganisationId(UUID orgId) Optional~SupplierProfile~
        +findAllProfiles() List~SupplierProfile~
    }

    class InMemoryVerificationRepository {
        -Map claims
        -Map evidenceMap
        -Map checks
        -Map attestations
        -Map tasks
        -PriorityBlockingQueue taskPriorityQueue
        -ReentrantReadWriteLock rwLock
    }

    class InMemoryAuditRepository {
        -ConcurrentLinkedDeque~AuditEvent~ auditLog
    }

    class SupplierSearchService {
        -SupplierRepository supplierRepo
        +search(String query, String cat, TrustBand band, Integer minCap, String sort, boolean asc, int page, int size) PaginatedResult
    }

    class TrustEvaluationService {
        -SupplierRepository supplierRepo
        -VerificationRepository verificationRepo
        +evaluateSupplier(UUID organisationId) TrustSnapshot
    }

    class VerificationService {
        -VerificationRepository verificationRepo
        -TrustEvaluationService trustEvaluationService
        -AuditRepository auditRepo
        +submitClaimWithEvidence(...) Claim
        +processVerifierDecision(...) boolean
    }

    class Exception {
        <<JDK>>
    }
    class SupplierNotFoundException
    class VerificationException
    class InvalidDataException

    SupplierRepository <|.. InMemorySupplierRepository
    SupplierRepository <|.. JdbcSupplierDao
    VerificationRepository <|.. InMemoryVerificationRepository
    AuditRepository <|.. InMemoryAuditRepository

    JdbcSupplierDao ..> DatabaseConfig : getsConnection
    SupplierSearchService --> SupplierRepository
    TrustEvaluationService --> SupplierRepository
    TrustEvaluationService --> VerificationRepository
    VerificationService --> VerificationRepository
    VerificationService --> TrustEvaluationService
    VerificationService --> AuditRepository

    Exception <|-- SupplierNotFoundException
    Exception <|-- VerificationException
    Exception <|-- InvalidDataException
```

---

## 4. Verification Spine & Trust Recomputation Sequence

The end-to-end verification lifecycle preserves strict separation of concerns: suppliers only assert claims and upload proof; verifiers write immutable checks and time-bounded attestations atomically under `ReentrantReadWriteLock`.

```mermaid
sequenceDiagram
    actor Sup as Supplier (SupplierMenu)
    participant VS as VerificationService
    participant VR as InMemoryVerificationRepository
    participant AR as AuditRepository
    actor Ver as Verifier (VerifierMenu)
    participant TE as TrustEvaluationService

    Sup->>VS: submitClaimWithEvidence(orgId, key, label, pillar, value, evidence)
    VS->>VR: saveClaim(claim [SUBMITTED]) + saveEvidence(evidence)
    VS->>VR: enqueueTask(VerificationTask in PriorityBlockingQueue)
    VS->>AR: recordEvent("claim.submitted")
    VS-->>Sup: Return submitted Claim

    Ver->>VR: findPendingTasks() [ordered by priority & SLA]
    Ver->>VS: processVerifierDecision(taskId, verifier, method, PASS, rationale, validity)
    VS->>VR: executeVerificationDecision(taskId, immutable Check, Attestation)
    Note over VR: Acquires writeLock -> records Check, writes ACTIVE Attestation, sets Claim VERIFIED, marks Task DECIDED
    VS->>AR: recordEvent("verification.decided")
    VS->>TE: evaluateSupplier(orgId)
    Note over TE: Computes weighted pillar scores; caps TrustBand if Identity/Legal missing
    TE-->>VR: Read active attestations
    TE-->>VS: Return updated TrustSnapshot
```

### Trust Evaluation Capping Rules

`TrustEvaluationService` applies strict gating rules before assigning a `TrustBand`:
1. No active `IDENTITY` attestation $\rightarrow$ capped at **`UNVERIFIED`** (`score <= 0.30`).
2. `IDENTITY` verified, but no active `LEGAL` attestation $\rightarrow$ capped at **`BASIC`** (`score <= 0.65`).
3. Both present, plus at least one high-rigor check (`HUMAN_AUDIT` or `SITE_VISIT`) and weighted score $\ge 0.85$ $\rightarrow$ **`AUDITED`**.
4. Otherwise $\rightarrow$ **`VERIFIED`**.

---

## 5. Concurrency, Background Daemons & Thread Lifecycle

Background daemons keep trust scores accurate over time without corrupting active terminal prompts.

```mermaid
flowchart LR
    DM["DecayMonitorDaemon (setDaemon=true, 4s sleep)"] -->|polls ACTIVE attestations| VR["InMemoryVerificationRepository"]
    VR -->|expired validUntil| DECAY["attestation.checkAndApplyDecay() -> EXPIRED"]
    DECAY --> AB["AlertBuffer (ConcurrentLinkedQueue)"]
    DECAY --> AR["AuditRepository (attestation.expired)"]

    SLA["SlaEscalationDaemon (setDaemon=true, 6s sleep)"] -->|scans SLA < 2h| PQ["PriorityBlockingQueue"]
    PQ -->|"remove -> escalate(+25) -> offer (re-heapify)"| SLA
    SLA --> AB

    AB -->|"drainAlerts() at safe prompt boundary"| CV["ConsoleView (System.out)"]

    PROD["Producer Thread"] -->|"produce() synchronized + notifyAll()"| CTQ["CustomTaskQueue~T~ (wait / notifyAll)"]
    CTQ -->|"consume() guarded while-loop"| CONS["Consumer Thread"]
```

### JVM Thread States Tracked (`ThreadLifecycleMonitor`)

```mermaid
stateDiagram-v2
    [*] --> NEW : "new DecayMonitorDaemon() / Thread()"
    NEW --> RUNNABLE : "start()"
    RUNNABLE --> TIMED_WAITING : "Thread.sleep(pollIntervalMillis) / wait(timeout)"
    TIMED_WAITING --> RUNNABLE : "interval elapsed"
    RUNNABLE --> WAITING : "CustomTaskQueue.wait() on empty or full"
    WAITING --> RUNNABLE : "notifyAll()"
    RUNNABLE --> BLOCKED : "contending on synchronized / intrinsic lock"
    BLOCKED --> RUNNABLE : "lock acquired"
    RUNNABLE --> TERMINATED : "stopMonitoring() / interrupt() / run() returns"
    TIMED_WAITING --> TERMINATED : "InterruptedException -> interrupt() + break"
    TERMINATED --> [*]
```

---

## 6. File I/O & Stream Pipelines

`SystemMenu` demonstrates three Java I/O mechanisms writing and validating data under `backend/data/`:

```mermaid
flowchart LR
    Profiles["Supplier Profiles + Organisations"] -->|"FileWriter + BufferedWriter"| CSV["data/suppliers_catalog_export.csv"]
    CSV -->|"FileReader + BufferedReader (CsvDataExporterImporter)"| Records["List~ExportRecord~ (Validated POJOs)"]

    Events["Audit Payloads"] -->|"FileOutputStream + DataOutputStream (0x53504659 SPFY header)"| BIN["data/audit_ledger.bin"]
    BIN -->|"FileInputStream + DataInputStream (AuditByteStreamHandler)"| ValidatedBin["List~BinaryAuditRecord~ (Header-Verified)"]

    Expr["Rule String (e.g. MIN_CAPACITY >= 50000)"] -->|"StringReader -> StreamTokenizer (RuleStreamTokenizer)"| Tokens["List~Token~ (TT_WORD, TT_NUMBER, CHAR)"]
```

---

## 7. Next Phase: Database (`db/`) & JDBC Transition

The repository layer is already decoupled so migrating from in-memory maps to MySQL 8.0 requires zero changes to `cli/` or `service/`:

1. **SQL Scripts (`db/`):**
   - `db/schema/` — `CREATE TABLE` DDL for the 15 normalized tables (`organisation`, `supplier_profile`, `claim`, `evidence`, `verification_check`, `attestation`, `verification_task`, `trust_snapshot`, `audit_event`, etc.).
   - `db/seeds/` — Initial seed inserts matching the canonical fixtures.
   - `db/procedures/` — Stored procedures, views, and triggers for ledger append guarantees and trust rollups.
   - `db/migrations/` — Incremental `ALTER TABLE` scripts.
2. **Java JDBC Layer (`backend/src/com/supify/dao/jdbc/` & `config/`):**
   - Place `mysql-connector-j-*.jar` in `backend/lib/` and include `-cp "bin;lib/*"` in `run.bat` and `test.bat`.
   - Complete `JdbcSupplierDao` methods (`saveSnapshot`, `findSnapshotByOrganisationId`) and add `JdbcVerificationDao` / `JdbcAuditDao` using parameterized `PreparedStatement` and `ResultSet` cursor mapping via `DatabaseConfig.getConnection()`.
   - Switch repository instantiation in `com.supify.app.Main` based on `DatabaseConfig.testConnection()`, falling back automatically to `InMemory*` when MySQL is unavailable.
