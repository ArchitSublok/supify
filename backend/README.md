# Supify — Java Backend & CLI

Clean pure-Java console application (**4 packages**: `model`, `dao`, `service`, `app` + `run.bat`).

---

## 1. Technical Implementation Map

| Capability | Implementation | File(s) |
| :--- | :--- | :--- |
| **OOP, Encapsulation & Enums** | 4-layer spine (`Supplier`, `Claim`, `Evidence`, `final VerificationCheck`, `Attestation`) + enums (`TrustBand`, `VerificationMethod`) | `com.supify.model.*` |
| **Generics & Collections** | Generic `Repository<T, ID>`, `HashMap`, `ArrayList`, `<>` diamond, `Collections.sort` | `Repository.java`, `SupifyPlatform.java` |
| **Lambda Expressions & Streams** | `.stream().filter(...).sorted(getComparator(sortBy)).collect(Collectors.toList())` search & comparator lambdas | `SupifyPlatform.java`, `Main.java` |
| **Custom Checked Exception** | `SupifyException extends Exception` for supplier/verifier validation guards | `SupifyException.java`, `SupifyPlatform.java` |
| **Multithreading & Synchronization** | Daemon `DecayMonitorThread(platform)` (`setDaemon(true)`, `volatile`, `sleep`, `interrupt`) sharing `synchronized SupifyPlatform` | `DecayMonitorThread.java`, `SupifyPlatform.java` |
| **File I/O (Character Streams)** | Buffered `FileWriter`/`FileReader` CSV catalog export/import (`data/suppliers.csv`) & append-mode `data/audit.log` | `FileHelper.java` |
| **JDBC & MySQL CRUD** | `Class.forName`, `DriverManager`, `PreparedStatement`, `executeQuery`/`executeUpdate`, `ResultSet` `mapRow` | `JdbcSupplierDao.java` |

---

## 2. Directory & 4-Package Structure

```text
backend/
├── src/com/supify/
│   ├── model/                    # 7 files: 4-layer spine entities + enums
│   │   ├── TrustBand.java
│   │   ├── VerificationMethod.java
│   │   ├── Supplier.java
│   │   ├── Claim.java
│   │   ├── Evidence.java
│   │   ├── VerificationCheck.java
│   │   └── Attestation.java
│   ├── dao/                      # 3 files: generic repo, File I/O & JDBC
│   │   ├── Repository.java
│   │   ├── FileHelper.java
│   │   └── JdbcSupplierDao.java
│   ├── service/                  # 3 files: exception, shared platform, daemon thread
│   │   ├── SupifyException.java
│   │   ├── SupifyPlatform.java
│   │   └── DecayMonitorThread.java
│   └── app/                      # CLI entry point
│       └── Main.java
├── bin/                          # Compiled .class files (gitignored)
├── lib/                          # mysql-connector-j-*.jar
├── run.bat                       # Compile & run Main
└── README.md
```

---

## 3. Compile & Run

### Windows Batch Script
Inside `backend\`:
- Run **`run.bat`** to compile and open the interactive CLI (`Buyer`, `Supplier`, `Verifier`, `Admin` menus).

### Manual Commands
```bat
cd backend
if exist bin rd /s /q bin
mkdir bin
javac -d bin -sourcepath src src\com\supify\app\Main.java src\com\supify\dao\JdbcSupplierDao.java
java -cp "bin;lib\*" com.supify.app.Main
```

---

## 4. Interactive CLI Menus (`Main.java`)

- **`[1] Buyer Menu`:** `[1]` List Suppliers · `[2]` Search & Sort Suppliers (by Trust Score, Capacity, or Name) · `[3]` Inspect Supplier Claims (4-layer `Claim → Evidence → Check → Attestation`) · `[4]` Compare 2 Suppliers
- **`[2] Supplier Menu (SUP-1)`:** `[1]` My Standing · `[2]` Submit Claim + Evidence (creates `SUBMITTED` claim only) · `[3]` Active Expiry Timers (watch `SUP-1`'s 30-second attestation count down)
- **`[3] Verifier Menu`:** `[1]` Pending Claims · `[2]` Approve / Reject Claim (creates immutable `VerificationCheck`, issues `Attestation` on PASS, recalculates `TrustBand`)
- **`[4] Admin Menu`:** `[1]` Thread Status (`main` + `DecayMonitor` daemon) · `[2]` CSV Export / Import (`data/suppliers.csv`) · `[3]` Audit Log (`data/audit.log` append-mode) · `[4]` MySQL JDBC Status (`url`, driver loaded, live connection check)
