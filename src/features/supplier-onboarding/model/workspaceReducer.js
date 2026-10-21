import { defaultValues } from './fields.js'
import { validateField } from './validation.js'

export function createInitialState() {
  return {
    values: { ...defaultValues }, touched: {}, fieldErrors: {}, attachments: [],
    revision: 0, savedRevision: -1,
    load: { status: 'idle', error: null },
    save: { status: 'idle', error: null, savedAt: null },
    submit: { status: 'idle', error: null, receipt: null },
    focusField: null,
  }
}

function edited(state, changes) {
  return { ...state, ...changes, revision: state.revision + 1,
    submit: { status: 'idle', error: null, receipt: null } }
}

export function workspaceReducer(state, action) {
  switch (action.type) {
    case 'LOAD_STARTED':
      return { ...state, load: { status: 'pending', error: null, requestId: action.requestId } }
    case 'LOAD_SUCCEEDED': {
      if (state.load.requestId !== action.requestId) return state
      const draft = action.draft
      return { ...createInitialState(),
        values: Object.fromEntries(Object.entries(defaultValues).map(([key, fallback]) => [key, draft?.values?.[key] ?? fallback])),
        attachments: draft?.attachments || [], revision: draft?.revision || 0,
        savedRevision: draft ? draft.revision : -1,
        save: { status: 'idle', error: null, savedAt: draft?.savedAt || null },
        submit: { status: draft?.receipt ? 'success' : 'idle', error: null, receipt: draft?.receipt || null },
        load: { status: 'success', error: null },
      }
    }
    case 'LOAD_FAILED':
      if (state.load.requestId !== action.requestId) return state
      return { ...state, load: { status: 'error', error: action.error } }
    case 'FIELD_CHANGED': {
      if (!(action.name in defaultValues) || state.submit.status === 'pending') return state
      const fieldErrors = { ...state.fieldErrors }
      if (state.touched[action.name] || fieldErrors[action.name]) {
        const error = validateField(action.name, action.value)
        if (error) fieldErrors[action.name] = error
        else delete fieldErrors[action.name]
      }
      return edited(state, { values: { ...state.values, [action.name]: action.value }, fieldErrors })
    }
    case 'FIELD_BLURRED': {
      const fieldErrors = { ...state.fieldErrors }
      const error = validateField(action.name, state.values[action.name])
      if (error) fieldErrors[action.name] = error
      else delete fieldErrors[action.name]
      return { ...state, touched: { ...state.touched, [action.name]: true }, fieldErrors }
    }
    case 'VALIDATION_FAILED':
      return { ...state, fieldErrors: action.errors, focusField: action.focusField || Object.keys(action.errors)[0] }
    case 'FOCUS_HANDLED':
      return { ...state, focusField: null }
    case 'SAVE_STARTED':
      return { ...state, save: { ...state.save, status: 'pending', error: null, requestId: action.requestId } }
    case 'SAVE_SUCCEEDED':
      if (state.save.requestId !== action.requestId) return state
      return { ...state, savedRevision: action.revision,
        save: { status: 'success', error: null, savedAt: action.savedAt } }
    case 'SAVE_FAILED':
      if (state.save.requestId !== action.requestId) return state
      return { ...state, save: { ...state.save, status: 'error', error: action.error } }
    case 'ATTACHMENT_ADDED':
      if (state.submit.status === 'pending') return state
      return edited(state, { attachments: [...state.attachments, action.attachment] })
    case 'ATTACHMENT_REMOVED':
      if (state.submit.status === 'pending') return state
      return edited(state, { attachments: state.attachments.filter((file) => file.id !== action.id) })
    case 'UPLOAD_STARTED':
    case 'UPLOAD_SUCCEEDED':
    case 'UPLOAD_FAILED': {
      const file = state.attachments.find((entry) => entry.id === action.id)
      if (!file || (action.type !== 'UPLOAD_STARTED' && file.requestId !== action.requestId)) return state
      const status = action.type === 'UPLOAD_STARTED' ? 'pending' : action.type === 'UPLOAD_SUCCEEDED' ? 'success' : 'error'
      return edited(state, { attachments: state.attachments.map((entry) => entry.id !== action.id ? entry : {
        ...entry, status, requestId: action.requestId, evidenceId: action.evidenceId || null, error: action.error || null,
      }) })
    }
    case 'SUBMIT_STARTED':
      return { ...state, submit: { status: 'pending', error: null, receipt: null, requestId: action.requestId } }
    case 'SUBMIT_SUCCEEDED':
      if (state.submit.requestId !== action.requestId) return state
      return { ...state, revision: state.revision + 1,
        submit: { status: 'success', error: null, receipt: action.receipt } }
    case 'SUBMIT_FAILED':
      if (state.submit.requestId !== action.requestId) return state
      return { ...state, submit: { status: 'error', error: action.error, receipt: null } }
    default:
      return state
  }
}
