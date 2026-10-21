import { useCallback, useEffect, useRef } from 'react'

export function useDraftAutosave(state, dispatch, repository) {
  const latest = useRef(state)
  latest.current = state
  const active = useRef(true)
  const inFlight = useRef(null)
  const controller = useRef(null)

  useEffect(() => {
    active.current = true
    return () => { active.current = false; controller.current?.abort() }
  }, [])

  const flush = useCallback(() => {
    if (inFlight.current) return inFlight.current
    if (latest.current.load.status !== 'success') return Promise.resolve(false)
    inFlight.current = (async () => {
      let savedRevision = latest.current.savedRevision
      while (active.current && latest.current.revision > savedRevision) {
        const snapshot = latest.current
        const requestId = crypto.randomUUID()
        controller.current = new AbortController()
        dispatch({ type: 'SAVE_STARTED', requestId })
        try {
          const result = await repository.saveWorkspaceDraft({ values: snapshot.values,
            attachments: snapshot.attachments, receipt: snapshot.submit.receipt },
          { revision: snapshot.revision, signal: controller.current.signal })
          if (!active.current) return false
          savedRevision = result.revision
          dispatch({ type: 'SAVE_SUCCEEDED', requestId, ...result })
        } catch (error) {
          if (active.current && error.name !== 'AbortError') dispatch({ type: 'SAVE_FAILED', requestId, error: error.message })
          return false
        }
      }
      return active.current
    })().finally(() => { inFlight.current = null })
    return inFlight.current
  }, [dispatch, repository])

  useEffect(() => {
    if (state.load.status !== 'success' || state.revision === state.savedRevision || state.save.status === 'error') return
    const timer = setTimeout(flush, 500)
    return () => clearTimeout(timer)
  }, [state.revision, state.savedRevision, state.load.status, state.save.status, flush])

  return flush
}
