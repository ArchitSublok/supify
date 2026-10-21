export const DRAFT_KEY = 'supify_supplier_onboarding_draft'
const RECEIPTS_KEY = 'supify_supplier_submission_receipts'
const fields = ['legalName', 'tradeName', 'location', 'category', 'website', 'capacity', 'moq', 'leadTime', 'gstNumber', 'qualityProcess']
const isObject = (value) => value !== null && typeof value === 'object' && !Array.isArray(value)
const isReceipt = (value) => isObject(value) && typeof value.id === 'string' &&
  typeof value.submittedAt === 'string' && Number.isFinite(Date.parse(value.submittedAt)) && value.status === 'demo-received'

export function parseWorkspaceDraft(raw) {
  const parsed = JSON.parse(raw)
  if (!isObject(parsed)) throw new Error('The saved draft is damaged. Retry or start a new draft.')
  if (parsed.version !== undefined && parsed.version !== 1) throw new Error('This draft version is not supported. The stored draft has been preserved.')
  const values = parsed.version === 1 ? parsed.values : parsed
  if (!isObject(values) || fields.some((key) => values[key] !== undefined && typeof values[key] !== 'string')) {
    throw new Error('The saved draft contains invalid fields. The stored draft has been preserved.')
  }
  const revision = parsed.version === 1 ? parsed.revision : 0
  if (!Number.isSafeInteger(revision) || revision < 0) throw new Error('The saved draft revision is invalid.')
  const attachments = parsed.version === 1 ? parsed.attachments : []
  if (!Array.isArray(attachments) || attachments.some((file) => !isObject(file) ||
    typeof file.id !== 'string' || typeof file.name !== 'string' || typeof file.type !== 'string' || !Number.isFinite(file.size) || file.size < 0 ||
    !['success', 'reselect'].includes(file.status) || (file.status === 'success' && typeof file.evidenceId !== 'string'))) {
    throw new Error('The saved evidence list is damaged. The stored draft has been preserved.')
  }
  if (new Set(attachments.map((file) => file.id)).size !== attachments.length) throw new Error('The saved evidence list contains duplicate identifiers.')
  const receipt = parsed.version === 1 ? parsed.receipt : null
  if (receipt !== undefined && receipt !== null && !isReceipt(receipt)) {
    throw new Error('The saved submission receipt is invalid.')
  }
  return { values: Object.fromEntries(fields.filter((key) => values[key] !== undefined).map((key) => [key, values[key]])),
    revision, attachments, receipt: receipt || null, savedAt: typeof parsed.savedAt === 'string' ? parsed.savedAt : null }
}

function pause(ms, signal) {
  return new Promise((resolve, reject) => {
    if (signal?.aborted) return reject(new DOMException('Cancelled', 'AbortError'))
    const abort = () => { clearTimeout(timer); reject(new DOMException('Cancelled', 'AbortError')) }
    const timer = setTimeout(() => { signal?.removeEventListener('abort', abort); resolve() }, ms)
    signal?.addEventListener('abort', abort, { once: true })
  })
}

export function createWorkspaceRepository({ getStorage = () => localStorage, latency = 350 } = {}) {
  const failures = new Set()
  let delay = latency
  async function request(operation, signal) {
    await pause(delay, signal)
    if (failures.delete(operation)) throw new Error(`Demo ${operation} failed. Your input is still here; please retry.`)
  }
  return {
    configureDemo({ delayMs, failNext } = {}) {
      if (delayMs !== undefined) delay = delayMs
      if (failNext) failures.add(failNext)
    },
    async getWorkspaceDraft({ signal } = {}) {
      await request('load', signal)
      const raw = getStorage().getItem(DRAFT_KEY)
      return raw === null ? null : parseWorkspaceDraft(raw)
    },
    async saveWorkspaceDraft(draft, { revision, signal } = {}) {
      await request('save', signal)
      const savedAt = new Date().toISOString()
      const record = { version: 1, values: draft.values, revision, savedAt, receipt: draft.receipt,
        attachments: draft.attachments.map(({ id, name, size, type, evidenceId, status }) => ({
          id, name, size, type, evidenceId: status === 'success' ? evidenceId : null,
          status: status === 'success' ? 'success' : 'reselect',
        })) }
      const raw = JSON.stringify(record)
      parseWorkspaceDraft(raw)
      getStorage().setItem(DRAFT_KEY, raw)
      return { revision, savedAt }
    },
    async resetWorkspaceDraft({ signal } = {}) {
      await request('reset', signal)
      const storage = getStorage()
      const raw = storage.getItem(DRAFT_KEY)
      if (raw !== null) storage.setItem(`${DRAFT_KEY}_backup_${Date.now()}`, raw)
      storage.removeItem(DRAFT_KEY)
    },
    async uploadEvidence(file, { signal } = {}) {
      await request('upload', signal)
      if (!['application/pdf', 'image/png', 'image/jpeg'].includes(file.type) || file.size > 25 * 1024 * 1024 || file.size <= 0) {
        throw new Error('Choose a non-empty PDF, PNG, or JPEG up to 25 MB.')
      }
      // shortcut: only metadata is simulated; replace with server file storage before production.
      return { evidenceId: `demo-evidence-${crypto.randomUUID()}`, name: file.name, size: file.size, type: file.type }
    },
    async submitWorkspace(payload, { idempotencyKey, signal } = {}) {
      await request('submit', signal)
      if (!idempotencyKey) throw new Error('A submission identity is required.')
      const storage = getStorage()
      const receipts = JSON.parse(storage.getItem(RECEIPTS_KEY) || '{}')
      if (!isObject(receipts) || Object.values(receipts).some((entry) => !isObject(entry) || typeof entry.fingerprint !== 'string' || !isReceipt(entry.receipt))) {
        throw new Error('Local submission history is damaged. Your draft has been preserved.')
      }
      const fingerprint = JSON.stringify(payload)
      const previous = receipts[idempotencyKey]
      if (previous) {
        if (previous.fingerprint !== fingerprint) throw new Error('This submission identity belongs to different content.')
        return previous.receipt
      }
      const receipt = { id: `DEMO-${crypto.randomUUID()}`, status: 'demo-received', submittedAt: new Date().toISOString() }
      storage.setItem(RECEIPTS_KEY, JSON.stringify({ ...receipts, [idempotencyKey]: { fingerprint, receipt } }))
      return receipt
    },
  }
}

export const workspaceRepository = createWorkspaceRepository()
