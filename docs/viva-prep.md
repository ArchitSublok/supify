# Supify — Pure-Java Backend & Database Viva Preparation Guide

> **Complete Topic-by-Topic Revision Book | Plain-English Explanations, Spoken Answers & Rapid-Fire Q&As**  
> Aligned 100% to the simplified **15-file pure-Java backend** (`backend/src/com/supify/` across **4 packages**: `model`, `dao`, `service`, `app`) and **5-table MySQL 8.0 schema** (`db/`).

---

## Quick Start: 30-Second Pitch & Live CLI Demo Path

### 30-Second Viva Pitch

> *"Supify is a B2B supplier discovery and verification backend written in Java. Any supplier can type 'we hold ISO 9001', and a buyer cannot cheaply tell if that is real or still valid today. Instead of a misleading `boolean isVerified` flag, we built a **4-layer verification spine**: a Supplier submits a **Claim** with **Evidence**; an independent Verifier writes an immutable **VerificationCheck**; and on PASS, the platform issues a time-bounded **Attestation** that expires automatically in the background via our daemon thread (`DecayMonitorThread`). All backend state sits inside one synchronized `SupifyPlatform` alongside `FileHelper` for CSV/log I/O and `JdbcSupplierDao` for MySQL CRUD."*

Two phrases examiners love:
- **Asymmetry Razor:** *"Every feature cuts either the cost of **finding** a supplier or the cost of **trusting** one."*
- **Authority Split:** *"The supplier asserts (`Claim` + `Evidence`); the platform attests (`VerificationCheck` + `Attestation`)."*

### 4 Packages, 15 Java Files (`backend/src/com/supify/`)

| Package | Files | Core Topics Demonstrated |
| :--- | :--- | :--- |
| `com.supify.model` (7) | `TrustBand`, `VerificationMethod`, `Supplier`, `Claim`, `Evidence`, `VerificationCheck`, `Attestation` | OOP, Encapsulation, Rich Enums with fields, `final` class immutability, `synchronized` state methods, `LocalDateTime` & `Duration` |
| `com.supify.dao` (3) | `Repository<T, ID>`, `FileHelper`, `JdbcSupplierDao` | Generics (`<T, ID>`), Character File I/O (`FileWriter`/`FileReader` + `BufferedWriter`/`BufferedReader` + append mode), Collections CSV integration, JDBC (`Class.forName`, `DriverManager`, `PreparedStatement`, `ResultSet`, CRUD) |
| `com.supify.service` (3) | `SupifyException`, `SupifyPlatform`, `DecayMonitorThread` | Custom checked exception (`extends Exception` + cause chaining), `HashMap`/`ArrayList`, `synchronized`, Java Stream API + lambdas (`search`), Authority Split, Trust Capping, Daemon Thread (`extends Thread`, `setDaemon(true)`, `volatile`, `sleep`, `interrupt`) |
| `com.supify.app` (2) | `Main`, `TestApp` | Interactive menus (`Scanner`, `switch`/`case`), try-with-resources & `finally` shutdown cleanup, lambda comparator sorting, 8-check test suite |

### Live CLI Demo Path (`backend/`)

- **Launch CLI:** `run.bat` (silently compiles into `bin/` and launches `com.supify.app.Main` with `-cp "bin;lib\*"`)
- **Run Automated Tests:** `test.bat` (runs `com.supify.app.TestApp` — **8 PASSED, 0 FAILED**)

| Menu | Keys to Press | What Happens Live |
| :--- | :--- | :--- |
| **Buyer Menu (`[1]`)** | `[1] → [1]`..`[4]` | `[1]` Lists all 5 seeded suppliers sorted by score; `[2]` runs Stream filter + lambda sort (`score`/`capacity`/`name`); `[3]` inspects `SUP-1`'s 4-layer `Claim → Evidence → Check → Attestation` chain; `[4]` compares 2 suppliers side by side |
| **Supplier Menu (`[2]`)** | `[2] → [1]`..`[3]` | `[1]` Views `SUP-1`'s standing; `[2]` submits a new `SUBMITTED` claim + PDF evidence (`CLM-113`); `[3]` displays live expiry timers (`SUP-1`'s capacity badge counts down from 30s and fires an `[ALERT]` via `DecayMonitorThread`) |
| **Verifier Menu (`[3]`)** | `[3] → [1]`..`[2]` | `[1]` Lists pending `SUBMITTED` claims with attached proof; `[2]` lets you approve/reject a claim, saving an immutable `VerificationCheck`, issuing an `Attestation` on PASS, and recalculating `TrustBand` immediately |
| **Admin Menu (`[4]`)** | `[4] → [1]`..`[4]` | `[1]` Thread Status (`main` + `DecayMonitor` daemon); `[2]` CSV Export/Import (`data/suppliers.csv`); `[3]` Append-mode Audit Log (`data/audit.log`); `[4]` MySQL JDBC Status (`url`, driver loaded, live connection test) |

