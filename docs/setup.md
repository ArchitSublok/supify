# Supify — Setup, Run & Demo Guide

Complete local setup instructions for all three parts of the repository: the React Frontend (`src/`), the Pure-Java Backend CLI (`backend/`), and the MySQL 8.0 Database & JDBC Layer (`db/`).

---

## 1. Repository Layout

```text
supplier-platform/
├── AGENTS.md                   # Single canonical working rules, invariants & status
├── README.md                   # Project overview & quick tour
├── src/                        # React frontend (Vite, React Router 6, Tailwind, Zustand)
├── public/data/                # JSON fixtures served over HTTP to the frontend
├── backend/
│   ├── src/com/supify/         # Pure-Java CLI, concurrency daemons, services, DAOs & tests (56 files)
│   ├── lib/                    # Optional external JARs (mysql-connector-j-*.jar, gitignored)
│   ├── bin/                    # Compiled .class files (gitignored)
│   ├── run.bat                 # One-click Windows compile & CLI launcher
│   ├── test.bat                # One-click Windows compile & automated test runner
│   └── README.md               # Java backend reference & syllabus mapping
├── db/
│   ├── schema/                 # MySQL 8.0 normalized DDL (01_supify_schema.sql)
│   ├── seeds/                  # Canonical seed fixtures (01_canonical_suppliers.sql)
│   ├── procedures/             # Views, decay procedure & append-only triggers
│   ├── migrations/             # Incremental ALTER TABLE migrations (future)
│   ├── setup_mysql.bat         # One-click Windows MySQL initializer
│   └── README.md               # Database structure & credential reference
└── docs/
    ├── setup.md                # This guide
    ├── understanding.md        # Problem framing, design axioms & trust model
    ├── architecture.md         # Frontend architecture & state contracts
    ├── backend-architecture.md # Backend UML class diagrams, sequences & concurrency charts
    └── database-architecture.md# MySQL 8.0 ER diagram & JDBC flow
```

---

## 2. Prerequisites

| Layer | Required Software | Minimum Version | Notes |
| :--- | :--- | :--- | :--- |
| **Frontend (`src/`)** | Node.js & npm | Node 18+ | Serves Vite dev server and runs Vitest suite |
| **Backend (`backend/`)** | Java Development Kit (JDK) | JDK 17+ | `javac` and `java` available on `PATH`; no Maven/Gradle needed |
| **Database (`db/`)** | MySQL Server + `mysql` CLI | MySQL 8.0+ | Optional for pure in-memory CLI mode; required for JDBC checks |
| **JDBC Driver** | `mysql-connector-j-*.jar` | 8.x+ (tested on 26.10.0) | Place inside `backend/lib/` (excluded from Git by `.gitignore`) |

---

## 3. Running the React Frontend (`src/`)

From the repository root:

```bash
npm install
npm run dev
```

- Opens on `http://localhost:5173`.
- Uses decoupled HTTP JSON fixtures in `public/data/*.json` (does not depend on `backend/`).
- Run automated frontend checks or production build:

```bash
npm test
npm run build
```

---

## 4. Running the Java Backend CLI (`backend/`)

The backend CLI runs completely standalone with zero external dependencies using thread-safe in-memory repositories populated by `SeedDataFactory`.

### Option A: One-Click Windows Batch Scripts

