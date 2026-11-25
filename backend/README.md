# Supify — Supplier Discovery & Evidence-Backed Verification Backend

> **Simplified Pure-Java Backend & Interactive CLI (20 Source Files)**  
> **Repository Structure:** Clean package layout under `backend/src/com/supify/` with Windows batch runners (`run.bat`, `test.bat`)  
> **Architecture & Setup:** See [`docs/backend-architecture.md`](../docs/backend-architecture.md) (UML class diagrams & flowcharts), [`docs/database-architecture.md`](../docs/database-architecture.md) (MySQL ER & JDBC flow), and [`docs/setup.md`](../docs/setup.md) (setup & troubleshooting).

---

## 1. Core Capabilities

A buyer cannot cheaply observe whether a supplier truly holds the certifications, capacity, and legal standing they claim.

The **Supify Backend & CLI** is a modular, pure-Java platform enabling procurement teams, suppliers, and verifiers to:
1. **Discover & Compare Suppliers:** Filter by keyword, category, trust band, and capacity using Lambda/Stream pipelines (`SupplierService`), and compare vendors side by side.
2. **Enforce the 4-Layer Verification Spine:** Model claims strictly as `Claim → Evidence → VerificationCheck → Attestation` (no boolean `isVerified` flag) with immutable verifier checks and time-bounded attestations.
3. **Run Background Expiry Monitoring:** Continuously scan and expire aged attestations via a daemon thread (`DecayMonitorThread`), surfacing non-intrusive alerts cleanly between CLI prompts alongside a bounded producer–consumer queue (`TaskBuffer`).
4. **Persist & Parse Data with Java I/O Streams:** Export/import supplier catalogs as CSV (`FileWriter`/`FileReader` + buffered streams), log/read audit events via byte streams (`FileOutputStream`/`FileInputStream`), and parse verification rules with `StreamTokenizer`.
5. **Connect to MySQL via JDBC:** Load `DatabaseConfig` settings and perform parameterized CRUD via `JdbcSupplierDao` (`PreparedStatement`, `ResultSet`).

---

## 2. Architecture Concept Mapping

| Concept | Implementation in Project | Location |
| :--- | :--- | :--- |
| **Multithreading & Daemons** | Background monitor extending `Thread`, `start()`, `run()`, `sleep()`, `join()`, `interrupt()`, plus `Runnable` lambda shutdown hook | `DecayMonitorThread.java`, `Main.java` |
| **Thread Lifecycle Inspection** | Live JVM thread state table (`NEW`, `RUNNABLE`, `BLOCKED`, `WAITING`, `TIMED_WAITING`, `TERMINATED`), priority & daemon flags | `DecayMonitorThread.java` |
| **Synchronization & Visibility** | `synchronized` repository & entity state transitions, `volatile boolean running` shutdown flag | `InMemoryStore.java`, `DecayMonitorThread.java`, `SupplierService.java` |
| **Inter-Thread Communication** | Guarded `while`-loop blocks using `synchronized`, `wait()`, and `notifyAll()` for producer–consumer coordination | `TaskBuffer.java` |
| **Lambda Expressions & Streams** | Composable `Predicate`, `Comparator`, method references (`::`), `filter()`, `map()`, `sorted()`, `distinct()`, `skip()`, `limit()` | `SupplierService.java`, `Repository.java` |
| **Generics & Type Safety** | Generic `Repository<T, ID>`, `TaskBuffer<T>`, diamond operator `<>`, bounded wildcards (`? super T`, `? extends R`) | `Repository.java`, `TaskBuffer.java`, `InMemoryStore.java` |
| **Custom Checked Exceptions** | Checked exception extending `java.lang.Exception` with message + cause chaining | `SupifyException.java` |
| **Character Streams & CSV** | `FileReader`, `FileWriter`, `BufferedReader`, `BufferedWriter`, collection CSV export/import & row validation | `CsvSupplierIO.java` |
| **Byte Streams & `StreamTokenizer`** | `FileInputStream`, `FileOutputStream` byte logging; `StringReader` + `StreamTokenizer` (`TT_WORD`, `TT_NUMBER`, operators) | `AuditFileIO.java` |
| **JDBC & CRUD Mapping** | `Class.forName`, `DriverManager.getConnection`, `PreparedStatement` binding, `executeQuery`/`executeUpdate`, `ResultSet` mapping | `DatabaseConfig.java`, `JdbcSupplierDao.java` |

---

## 3. Directory & Package Structure

