# Supify — Advanced React Architecture Guide

This document provides a comprehensive overview of modern React patterns, state management architecture, routing design, and form handling strategies implemented in Supify. It is designed as an architectural reference and defense guide for project presentations and vivas.

---

## 1. Multi-Page SPA Design & Routing Architecture

Supify is built as a single-page application (SPA) with deep nested routing enabled by `react-router-dom`:

* **Data Router Initialization (`src/main.jsx`)**: Uses `createBrowserRouter` and `RouterProvider` to support advanced routing capabilities, such as `useBlocker` navigation guards.
* **Layout Routes & `<Outlet />` Composition**:
  * **Top-Level Root**: Global layout in `src/app/App.jsx` hosting the persistent `Header`, toast notification root, and authentication modal.
  * **Nested Supplier Workspace (`/supplier`)**: `WorkspaceLayout` manages the sidebar tabs, progress trackers, and draft sync status while rendering child routes via `<Outlet />`:
    * `/supplier` &rarr; `WorkspaceDashboard` (Checklist + status overview)
    * `/supplier/onboarding/:step` &rarr; `OnboardingScreen` containing step-level routes (`OnboardingStep`)
    * `/supplier/portfolio/*` &rarr; `PortfolioScreen` sub-tabs (`PortfolioAbout`, `PortfolioCapabilities`, `PortfolioEvidence`)
* **Dynamic Route Parameters & URL State**:
  * `:supplierId` in `/suppliers/:supplierId`
  * `:taskId` in `/verifier/task/:taskId`
  * `:step` in `/supplier/onboarding/:step`
  * `useSearchParams` is used for filters (`?q=...&band=...&region=...`), verification deep links (`?supplier=...`), and review queue filters (`?tasks=done`), ensuring all UI states are shareable and bookmarkable.
* **Role-Based Protected Routes**:
  * `src/app/ProtectedRoute.jsx` checks the authenticated user role from `UserRoleContext`. Unprivileged access renders an interactive role-switch screen rather than an unhelpful 404 or redirect loop.
* **Wildcard Fallback (404)**:
  * `src/app/NotFound.jsx` catches all unmatched routes at the root (`/*`) and sub-levels (`/supplier/*`).

---

## 2. State Management Architecture: Global vs Local State

Supify uses a tiered state management approach tailored to state lifecycle and locality:

```
┌────────────────────────────────────────────────────────┐
│                   Global Role Context                  │
│       UserRoleContext: 'buyer' | 'supplier' | 'verifier'│
└──────────────────────────┬─────────────────────────────┘
                           │
         ┌─────────────────┴─────────────────┐
         ▼                                   ▼
┌──────────────────┐               ┌──────────────────┐
│  Zustand Store   │               │ WorkspaceContext │
│   (taskStore)    │               │  (useReducer)    │
│ Cross-page tasks │               │ Form draft & sync│
└──────────────────┘               └──────────────────┘
```

### A. Global Identity: React Context API (`UserRoleContext.jsx`)
* **Purpose**: Maintains active session role across the entire application.
* **Pattern**: Lightweight `createContext` + `useState` provider wrapping the application root in `main.jsx`.
* **Consumers**: `Header.jsx`, `ProtectedRoute.jsx`, `App.jsx`, and `VerificationDashboard.jsx`.

### B. Cross-Component Store: Zustand (`taskStore.js`)
* **Purpose**: Manages preparation checklist tasks across multiple workspace views without unnecessary Context provider boilerplate.
* **Implementation**: `create()` from Zustand with actions (`addTask`, `toggleTask`, `removeTask`).
* **Why Zustand over Redux Toolkit?**: Zero boilerplate, minimal bundle size, direct hook selectors (`useTaskStore((s) => s.tasks)`), and avoids wrapping the tree with store providers.

### C. Feature State Machine: Context + `useReducer` (`WorkspaceContext.jsx` & `workspaceReducer.js`)
* **Purpose**: Manages complex supplier onboarding draft state, form validation, upload queues, and autosave status.
* **Implementation**: 16 discrete actions (`LOAD_STARTED`, `FIELD_CHANGED`, `VALIDATION_FAILED`, `UPLOAD_STARTED`, `SUBMIT_SUCCEEDED`, etc.).
* **Separation of Concerns**: Pure state transitions reside in the reducer; async I/O, timers, and abort controllers reside in custom hooks and repository layers.

---

## 3. Form Handling & Validation Strategies

* **Reusable Input Component (`src/shared/ui/FormField.jsx`)**:
  * Polymorphic input renderer: dynamically renders `<input>`, `<select>`, or `<textarea>`.
  * Accessibility-first: automatic unique ID generation via `useId()`, coupled with `aria-invalid` and `aria-describedby` pointing to error and hint elements.
  * Used consistently across onboarding forms, dashboard task inputs, and global auth modals.
* **Three-Tier Validation Strategy (`validation.js`)**:
  1. **Immediate On-Blur**: Flags errors as soon as the user exits an input (`FIELD_BLURRED`).
  2. **Step Navigation Guard**: Validates all fields in the active step when clicking "Next"; blocks progression and focuses the first invalid input if errors exist.
  3. **Pre-Submission Full Audit**: Validates the entire payload across all steps prior to ledger submission.

---

## 4. Async State & Error Handling

* **Async State Lifecycle**:
  Every remote or storage operation follows an explicit status lifecycle:
  $$\text{idle} \longrightarrow \text{pending} \longrightarrow \text{success} \mid \text{error}$$
* **Network & I/O Resilience**:
  * **AbortController & Cleanups**: In-flight requests are automatically aborted on component unmount or fast route transitions.
  * **Request ID Tracking**: Prevents race conditions and stale response overwrites.
  * **Debounced Autosave (`useDraftAutosave.js`)**: Serializes writes to storage with a 500ms debounce to prevent thrashing.
  * **User Retry Controls**: Failed operations in `SearchScreen.jsx`, `WorkspaceLayout.jsx`, and file uploads display actionable retry buttons.
* **Global Error Boundary (`src/shared/ui/ErrorBoundary.jsx`)**:
  * Class-based React Error Boundary wrapping the root app in `main.jsx`.
  * Traps unhandled rendering exceptions, preventing blank-screen crashes and presenting a recovery action.

---

## 5. Key Talking Points for Presentations & Vivas

| Question / Topic | Answer in Supify Architecture |
| :--- | :--- |
| **Why both Context API and Zustand?** | Context API is ideal for low-frequency global identity (user role), while Zustand provides decoupled, high-performance state selection for cross-screen task management without provider nesting. |
| **Why `useReducer` instead of multiple `useState` calls in Workspace?** | The onboarding draft has 15+ interconnected fields, revision counters, upload queues, and async statuses. `useReducer` centralizes state transitions in one testable pure function. |
| **How does URL-driven state work?** | Critical filters (`q`, `band`, `region`, `supplier`) are reflected in search params via `useSearchParams`, allowing shareable URLs, browser back/forward navigation, and persistent filtering. |
| **How is memory leakage avoided in async calls?** | All `useEffect` hooks conducting async operations utilize cleanup functions bound to `AbortController` signals or local cancellation tokens (`cancelled = true`). |
| **How is form accessibility handled?** | `FormField` generates deterministic IDs via `useId()` and links them to error messages using `aria-describedby` and `aria-invalid`. |