---

## Topic 1: Problem Framing, Domain Model & 4-Layer Verification Spine

### 1.1 Say This in 15 Seconds
> *"Trust is not a yes/no boolean — knowing a supplier is verified means knowing **what** they claimed, **what proof** they uploaded, **how** a verifier checked it, and **when it expires**. We model this explicitly with 4 linked classes: `Claim`, `Evidence`, immutable `VerificationCheck`, and time-bounded `Attestation`, summarized into a `TrustBand` and numeric `trustScore`."*

### 1.2 The 4 Verification Layers (`com.supify.model`)
1. **Layer 1 — `Claim` (`Claim.java`):** What the supplier asserts — `id`, `supplierId`, data-driven `claimTypeKey` (e.g., `"gst"`, `"iso_9001"` — **no hardcoded `switch (claimType)`**), `label`, `pillar`, `value`, and `status` (`"SUBMITTED"`, `"VERIFIED"`, `"REJECTED"`, `"EXPIRED"`).
2. **Layer 2 — `Evidence` (`Evidence.java`):** Attached proof document linked via `claimId` — `id`, `title`, `docType`, and `filePath`.
3. **Layer 3 — `VerificationCheck` (`VerificationCheck.java`):** Strictly immutable (`public final class`, all fields `private final`, no setters) verdict storing `id`, `claimId`, `VerificationMethod`, `outcome` (`"PASS"`/`"FAIL"`), `verifierName`, `LocalDateTime checkedAt`, and mandatory `note`.
4. **Layer 4 — `Attestation` (`Attestation.java`):** Created **only** when `approved == true`. Stores `validUntil = verifiedAt.plusSeconds(validSeconds)`, `state` (`"ACTIVE"`, `"EXPIRED"`, `"REVOKED"`), `isExpired()`, `synchronized expireIfNeeded()`, `revoke()`, and `secondsLeft()`.

### 1.3 Rigor Weights & Trust Capping Rules (`SupifyPlatform.recalculateTrust`)
`VerificationMethod` stores each method's rigor weight cleanly inside the enum:
- `SELF_DECLARED (0.25)` · `DOCUMENT_REVIEW (0.60)` · `THIRD_PARTY_API (0.85)` · `HUMAN_AUDIT (0.95)` · `SITE_VISIT (1.00)`

Inside `SupifyPlatform.recalculateTrust(supplierId)`, only **`ACTIVE`** attestations count:
1. Compute `avgScore = Math.min(1.0, total / Math.max(3, activeCount))` *(dividing by at least 3 prevents a supplier with just one good check from jumping to 1.00!)*.
2. Apply 4 capping rules in order:
   - Missing active `IDENTITY` attestation → **`TrustBand.UNVERIFIED`**, `avgScore ≤ 0.30`.
   - Active `IDENTITY`, missing active `LEGAL` → **`TrustBand.BASIC`**, `avgScore ≤ 0.65`.
   - Active `IDENTITY` + `LEGAL`, plus at least one `HUMAN_AUDIT` or `SITE_VISIT`, and `avgScore ≥ 0.85` → **`TrustBand.AUDITED`**.
   - Otherwise → **`TrustBand.VERIFIED`**.

### 1.4 Seeded Sample Suppliers (`SupifyPlatform.loadSampleData`)
- **`SUP-1` (Aravind Fasteners):** Starts **`AUDITED` (`0.85`)** with 4 active attestations — `(0.85 + 0.60 + 0.95 + 1.00) / 4 = 0.85`. Its 4th claim (`CAPABILITY`, `SITE_VISIT`) has a **30-second TTL** so you can watch live decay to **`VERIFIED` (`0.80`)** during a demo!
- **`SUP-2` (Monarch Industrial):** **`AUDITED` (`0.93`)** (`(0.85 + 0.95 + 1.00) / 3`) plus 1 pending `SUBMITTED` claim (`ISO 14001`) ready for the Verifier Menu.
- **`SUP-3` (Kavach Safety):** **`VERIFIED` (`0.48`)** — has `IDENTITY` + `LEGAL`, no audit (`(0.60 + 0.85) / 3`).
- **`SUP-4` (Suryodaya Solar):** **`BASIC` (`0.20`)** — has `IDENTITY` only (`0.60 / 3`).
- **`SUP-5` (Bharat Forgings):** **`UNVERIFIED` (`0.00`)** — only 1 pending `SUBMITTED` claim (zero active attestations).

