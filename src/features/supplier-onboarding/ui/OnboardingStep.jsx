import { useEffect } from 'react'
import { Navigate, useNavigate, useParams } from 'react-router-dom'
import { onboardingSteps } from '../model/steps.js'
import { fieldGroups, stepForField } from '../model/fields.js'
import { validateEvidence, validateValues } from '../model/validation.js'
import { useSupplierWorkspace } from '../model/useSupplierWorkspace.js'
import { FormField } from '../../../shared/ui/FormField'
import { DraftStatus } from './WorkspaceLayout'

export function OnboardingStep() {
  const { step } = useParams()
  const navigate = useNavigate()
  const { state, dispatch, saveDraft, submit, addFiles, removeFile, retryUpload } = useSupplierWorkspace()
  const index = onboardingSteps.findIndex((entry) => entry.key === step)
  const current = onboardingSteps[index]
  const pending = state.submit.status === 'pending'
  const unresolvedFiles = state.attachments.some((file) => file.status !== 'success')

  // URL entry, stepper clicks, and browser forward all use the same validation gate.
  useEffect(() => {
    if (index < 0) return
    const errors = validateValues(state.values, onboardingSteps.slice(0, index).map((entry) => entry.key))
    if (Object.keys(errors).length) {
      dispatch({ type: 'VALIDATION_FAILED', errors })
      navigate(`/supplier/onboarding/${stepForField(Object.keys(errors)[0])}`, { replace: true })
    }
  }, [index, state.values, dispatch, navigate])

  useEffect(() => {
    if (!state.focusField || stepForField(state.focusField) !== step) return
    document.getElementById(`field-${state.focusField}`)?.focus()
    dispatch({ type: 'FOCUS_HANDLED' })
  }, [state.focusField, step, dispatch])

  useEffect(() => {
    const first = Object.keys(state.fieldErrors)[0]
    if (first && state.submit.status === 'error') navigate(`/supplier/onboarding/${stepForField(first)}`)
  }, [state.submit.status, state.fieldErrors, navigate])

  if (!current) return <Navigate to="/supplier/onboarding/organisation" replace />

  async function handleSubmit(event) {
    event.preventDefault()
    if (pending) return
    const errors = validateValues(state.values, index === 3 ? undefined : [step])
    if (Object.keys(errors).length) {
      dispatch({ type: 'VALIDATION_FAILED', errors })
      navigate(`/supplier/onboarding/${stepForField(Object.keys(errors)[0])}`)
      return
    }
    if (index === 3) await submit()
    else {
      saveDraft()
      navigate(`/supplier/onboarding/${onboardingSteps[index + 1].key}`)
    }
  }

  const visibleErrors = Object.entries(state.fieldErrors).filter(([name]) => index === 3 || stepForField(name) === step)
  return <form onSubmit={handleSubmit} noValidate aria-busy={pending}>
    <p className="text-label-uppercase text-brand-teal">Step {index + 1} of 4</p>
    <h2 className="text-title-lg md:text-display-sm font-bold mt-2">{current.heading}</h2>
    <p className="text-sm text-body-muted mt-3 mb-7">{current.blurb}</p>
    {visibleErrors.length > 0 && <div role="alert" className="workspace-error mb-6">
      <p className="font-semibold">Please check the following fields:</p>
      <ul className="list-disc pl-5 mt-2">{visibleErrors.map(([name, error]) => <li key={name}><button type="button" className="underline text-left"
        onClick={() => { dispatch({ type: 'VALIDATION_FAILED', errors: state.fieldErrors, focusField: name }); navigate(`/supplier/onboarding/${stepForField(name)}`) }}>{error}</button></li>)}</ul>
    </div>}
    <fieldset disabled={pending} className="space-y-5 min-w-0">
      {fieldGroups[step]?.map((field) => <FormField key={field.name} {...field} value={state.values[field.name]}
        error={state.fieldErrors[field.name]}
        onChange={(event) => dispatch({ type: 'FIELD_CHANGED', name: field.name, value: event.target.value })}
        onBlur={() => dispatch({ type: 'FIELD_BLURRED', name: field.name })} />)}
      {step === 'claims' && <section className="border-2 border-dashed border-outline-variant rounded-xl p-5">
        <h3 className="font-bold">Quality certification evidence</h3>
        <p id="evidence-hint" className="text-xs text-body-muted mt-2 mb-4">PDF, PNG, or JPEG, up to 25 MB each. Demo only: file metadata is recorded locally; file contents are not uploaded.</p>
        <label htmlFor="evidence-files" className="block font-semibold text-sm mb-2">Select evidence files (optional)</label>
        <input id="evidence-files" type="file" multiple accept="application/pdf,image/png,image/jpeg" aria-describedby="evidence-hint"
          className="w-full text-sm file:mr-3 file:rounded-lg file:border file:border-hairline file:bg-background file:px-3 file:py-2"
          onChange={(event) => { addFiles(Array.from(event.target.files)); event.target.value = '' }} />
        {state.attachments.length > 0 && <ul className="space-y-3 mt-5">{state.attachments.map((file) => <li key={file.id} className="bg-background rounded-lg p-3 text-sm break-words">
          <div className="flex flex-wrap justify-between gap-2"><strong className="break-all">{file.name}</strong><span className="text-xs text-body-muted">{(file.size / 1024).toFixed(1)} KB</span></div>
          <p role="status" className="text-xs mt-2">{file.status === 'pending' ? 'Recording demo evidence…' : file.status === 'success' ? 'Demo evidence attached' : file.status === 'reselect' ? 'Please remove and reselect this file after reloading.' : file.error}</p>
          <div className="flex gap-4 mt-2">
            {file.status === 'error' && !validateEvidence(file) && <button type="button" className="workspace-text-button" onClick={() => retryUpload(file.id)}>Retry {file.name}</button>}
            <button type="button" className="workspace-text-button" onClick={() => removeFile(file.id)}>Remove {file.name}</button>
          </div>
        </li>)}</ul>}
      </section>}
      {step === 'review' && <>
        {Object.entries(fieldGroups).map(([key, fields]) => <section key={key} className="border border-hairline rounded-xl p-4">
          <div className="flex items-center justify-between mb-3"><h3 className="font-semibold">{onboardingSteps.find((entry) => entry.key === key).label}</h3>
            <button type="button" className="workspace-text-button" onClick={() => navigate(`/supplier/onboarding/${key}`)}>Edit {key}</button></div>
          <dl className="space-y-3 text-sm">{fields.map(({ name, label }) => <div key={name} className="grid sm:grid-cols-2 gap-1"><dt className="text-body-muted">{label}</dt><dd className="font-medium break-words min-w-0">{state.values[name] || 'Not provided'}</dd></div>)}</dl>
        </section>)}
        <section className="border border-hairline rounded-xl p-4 text-sm"><h3 className="font-semibold mb-2">Evidence attachments</h3>
          {state.attachments.length ? <ul className="space-y-2">{state.attachments.map((file) => <li key={file.id} className="break-all">{file.name} — {file.status === 'success' ? 'demo attached' : 'needs attention'}</li>)}</ul> : <p>No evidence selected. Certification evidence is optional.</p>}
          {unresolvedFiles && <p role="alert" className="text-error mt-3">Finish, retry, or remove incomplete files in Claims & Evidence before submitting.</p>}
        </section>
        <p className="text-sm bg-brand-mint/30 p-4 rounded-xl">Submitting creates a local demo receipt. Your listing stays self-declared; no real verification queue ticket is created.</p>
      </>}
    </fieldset>
    {state.submit.error && <div role="alert" className="workspace-error mt-5">{state.submit.error}</div>}
    {state.submit.receipt && <section role="status" className="mt-5 rounded-xl bg-brand-mint/40 p-5 break-words">
      <h3 className="font-bold">Demo listing received</h3><p className="mt-2 text-sm">Receipt: {state.submit.receipt.id}</p>
      <p className="mt-2 text-xs">{new Date(state.submit.receipt.submittedAt).toLocaleString()} · Local simulation; awaiting a future backend integration.</p>
    </section>}
    <div className="mt-8 border-t border-hairline pt-5 space-y-5">
      <DraftStatus />
      <div className="flex flex-wrap items-center gap-3">
        {index > 0 && <button type="button" className="workspace-secondary" disabled={pending} onClick={() => navigate(`/supplier/onboarding/${onboardingSteps[index - 1].key}`)}>Back</button>}
        <button type="button" className="workspace-secondary" disabled={pending || state.save.status === 'pending'} onClick={saveDraft}>Save draft</button>
        <button type="submit" className="workspace-primary sm:ml-auto" disabled={pending || (index === 3 && (unresolvedFiles || state.submit.status === 'success'))}>
          {pending ? 'Submitting…' : index === 3 ? state.submit.status === 'success' ? 'Demo submitted' : 'Submit demo listing' : 'Save and continue →'}
        </button>
      </div>
    </div>
  </form>
}
