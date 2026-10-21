import { Link, Outlet, useLocation } from 'react-router-dom'
import { onboardingSteps } from '../model/steps.js'
import { useSupplierWorkspace } from '../model/useSupplierWorkspace.js'

export function OnboardingScreen() {
  const { state } = useSupplierWorkspace()
  const { pathname } = useLocation()
  const key = pathname.split('/').pop()
  const index = onboardingSteps.findIndex((step) => step.key === key)
  return <div className="grid grid-cols-1 lg:grid-cols-12 gap-7 items-start">
    <aside className="lg:col-span-4 space-y-4">
      <div className="mb-5"><h2 className="text-title-lg font-bold">Your company listing</h2>
        <p className="text-sm text-body-muted mt-2">Four steps from company details to a complete draft.</p></div>
      <nav aria-label="Listing steps" className="space-y-2">
        {onboardingSteps.map((step, i) => <Link key={step.key} to={`/supplier/onboarding/${step.key}`}
          aria-label={`Step ${step.number}: ${step.label}`}
          aria-current={key === step.key ? 'step' : undefined}
          aria-disabled={state.submit.status === 'pending'}
          onClick={(event) => { if (state.submit.status === 'pending') event.preventDefault() }}
          className={`flex items-center gap-3 p-4 rounded-xl border ${key === step.key ? 'border-primary bg-surface-card shadow-sm' : 'border-hairline hover:bg-surface-card'}`}>
          <span className={`flex h-8 w-8 shrink-0 items-center justify-center rounded-full text-xs font-bold ${key === step.key ? 'bg-primary text-white' : 'bg-surface-strong'}`}>{step.number}</span>
          <span><span className="block text-sm font-bold">{step.label}</span><span className="block text-xs text-body-muted mt-1">{step.detail}</span></span>
          {i < index && <span className="ml-auto text-xs text-brand-teal">Done</span>}
        </Link>)}
      </nav>
      <p className="text-xs text-body-muted p-4 leading-relaxed">Drafts stay in this browser. Evidence is simulated and claims remain self-declared until a real verifier reviews them.</p>
    </aside>
    <section className="lg:col-span-8 workspace-panel min-w-0"><Outlet /></section>
  </div>
}
