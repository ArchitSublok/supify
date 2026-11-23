# Supify — Java Backend & Concurrency CLI

High-performance, pure-Java backend demonstration and interactive CLI console for the **Supify** Supplier Discovery & Evidence-Backed Verification Platform.

---

## 1. Quick Start

### Compile
From the repository root:
```powershell
$javaFiles = (Get-ChildItem -Path "backend/src/main/java" -Recurse -Filter "*.java").FullName
javac -d backend/bin $javaFiles
```

### Run Interactive Console
```powershell
java -cp backend/bin com.supify.SupifyApplication
```

Or using the helper script:
```powershell
cd backend
.\run-cli.ps1
```

---

## 2. Core Architecture & Verification Spine

The backend models the platform's non-negotiable **4-Layer Verification Spine**:

```
Layer 1: Claim Assertion   (Supplier asserts identity, quality, or capability)
   ↓
Layer 2: Evidentiary Proof (Supplier uploads PDF, hash, certificate, or test report)
   ↓
Layer 3: Verification     (Trained verifier records immutable forensic check)
   ↓
Layer 4: Attestation       (Platform grants time-bounded, decaying attestation)
```

- **Domain Axioms Preserved:**
  - **Never a boolean flag:** Attestation state is explicitly `{ method, verifiedAt, validUntil, attestor, evidence }`.
  - **Decay & Recency:** Attestations expire over time, dynamically triggering trust score degradation.
  - **Audit Immutability:** Consequential events are recorded in an append-only atomic audit ledger with strictly monotonic sequence numbers (`AtomicLong`).
  - **Thread-Safety:** Spine mutations are synchronized under atomic `ReentrantReadWriteLock` and thread-safe collections.

---

## 3. Academic & Syllabus Concept Coverage

| Syllabus Topic | Concepts Implemented | Core Java Files |
| :--- | :--- | :--- |
| **Multithreading & Concurrency** | Processes vs threads, sequential vs concurrent execution, `start()`, `run()`, `sleep()`, `join()`, `interrupt()` | `DecayMonitorDaemon.java`, `SlaEscalationDaemon.java` |
| **Thread Lifecycle & Scheduling** | `NEW`, `RUNNABLE`, `BLOCKED`, `WAITING`, `TIMED_WAITING`, `TERMINATED`, `setDaemon(true)`, `setPriority()` | `ThreadLifecycleMonitor.java`, `DecayMonitorDaemon.java` |
| **Synchronization & Locks** | Race conditions, critical sections, `synchronized` methods/blocks, intrinsic locks, atomicity, `volatile` | `CustomTaskQueue.java`, `InMemoryVerificationRepository.java` |
| **Inter-Thread Communication** | Guarded blocks with `wait()`, `notifyAll()`, producer-consumer coordination | `CustomTaskQueue.java` |
| **Non-Blocking Alert Buffering** | `ConcurrentLinkedQueue<String>` prevents background threads from corrupting console prompts | `AlertBuffer.java`, `ConsoleView.java` |
| **Lambda Expressions & Streams** | `filter()`, `map()`, `sorted()`, `distinct()`, `skip()`, `limit()`, `collect()`, method references (`::`) | `SupplierSearchService.java`, `TrustEvaluationService.java` |
| **Generics & Type Safety** | Generic interfaces, diamond operator `<>`, bounded wildcards (`? extends`, `? super`) | `GenericRepository.java`, `PaginatedResult.java` |
| **Character Streams & CSV** | `FileReader`, `FileWriter`, `BufferedReader`, `BufferedWriter`, Collections import/export | `CsvDataExporterImporter.java` |
| **Byte Streams & Binary I/O** | `FileInputStream`, `FileOutputStream`, `DataInputStream`, `DataOutputStream`, magic header verification (`0x53504659`) | `AuditByteStreamHandler.java` |
| **StreamTokenizer** | Character classification, `TT_WORD`, `TT_NUMBER`, `TT_EOL`, custom word/char parsing for rule expressions | `RuleStreamTokenizer.java` |
| **JDBC Architecture** | `DriverManager`, `Connection`, `PreparedStatement` parameter binding, secure SQL, cursor iteration (`rs.next()`), typed getters | `DatabaseConfig.java`, `JdbcSupplierDao.java` |

---

## 4. Interactive CLI Consoles

The CLI provides four distinct personas/consoles matching the React frontend:

1. **Buyer Discovery Console:**
   - Multi-faceted supplier search with lambda-filtered Stream pipelines.
   - Live Dossier & Claims Ledger view.
   - Side-by-side comparison tray (up to 4 vendors) with ANSI-formatted matrix.

2. **Supplier Workspace:**
   - Onboarding profile standings and pillar breakdowns.
   - 4-Step Intake Wizard for asserting new claims with evidence attachment.
   - Live attestation decay tracking countdowns (demonstrating the 35s live expiring attestation).

3. **Verifier Station:**
   - Dynamic priority review queue ordered by `PriorityBlockingQueue` heap.
   - Full 4-layer dossier inspection (Claim -> Evidence -> Check -> Attestation).
   - Forensic decision recording (Pass/Fail, audit rationale, validity duration).
   - Priority boost and heap re-heapification SLA test.

4. **System, Concurrency & I/O Diagnostics:**
   - Real-time Thread Lifecycle Monitor (`ThreadLifecycleMonitor`).
   - Daemon alert buffer log drainage.
   - Character streams CSV export & import benchmark.
   - Byte streams binary audit record serialization & magic header verification.
   - StreamTokenizer rule engine parser.
   - JDBC connection status check.
   - Immutable system audit trail.
