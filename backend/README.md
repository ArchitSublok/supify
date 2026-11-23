# Supify — Supplier Discovery & Evidence-Backed Verification Backend

> **Pure Java Backend & Concurrency CLI | Modular Multi-Package Architecture**  
> **Repository Structure:** Flat `src/com/supify/` layout with Windows batch runners (`run.bat`, `test.bat`)

---

## 1. Problem Statement & Objectives

A buyer cannot cheaply observe whether a supplier truly possesses the certifications, capacity, and legal standing they claim.

The **Supify Backend & CLI** is a modular, pure-Java platform engineered for procurement teams, suppliers, and trained verifiers to:
1. **Discover & Compare Suppliers:** Search and filter suppliers by keyword, category, trust band, and production capacity using Lambda/Stream pipelines, and compare up to 4 vendors side by side.
2. **Enforce the 4-Layer Verification Spine:** Model claims strictly as `Claim -> Evidence -> VerificationCheck -> Attestation` (never boolean flags) with explicit method, timestamp, validity window, and rationale.
3. **Run Concurrent Background Monitors:** Continuously expire aged attestations (`DecayMonitorDaemon`) and escalate near-SLA verification tasks in a priority heap (`SlaEscalationDaemon`), delivering non-intrusive alerts through `AlertBuffer`.
4. **Persist & Parse Data with Java I/O Streams:** Export/import supplier catalogs as CSV (`FileReader`/`FileWriter`/`BufferedReader`/`BufferedWriter`), append binary audit records with magic header validation (`FileInputStream`/`FileOutputStream`), and tokenize rule expressions (`StreamTokenizer`).
5. **Handle Domain & Input Errors Safely:** Enforce custom checked exceptions (`SupplierNotFoundException`, `VerificationException`, `InvalidDataException`) and EOF-safe console scanning.

---

## 2. Architecture & Syllabus Rubric Mapping

| Syllabus / Rubric Topic | Implementation in Project | Location |
| :--- | :--- | :--- |
| **Multithreading & Concurrency** | Background daemons extending `Thread`, `start()`, `run()`, `sleep()`, `join()`, `interrupt()` | `DecayMonitorDaemon.java`, `SlaEscalationDaemon.java` |
| **Thread Lifecycle & Scheduling** | Live JVM thread inspection (`NEW`, `RUNNABLE`, `BLOCKED`, `WAITING`, `TIMED_WAITING`, `TERMINATED`), daemon flags, priorities | `ThreadLifecycleMonitor.java` |
| **Synchronization & Locks** | Atomic 4-layer spine updates under `ReentrantReadWriteLock`; `AtomicLong` audit sequencing | `InMemoryVerificationRepository.java`, `AuditEvent.java` |
| **Inter-Thread Communication** | Guarded blocks using `synchronized`, `wait()`, and `notifyAll()` for producer-consumer coordination; non-blocking `ConcurrentLinkedQueue` alerts | `CustomTaskQueue.java`, `AlertBuffer.java` |
| **Lambda Expressions & Streams** | Composable `Predicate`, method references (`::`), `filter()`, `map()`, `sorted()`, `distinct()`, `skip()`, `limit()` | `SupplierSearchService.java`, `TrustEvaluationService.java` |
| **Generics & Type Safety** | Generic repository interfaces, diamond operator `<>`, bounded wildcards (`? super T`, `? extends R`) | `GenericRepository.java`, `SupplierRepository.java`, `PaginatedResult.java` |
| **Custom Exceptions** | Checked domain exception classes extending `java.lang.Exception` | `SupplierNotFoundException.java`, `VerificationException.java`, `InvalidDataException.java` |
| **Character Streams & CSV** | `FileReader`, `FileWriter`, `BufferedReader`, `BufferedWriter`, collection CSV round-trip | `CsvDataExporterImporter.java` |
| **Byte Streams & Binary I/O** | `FileInputStream`, `FileOutputStream`, `DataInputStream`, `DataOutputStream` with `0x53504659` (`SPFY`) header | `AuditByteStreamHandler.java` |
| **StreamTokenizer** | Token classification (`TT_WORD`, `TT_NUMBER`, operators) for verification rules | `RuleStreamTokenizer.java` |
| **JDBC Architecture** *(Next Phase)* | `DriverManager`, `Connection`, `PreparedStatement` parameter binding, `ResultSet` cursor mapping | `DatabaseConfig.java`, `JdbcSupplierDao.java` |

