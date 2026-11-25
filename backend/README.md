# Supify — Java Backend & CLI

Clean pure-Java console application (**15 `.java` files across 4 packages** + `run.bat` & `test.bat`).

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
| **Automated Verification** | Zero-dependency 8-check suite | `TestApp.java` |

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
│   └── app/                      # 2 files: CLI entry + automated tests
│       ├── Main.java
│       └── TestApp.java
├── bin/                          # Compiled .class files (gitignored)
├── lib/                          # Optional mysql-connector-j-*.jar
├── run.bat                       # Compile & run Main
├── test.bat                      # Compile & run TestApp
└── README.md
```

---

## 3. Compile & Run

### Windows Batch Scripts
Inside `backend\`:
- Run **`run.bat`** to compile and open the interactive CLI (`Buyer`, `Supplier`, `Verifier`, `Admin` menus).
- Run **`test.bat`** to compile and run the 8-check test suite (`TestApp`).

### Manual Commands
```bat
cd backend
if exist bin rd /s /q bin
mkdir bin
javac -d bin -sourcepath src src\com\supify\app\Main.java src\com\supify\app\TestApp.java src\com\supify\dao\JdbcSupplierDao.java
java -cp "bin;lib\*" com.supify.app.Main
java -cp "bin;lib\*" com.supify.app.TestApp
```

---

## 4. Interactive CLI Menus (`Main.java`)

- **`[1] Buyer Menu`:** `[1]` List Suppliers · `[2]` Search & Sort Suppliers (by Trust Score, Capacity, or Name) · `[3]` Inspect Supplier Claims (4-layer `Claim → Evidence → Check → Attestation`) · `[4]` Compare 2 Suppliers
- **`[2] Supplier Menu (SUP-1)`:** `[1]` My Standing · `[2]` Submit Claim + Evidence (creates `SUBMITTED` claim only) · `[3]` Active Expiry Timers (watch `SUP-1`'s 30-second attestation count down)
- **`[3] Verifier Menu`:** `[1]` Pending Claims · `[2]` Approve / Reject Claim (creates immutable `VerificationCheck`, issues `Attestation` on PASS, recalculates `TrustBand`)
- **`[4] Admin Menu`:** `[1]` Thread Status (`main` + `DecayMonitor` daemon) · `[2]` CSV Export / Import (`data/suppliers.csv`) · `[3]` Audit Log (`data/audit.log` append-mode) · `[4]` MySQL JDBC Status (`url`, driver loaded, live connection test)

---

## 5. Automated Test Suite (`TestApp.java` — 8 Checks)

1. `1. Seed Data & Generic Repository` — loads 5 sample suppliers, verifies `save`, `findById`, `deleteById`, and `count()`
2. `2. Stream Search & Sorting` — verifies Stream filtering by keyword + `TrustBand`, capacity/name sorting, and `getAllCategories()`
3. `3. 4-Layer Spine (Claim -> Evidence -> Check -> Attestation)` — submits claim + PDF proof, records PASS checks, verifies `Attestation` creation and `TrustBand.VERIFIED`
4. `4. Trust Band Capping & Rejection` — proves `UNVERIFIED (≤ 0.30)` without `IDENTITY`, `BASIC (≤ 0.65)` without `LEGAL`, and `FAIL` → `"REJECTED"` flow
5. `5. Daemon Auto-Decay & Alert` — verifies `DecayMonitorThread.isDaemon()`, 1-second expiry transition to `"EXPIRED"`, trust downgrade, and alert queueing
6. `6. CSV Export/Import & Audit Log (File I/O)` — exports 5 suppliers, appends a malformed row to test graceful skipping on import, and verifies `appendAuditLog`/`readAuditLog`
7. `7. Checked SupifyException Guards` — validates missing supplier, blank label, missing claim, and blank verifier rationale throw `SupifyException`
8. `8. JDBC Config & Driver Check` — validates MySQL JDBC URL format and driver check state via `JdbcSupplierDao`
