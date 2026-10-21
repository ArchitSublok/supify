import { Link, NavLink, Outlet } from 'react-router-dom'
import { useSupplierWorkspace } from '../model/useSupplierWorkspace.js'
import { validateField } from '../model/validation.js'

export function PortfolioScreen() {
  const { state } = useSupplierWorkspace()
  const { values } = state
  return <section className="workspace-panel p-0 overflow-hidden">
    <div className="bg-brand-teal text-white p-6 md:p-10 relative">
      <p className="text-label-uppercase text-brand-mint">Supplier portfolio · Private preview</p>
      <h2 className="text-display-sm md:text-display-lg font-bold mt-4 max-w-4xl break-words">{values.tradeName || 'Your company name'}</h2>
      <p className="text-sm text-white/80 mt-4 break-words max-w-3xl">{values.legalName || 'Add your registered company name'}</p>
      <p className="text-sm mt-5">{values.location || 'Operating location not provided'}</p>
      <div className="flex flex-wrap gap-3 items-center mt-6"><span className="workspace-badge bg-brand-mint text-brand-teal">Self-declared listing</span>
        <Link className="text-sm underline underline-offset-4" to="/supplier/onboarding/organisation">Edit company details</Link></div>
    </div>
    <div className="p-6 md:p-8">
      <nav aria-label="Portfolio sections" className="flex flex-wrap gap-3 border-b border-hairline pb-4 mb-7">
        {[['', 'About'], ['/capabilities', 'Capabilities'], ['/evidence', 'Evidence']].map(([path, label]) => <NavLink key={label}
          to={`/supplier/portfolio${path}`} end={!path} className={({ isActive }) => `workspace-tab ${isActive ? 'bg-brand-mint text-brand-teal' : 'hover:bg-surface-strong'}`}>{label}</NavLink>)}
      </nav>
      <Outlet />
      <p className="text-xs text-body-muted border-t border-hairline mt-8 pt-5">This preview uses your current draft. It is not published to buyer discovery and does not establish verification.</p>
    </div>
  </section>
}

export function PortfolioAbout() {
  const { state: { values } } = useSupplierWorkspace()
  return <div className="max-w-3xl space-y-5"><h3 className="text-title-lg font-bold">About the company</h3>
    <p className="text-body-muted">{values.tradeName || 'This supplier'} lists capabilities in {values.category || 'a category to be provided'}{values.location ? ` from ${values.location}` : ''}.</p>
    <dl className="space-y-4 text-sm"><div><dt className="text-body-muted">Registered name</dt><dd className="font-semibold mt-1 break-words">{values.legalName || 'Not provided'}</dd></div>
      <div><dt className="text-body-muted">Website</dt><dd className="font-semibold mt-1 break-all">{values.website && !validateField('website', values.website)
        ? <a className="underline" href={values.website} target="_blank" rel="noopener noreferrer">{values.website} ↗</a> : 'Not provided or needs a valid URL'}</dd></div>
      <div><dt className="text-body-muted">Registry identifier (self-declared)</dt><dd className="font-semibold mt-1 break-all">{values.gstNumber || 'Not provided'}</dd></div></dl>
  </div>
}

export function PortfolioCapabilities() {
  const { state: { values } } = useSupplierWorkspace()
  return <div><h3 className="text-title-lg font-bold mb-5">Manufacturing capabilities</h3>
    <dl className="grid sm:grid-cols-2 gap-4">{[['Primary category', 'category'], ['Monthly capacity', 'capacity'], ['Minimum order', 'moq'], ['Typical lead time', 'leadTime']].map(([label, name]) => <div key={name} className="rounded-xl bg-background p-5 min-w-0"><dt className="text-xs text-body-muted mb-2">{label}</dt><dd className="font-semibold break-words">{values[name] || 'Not provided'}</dd></div>)}</dl>
    <h4 className="font-semibold mt-6 mb-2">Quality management process</h4><p className="text-sm text-body-muted whitespace-pre-wrap break-words">{values.qualityProcess || 'No quality management process described yet.'}</p>
  </div>
}

export function PortfolioEvidence() {
  const { state } = useSupplierWorkspace()
  return <div><h3 className="text-title-lg font-bold mb-3">Evidence on your draft</h3>
    <p className="text-sm text-body-muted mb-5">These are demo attachment records, not independently checked certifications.</p>
    {state.attachments.length ? <ul className="space-y-3">{state.attachments.map((file) => <li key={file.id} className="rounded-xl bg-background p-4 flex flex-wrap justify-between gap-3 text-sm"><strong className="break-all">{file.name}</strong><span>{file.status === 'success' ? 'Demo attached · self-declared' : 'Incomplete · needs attention'}</span></li>)}</ul>
      : <div className="rounded-xl border border-dashed border-outline-variant p-6"><p>No evidence attached yet.</p><Link className="workspace-text-button inline-block mt-3" to="/supplier/onboarding/claims">Add evidence to your listing</Link></div>}
  </div>
}