### 1.5 Spoken Viva Q&As — Topic 1
- **Q1. What problem does Supify solve, and what is the Asymmetry Razor?**  
  *Say:* *"Buyers cannot cheaply verify if supplier claims are true or still valid today. Our Asymmetry Razor says every feature must reduce either the cost of finding a supplier or the cost of trusting one."*
- **Q2. Why not just use `boolean isVerified` inside `Supplier`?**  
  *Say:* *"A boolean hides how a claim was checked, who verified it, why, and when it expires. Our 4-layer spine (`Claim → Evidence → VerificationCheck → Attestation`) makes trust transparent and time-bounded."*
- **Q3. How do you enforce Separate Write Authority?**  
  *Say:* *"`SupifyPlatform.submitClaim(...)` only creates a `Claim` in `SUBMITTED` status and `Evidence`. Only `verifyClaim(...)` (called from the Verifier Menu) can create a `VerificationCheck` and `Attestation`."*
- **Q4. Why is `VerificationCheck` marked `public final` with no setters?**  
  *Say:* *"Once a verifier records PASS or FAIL with a written rationale, that audit verdict must never be altered. `final` class + `private final` fields enforce compile-time immutability."*
- **Q5. Why does `SUP-1` drop from `AUDITED (0.85)` to `VERIFIED (0.80)` after 30 seconds?**  
  *Say:* *"When its 30-second `SITE_VISIT` (`1.00`) attestation expires, active attestations drop from 4 to 3: `(0.85 + 0.60 + 0.95) / 3 = 0.80`, which falls below the `0.85` `AUDITED` threshold."*
- **Q6. Why store `claimTypeKey` as a `String` instead of subclasses like `GstClaim` or a `switch (claimType)`?**  
  *Say:* *"Keeping `claimTypeKey` as data means adding a new certification requires zero new Java subclasses or `switch` edits."*

---

## Topic 2: Core OOP, Strings, Arrays, Wrappers & Enums

### 2.1 Say This in 15 Seconds
> *"Our 15-file backend applies all 4 OOP pillars cleanly across 4 packages (`model`, `dao`, `service`, `app`): private encapsulated entities, interface abstraction via `Repository<T, ID>`, inheritance via `DecayMonitorThread extends Thread` and `SupifyException extends Exception`, composition inside `SupifyPlatform`, and runtime polymorphism where `InMemory`-style `SupifyPlatform` and `JdbcSupplierDao` share one `Repository` contract."*

### 2.2 OOP Concept Map to Exact Classes
- **Encapsulation:** All fields in `Supplier`, `Claim`, `Evidence`, `Attestation`, `VerificationCheck` are `private` with controlled getters/setters (`Claim.setStatus`, `Attestation.expireIfNeeded`, `Supplier.setTrustBand`).
- **Abstraction:** `Repository<T, ID>` declares what CRUD operations exist (`save`, `findById`, `findAll`, `deleteById`, `count`) without exposing whether storage is a `HashMap` or MySQL.
- **Inheritance (`IS-A`) vs. Composition (`HAS-A`):**
  - *Inheritance:* `DecayMonitorThread extends Thread`, `SupifyException extends Exception`.
  - *Composition:* `DecayMonitorThread` holds a `SupifyPlatform` reference (`has-a`); `SupifyPlatform` holds collections of `Supplier`, `Claim`, `Evidence`, `VerificationCheck`, and `Attestation`.
- **Polymorphism:**
  - *Compile-time (Overloading):* `DecayMonitorThread(SupifyPlatform)` vs `DecayMonitorThread(SupifyPlatform, int delayMs)`; `SupifyException(String)` vs `SupifyException(String, Throwable)`.
  - *Runtime (Overriding):* `@Override run()` in `DecayMonitorThread`; `@Override save/findById/findAll/deleteById/count` in both `SupifyPlatform` and `JdbcSupplierDao`; `@Override toString()` in `Supplier`, `Claim`, `Evidence`, `VerificationCheck`.
- **Strings, Arrays, Wrappers & Rich Enums:**
  - *String & Immutability:* `String` is immutable (pooled in String Constant Pool, safe as `HashMap<String, Supplier>` keys and JDBC SQL templates); `.trim()`, `.toLowerCase()`, `.equalsIgnoreCase()` are used across `SupifyPlatform` and `FileHelper`. Always compare strings with `.equals()`, never `==`!
  - *Arrays:* `String[] args` in `Main`/`TestApp`, and `String[] parts = line.split(",")` in `FileHelper.importFromCsv`.
  - *Wrappers:* `Integer.parseInt`, `Double.parseDouble` convert CSV strings back into primitives inside `FileHelper`. Remember `Integer` caches `-128..127`!
  - *Rich Enums:* `TrustBand` (`UNVERIFIED(0)..AUDITED(3)`) and `VerificationMethod` (`SELF_DECLARED(0.25)..SITE_VISIT(1.00)`) carry fields, constructors, and getters (`getLevel()`, `getScore()`).