Inside `backend\`:
- Double-click **`run.bat`** (or run `.\run.bat`) to compile all sources into `bin\` and launch `com.supify.app.Main`.
- Double-click **`test.bat`** (or run `.\test.bat`) to compile and run the automated 22-assertion test suite (`com.supify.test.SupifyTestRunner`).

### Option B: Manual Compile & Run Commands

From `backend\`:

```bat
if not exist bin mkdir bin
javac -d bin -sourcepath src src\com\supify\app\Main.java src\com\supify\test\SupifyTestRunner.java src\com\supify\dao\jdbc\JdbcSupplierDao.java
java -cp "bin;lib\*" com.supify.app.Main
```

Run the automated test runner manually:

```bat
java -cp "bin;lib\*" com.supify.test.SupifyTestRunner
```

### Quick 60-Second CLI Demo Path

1. Launch `backend\run.bat`.
2. **Guided 5-Step Walkthrough:** At the Main Menu, press **`5`** to run `DemoRunner` (runs discovery search, submits a claim + PDF proof, approves it as a verifier, escalates a task in `PriorityBlockingQueue` + posts via `AlertBuffer`, parses a rule via `StreamTokenizer`, and inspects active JVM threads on isolated fixtures).
3. **One-Key Catalog View:** Return to Main Menu, choose **`[1] Buyer Console`**, then press **`[1] List All Suppliers`** to print the trust-ranked ASCII supplier table immediately.
4. **Live Expiry & Queue Review:** Explore **`[2] Supplier Console -> [3]`** to watch the 35-second expiring attestation count down, and **`[3] Verifier Console -> [1]`** to see background SLA escalation alerts surfaced cleanly at prompt boundaries.

---

## 5. Setting Up MySQL 8.0 & JDBC (`db/`)

### Step 1: Initialize Schema, Seeds & Triggers

Run **`db\setup_mysql.bat`** (from Explorer or terminal):
- Detects `mysql.exe` on `PATH` or `C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe`.
- Prompts once for your MySQL username (default `root`) and password (`-p`).
- Loads in order:
  1. `db/schema/01_supify_schema.sql` (creates `supify` database + 9 InnoDB tables)
  2. `db/seeds/01_canonical_suppliers.sql` (seeds 5 canonical suppliers, claims, evidence, checks, attestations, and tasks)
  3. `db/procedures/01_views_and_procedures.sql` (creates `vw_supplier_catalog`, `vw_verifier_queue`, append-only triggers, and `sp_expire_stale_attestations`)

### Step 2: Stage the MySQL JDBC Driver

Copy `mysql-connector-j-*.jar` into **`backend\lib\`** (ignored by `.gitignore`). Both `backend\run.bat` and `backend\test.bat` include `-cp "bin;lib\*"` automatically.

### Step 3: Pass Database Credentials (Optional)

If your local MySQL password differs from `root`, configure environment variables (recommended) or `-D` JVM properties before starting `backend\run.bat`:

```bat
set SUPIFY_DB_URL=jdbc:mysql://localhost:3306/supify?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
set SUPIFY_DB_USER=root
set SUPIFY_DB_PASS=your_password
```

| Setting | JVM Property | Environment Variable | Default |
| :--- | :--- | :--- | :--- |
| JDBC URL | `-Dsupify.db.url=...` | `SUPIFY_DB_URL` | `jdbc:mysql://localhost:3306/supify?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC` |
| DB User | `-Dsupify.db.user=...` | `SUPIFY_DB_USER` | `root` |
| DB Password | `-Dsupify.db.pass=...` | `SUPIFY_DB_PASS` | `root` |

### Step 4: Verify Driver & Connection Inside the CLI

Launch `backend\run.bat`, open **`[4] System & I/O`**, and select **`[6] Database (JDBC) Status`** to confirm:
- `Driver Available: YES (com.mysql.cj.jdbc.Driver)`
- `Live Connection: CONNECTED` (once `setup_mysql.bat` has created `supify` and valid credentials are set).

---

## 6. Windows Troubleshooting

- **`javac` or `mysql` not recognized:** Add your JDK `bin` folder (and `C:\Program Files\MySQL\MySQL Server 8.0\bin`) to Windows `PATH`, or invoke the full binary path.
- **PowerShell splitting `-cp bin;lib\*`:** Always quote the classpath as **`"bin;lib\*"`** so PowerShell does not interpret `;` as a command separator.
- **Trigger error when modifying `verification_check`:** By design (`trg_verification_check_no_update` and `trg_verification_check_no_delete`), rows in `verification_check` are append-only. Use `INSERT IGNORE` (as `01_canonical_suppliers.sql` does) when re-seeding.
- **Stale `.class` files in `backend/bin/`:** Delete `backend/bin/` and re-run `backend\run.bat` for a clean rebuild.
