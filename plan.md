# Supplier Workspace: React Feature Plan

## Goal

Improve the existing Supplier Workspace with validated forms, predictable async
state, draft persistence, evidence uploads, and useful loading/error feedback.
Use `useReducer` and focused custom hooks to keep the page understandable and
maintainable. The workspace implementation is now available; see
`docs/react-workspace.md` for routes, state ownership, demo controls, and checks.

## Implemented additions

- Nested SPA routes for the dashboard, four onboarding steps, and a three-page
  supplier portfolio (About, Capabilities, Evidence).
- Workspace Context with `useReducer`, `useSupplierWorkspace`, and
  `useDraftAutosave`; URL state for navigation and checklist filtering.
- Zustand store for the shared preparation checklist, with local `useState`
  for independent input and feedback state.
- Reusable `FormField`, blur/step/final validation, error summaries and focus,
  debounced serialized saves, storage recovery, upload retry/removal, navigation
  protection, and duplicate-submit protection with demo receipts.
- Focused Vitest/React Testing Library coverage and desktop/mobile browser QA.
- Real backend writes and uploads remain deferred as described below.

## Baseline before these changes

- `/supplier` and `/supplier/onboarding` both render
  `src/features/supplier-onboarding/ui/OnboardingScreen.jsx`.
- The screen contains four steps: Organisation, Capabilities, Claims & Evidence,
  and Review & Publish. Fields are controlled through `useState`.
- Drafts are written to `localStorage` after every change using the key
  `supify_supplier_onboarding_draft`. Read/write failures are silently ignored.
- Step navigation does not validate input. File selection only displays a toast.
  Publishing shows a success message and navigates away without an API request.
- `OnboardingStep.jsx` is a separate outlet-based implementation that is not
  mounted by the current routes. Extend the active screen and consolidate shared
  step metadata in `model/steps.js` rather than building two competing flows.
- The supplier repository currently reads fixtures and has no draft, upload, or
  submission methods. Backend persistence must be added or explicitly simulated.

## 1. Form handling and validation

- Wrap the active step in a semantic `<form>` with an `onSubmit` handler so Enter
  and the primary button follow the same validation path.
- Keep controlled fields, adding stable `id`/`htmlFor` pairs, field errors,
  touched state, and a visible indication of unsaved changes.
- Initially require trade name, legal name, location, category, and capacity,
  matching the active screen's required labels. Keep MOQ and lead time optional
  unless product requirements change. Validate an optional website as HTTP/HTTPS.
- Keep capacity, MOQ, and lead time as human-readable text for this iteration;
  do not impose numeric parsing on values such as `50,000 units / month`.
- Treat the current registry field as a generic identifier. Apply GST-specific
  formatting only if an explicit identifier type is introduced.
- Validate on blur, on attempted forward navigation, and before final submission.
  Revalidate previously reported errors as the user corrects them.
- Allow backward navigation without validation. Forward stepper clicks must
  validate intervening steps so they cannot bypass required fields.
- On invalid submission, open the earliest invalid step, show an error summary,
  and focus its first invalid field. Preserve all entered values.
- Review should show every submitted field and attachment, with Edit actions
  returning to the relevant step.

## 2. State management with `useReducer`

Introduce a pure reducer in `model/workspaceReducer.js`. Model related transitions
together rather than adding many independent boolean flags.

```js
const initialState = {
  values: {}, // initialized with the existing field defaults
  touched: {},
  fieldErrors: {},
  revision: 0,
  savedRevision: -1,
  load: { status: 'idle', error: null },
  save: { status: 'idle', error: null, savedAt: null },
  submit: { status: 'idle', error: null, receipt: null },
  attachments: [], // each has an id, metadata, upload status, and error
}
```

The current step now lives in the route URL, keeping browser navigation consistent.
Use `idle | pending | success | error` for each operation's status. Derive
`isDirty` from `revision !== savedRevision`, and derive button availability from
validation and operation status rather than storing duplicate flags.

