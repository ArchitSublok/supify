import { describe, expect, it } from 'vitest'
import { defaultValues } from './fields.js'
import { validateEvidence, validateValues } from './validation.js'
import { createInitialState, workspaceReducer as reduce } from './workspaceReducer.js'
import { createWorkspaceRepository, DRAFT_KEY, parseWorkspaceDraft } from '../../../shared/api/workspaceRepository.js'
import { useTaskStore } from './taskStore.js'

describe('workspace validation', () => {
  it('requires the labelled core fields, keeps production terms readable, and rejects unsafe URLs', () => {
    expect(validateValues(defaultValues)).toEqual({})
    const errors = validateValues({ ...defaultValues, tradeName: '   ', website: 'javascript:alert(1)', moq: '', leadTime: '' })
    expect(Object.keys(errors)).toEqual(['tradeName', 'website'])
    expect(validateValues({ ...defaultValues, capacity: '50,000 units / month' })).toEqual({})
  })
  it('rejects empty, oversize, and unsupported evidence', () => {
    expect(validateEvidence({ type: 'application/pdf', size: 5 })).toBe('')
    expect(validateEvidence({ type: 'application/pdf', size: 0 })).toMatch(/empty/)
    expect(validateEvidence({ type: 'application/pdf', size: 26 * 1024 * 1024 })).toMatch(/25 MB/)
    expect(validateEvidence({ type: 'text/plain', size: 5 })).toMatch(/PDF/)
  })
})

describe('workspace reducer', () => {
  it('does not mark newer edits saved when an older save finishes', () => {
    let state = createInitialState()
    state = reduce(state, { type: 'SAVE_STARTED', requestId: 'save-1' })
    state = reduce(state, { type: 'FIELD_CHANGED', name: 'tradeName', value: 'Updated' })
    state = reduce(state, { type: 'SAVE_SUCCEEDED', requestId: 'save-1', revision: 0, savedAt: 'now' })
    expect(state.revision).toBe(1)
    expect(state.savedRevision).toBe(0)
    expect(state.values.tradeName).toBe('Updated')
    expect(reduce(state, { type: 'SAVE_SUCCEEDED', requestId: 'stale', revision: 20 })).toBe(state)
  })
  it('ignores stale load and upload completions, including removed evidence', () => {
    let state = reduce(createInitialState(), { type: 'LOAD_STARTED', requestId: 'new' })
    expect(reduce(state, { type: 'LOAD_SUCCEEDED', requestId: 'old', draft: null })).toBe(state)
    state = reduce(state, { type: 'ATTACHMENT_ADDED', attachment: { id: 'file', status: 'idle' } })
    state = reduce(state, { type: 'UPLOAD_STARTED', id: 'file', requestId: 'retry' })
    expect(reduce(state, { type: 'UPLOAD_SUCCEEDED', id: 'file', requestId: 'old' })).toBe(state)
    state = reduce(state, { type: 'ATTACHMENT_REMOVED', id: 'file' })
    expect(reduce(state, { type: 'UPLOAD_SUCCEEDED', id: 'file', requestId: 'retry' })).toBe(state)
  })
  it('revalidates corrected fields and freezes edits while submitting', () => {
    let state = reduce(createInitialState(), { type: 'FIELD_CHANGED', name: 'tradeName', value: '' })
    state = reduce(state, { type: 'FIELD_BLURRED', name: 'tradeName' })
    expect(state.fieldErrors.tradeName).toBeTruthy()
    state = reduce(state, { type: 'FIELD_CHANGED', name: 'tradeName', value: 'Fixed' })
    expect(state.fieldErrors.tradeName).toBeUndefined()
    state = reduce(state, { type: 'SUBMIT_STARTED', requestId: 'submit' })
    expect(reduce(state, { type: 'FIELD_CHANGED', name: 'tradeName', value: 'Too late' })).toBe(state)
  })
})

