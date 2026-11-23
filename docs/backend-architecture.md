# Supify — Java Backend Architecture, UML Class Diagrams & Flowcharts

Technical architecture reference for the 56-file pure-Java backend under `backend/src/com/supify/`.  
See [`setup.md`](setup.md) to run locally, [`understanding.md`](understanding.md) for domain axioms, [`database-architecture.md`](database-architecture.md) for the MySQL 8.0 ER diagram, and [`backend/README.md`](../backend/README.md) for syllabus mapping.

---

## 1. System Overview & Layered Package Architecture

The backend isolates interactive terminal I/O (`cli`), business logic (`service`), data persistence (`dao`), and concurrency workers (`concurrency`) behind repository interfaces (`SupplierRepository`, `VerificationRepository`, `AuditRepository`). Because services depend only on interfaces, `InMemory*` repositories and `JdbcSupplierDao` are interchangeable without modifying `service` or `cli`.

```mermaid
flowchart TD
    APP["app (Main)"] --> CLI["cli (BuyerMenu, SupplierMenu, VerifierMenu, SystemMenu, DemoRunner)"]
    APP --> CONC["concurrency (DecayMonitorDaemon, SlaEscalationDaemon)"]
    CLI --> SVC["service (SupplierSearchService, TrustEvaluationService, VerificationService)"]
    CLI --> CONC
    CLI --> IO["io (CsvDataExporterImporter, AuditByteStreamHandler, RuleStreamTokenizer)"]
    CLI --> UTIL["util (TableFormatter, SeedDataFactory)"]

    SVC --> DAO["dao (SupplierRepository, VerificationRepository, AuditRepository, GenericRepository)"]
    CONC --> DAO
    UTIL --> DAO

    MEM["dao.memory (InMemorySupplierRepository, InMemoryVerificationRepository, InMemoryAuditRepository)"] --> DAO
    JDBC["dao.jdbc (JdbcSupplierDao)"] --> DAO
    JDBC --> CFG["config (DatabaseConfig)"]
    CLI --> CFG

    CLI --> EXC["exception (SupplierNotFoundException, VerificationException, InvalidDataException)"]
    SVC --> MODEL["model (Entities & Enums)"]
    DAO --> MODEL
    IO --> MODEL

    TEST["test (SupifyTestRunner)"] --> SVC
    TEST --> CONC
    TEST --> IO
    TEST --> MEM
    TEST --> EXC
    TEST --> DAO
```

### Package Inventory (`backend/src/com/supify/`)

| Package | Role | Key Classes |
| :--- | :--- | :--- |
| `app` | Application bootstrap & graceful shutdown | `Main` |
| `cli` | Pure-ASCII role consoles, session state & demo runner | `ConsoleView`, `SessionContext`, `BuyerMenu`, `SupplierMenu`, `VerifierMenu`, `SystemMenu`, `DemoRunner` |
| `service` | Stream search, trust scoring & verification orchestration | `SupplierSearchService`, `TrustEvaluationService`, `VerificationService` |
| `dao` | Repository contracts, in-memory stores & MySQL JDBC DAO | `GenericRepository`, `SupplierRepository`, `VerificationRepository`, `AuditRepository`, `InMemory*`, `JdbcSupplierDao` |
| `model` | 4-layer verification spine entities & 10 domain enums | `Organisation`, `SupplierProfile`, `Claim`, `Evidence`, `VerificationCheck`, `Attestation`, `VerificationTask`, `TrustSnapshot`, `AuditEvent`, `PaginatedResult` |
| `concurrency` | Background daemons, wait/notify queue & alert bridge | `DecayMonitorDaemon`, `SlaEscalationDaemon`, `CustomTaskQueue`, `AlertBuffer`, `ThreadLifecycleMonitor` |
| `io` | Character/byte file streams & rule tokenizer | `CsvDataExporterImporter`, `AuditByteStreamHandler`, `RuleStreamTokenizer` |
| `exception` | Checked domain exception hierarchy | `SupplierNotFoundException`, `VerificationException`, `InvalidDataException` |
| `config` | Environment-aware JDBC driver & connection settings | `DatabaseConfig` |
| `util` | Seed fixtures & ASCII table formatter | `SeedDataFactory`, `TableFormatter` |
| `test` | Zero-dependency automated test suite (22 assertions) | `SupifyTestRunner` |