### 2.3 Spoken Viva Q&As — Topic 2
- **Q7. Explain `this`, `super`, `static`, and `final` using examples from this project.**  
  *Say:* *"`this` disambiguates fields in constructors (`this.platform = platform`); `super` calls parent constructors (`super(\"DecayMonitor\")` in `DecayMonitorThread`, `super(message)` in `SupifyException`); `static` lets helper/JDBC methods run without creating objects (`FileHelper.exportToCsv`, `JdbcSupplierDao.getConnection`); and `final` locks references (`private final Map...`), blocks overriding, and makes `VerificationCheck` non-subclassable."*
- **Q8. Is Java pass-by-value or pass-by-reference when you pass a `Supplier` into `platform.save(sup)`?**  
  *Say:* *"Always pass-by-value! Java copies the object's reference (address) into the parameter, so `recalculateTrust` can mutate that Heap `Supplier` via `setTrustBand`, while reassigning the parameter variable wouldn't affect the caller."*
- **Q9. Why use `Repository` as an `interface` instead of an `abstract class`?**  
  *Say:* *"Because `SupifyPlatform` and `JdbcSupplierDao` share a storage capability contract without sharing instance state or wasting Java's single-inheritance slot."*

---

## Topic 3: Exception Handling & Resource Safety

### 3.1 Say This in 15 Seconds
> *"We separate checked business errors from unchecked runtime errors. `SupifyException extends Exception` is a custom **checked** exception thrown when supplier or verifier input violates domain rules, forcing the CLI to catch it gracefully. File streams and JDBC resources always use **`try-with-resources`** so they auto-close even if an exception occurs."*

### 3.2 Checked vs. Unchecked in Our Code
- **Custom Checked Exception (`SupifyException.java`):** Extends `Exception` with two constructors (`(String message)` and `(String message, Throwable cause)`). Thrown by `SupifyPlatform.submitClaim` (missing supplier, blank label) and `verifyClaim` (missing claim, blank rationale) and caught in `Main.java` (`catch (SupifyException e)`).
- **Exception Chaining & Wrapping:**
  - `JdbcSupplierDao.getConnection()` catches `ClassNotFoundException` and rethrows `new SQLException(lastError, e)` (preserving the root cause).
  - `JdbcSupplierDao` CRUD methods catch checked `SQLException` and throw `RuntimeException` because `Repository<T, ID>` doesn't declare `throws SQLException`.
- **Unchecked Exceptions Caught Safely:**
  - `Main.readInt()` catches `NumberFormatException` on bad console typing and returns a default fallback.
  - `FileHelper.importFromCsv()` catches `IllegalArgumentException` (which is the superclass of *both* `NumberFormatException` and invalid `TrustBand.valueOf(...)`!) so one malformed CSV row prints a warning and is skipped instead of crashing the import.
- **`finally` & `try-with-resources`:**
  - `Main.main()` wraps `try (Scanner sc = new Scanner(System.in)) { ... } finally { monitor.stopMonitor(); }` — guaranteeing both `Scanner.close()` and daemon shutdown even on unexpected exits.
  - `FileHelper` (`FileWriter`/`BufferedWriter`, `FileReader`/`BufferedReader`) and `JdbcSupplierDao` (`Connection`, `PreparedStatement`, `ResultSet`) use `try-with-resources` (`AutoCloseable`), closing resources in **reverse order** without leaking handles or losing primary exceptions.

### 3.3 Spoken Viva Q&As — Topic 3
- **Q10. Difference between `throw` and `throws`?**  
  *Say:* *"`throw` actually throws one exception object inside a method (`throw new SupifyException(\"...\")`), while `throws` declares in the method signature that checked exceptions may propagate to the caller."*
- **Q11. Why does `FileHelper.importFromCsv()` catch `IllegalArgumentException` instead of catching only `NumberFormatException`?**  
  *Say:* *"Because `NumberFormatException` is a subclass of `IllegalArgumentException`, and `TrustBand.valueOf(...)` also throws `IllegalArgumentException` for bad enum strings — so catching `IllegalArgumentException` handles both bad numbers and bad bands cleanly in one block."*