| Action group | Responsibility |
| --- | --- |
| `FIELD_CHANGED`, `FIELD_BLURRED` | Update values, touched state, and edit revision |
| `VALIDATION_FAILED`, `FOCUS_HANDLED` | Show errors and coordinate accessible focus; route navigation selects the step |
| `LOAD_STARTED/SUCCEEDED/FAILED` | Restore the draft or expose a recoverable load error |
| `SAVE_STARTED/SUCCEEDED/FAILED` | Track persistence of a specific revision |
| `ATTACHMENT_ADDED/REMOVED`, `UPLOAD_STARTED/SUCCEEDED/FAILED` | Track each evidence file independently |
| `SUBMIT_STARTED/SUCCEEDED/FAILED` | Track submission and retain the returned receipt |

Reducers must not fetch, write storage, create timers, mutate state, or navigate.
Attach request IDs and draft revisions to async actions; ignore stale completions.
A save of revision 3 must not mark revision 4 as saved.

## 3. Hooks and responsibilities

| Hook | Planned usage |
| --- | --- |
| `useReducer` | Own form, navigation, validation, and operation transitions |
| `useSupplierWorkspace` (custom) | Expose state and commands such as updateField, nextStep, retryLoad, and submit |
| `useDraftAutosave` (custom) | Debounce persistence, track the saved revision, and clean up timers |
| `useEffect` | Restore drafts and synchronize autosave; return cleanup functions |
| `useRef` | Track active request IDs/controllers, an immediate submit lock, and focus targets |
| `useState` | Keep small independent UI details, such as an expanded help panel |

Use `useMemo` or `useCallback` only when a measured calculation cost or stable
consumer dependency warrants them. Trigger upload and submission from user events,
not effects that could repeat on mount.

## 4. Async operations and API boundary

Extend `src/shared/api/contract.js` and its repository implementation with a
documented workspace contract, then update `docs/contract/supplier-api.md`.

| Proposed method | Expected result |
| --- | --- |
| `getWorkspaceDraft({ signal })` | Validated draft and revision, or `null` for a new workspace |
| `saveWorkspaceDraft(draft, { revision, signal })` | Persisted revision and save timestamp |
| `uploadEvidence(file, { signal })` | Evidence ID and display metadata |
| `submitWorkspace(payload, { idempotencyKey, signal })` | Submission ID, status, and timestamp |

- Keep components dependent on domain data, with storage/network details behind
  the repository boundary. Normalize failures into a message, optional field
  errors, and whether retry is appropriate.
- Start with a clearly labelled demo implementation using local persistence and
  deterministic latency/failure controls. Local storage is synchronous; wrapping
  it in a Promise does not make it a real remote save.
- Demo uploads/submissions must be labelled as simulations. Do not claim a real
  verification ticket, remote file storage, or ingest hash exists until supported.
- Use `AbortController` where supported and ignore stale results after navigation,
  retry, or unmount. Cancellation must not appear as a user-facing failure.
- Serialize draft saves and coalesce queued changes to the newest revision so
  older writes cannot overwrite newer drafts. Request-ID checks alone protect
  the UI, but do not prevent stale writes on the server.
- Guard submission immediately with a ref as well as disabling the button.
  Reuse an idempotency key when retrying the same payload; real duplicate prevention
  also requires backend support. Freeze form edits during final submission.
- Navigate or show the receipt only after confirmed success. Preserve the draft
  and input on failure; map server field errors back to their steps.

## 5. Draft persistence and recovery

- Validate restored data and migrate the existing unversioned draft to a versioned
  format. Merge known fields with defaults; handle malformed JSON and wrong types.
- Complete restoration before enabling autosave, preventing defaults from
  overwriting a saved draft. For a damaged draft, offer explicit recovery/reset
  without immediately overwriting the original stored value.
- Debounce autosave by approximately 500 ms and offer a Save draft action that
  flushes pending changes. Draft saving must allow incomplete form data.
- Show `Unsaved changes`, `Saving...`, `Saved locally at ...`, or a persistent
  save failure with Retry. Show a remote-save label only for actual remote success.
- Keep storage-quota and access failures visible while allowing continued editing.
- Warn on browser unload while changes are unsaved, using the browser-supported
  mechanism. Use an in-app navigation guard only if supported by the router setup;
  provide an explicit save/discard flow otherwise.

## 6. Evidence file handling

- Replace the toast-only button with an accessible file input accepting PDF,
  PNG, and JPEG files up to the existing UI limit of 25 MB per file.
