import { useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { useSupplierWorkspace } from '../model/useSupplierWorkspace.js'
import { useTaskStore } from '../model/taskStore.js'
import { validateValues } from '../model/validation.js'
import { allFields } from '../model/fields.js'
import { FormField } from '../../../shared/ui/FormField'
import { DraftStatus } from './WorkspaceLayout'

export function WorkspaceDashboard() {
  const { state, saveDraft } = useSupplierWorkspace()
  const tasks = useTaskStore((store) => store.tasks)
  const addTask = useTaskStore((store) => store.addTask)
  const toggleTask = useTaskStore((store) => store.toggleTask)
  const removeTask = useTaskStore((store) => store.removeTask)
  const [title, setTitle] = useState('')
  const [error, setError] = useState('')
  const [params, setParams] = useSearchParams()
  const filter = ['open', 'done'].includes(params.get('tasks')) ? params.get('tasks') : 'all'
  const visible = tasks.filter((task) => filter === 'all' || (filter === 'done' ? task.done : !task.done))
  const errors = validateValues(state.values)
  const required = allFields.filter((field) => field.required)
  const completed = required.filter((field) => !errors[field.name]).length
  const done = tasks.filter((task) => task.done).length
  const attached = state.attachments.filter((file) => file.status === 'success').length

  return <div className="space-y-7">
    <section className="grid md:grid-cols-3 gap-4" aria-label="Workspace summary">
      {[
        ['Required details', `${completed}/${required.length}`, 'Core fields ready for review', 'bg-brand-mint/40'],
        ['Evidence attached', String(attached), 'Demo records; claims are self-declared', 'bg-brand-peach/30'],
        ['Checklist progress', `${done}/${tasks.length}`, 'Preparation tasks completed', 'bg-brand-lavender/25'],
      ].map(([label, value, hint, color]) => <div key={label} className={`workspace-panel ${color}`}>
        <p className="text-xs font-semibold uppercase tracking-wide">{label}</p>
        <p className="text-display-md font-bold my-3">{value}</p><p className="text-xs text-body-muted">{hint}</p>
      </div>)}
    </section>
    <div className="grid lg:grid-cols-12 gap-7 items-start">
      <section className="lg:col-span-5 workspace-panel space-y-5">
        <p className="text-label-uppercase text-brand-teal">Your company</p>
        <h2 className="text-display-sm font-bold break-words">{state.values.tradeName || 'Your supplier listing'}</h2>
        <p className="text-sm text-body-muted break-words">{state.values.location || 'Add your operating location'}</p>
        <div className="border-t border-hairline pt-5"><DraftStatus /></div>
        <p className="text-sm">{state.submit.receipt ? 'Demo submission received. A real verification review has not started.' : 'Complete your listing, check your portfolio, and submit when ready.'}</p>
        {state.submit.receipt && <p className="text-xs break-all">Receipt: {state.submit.receipt.id}</p>}
        <div className="flex flex-wrap gap-3"><Link className="workspace-primary" to="/supplier/onboarding/organisation">Edit company listing</Link>
          <Link className="workspace-secondary" to="/supplier/portfolio">Preview portfolio</Link></div>
        <button className="workspace-text-button" disabled={state.save.status === 'pending'} onClick={saveDraft}>Save draft now</button>
      </section>
      <section className="lg:col-span-7 workspace-panel">
        <div className="flex flex-wrap justify-between gap-3 items-center"><h2 className="text-title-lg font-bold">Before you submit</h2><span className="workspace-badge bg-surface-strong">{tasks.length - done} remaining</span></div>
        <p className="text-sm text-body-muted mt-2">A personal preparation checklist. Tasks stay available across workspace pages for this session.</p>
        <div className="flex gap-2 my-5" role="group" aria-label="Filter preparation tasks">
          {['all', 'open', 'done'].map((value) => <button key={value} type="button" aria-pressed={filter === value}
            className={`workspace-tab capitalize ${filter === value ? 'bg-primary text-white' : 'bg-surface-strong'}`}
            onClick={() => setParams((current) => { const next = new URLSearchParams(current); value === 'all' ? next.delete('tasks') : next.set('tasks', value); return next })}>{value[0].toUpperCase() + value.slice(1)}</button>)}
        </div>
        {visible.length ? <ul className="divide-y divide-hairline">{visible.map((task) => <li key={task.id} className="flex gap-3 items-start py-4">
          <label className="flex items-start gap-3 flex-1 min-w-0"><input type="checkbox" className="mt-1 accent-brand-teal" checked={task.done} onChange={() => toggleTask(task.id)} />
            <span className={`text-sm break-words min-w-0 ${task.done ? 'line-through text-body-muted' : ''}`}>{task.title}</span></label>
          <button type="button" className="text-body-muted hover:text-error shrink-0" aria-label={`Remove task: ${task.title}`} onClick={() => removeTask(task.id)}><span aria-hidden="true" className="material-symbols-outlined text-lg">close</span></button>
        </li>)}</ul> : <p role="status" className="rounded-xl bg-background p-5 text-sm">{tasks.length ? 'No tasks in this view.' : 'Your checklist is empty. Add a preparation task below.'}</p>}
        <form className="mt-5 border-t border-hairline pt-5 space-y-3" noValidate onSubmit={(event) => {
          event.preventDefault()
          if (!addTask(title)) { setError(title.trim() ? 'Use 120 characters or fewer.' : 'Enter a task to add.'); return }
          setTitle(''); setError('')
        }}>
          <FormField name="task" label="Add a preparation task" value={title} error={error} maxLength={120}
            placeholder="For example, check the latest certificate" onChange={(event) => { setTitle(event.target.value); if (error) setError('') }} />
          <button type="submit" className="workspace-secondary">Add task</button>
        </form>
      </section>
    </div>
  </div>
}