- **Q12. Why prefer `try-with-resources` over closing connections in `finally`?**  
  *Say:* *"It automatically closes every `AutoCloseable` resource in reverse creation order, requires no verbose null checks, and preserves suppressed exceptions if both `try` and `close()` fail."*

---

## Topic 4: Generics, Collections, Lambdas & Stream API

### 4.1 Say This in 15 Seconds
> *"Our `Repository<T, ID>` interface uses **Generics** for compile-time type safety across both `SupifyPlatform` and `JdbcSupplierDao`. Inside `SupifyPlatform`, we store records in `HashMap` ($O(1)$ lookup by ID) and `ArrayList` (ordered history), and power `search()` with **Java Streams** and **Lambda expressions** for declarative filtering and sorting."*

### 4.2 Exact Usage in the 15 Files
- **Generics (`Repository.java`):**
  - `public interface Repository<T, ID>` defines `save(T)`, `findById(ID)`, `findAll()`, `deleteById(ID)`, `count()`.
  - `SupifyPlatform implements Repository<Supplier, String>` and `JdbcSupplierDao implements Repository<Supplier, String>` bind `T = Supplier`, `ID = String`.
  - Uses the **diamond operator** (`new HashMap<>()`, `new ArrayList<>()`) for type inference.
  - Note on Type Erasure: Java erases generic `<T>` at bytecode level (which is why you can't write `List<int>` — must use `List<Integer>`).
- **Collections in `SupifyPlatform`:**
  - `Map<String, Supplier> suppliers`, `Map<String, Claim> claims`, `Map<String, Attestation> attestations` (`HashMap` gives $O(1)$ average lookup by ID; `HashMap` uses `hashCode()` + `.equals()` and treeifies buckets ≥ 8 nodes in Java 8+).
  - `List<Evidence> evidences`, `List<VerificationCheck> checks`, `List<String> alerts` (`ArrayList` preserves chronological insert order).
  - **Defensive Copy:** `findAll()` returns `new ArrayList<>(suppliers.values())` so iterating callers never hit `ConcurrentModificationException`.
  - **Collections Utility:** `getAllCategories()` deduplicates into an `ArrayList` and calls `Collections.sort(categories)`.
- **Lambdas & Stream Pipeline (`SupifyPlatform.search` & `getComparator`):**
  ```java
  return findAll().stream()
          .filter(s -> q.isEmpty() || s.getName().toLowerCase().contains(q)
                  || s.getCity().toLowerCase().contains(q)
                  || s.getCategory().toLowerCase().contains(q))
          .filter(s -> minBand == null || s.getTrustBand().getLevel() >= minBand.getLevel())
          .sorted(getComparator(sortBy))
          .collect(Collectors.toList());
  ```
  - **Functional Interfaces used:** `Predicate<Supplier>` inside `.filter(...)`, and `Comparator<Supplier>` returned as lambdas inside `getComparator(sortBy)` (`(a, b) -> Integer.compare(...)`, `(a, b) -> a.getName().compareToIgnoreCase(...)`, `(a, b) -> Double.compare(...)`), plus `all.sort((a, b) -> a.getId().compareTo(b.getId()))` in `Main.pickSupplier`.
  - **Stream Laziness:** `.filter()` and `.sorted()` are **intermediate** lazy steps; nothing runs until the **terminal** `.collect(Collectors.toList())` triggers the pipeline. A consumed Stream cannot be reused.

### 4.3 Spoken Viva Q&As — Topic 4
- **Q13. What is the difference between a Collection and a Stream?**  
  *Say:* *"A Collection (`ArrayList`, `HashMap`) stores data in memory and can be iterated many times. A Stream is a one-time-use, lazy pipeline that computes transformed/filtered results on demand without mutating the source collection."*
- **Q14. Why can't a local variable like `q` inside `SupifyPlatform.search()` be reassigned before being used inside the `.filter(s -> ...)` lambda?**  
  *Say:* *"Because Java requires local variables captured by a lambda to be `final` or **effectively final**, since the lambda captures a copy of the variable's value."*
- **Q15. Explain what happens inside `HashMap` when two keys have the same `hashCode()`?**  
  *Say:* *"That's a hash collision — both map to the same bucket. `HashMap` uses `.equals()` to tell distinct keys apart and chains them in that bucket (converting long chains ≥ 8 to a balanced Red-Black tree in Java 8+ for $O(\log n)$ lookup)."*

---

## Topic 5: Multithreading, Daemon Lifecycle & Synchronization

### 5.1 Say This in 15 Seconds
> *"`DecayMonitorThread extends Thread`, sets `setDaemon(true)` so it never blocks JVM exit, and loops with `Thread.sleep(delayMs)` checking `platform.checkExpiredAttestations()`. Because `main` and `DecayMonitor` share the same `SupifyPlatform` object, all state-changing methods are `synchronized`, and `volatile boolean running` ensures a clean stop signal across CPU caches."*

### 5.2 Thread Lifecycle & Synchronization Mechanics
- **6 JVM Thread States (`Thread.State`):** `NEW` → `RUNNABLE` ↔ `BLOCKED` / `WAITING` / `TIMED_WAITING` (`Thread.sleep(delayMs)`) → `TERMINATED`.
- **Starting (`start()` vs `run()`):**
  - `monitor.start()` spawns a new OS thread and call stack, then calls `run()`. Calling `monitor.run()` directly runs on `main` and freezes the menu!
- **Daemon Thread (`setDaemon(true)`):**
  - Must be set *before* `start()`. JVM exits automatically when all user (non-daemon) threads terminate.
- **Cooperative Cancellation (`stopMonitor()`):**
  - Sets `running = false` (`volatile` forces immediate visibility from main RAM instead of stale CPU cache) and calls `interrupt()` so if the thread is sleeping inside `Thread.sleep(4000)`, it immediately catches `InterruptedException` and breaks out of the `while (running)` loop.
- **Synchronization (`synchronized` monitor locks):**
  - `SupifyPlatform` synchronizes `nextId`, `save`, `findById`, `findAll`, `submitClaim`, `verifyClaim`, `recalculateTrust`, `checkExpiredAttestations`, and `getAndClearAlerts` on `this` (`SupifyPlatform`'s intrinsic lock).
  - Mutable model helpers (`Attestation.expireIfNeeded`, `Claim.setStatus`, `Supplier.setTrustBand`) are also `synchronized`.
- **Viva Comparison — `sleep()` vs `wait()` / `notifyAll()`:**
  - `Thread.sleep(ms)` pauses the current thread for a fixed time and **keeps holding** any monitor locks.
  - `Object.wait()` / `notifyAll()` (must be called inside `synchronized` on the lock object, or `IllegalMonitorStateException` is thrown, and always checked inside a `while` guard for spurious wakeups) **releases the monitor lock** while waiting for another thread to change a condition (such as producer–consumer queues). In Supify, our background monitor wakes on a fixed polling timer (`Thread.sleep(delayMs)`) rather than blocking on a producer condition, so it uses `sleep()` + `synchronized` shared methods + `volatile running` + `interrupt()`.

### 5.3 Spoken Viva Q&As — Topic 5
- **Q16. Why can't you rely ONLY on `volatile` instead of `synchronized` inside `SupifyPlatform.nextId()`?**  
  *Say:* *"`volatile` guarantees visibility across CPU caches, NOT atomicity. `idCounter++` is a 3-step read-add-write operation, so two threads could still generate duplicate IDs without `synchronized`."*
- **Q17. What does `Admin Menu → [1] Thread Status` show (`printThreadInfo()`)?**  
  *Say:* *"It prints the currently executing `main` thread (`RUNNABLE`, `daemon=false`) alongside our background `DecayMonitor` thread (`TIMED_WAITING` during `sleep` or `RUNNABLE`, `daemon=true`)."*
- **Q18. What happens if a thread calls `wait()` or `notifyAll()` outside a `synchronized` block?**  
  *Say:* *"The JVM immediately throws `IllegalMonitorStateException` because the current thread does not own the object's intrinsic monitor lock."*

---

## Topic 6: File Handling & Collections Integration (`FileHelper`)

### 6.1 Say This in 15 Seconds
> *"`FileHelper` bridges live in-memory `List<Supplier>` collections with persistent disk files using Java **Character Streams** (`FileWriter`/`FileReader` wrapped in `BufferedWriter`/`BufferedReader` inside `try-with-resources`). It exports and imports `data/suppliers.csv` safely while skipping malformed rows, and appends timestamped events to `data/audit.log` using `FileWriter(file, true)`."*

### 6.2 How `FileHelper` Works
1. **CSV Export (`exportToCsv(file, List<Supplier>)`):**
   - Calls `ensureParentDir(file)` (`mkdirs()`).
   - Uses `BufferedWriter(new FileWriter(file))` to write header `id,name,city,category,capacity,moq,leadTime,trustBand,trustScore`.
   - Sanitizes strings via `clean()` (replacing `,` with space) so commas in a supplier name don't shift CSV columns, and rounds `trustScore` to 2 decimals.
2. **CSV Import (`importFromCsv(file)` → `List<Supplier>`):**
   - Reads lines with `BufferedReader.readLine()`, skips the header and blank lines, splits via `line.split(",")`, checks `parts.length >= 9`, parses `Integer.parseInt`, `TrustBand.valueOf`, `Double.parseDouble` inside a per-row `try-catch (IllegalArgumentException)`, logs bad rows, and returns valid `List<Supplier>`.
3. **Append-Mode Audit Log (`appendAuditLog` & `readAuditLog`):**
   - Uses `new FileWriter(file, true)` (`true` = **append mode**, preserving past log entries instead of overwriting `data/audit.log`!) wrapped in `BufferedWriter`, writing `[" + LocalDateTime.now() + "] " + message`.
   - `readAuditLog(file)` checks `if (!file.exists()) return lines;` and reads all lines into `List<String>`.
4. **Byte Streams vs. Character Streams (Quick Viva Knowledge):**
   - **Character streams** (`FileReader`/`FileWriter`, 16-bit Unicode `char`) are ideal for human-readable CSV/log text files like ours; wrapping in `BufferedReader`/`BufferedWriter` minimizes disk syscalls and adds `readLine()`/`newLine()`.
   - **Byte streams** (`FileInputStream`/`FileOutputStream`, 8-bit `byte`) are used when processing raw binary data (PDFs, images, serialized bytes, or exact byte-level encoding buffers).

### 6.3 Spoken Viva Q&As — Topic 6
- **Q19. Why pass `true` to `new FileWriter(file, true)` inside `appendAuditLog`?**  
  *Say:* *"Without the second boolean argument `true`, `FileWriter` truncates the file to zero bytes every time it opens, wiping out previous audit entries. Passing `true` enables append mode."*
- **Q20. Why wrap `FileReader` inside `BufferedReader`?**  
  *Say:* *"Raw `FileReader.read()` hits the OS for every single character. `BufferedReader` buffers chunks in memory for speed and provides convenient line-by-line `readLine()`."*

---

## Topic 7: MySQL 8.0 5-Table Database & JDBC (`JdbcSupplierDao`)

### 7.1 Say This in 15 Seconds
> *"Our MySQL database has 5 clean InnoDB tables matching our 5 Java domain entities **1:1** (`supplier`, `claim`, `evidence`, `verification_check`, `attestation`). In Java, `JdbcSupplierDao implements Repository<Supplier, String>` uses the **Type-4 MySQL driver**, `DriverManager.getConnection`, and **`PreparedStatement`** with `?` parameter binding to run all CRUD operations safely without SQL injection."*

### 7.2 5-Table Schema (`db/schema/01_supify_schema.sql`)
1. `supplier` (`id PK`, `name`, `city`, `category`, `capacity_per_month`, `moq`, `lead_time`, `trust_band`, `trust_score`, `created_at`) — no `is_verified` boolean!
2. `claim` (`id PK`, `supplier_id FK → supplier(id) ON DELETE CASCADE`, `claim_type_key`, `label`, `pillar`, `asserted_value`, `status`)
3. `evidence` (`id PK`, `claim_id FK → claim(id) ON DELETE CASCADE`, `title`, `kind`, `storage_ref`)
4. `verification_check` (`id PK`, `claim_id FK → claim(id) ON DELETE CASCADE`, `method`, `outcome`, `performed_by`, `rationale`)
5. `attestation` (`id PK`, `claim_id FK → claim(id) ON DELETE CASCADE`, `source_check_id FK → verification_check(id) ON DELETE RESTRICT`, `method`, `verified_at`, `valid_until`, `state`, `issued_by`)

### 7.3 JDBC Mechanics in `JdbcSupplierDao.java`
- **Driver & URL:**
  - Driver class: `"com.mysql.cj.jdbc.Driver"` (pure-Java **Type-4** driver JAR placed in `backend/lib/`).
  - URL: `"jdbc:mysql://localhost:3306/supify?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"`.
  - Reads optional `SUPIFY_DB_PASS` environment variable (defaults to `"root"`).
- **Connection & Status:** `Class.forName(DRIVER)` checks driver presence; `DriverManager.getConnection(URL, USER, getPassword())` opens the session; `testConnection()` opens a `try-with-resources` connection and checks `!conn.isClosed()`.
- **Full CRUD (`PreparedStatement` + `ResultSet`):**
  - **Create / Upsert (`save`):** Checks `if (findById(s.getId()) != null) deleteById(s.getId());`, then runs parameterized `INSERT INTO supplier (...) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)` with `ps.executeUpdate()`.
  - **Read One (`findById`):** Parameterized `SELECT ... FROM supplier WHERE id = ?` with `ps.executeQuery()`, `if (rs.next()) return mapRow(rs);`, else `null`.
  - **Read All (`findAll`):** `SELECT ... FROM supplier ORDER BY trust_score DESC` with `while (rs.next()) list.add(mapRow(rs));`.
  - **Update (`updateTrust`):** `UPDATE supplier SET trust_band = ?, trust_score = ? WHERE id = ?` → returns `ps.executeUpdate() > 0`.
  - **Delete (`deleteById`):** `DELETE FROM supplier WHERE id = ?` → cascades to child tables in MySQL (`ON DELETE CASCADE`).
  - **Count (`count`):** `SELECT COUNT(*) FROM supplier` → returns `rs.getLong(1)`.
  - **`mapRow(ResultSet rs)`:** Uses column names (`rs.getString("name")`, `rs.getInt("moq")`, `TrustBand.valueOf(rs.getString("trust_band"))`, `rs.getDouble("trust_score")`) to construct a `Supplier` object cleanly.

### 7.4 Spoken Viva Q&As — Topic 7
- **Q21. Why use `PreparedStatement` with `?` instead of concatenating strings into a `Statement`?**  
  *Say:* *"`PreparedStatement` pre-compiles the SQL template and binds inputs strictly as data values via `setString`/`setInt`/`setDouble`, completely blocking SQL injection attacks."*
- **Q22. Difference between `executeQuery()` and `executeUpdate()`?**  
  *Say:* *"`executeQuery()` runs `SELECT` queries and returns a `ResultSet` cursor. `executeUpdate()` runs `INSERT`/`UPDATE`/`DELETE` and returns an `int` row count."*
- **Q23. Why do we use `if (rs.next())` in `findById()` and `while (rs.next())` in `findAll()`?**  
  *Say:* *"A `ResultSet` cursor starts **before** the first row. `rs.next()` advances one row and returns `false` when no rows remain — forgetting `rs.next()` throws `SQLException`."*
- **Q24. Why does `attestation.source_check_id` use `ON DELETE RESTRICT` while `claim_id` uses `ON DELETE CASCADE`?**  
  *Say:* *"If an entire supplier or claim is deleted, child rows cascade cleanly; `ON DELETE RESTRICT` on `source_check_id` prevents deleting an audit `verification_check` while an `attestation` still points to it as proof."*

---

## Topic 8: `TestApp` 8-Check Verification & 1-Minute Cheat Sheet

`TestApp.java` runs cleanly via `backend\test.bat` with zero external test frameworks (**8 PASSED, 0 FAILED**):
1. Seed Data & Generic `Repository` (`save`, `findById`, `deleteById`, `count() == 5`)
2. Stream Search & Sorting (`search`, `TrustBand` threshold, `getAllCategories`)
3. 4-Layer Spine (`Claim → Evidence → VerificationCheck → Attestation`)
4. Trust Band Capping (`UNVERIFIED ≤ 0.30`, `BASIC ≤ 0.65`) & `REJECTED` verdict
5. Daemon Auto-Decay (`DecayMonitorThread.isDaemon()`, 1s expiry → `EXPIRED` + alert)
6. CSV Export/Import + malformed row skip + append-mode `audit.log` (`FileHelper`)
7. Checked `SupifyException` validation guards (missing IDs, blank labels, blank notes)
8. JDBC Config & Driver Check (`JdbcSupplierDao.getUrl()`, `isDriverLoaded()`)

| Topic Asked by Examiner | Point Directly To |
| :--- | :--- |
| Domain / Why no boolean flag | `Claim`, `Evidence`, `VerificationCheck`, `Attestation` + `SupifyPlatform.recalculateTrust` |
| Immutability & Enums | `public final class VerificationCheck`, `TrustBand(level)`, `VerificationMethod(score)` |
| Generics & Interface Polymorphism | `Repository<T, ID>` implemented by both `SupifyPlatform` & `JdbcSupplierDao` |
| Streams & Lambdas | `SupifyPlatform.search` (`.stream().filter().sorted().collect()`) & `getComparator` |
| Checked Exception | `SupifyException extends Exception` + guards in `submitClaim`/`verifyClaim` |
| Multithreading & Synchronization | `DecayMonitorThread` (`setDaemon(true)`, `volatile`, `sleep`, `interrupt`) + `synchronized SupifyPlatform` |
| File I/O + Collections | `FileHelper` (`FileWriter`/`FileReader` + `Buffered`, CSV & append-mode `audit.log`) |
| JDBC & MySQL CRUD | `JdbcSupplierDao` (`PreparedStatement`, `ResultSet`, `mapRow`) + 5 InnoDB tables in `db/` |