---

## 3. Directory & Package Structure

```text
backend/
├── src/
│   └── com/
│       └── supify/
│           ├── app/
│           │   └── Main.java                     # Interactive CLI launcher, daemons, shutdown hook
│           ├── cli/
│           │   ├── BuyerMenu.java                # Discovery search, dossier viewer, comparison tray
│           │   ├── ConsoleView.java              # ANSI formatting, EOF-safe prompts, alert draining
│           │   ├── SessionContext.java           # Active persona role & 4-vendor comparison tray
│           │   ├── SupplierMenu.java             # Standings, 4-step intake wizard, decay timers
│           │   ├── SystemMenu.java               # Thread monitor, CSV, byte streams, tokenizer, JDBC status
│           │   └── VerifierMenu.java             # Priority task queue, dossier inspection, Pass/Fail decisions
│           ├── concurrency/
│           │   ├── AlertBuffer.java              # Thread-safe ConcurrentLinkedQueue alert bridge
│           │   ├── CustomTaskQueue.java          # Bounded wait()/notifyAll() producer-consumer queue
│           │   ├── DecayMonitorDaemon.java       # Background attestation expiry scanner
│           │   ├── SlaEscalationDaemon.java      # SLA escalation & PriorityBlockingQueue heap re-sort
│           │   └── ThreadLifecycleMonitor.java   # JVM thread state & scheduling table reporter
│           ├── config/
│           │   └── DatabaseConfig.java           # JDBC driver, URL & connection test utility
│           ├── dao/
│           │   ├── AuditRepository.java
│           │   ├── GenericRepository.java
│           │   ├── SupplierRepository.java
│           │   ├── VerificationRepository.java
│           │   ├── jdbc/
│           │   │   └── JdbcSupplierDao.java      # PreparedStatement & ResultSet DAO implementation
│           │   └── memory/
│           │       ├── InMemoryAuditRepository.java
│           │       ├── InMemorySupplierRepository.java
│           │       └── InMemoryVerificationRepository.java
│           ├── exception/
│           │   ├── InvalidDataException.java
│           │   ├── SupplierNotFoundException.java
│           │   └── VerificationException.java
│           ├── io/
│           │   ├── AuditByteStreamHandler.java   # Binary audit record writer/reader with SPFY header
│           │   ├── CsvDataExporterImporter.java  # Character stream CSV catalog serializer/parser
│           │   └── RuleStreamTokenizer.java      # StreamTokenizer rule expression parser
│           ├── model/
│           │   ├── enums/                        # Pillar, TrustBand, VerificationMethod, ClaimStatus, ...
│           │   └── ...                           # Organisation, SupplierProfile, Claim, Evidence, Check, Attestation, ...
│           ├── service/
│           │   ├── SupplierSearchService.java    # Multi-facet lambda/Stream discovery & pagination
│           │   ├── TrustEvaluationService.java   # Pillar weighting, rigor scoring, band capping
│           │   └── VerificationService.java      # Intake & atomic Pass/Fail attestation recording
│           ├── util/
│           │   ├── SeedDataFactory.java          # 5 canonical fixtures + 35s live expiring attestation
│           │   └── TableFormatter.java           # Unicode box-drawing terminal table renderer
│           └── test/
│               └── SupifyTestRunner.java         # Automated zero-dependency verification suite
├── bin/                                          # Compiled bytecode (.class files)
├── lib/                                          # External JAR directory (e.g., mysql-connector-j)
├── run.bat                                       # One-click Windows compile & CLI launcher
├── test.bat                                      # One-click Windows compile & test runner
└── README.md
```

---

## 4. Class Responsibilities