---

## 2. Core Domain Model & Enums — UML Class Diagram

Supify models verification as a **4-layer evidence spine** (`Claim -> Evidence -> VerificationCheck -> Attestation`) rather than a boolean verification flag. Mutable entities inherit UUID identity and timestamps from `AbstractAuditEntity`, while audit records (`VerificationCheck`, `AuditEvent`) are immutable `final` classes.

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
        +touch() void
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

    class Pillar {
        <<enumeration>>
        IDENTITY
        LEGAL
        FINANCIAL
        QUALITY
        CAPABILITY
        COMPLIANCE
        REPUTATION
    }

    class TrustBand {
        <<enumeration>>
        UNVERIFIED
        BASIC
        VERIFIED
        AUDITED
    }

    class VerificationMethod {
        <<enumeration>>
        SELF_DECLARED
        DOCUMENT_REVIEW
        THIRD_PARTY_API
        HUMAN_AUDIT
        SITE_VISIT
    }

    class ClaimStatus {
        <<enumeration>>
        DRAFT
        SUBMITTED
        IN_REVIEW
        VERIFIED
        REJECTED
        EXPIRED
        REVOKED
    }

    class CheckOutcome {
        <<enumeration>>
        PASS
        FAIL
        INCONCLUSIVE
        NEEDS_MORE_INFO
    }

    class AttestationState {
        <<enumeration>>
        ACTIVE
        EXPIRED
        REVOKED
        SUPERSEDED
    }

    class TaskState {
        <<enumeration>>
        QUEUED
        CLAIMED
        DECIDED
        ESCALATED
    }

    class EvidenceKind {
        <<enumeration>>
        PDF
        IMAGE
        API_TRANSCRIPT
        SITE_REPORT
    }

    class ListingState {
        <<enumeration>>
        DRAFT
        PENDING_REVIEW
        LISTED
        SUSPENDED
        DELISTED
    }

    class UserRole {
        <<enumeration>>
        BUYER
        SUPPLIER
        VERIFIER
        ADMIN
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

    SupplierProfile --> ListingState
    SupplierProfile --> TrustBand
    Claim --> Pillar
    Claim --> ClaimStatus
    Evidence --> EvidenceKind
    VerificationCheck --> VerificationMethod
    VerificationCheck --> CheckOutcome
    Attestation --> VerificationMethod
    Attestation --> AttestationState
    VerificationTask --> TaskState
```

---

## 3. Services, Repositories, JDBC Config & Exceptions — UML Class Diagram

Services depend strictly on repository interfaces (`SupplierRepository`, `VerificationRepository`, `AuditRepository`). `JdbcSupplierDao` uses `DatabaseConfig` to run parameterized MySQL CRUD queries and `TrustSnapshot` JSON serialization with zero external JSON libraries.

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
        +deleteOrganisationById(UUID id) boolean
        +saveProfile(SupplierProfile profile) SupplierProfile
        +findProfileByOrganisationId(UUID orgId) Optional~SupplierProfile~
        +findAllProfiles() List~SupplierProfile~
        +findProfilesMatching(Predicate predicate) List~SupplierProfile~
        +saveSnapshot(TrustSnapshot snapshot) TrustSnapshot
        +findSnapshotByOrganisationId(UUID orgId) Optional~TrustSnapshot~
    }

    class DatabaseConfig {
        +configure(String dbUrl, String dbUser, String dbPass)$ void
        +getUrl()$ String
        +getUser()$ String
        +getLastError()$ String
        +isDriverAvailable()$ boolean
        +getConnection()$ Connection
        +testConnection()$ boolean
    }

    class InMemoryVerificationRepository {
        -Map~UUID, Claim~ claims
        -Map~UUID, Evidence~ evidenceMap
        -Map~UUID, VerificationCheck~ checks
        -Map~UUID, Attestation~ attestations
        -Map~UUID, VerificationTask~ tasks
        -PriorityBlockingQueue~VerificationTask~ taskPriorityQueue
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

    JdbcSupplierDao ..> DatabaseConfig : acquires connections
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

## 4. CLI Consoles & Concurrency Workers — UML Class Diagram

`Main` wires shared session state (`SessionContext`), ANSI/ASCII terminal I/O (`ConsoleView`), the four role consoles (`BuyerMenu`, `SupplierMenu`, `VerifierMenu`, `SystemMenu`), the repeatable guided walkthrough (`DemoRunner`), and two background daemon threads (`DecayMonitorDaemon`, `SlaEscalationDaemon`).

```mermaid
classDiagram
    class Main {
        +main(String[] args)$ void
    }

    class ConsoleView {
        -Scanner scanner
        +hasNext() boolean
        +checkAndPrintAlerts() void
        +printBanner(SessionContext session) void
        +printSection(String title) void
        +printOk(String msg) void
        +printWarn(String msg) void
        +printError(String msg) void
        +promptString(String label) String
        +promptInt(String label, int defaultValue) int
        +pressEnterToContinue() void
    }

    class SessionContext {
        -UserRole currentRole
        -String currentUserName
        -Set~UUID~ comparisonTray
        +getCurrentRole() UserRole
        +setCurrentRole(UserRole role) void
        +addToComparison(UUID supplierId) boolean
        +removeFromComparison(UUID supplierId) boolean
    }

    class BuyerMenu {
        +display() void
    }

    class SupplierMenu {
        +display() void
    }

    class VerifierMenu {
        +display() void
    }

    class SystemMenu {
        +display() void
    }

    class DemoRunner {
        +runGuidedDemo() void
    }

    class Thread {
        <<JDK>>
    }

    class DecayMonitorDaemon {
        -long pollIntervalMillis
        -volatile boolean running
        +run() void
        +stopMonitoring() void
    }

    class SlaEscalationDaemon {
        -long pollIntervalMillis
        -volatile boolean running
        +run() void
        +stopDaemon() void
    }

    class CustomTaskQueue~T~ {
        -Queue~T~ queue
        -int capacity
        -boolean active
        +produce(T item) void
        +consume() T
        +pollWithTimeout(long timeoutMillis) T
        +shutdown() void
    }

    class AlertBuffer {
        -AlertBuffer INSTANCE$
        -ConcurrentLinkedQueue~String~ buffer
        +getInstance()$ AlertBuffer
        +post(String alertMessage) void
        +postAlert(String alertMessage) void
        +drainAlerts() List~String~
    }

    class ThreadLifecycleMonitor {
        +captureSupifyThreads()$ List~ThreadSnapshot~
        +generateThreadStateReport()$ String
    }

    Main --> BuyerMenu
    Main --> SupplierMenu
    Main --> VerifierMenu
    Main --> SystemMenu
    Main --> DemoRunner
    Main --> DecayMonitorDaemon
    Main --> SlaEscalationDaemon
    Main --> SessionContext
    Main --> ConsoleView

    BuyerMenu --> ConsoleView
    BuyerMenu --> SessionContext
    SupplierMenu --> ConsoleView
    SupplierMenu --> SessionContext
    VerifierMenu --> ConsoleView
    VerifierMenu --> SessionContext
    SystemMenu --> ConsoleView
    SystemMenu --> SessionContext
    SystemMenu --> ThreadLifecycleMonitor
    SystemMenu --> AlertBuffer
    DemoRunner --> ConsoleView
    DemoRunner --> AlertBuffer
    DemoRunner --> ThreadLifecycleMonitor

    Thread <|-- DecayMonitorDaemon
    Thread <|-- SlaEscalationDaemon
    DecayMonitorDaemon --> AlertBuffer : posts expiry alerts
    SlaEscalationDaemon --> AlertBuffer : posts SLA alerts
    ConsoleView --> AlertBuffer : drains at prompt boundaries
    SessionContext --> UserRole
```

---

## 5. Verification Spine & Trust Recomputation Sequence

The end-to-end verification lifecycle preserves strict separation of concerns: `VerificationService.submitClaimWithEvidence` binds uploaded proof to the newly generated `claim.getId()` and enqueues a review task, while verifiers write immutable checks and time-bounded attestations atomically under `ReentrantReadWriteLock`.

```mermaid
sequenceDiagram
    actor Sup as Supplier (SupplierMenu)
    participant VS as VerificationService
    participant VR as InMemoryVerificationRepository
    participant AR as AuditRepository
    actor Ver as Verifier (VerifierMenu)
    participant TE as TrustEvaluationService

    Sup->>VS: submitClaimWithEvidence(orgId, supplierName, key, label, pillar, value, evidence)
    VS->>VR: saveClaim(claim [SUBMITTED]) + saveEvidence(evidence linked to claimId)
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

## 6. Concurrency, Background Daemons & Thread Lifecycle

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

    PROD["Producer Thread"] -->|"produce() synchronized + notifyAll()"| CTQ["CustomTaskQueue (wait / notifyAll)"]
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

## 7. File I/O & Stream Pipelines

`SystemMenu` exercises three Java I/O mechanisms writing and validating files under `backend/data/`:

```mermaid
flowchart LR
    Profiles["Supplier Profiles + Organisations"] -->|"FileWriter + BufferedWriter"| CSV["data/suppliers_catalog_export.csv"]
    CSV -->|"FileReader + BufferedReader (CsvDataExporterImporter)"| Records["ExportRecord List (Validated POJOs)"]

    Events["Audit Payloads"] -->|"FileOutputStream + DataOutputStream (0x53504659 SPFY header)"| BIN["data/audit_ledger.bin"]
    BIN -->|"FileInputStream + DataInputStream (AuditByteStreamHandler)"| ValidatedBin["BinaryAuditRecord List (Header-Verified)"]

    Expr["Rule String (e.g. MIN_CAPACITY >= 50000)"] -->|"StringReader -> StreamTokenizer (RuleStreamTokenizer)"| Tokens["Token List (TT_WORD, TT_NUMBER, CHAR)"]
```

---

## 8. Database (`db/`) & JDBC Status

See [`database-architecture.md`](database-architecture.md) for the complete 9-table ER diagram and MySQL data-flow chart.

- **Implemented now:**
  - `db/schema/01_supify_schema.sql`, `db/seeds/01_canonical_suppliers.sql`, `db/procedures/01_views_and_procedures.sql`, and `db/setup_mysql.bat`.
  - `DatabaseConfig` supporting `SUPIFY_DB_*` env vars, `-Dsupify.db.*` properties, driver detection (`isDriverAvailable()`), and live connection diagnostics (`SystemMenu` option `[6]`).
  - Full-CRUD `JdbcSupplierDao` (`organisation`, `supplier_profile`, and `trust_snapshot` persistence via `PreparedStatement` + `ResultSet`).
  - `backend/run.bat` and `backend/test.bat` wired with `-cp "bin;lib\*"`.
- **Next increment:**
  - Add `JdbcVerificationDao` and `JdbcAuditDao` implementing `VerificationRepository` and `AuditRepository`.
  - Switch repository instantiation in `com.supify.app.Main` based on a `--db` flag or `DatabaseConfig.testConnection()`, falling back cleanly to `InMemory*` when offline.
