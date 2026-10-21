# Supplier data contract (database/API later)

The React app currently reads fixtures through `src/shared/api/supplierRepository.js`.
That module is the only place that should change when a real API and relational database are added.

## Planned reads

| Method | Purpose | Current implementation |
| --- | --- | --- |
| `search(filters)` | Search projection for the buyer discovery surface | Filters fixture records |
| `getById(supplierId)` | Supplier profile and claim ledger | Returns one fixture record |
| `getVerificationQueue()` | Cross-supplier verifier work queue | Returns fixture task rows |

The eventual database must retain the ledger described in `docs/understanding.md`: claims, evidence,
verification checks, and attestations are separate records. A trust snapshot is a server-computed,
rebuildable read projection; the React client only renders it.

## Replacement seam

Create an API-backed implementation of `SupplierRepository` and replace the exported instance in
`src/shared/api/supplierRepository.js`. Keep the wire-to-domain mapping in
`src/entities/supplier/model/adapter.js`; feature and entity components must continue to consume only
the mapped domain shape.

## Workspace writes (local demo)

The fixture supplier repository delegates these operations to
`src/shared/api/workspaceRepository.js`. Components access them through the
workspace Context. All operations return promises and accept an AbortSignal.

| Method | Result |
| --- | --- |
| `getWorkspaceDraft({ signal })` | Validated draft or `null`; legacy flat drafts are normalized |
| `saveWorkspaceDraft(draft, { revision, signal })` | `{ revision, savedAt }` after a localStorage write |
| `resetWorkspaceDraft({ signal })` | Backs up the original stored value, then clears the active draft |
| `uploadEvidence(file, { signal })` | `{ evidenceId, name, size, type }`; simulated metadata only |
| `submitWorkspace(payload, { idempotencyKey, signal })` | `{ id, status: 'demo-received', submittedAt }` after recording a local demo receipt |

Drafts use the existing `supify_supplier_onboarding_draft` key with
`{ version: 1, values, attachments, revision, savedAt, receipt }`. Values contain
known string fields only. Attachment records contain metadata and a demo evidence
ID on success; incomplete records restore as `reselect`. Actual File objects and
contents are never serialized. Version/type/JSON failures preserve the original
draft; reset stores a timestamped backup first.

The autosave hook serializes writes and coalesces newer edits. Submission keys
are reused for retries of unchanged payloads in the current provider session.
Local demo receipt history rejects key reuse for different payloads. Production
still requires server-side idempotency and validation; this is not a shared API.

`configureDemo({ delayMs, failNext })` controls latency and a one-time failure for
`load`, `save`, `upload`, or `submit`. The workspace exposes these controls in a
collapsed demo panel. Storage failures reject normally; cancellation rejects with
`AbortError` and is not presented as a failure to the user.

See [the React workspace guide](../react-workspace.md) for state ownership,
validation behavior, routes, and verification instructions.