```text
backend/
├── src/
│   └── com/
│       └── supify/
│           ├── app/
│           │   └── Main.java                     # Interactive CLI consoles, canonical seeds, shutdown hook
│           ├── model/
│           │   ├── Attestation.java              # Layer 4: Time-bounded ACTIVE / EXPIRED / REVOKED standing
│           │   ├── Claim.java                    # Layer 1: Supplier assertion keyed by claimTypeKey + pillar
│           │   ├── Evidence.java                 # Layer 2: Document/report attached to claimId
│           │   ├── Supplier.java                 # Supplier record, capacity metrics & trust standing
│           │   ├── TrustBand.java                # UNVERIFIED, BASIC, VERIFIED, AUDITED
│           │   ├── VerificationCheck.java        # Layer 3: Immutable final verifier record
│           │   └── VerificationMethod.java       # SELF_DECLARED .. SITE_VISIT rigor weights
│           ├── dao/
│           │   ├── DatabaseConfig.java           # Environment/property-aware MySQL JDBC connection utility
│           │   ├── InMemoryStore.java            # Thread-safe in-memory Repository implementation
│           │   ├── JdbcSupplierDao.java          # PreparedStatement CRUD DAO against organisation + supplier_profile
│           │   └── Repository.java               # Generic CRUD interface + wildcard mapList helper
│           ├── service/
│           │   ├── SupplierService.java          # Stream search, sorting, pagination & TrustBand capping rules
│           │   └── VerificationService.java      # Claim intake & atomic verifier PASS/FAIL workflow
│           ├── concurrency/
│           │   ├── DecayMonitorThread.java       # Daemon expiry scanner, buffered alerts & thread state printer
│           │   └── TaskBuffer.java               # Bounded wait()/notifyAll() producer-consumer queue
│           ├── io/
│           │   ├── AuditFileIO.java              # Byte-stream audit log + StreamTokenizer rule parser
│           │   └── CsvSupplierIO.java            # Character-stream CSV catalog export & import
│           ├── exception/
│           │   └── SupifyException.java          # Checked domain exception with cause chaining
│           └── test/
│               └── SupifyTestRunner.java         # Automated zero-dependency 10-group test suite
├── bin/                                          # Compiled bytecode (.class files)
├── lib/                                          # External JAR directory (e.g., mysql-connector-j-*.jar)
├── run.bat                                       # One-click Windows compile & CLI launcher (-cp "bin;lib\*")
├── test.bat                                      # One-click Windows compile & test runner (-cp "bin;lib\*")
└── README.md
```

---

## 4. Compilation & Execution Guide

### Prerequisites
- Java Development Kit (JDK 17 or higher) on `PATH`.

### Option A: One-Click Windows Batch Scripts
From Windows Explorer or Command Prompt inside `backend\`:
- Double-click **`run.bat`** (or run `run.bat`) to compile and launch the interactive CLI.
- Double-click **`test.bat`** (or run `test.bat`) to compile and run the 10-group test suite (`SupifyTestRunner`).

### Option B: Running Manually from Command Prompt / PowerShell
1. Open a terminal inside `backend\`:
   ```bat
   cd backend
   if not exist bin mkdir bin
   ```
2. Compile all source files into `bin\`:
   ```bat
   javac -d bin -sourcepath src src\com\supify\app\Main.java src\com\supify\test\SupifyTestRunner.java src\com\supify\dao\JdbcSupplierDao.java
   ```
3. Launch the interactive CLI application:
   ```bat
   java -cp "bin;lib\*" com.supify.app.Main
   ```
4. Run the automated verification test suite:
   ```bat
   java -cp "bin;lib\*" com.supify.test.SupifyTestRunner
   ```

---

## 5. Key Interactive Flows in the CLI

1. **Buyer Catalog & Comparison (`Main → [1]`):** List all 5 suppliers ranked by trust score, run multi-filter Stream searches (`[2]`), inspect any supplier's full 4-layer claim lineage (`[3]`), or compare 2 suppliers side by side (`[4]`).
2. **Supplier Claim Intake & 30s Decay Timer (`Main → [2]`):** View `SUP-1`'s standing (`[1]`), submit a new claim with evidence (`[2]`), or watch active attestation countdowns (`[3]`, including `SUP-1`'s 30-second attestation triggering `DecayMonitorThread` auto-expiration).
3. **Verifier Decision Queue (`Main → [3]`):** Inspect submitted claims and attached proof (`[1]`) and approve or reject them (`[2]`), immediately recording an immutable `VerificationCheck`, time-bounded `Attestation`, and updated `TrustBand`.
4. **System & Admin Tools (`Main → [4]`):** Print live JVM thread states (`[1]`), run the `TaskBuffer` worker check (`[2]`), export/import `data/suppliers.csv` (`[3]`), write/read `data/audit.log` (`[4]`), tokenize rule expressions (`[5]`), and inspect MySQL JDBC status (`[6]`).