- Show each file's name, size, pending/success/error status, Remove, and Retry.
  Use indeterminate upload feedback unless the transport provides real progress.
- Validate file type and size before upload. A real backend must validate them
  again. Replace the hard-coded `isoCertUploaded` flag with attachment state.
- Block final submission while selected files are uploading or have unresolved
  failures; let users retry or remove failed files. Certification remains optional
  unless a later product requirement makes it mandatory.
- Do not JSON-serialize `File` objects into localStorage. Persist successful
  evidence references only; after reload, ask users to reselect unsent files.
- Cancel removed-file uploads and ignore their late results. Revoke any object
  URLs used for previews when files are removed or the page unmounts.

## 7. Loading, error, and success experience

| Situation | Workspace behavior |
| --- | --- |
| Initial draft loading | Show a labelled skeleton/loading region; prevent editing until restoration finishes |
| No saved draft | Display an editable initial form; do not treat this as an error |
| Draft load failure | Show a persistent explanation with Retry and a safe recovery path |
| Draft saving | Keep editing available and show inline saving status |
| Save failure | Keep values and show Retry next to the unsaved status |
| Upload pending/failure | Keep feedback and recovery actions beside the affected file |
| Submission pending | Show `Submitting...`; disable edits and repeat submission |
| Submission failure | Preserve values and show a form-level error plus any field errors |
| Submission success | Show the confirmed receipt and review status; label demo results clearly |

Use `aria-invalid` and `aria-describedby` for field feedback, `aria-busy` for
pending regions, and a polite live region for operation status. Use semantic
buttons for the stepper. Toasts may supplement persistent feedback, but should
not be the only place users can find an error or submission result.

## 8. Implementation order and files

1. **State and validation:** add `model/workspaceReducer.js` and
   `model/validation.js`; consolidate step definitions in `model/steps.js`.
2. **Form integration:** add `model/useSupplierWorkspace.js`; refactor the active
   `ui/OnboardingScreen.jsx` into validated forms while preserving both routes.
   Extract step UI only where it makes the screen easier to read.
3. **Draft lifecycle:** add `model/useDraftAutosave.js`, migration/recovery logic,
   and draft repository methods; connect save and load feedback.
4. **Evidence and submission:** implement repository operations, per-file state,
   duplicate-submit protection, and a confirmed success receipt.
5. **Polish and verification:** check keyboard/focus behavior, narrow layouts,
   retry paths, and race conditions; update the API documentation.

Keep feature-specific form feedback components under
`src/features/supplier-onboarding/ui/`. Promote components to `src/shared/ui/`
only when they are actually reused. Keep the existing React/JavaScript stack;
no additional form library is needed. Zustand was added for the user's requested
introduction to an external state store, using the dashboard preparation checklist.

## 9. Verification and acceptance criteria

The project now has `npm test` and `npm run test:watch` using Vitest and React
Testing Library for reducer, repository, hook, and routed interaction checks.

- [x] Both supplier routes render the enhanced workspace.
- [x] Forward navigation cannot bypass invalid required fields; back navigation
      preserves values and final submission validates every step.
- [x] Valid drafts survive reload; malformed or unavailable storage produces
      recoverable feedback without silently losing input.
- [x] A slow save cannot clear the unsaved status for newer edits or overwrite
      a more recent persisted draft.
- [x] Loading, empty, success, failure, and retry paths are exercised with
      deterministic mock responses rather than random failures.
- [x] Invalid files are rejected, failed uploads can be retried or removed, and
      removed-file responses cannot restore an attachment.
- [x] Rapid submit clicks produce one active request; retry preserves the same
      submission identity when the payload has not changed.
- [x] Failed submission preserves all values; successful submission displays a
      receipt only after the repository confirms the result.
- [x] Navigation/unmount cancels or ignores pending work without stale UI updates.
- [x] Keyboard-only users can navigate, submit, locate errors, and retry operations.
- [x] Run the new focused tests and `npm run build`; manually check desktop and
      narrow layouts with slow responses and simulated failures.

## Deferred work

Real backend deployment, authentication/ownership enforcement, server-side file
storage/scanning, and verification-queue integration are follow-up work. The
planned frontend contract should support those integrations while clearly
distinguishing local demo behavior from real persistence and submission.
