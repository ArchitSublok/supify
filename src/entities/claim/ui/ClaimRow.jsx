export function ClaimRow({ claim, onOpenEvidence }) {
  const date = claim.verifiedAt
    ? `Established ${new Intl.DateTimeFormat('en-US', { day: 'numeric', month: 'short', year: 'numeric' }).format(new Date(claim.verifiedAt))}`
    : 'Not yet independently established'

  const stateConfig = {
    verified: {
      icon: 'check_circle',
      bgClass: 'bg-brand-mint/20 text-brand-teal border-brand-teal/30',
      badge: 'Verified',
    },
    under_review: {
      icon: 'hourglass_empty',
      bgClass: 'bg-brand-ochre/20 text-primary border-brand-ochre/40',
      badge: 'Under Review',
    },
    submitted: {
      icon: 'pending',
      bgClass: 'bg-surface-variant text-on-surface-variant border-hairline',
      badge: 'Submitted',
    },
  }

  const config = stateConfig[claim.state] || stateConfig.submitted

  return (
    <article className="p-md bg-surface-card rounded-xl border border-hairline hover:border-outline transition-colors flex flex-col sm:flex-row justify-between items-start sm:items-center gap-md group">
      <div className="flex items-start gap-3 flex-1">
        <div className={`w-8 h-8 rounded-full flex items-center justify-center flex-shrink-0 border ${config.bgClass}`}>
          <span className="material-symbols-outlined text-base" data-fill={claim.state === 'verified' ? 'true' : 'false'}>
            {config.icon}
          </span>
        </div>
        <div className="flex flex-col">
          <div className="flex items-center gap-2 flex-wrap">
            <h4 className="font-title-md text-title-md text-primary font-semibold">{claim.label}</h4>
            <span className={`text-[11px] font-semibold uppercase px-2 py-0.5 rounded border ${config.bgClass}`}>
              {config.badge}
            </span>
          </div>
          <p className="font-body-md text-body-md text-primary mt-0.5">{claim.value}</p>
          <div className="flex items-center gap-2 text-xs text-body-muted mt-1">
            <span className="font-medium">{claim.methodLabel}</span>
            <span>·</span>
            <span>{date}</span>
          </div>
        </div>
      </div>

      <div className="flex items-center gap-3 w-full sm:w-auto justify-between sm:justify-end border-t sm:border-t-0 pt-2 sm:pt-0 border-hairline">
        <span className="inline-flex items-center gap-1 text-xs text-body-muted bg-surface px-2.5 py-1 rounded-full border border-hairline">
          <span className="material-symbols-outlined text-sm">attach_file</span>
          {claim.evidenceCount || 0} {claim.evidenceCount === 1 ? 'evidence item' : 'evidence items'}
        </span>
        {onOpenEvidence && (
          <button
            onClick={() => onOpenEvidence(claim)}
            className="text-xs font-button text-button text-brand-teal hover:underline flex items-center gap-0.5"
          >
            Inspect <span className="material-symbols-outlined text-xs">arrow_forward</span>
          </button>
        )}
      </div>
    </article>
  )
}