describe('local demo repository', () => {
  it('migrates legacy drafts and rejects malformed values without changing storage', async () => {
    const repository = createWorkspaceRepository({ latency: 0 })
    localStorage.setItem(DRAFT_KEY, JSON.stringify({ tradeName: 'Legacy supplier', isoCertUploaded: true }))
    expect((await repository.getWorkspaceDraft()).values).toEqual({ tradeName: 'Legacy supplier' })
    localStorage.setItem(DRAFT_KEY, '{damaged')
    await expect(repository.getWorkspaceDraft()).rejects.toThrow()
    expect(localStorage.getItem(DRAFT_KEY)).toBe('{damaged')
    expect(() => parseWorkspaceDraft(JSON.stringify({ tradeName: 42 }))).toThrow(/invalid fields/)
    expect(() => parseWorkspaceDraft('null')).toThrow(/damaged/)
    expect(() => parseWorkspaceDraft('{"version":99}')).toThrow(/version/)
  })
  it('backs up damaged drafts before explicit reset', async () => {
    const repository = createWorkspaceRepository({ latency: 0 })
    localStorage.setItem(DRAFT_KEY, 'damaged')
    await repository.resetWorkspaceDraft()
    expect(localStorage.getItem(DRAFT_KEY)).toBeNull()
    const backup = Object.keys(localStorage).find((key) => key.startsWith(`${DRAFT_KEY}_backup_`))
    expect(localStorage.getItem(backup)).toBe('damaged')
  })
  it('persists references instead of File objects and asks for unsent files again', async () => {
    const repository = createWorkspaceRepository({ latency: 0 })
    const file = new File(['certificate'], 'quality.pdf', { type: 'application/pdf' })
    await repository.saveWorkspaceDraft({ values: defaultValues, receipt: null,
      attachments: [{ id: 'pending', name: file.name, size: file.size, type: file.type, status: 'pending', file }] }, { revision: 2 })
    const restored = await repository.getWorkspaceDraft()
    expect(restored.attachments[0].status).toBe('reselect')
    expect(restored.attachments[0].file).toBeUndefined()
    expect(restored.revision).toBe(2)
  })
  it('fails once deterministically and returns the same receipt when retried', async () => {
    const repository = createWorkspaceRepository({ latency: 0 })
    repository.configureDemo({ failNext: 'submit' })
    const payload = { values: defaultValues, evidence: [] }
    await expect(repository.submitWorkspace(payload, { idempotencyKey: 'key' })).rejects.toThrow(/Demo submit failed/)
    const receipt = await repository.submitWorkspace(payload, { idempotencyKey: 'key' })
    expect(await repository.submitWorkspace(payload, { idempotencyKey: 'key' })).toEqual(receipt)
    await expect(repository.submitWorkspace({ values: {} }, { idempotencyKey: 'key' })).rejects.toThrow(/different content/)
  })
  it('surfaces storage failures and cancellation', async () => {
    const repository = createWorkspaceRepository({ latency: 0, getStorage: () => { throw new Error('Storage blocked') } })
    await expect(repository.getWorkspaceDraft()).rejects.toThrow(/Storage blocked/)
    const controller = new AbortController()
    controller.abort()
    await expect(repository.getWorkspaceDraft({ signal: controller.signal })).rejects.toMatchObject({ name: 'AbortError' })
  })
})

it('Zustand task actions validate input and update the shared checklist', () => {
  const initial = useTaskStore.getState().tasks
  expect(useTaskStore.getState().addTask('   ')).toBe(false)
  expect(useTaskStore.getState().addTask('x'.repeat(121))).toBe(false)
  expect(useTaskStore.getState().addTask('  Check certificate  ')).toBe(true)
  const task = useTaskStore.getState().tasks.at(-1)
  expect(task.title).toBe('Check certificate')
  useTaskStore.getState().toggleTask(task.id)
  expect(useTaskStore.getState().tasks.at(-1).done).toBe(true)
  useTaskStore.getState().removeTask(task.id)
  expect(useTaskStore.getState().tasks).toEqual(initial)
})
