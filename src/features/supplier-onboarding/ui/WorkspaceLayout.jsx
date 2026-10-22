import { useEffect, useRef, useState } from 'react'
import { NavLink, Outlet, useBlocker } from 'react-router-dom'
import { WorkspaceProvider } from '../model/WorkspaceContext'
import { useSupplierWorkspace } from '../model/useSupplierWorkspace.js'
import { useTaskStore } from '../model/taskStore.js'

export function WorkspaceLayout({ repository }) {
  return <WorkspaceProvider repository={repository}><WorkspaceShell /></WorkspaceProvider>
}

export function DraftStatus() {
  const { state, isDirty, saveDraft } = useSupplierWorkspace()
  const { save } = state
  const label = save.status === 'pending' ? 'Saving locally…' : save.status === 'error' ? 'Draft not saved'
    : isDirty ? 'Unsaved changes' : save.savedAt ? `Saved locally at ${new Date(save.savedAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}` : 'Draft ready'
  return <div className="space-y-2 text-sm">
    <p role="status" className={save.status === 'error' ? 'text-error' : 'text-brand-teal'}>{label}</p>
    {save.error && <div role="alert"><p className="text-error">{save.error}</p>
      <button type="button" className="workspace-text-button" onClick={saveDraft}>Retry save</button></div>}
  </div>
}

function NavigationGuard() {
  const { state, isDirty, saveDraft } = useSupplierWorkspace()
  const pending = state.submit.status === 'pending'
  const blocker = useBlocker(({ nextLocation }) => pending || (isDirty &&
    nextLocation.pathname !== '/supplier' && !nextLocation.pathname.startsWith('/supplier/')))
  const dialog = useRef(null)
  const [saving, setSaving] = useState(false)
  useEffect(() => {
    if (blocker.state === 'blocked') dialog.current?.showModal()
    else dialog.current?.close()
  }, [blocker.state])
  return <dialog ref={dialog} className="workspace-dialog" aria-labelledby="leave-title"
    onCancel={(event) => { event.preventDefault(); blocker.reset?.() }}>
    <h2 id="leave-title" className="text-title-lg font-bold">{pending ? 'Submission in progress' : 'Save your work before leaving?'}</h2>
    <p className="mt-3 text-body-muted">{pending ? 'Please wait for the submission result before navigating away.' : 'Your latest changes have not been saved locally yet.'}</p>
    {blocker.state === 'blocked' && state.save.error && <p role="alert" className="mt-3 text-error">{state.save.error}</p>}
    <div className="mt-6 flex flex-wrap gap-3">
      <button className="workspace-secondary" disabled={saving} onClick={() => blocker.reset?.()}>Stay here</button>
      {!pending && <>
        <button className="workspace-secondary" disabled={saving} onClick={() => blocker.proceed?.()}>Leave without saving</button>
        <button className="workspace-primary" disabled={saving} onClick={async () => {
          setSaving(true)
          const saved = await saveDraft()
          setSaving(false)
          if (saved) blocker.proceed?.()
        }}>{saving ? 'Saving…' : 'Save and leave'}</button>
      </>}
    </div>
  </dialog>
}

function WorkspaceShell() {
  const { state, retryLoad, resetDraft } = useSupplierWorkspace()
  const remaining = useTaskStore((store) => store.tasks.filter((task) => !task.done).length)
  const loaded = state.load.status === 'success'
  return <main id="main-content" className="flex-grow w-full max-w-container-max mx-auto px-lg py-8 md:py-12">
    <div className="flex flex-wrap justify-between gap-5 items-start mb-7">
      <div><p className="text-label-uppercase text-brand-teal mb-2">Supplier Workspace</p>
        <h1 className="text-display-sm md:text-display-md font-bold">Build your supplier presence.</h1>
        <p className="text-body-muted mt-3 max-w-2xl">Keep your company details, capabilities, and evidence together. Give buyers a clearer picture of what you can supply.</p></div>

    </div>
    <nav aria-label="Supplier workspace" className="flex flex-wrap gap-2 mb-8 border-b border-hairline pb-4">
      {[
        { to: '/supplier', end: true, label: 'Overview', icon: 'space_dashboard' },
        { to: '/supplier/onboarding', label: 'Company listing', icon: 'edit_note' },
        { to: '/supplier/portfolio', label: 'Portfolio preview', icon: 'storefront' },
      ].map(({ to, end, label, icon }) => <NavLink key={to} to={to} end={end}
        className={({ isActive }) => `workspace-tab ${isActive ? 'bg-primary text-on-primary' : 'hover:bg-surface-strong'}`}>
        <span aria-hidden="true" className="material-symbols-outlined text-lg">{icon}</span>{label}
        {end && <span className="text-xs opacity-70">{remaining} to do</span>}
      </NavLink>)}
    </nav>
    {!loaded ? <section className="workspace-panel min-h-96" aria-busy={state.load.status !== 'error'}>
      {state.load.status === 'error' ? <div role="alert" className="max-w-lg">
        <h2 className="text-title-lg font-bold">We couldn't restore your draft</h2>
        <p className="mt-3 text-error">{state.load.error}</p>
        <p className="mt-3 text-sm text-body-muted">Retry first. Starting fresh backs up the stored draft before resetting it.</p>
        <div className="flex flex-wrap gap-3 mt-5"><button className="workspace-primary" onClick={retryLoad}>Retry loading</button>
          <button className="workspace-secondary" onClick={resetDraft}>Back up and start fresh</button></div>
      </div> : <div role="status" aria-label="Loading supplier workspace" className="space-y-5 motion-safe:animate-pulse">
        <p>Loading your saved workspace…</p><div className="h-10 w-2/3 bg-surface-strong rounded" /><div className="h-28 bg-surface-strong rounded" /><div className="h-16 bg-surface-strong rounded" />
      </div>}
    </section> : <Outlet />}

    <NavigationGuard />
  </main>
}
