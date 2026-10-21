import { useCallback, useEffect, useReducer, useRef } from 'react'
import { supplierRepository } from '../../../shared/api/supplierRepository'
import { createInitialState, workspaceReducer } from './workspaceReducer.js'
import { validateEvidence, validateValues } from './validation.js'
import { useDraftAutosave } from './useDraftAutosave.js'
import { WorkspaceContext } from './useSupplierWorkspace.js'

export function WorkspaceProvider({ children, repository = supplierRepository }) {
  const [state, dispatch] = useReducer(workspaceReducer, undefined, createInitialState)
  const latest = useRef(state)
  latest.current = state
  const active = useRef(true)
  const loadController = useRef(null)
  const uploads = useRef(new Map())
  const files = useRef(new Map())
  const submitController = useRef(null)
  const submitLock = useRef(false)
  const submissionIdentity = useRef(null)
  const saveDraft = useDraftAutosave(state, dispatch, repository)

  const load = useCallback(async (reset = false) => {
    loadController.current?.abort()
    uploads.current.forEach((controller) => controller.abort())
    uploads.current.clear()
    files.current.clear()
    submissionIdentity.current = null
    const controller = new AbortController()
    loadController.current = controller
    const requestId = crypto.randomUUID()
    dispatch({ type: 'LOAD_STARTED', requestId })
    try {
      if (reset) await repository.resetWorkspaceDraft({ signal: controller.signal })
      const draft = await repository.getWorkspaceDraft({ signal: controller.signal })
      if (active.current && !controller.signal.aborted) dispatch({ type: 'LOAD_SUCCEEDED', requestId, draft })
    } catch (error) {
      if (active.current && error.name !== 'AbortError') dispatch({ type: 'LOAD_FAILED', requestId, error: error.message })
    }
  }, [repository])

  useEffect(() => {
    active.current = true
    load()
    return () => {
      active.current = false
      loadController.current?.abort()
      submitController.current?.abort()
      uploads.current.forEach((controller) => controller.abort())
    }
  }, [load])

  async function upload(id) {
    const file = files.current.get(id)
    if (!file || submitLock.current) return
    uploads.current.get(id)?.abort()
    const controller = new AbortController()
    uploads.current.set(id, controller)
    const requestId = crypto.randomUUID()
    dispatch({ type: 'UPLOAD_STARTED', id, requestId })
    try {
      const result = await repository.uploadEvidence(file, { signal: controller.signal })
      if (active.current && !controller.signal.aborted) {
        files.current.delete(id)
        dispatch({ type: 'UPLOAD_SUCCEEDED', id, requestId, evidenceId: result.evidenceId })
      }
    } catch (error) {
      if (active.current && error.name !== 'AbortError') dispatch({ type: 'UPLOAD_FAILED', id, requestId, error: error.message })
    } finally {
      if (uploads.current.get(id) === controller) uploads.current.delete(id)
    }
  }

  function addFiles(selectedFiles) {
    if (submitLock.current) return
    for (const file of selectedFiles) {
      const id = crypto.randomUUID()
      const error = validateEvidence(file)
      files.current.set(id, file)
      dispatch({ type: 'ATTACHMENT_ADDED', attachment: {
        id, name: file.name, size: file.size, type: file.type, status: error ? 'error' : 'idle', error,
      } })
      if (!error) upload(id)
    }
  }

  function removeFile(id) {
    if (submitLock.current) return
    uploads.current.get(id)?.abort()
    uploads.current.delete(id)
    files.current.delete(id)
    dispatch({ type: 'ATTACHMENT_REMOVED', id })
  }

  async function submit() {
    if (submitLock.current || latest.current.submit.status === 'pending' || latest.current.load.status !== 'success') return
    const snapshot = latest.current
    const errors = validateValues(snapshot.values)
    if (Object.keys(errors).length) { dispatch({ type: 'VALIDATION_FAILED', errors }); return }
    if (snapshot.attachments.some((file) => file.status !== 'success')) return
    if (snapshot.submit.status === 'success') return
    submitLock.current = true
    const requestId = crypto.randomUUID()
    const payload = { values: snapshot.values,
      evidence: snapshot.attachments.map(({ evidenceId, name, size, type }) => ({ evidenceId, name, size, type })) }
    const fingerprint = JSON.stringify(payload)
    if (submissionIdentity.current?.fingerprint !== fingerprint) {
      submissionIdentity.current = { fingerprint, key: crypto.randomUUID() }
    }
    submitController.current = new AbortController()
    dispatch({ type: 'SUBMIT_STARTED', requestId })
    try {
      if (!await saveDraft()) throw new Error('Save your draft successfully before submitting. Please retry the save.')
      const receipt = await repository.submitWorkspace(payload, {
        idempotencyKey: submissionIdentity.current.key, signal: submitController.current.signal,
      })
      if (active.current) dispatch({ type: 'SUBMIT_SUCCEEDED', requestId, receipt })
    } catch (error) {
      if (active.current && error.name !== 'AbortError') {
        dispatch({ type: 'SUBMIT_FAILED', requestId, error: error.message })
        if (error.fieldErrors) dispatch({ type: 'VALIDATION_FAILED', errors: error.fieldErrors })
      }
    } finally {
      submitLock.current = false
    }
  }

  const isDirty = state.load.status === 'success' && state.revision !== state.savedRevision
  useEffect(() => {
    const beforeUnload = (event) => {
      if (isDirty || state.submit.status === 'pending') { event.preventDefault(); event.returnValue = '' }
    }
    window.addEventListener('beforeunload', beforeUnload)
    return () => window.removeEventListener('beforeunload', beforeUnload)
  }, [isDirty, state.submit.status])

  return (
    <WorkspaceContext.Provider value={{ state, dispatch, isDirty, repository, saveDraft, submit,
      retryLoad: () => load(), resetDraft: () => load(true), addFiles, removeFile, retryUpload: upload }}>
      {children}
    </WorkspaceContext.Provider>
  )
}
