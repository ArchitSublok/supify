# React concepts in the Supplier Workspace

The workspace applies the requested routing, state, async, and form concepts to
Supify. The mini projects are a supplier portfolio and a preparation dashboard,
both using the same live company draft.

## Run and explore

```sh
npm install
npm run dev
npm test
npm run build
```

| Route | What to try |
| --- | --- |
| `/supplier` | Add, complete, filter, and remove preparation tasks; inspect summary cards |
| `/supplier?tasks=done` | Share a checklist view; task contents are scoped to the current session |
| `/supplier/onboarding` | Redirects to the Organisation step |
| `/supplier/onboarding/organisation` | Edit controlled company fields and test URL/required validation |
| `/supplier/onboarding/capabilities` | Enter human-readable capacity, MOQ, and lead time |
| `/supplier/onboarding/claims` | Attach sample evidence and exercise file errors/retry/removal |
| `/supplier/onboarding/review` | Inspect all fields, edit earlier steps, and submit a demo listing |
| `/supplier/portfolio` | View a draft-based About page |
| `/supplier/portfolio/capabilities` | View a separate manufacturing capabilities page |
| `/supplier/portfolio/evidence` | View attachment records or the empty state |

## Navigation and multi-page SPA structure

`src/app/App.jsx` composes nested React Router routes. `WorkspaceLayout` retains
the provider while its `<Outlet>` changes between dashboard, onboarding, and
portfolio pages. Onboarding and portfolio each have their own nested outlets.
Links support direct entry, browser back/forward, and normal browser link actions.

`src/main.jsx` uses a data router so the workspace can use `useBlocker`. Leaving
with unsaved changes offers Stay, Leave without saving, and Save and leave.
Browser reload/close uses `beforeunload`; browser policy controls that prompt.
Navigation during final submission is blocked until the user sees the result.

All four onboarding steps are URL-addressable. Entering a later step validates
earlier required fields and redirects to the first invalid step. Going backward
does not require the current step to be valid. `steps.js` is the shared source
for navigation labels and headings.

## Global, scoped, URL, and local state

| Owner | State | Why it lives there |
| --- | --- | --- |
| Context + `useReducer` | Draft values, errors, touched fields, revisions, attachments, async statuses | Shared by workspace pages, scoped to the workspace lifecycle |
| Zustand `useTaskStore` | Preparation tasks and add/toggle/remove actions | Shared by dashboard, summary, and navigation count without prop drilling |
| URL | Onboarding step, portfolio section, task filter | Back/forward and deep links should restore the selected view |
| Component `useState` | New-task input, validation message, expanded demo feedback | Independent UI details that other pages do not need |
| `useRef` | Request controllers, in-flight promises, submit lock, file handles | Mutable coordination data that does not itself render UI |

The Zustand store is the introduction to an external state library. Components
select the values/actions they need with `useTaskStore(selector)`. It holds the
checklist; it does not duplicate the form reducer. Tasks survive SPA navigation
but intentionally reset on a full page reload. Company drafts persist locally.

`useSupplierWorkspace()` reads Context and exposes the reducer state plus commands.
The reducer is pure: it neither accesses storage nor starts requests. Related
transitions use named actions such as `FIELD_CHANGED`, `SAVE_SUCCEEDED`, and
`SUBMIT_FAILED`. Editing a submitted draft clears its current submission receipt.

## Async state and recovery

`load`, `save`, and `submit` each have `idle`, `pending`, `success`, and `error`
statuses. Each attachment has its own status. These independent states allow
editing while saving, showing an upload error beside its file, and keeping form
data after a failed submission.

- `useDraftAutosave` waits 500 ms after changes. It serializes saves and writes
  the newest snapshot next if more edits arrive during a save. The saved revision
  is separate from the edit revision so an older completion cannot mark newer
  values as saved. Save draft flushes the same pipeline immediately.
- Initial restoration finishes before autosave starts. The repository migrates
  the original unversioned draft shape, validates versioned drafts, and exposes
  invalid storage as a recoverable error. Explicit reset backs up the original.
- Load, upload, and save request IDs reject stale completions. Abort controllers
  cancel supported work on removal, reload, or unmount.
- Final submission validates everything, requires resolved attachments and a
  successful draft save, freezes edits, and uses an immediate ref lock. Retrying
  the unchanged payload reuses its idempotency key within the provider session.
- The repository returns a local demo receipt after recording it. The UI shows
  success only after that result and then autosaves the receipt with the draft.

Open **Demo response controls** below the workspace to select a two-second delay
or fail the next load/save/upload/submit request once. For a load failure, arm it
and use Reload saved draft after any unsaved edits finish saving. Errors include
specific retry actions, and toasts are not required to recover.

## Form handling and reusable inputs

`FormField` is used for both company fields and the dashboard's task input. It
connects labels, required markers, hint/error descriptions, `aria-invalid`, and
consistent focus styling. It renders input, select, or textarea with the same
interface. `fieldGroups` supplies listing field metadata.

Validation combines three strategies:

1. **Field-level:** validate on blur, then revalidate reported errors while typing.
2. **Step-level:** validate on form submit or forward navigation, show a summary,
   and focus the first invalid input. Semantic forms also support Enter.
3. **Final validation:** validate all steps before creating a submission. The
   command can map repository `fieldErrors` back to the appropriate step.

The required fields match the existing labels. Website URLs require HTTP/HTTPS.
Capacity and production terms stay readable text. The registry field accepts
generic identifiers without imposing GST-specific rules. Evidence accepts
non-empty PDF, PNG, and JPEG files up to 25 MB. Failed files can be retried or
removed. Unsent files must be reselected after reload; `File` objects are never
serialized into localStorage.

## Tests and boundaries

Vitest and React Testing Library cover required/URL/file validation, legacy and
damaged drafts, backup/reset, storage failure, stale completions, serialized
saves, cancellation, navigation gates, focus, portfolio updates, Zustand actions,
upload retry, duplicate-submit protection, and receipt persistence. Browser QA
also checks the workspace at desktop and mobile breakpoints.

This remains a frontend demo. File contents are not stored, no backend verification
ticket is created, and the portfolio is a private preview. Production needs an
authenticated API, ownership checks, server validation, durable idempotency, and
real evidence storage. Workspace data and demo receipts use localStorage, whose
access and quota failures are shown in the UI.

Implementation followed the reuse-first approach in
[Ponytail's skill](https://github.com/DietrichGebert/ponytail/blob/main/skills/ponytail/SKILL.md).
No Ponytail lifecycle hooks or global agent settings were installed.