- **`com.supify.app.Main`**: Bootstraps in-memory repositories, populates fixtures via `SeedDataFactory`, launches `DecayMonitorDaemon` and `SlaEscalationDaemon`, wires all four role consoles, and handles graceful shutdown.
- **`com.supify.dao.memory.InMemoryVerificationRepository`**: Unifies `Claim`, `Evidence`, `VerificationCheck`, `Attestation`, and `VerificationTask` mutations under one `ReentrantReadWriteLock` alongside a `PriorityBlockingQueue`.
- **`com.supify.service.SupplierSearchService`**: Combines dynamic `Predicate` filters, method references, stream sorting/pagination, and `PaginatedResult` mapping.
- **`com.supify.service.TrustEvaluationService`**: Computes weighted multi-pillar scores from active attestations only, enforcing Trust Band caps when foundational identity or legal claims are missing.
- **`com.supify.exception.*`**: Checked exception hierarchy (`SupplierNotFoundException`, `VerificationException`, `InvalidDataException`) extending `java.lang.Exception`.
- **`com.supify.test.SupifyTestRunner`**: Automated 9-part verification runner validating fixtures, Stream search, 4-layer spine decisions, producer-consumer `wait()`/`notifyAll()`, priority heap re-sorting, CSV I/O, binary streams, `StreamTokenizer`, and exceptions.

---

## 5. Compilation & Execution Guide

### Prerequisites
- Java Development Kit (JDK 17 or higher) on `PATH`.

### Option A: One-Click Windows Batch Scripts
From Windows Explorer or Command Prompt inside `backend\`:
- Double-click **`run.bat`** (or run `run.bat`) to compile and launch the interactive CLI.
- Double-click **`test.bat`** (or run `test.bat`) to compile and run the automated test suite.

### Option B: Running Manually from Command Prompt / PowerShell
1. Open a terminal inside `backend\`:
   ```bat
   cd backend
   if not exist bin mkdir bin
   ```
2. Compile all source files into `bin\`:
   ```bat
   javac -d bin -sourcepath src src\com\supify\app\Main.java src\com\supify\test\SupifyTestRunner.java src\com\supify\dao\jdbc\JdbcSupplierDao.java
   ```
3. Launch the interactive CLI application:
   ```bat
   java -cp bin com.supify.app.Main
   ```
4. Run the automated verification test suite:
   ```bat
   java -cp bin com.supify.test.SupifyTestRunner
   ```

---

## 6. Sample Scenarios & Evaluation Viva Q&A

### Key Demo Scenarios
1. **Buyer Multi-Facet Search (`Main -> [1] -> [1]`):** Press Enter across prompts to list all 5 suppliers ranked by verified trust score, or filter by `Fasteners` / `score`.
2. **Verifier SLA Priority Escalation (`Main -> [3] -> [4]`):** Escalates a pending task in `PriorityBlockingQueue` via remove + mutate + re-offer so the priority heap re-balances immediately.
3. **35-Second Live Decay Demo (`Main -> [2] -> [3]`):** Observe the seeded short-lived attestation count down and trigger a non-intrusive `DecayMonitorDaemon` alert in `AlertBuffer` on expiration.
4. **File I/O & Tokenizer Diagnostics (`Main -> [4]`):** Inspect live JVM thread states, run CSV export/import to `data/`, write/read binary `SPFY` audit packets, and tokenize rule expressions.

### Viva Q&A

**Q1: Why does `SlaEscalationDaemon` remove and re-offer a task in `PriorityBlockingQueue` instead of mutating priority in place?**  
*Answer:* `PriorityBlockingQueue` maintains heap order only during insertion (`offer`) and removal (`poll`/`remove`). Mutating a queued object's `priority` field in place does not trigger sift-up or sift-down, leaving the heap corrupted until the element is removed and re-offered.

**Q2: How do background daemon threads avoid scrambling interactive CLI prompts?**  
*Answer:* Daemons never write directly to `System.out`. Instead, they push messages into the singleton `AlertBuffer` (`ConcurrentLinkedQueue<String>`), which `ConsoleView` drains only at clean prompt boundaries.

**Q3: Why model verification as a 4-layer spine instead of a `boolean isVerified` flag?**  
*Answer:* A boolean hides who verified a claim, by what rigor (`DOCUMENT_REVIEW` vs. `SITE_VISIT`), when it was checked, and when it expires. Separating `Claim -> Evidence -> VerificationCheck -> Attestation` makes trust explainable, auditable, and time-bounded.
