function formatDate(value) {
  if (!value) return ''
  try {
    return new Intl.DateTimeFormat('en-US', { day: 'numeric', month: 'short', year: 'numeric' }).format(new Date(value))
  } catch (e) {
    return value
  }
}

export function ClaimRow({ claim, isOpen = false, onToggle = null, onOpenEvidence = null }) {
  const date = claim.verifiedAt
    ? `Established ${formatDate(claim.verifiedAt)}`
    : 'Not yet independently established'

  const validity = claim.validUntil
    ? `Attestation ${new Date(claim.validUntil).getTime() < Date.now() ? 'lapsed' : 'valid until'} ${formatDate(claim.validUntil)}`
    : null

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
    needs_more_info: {
      icon: 'help_outline',
      bgClass: 'bg-brand-ochre/20 text-primary border-brand-ochre/40',
      badge: 'Needs Info',
    },
    submitted: {
      icon: 'pending',
      bgClass: 'bg-surface-variant text-on-surface-variant border-hairline',
      badge: 'Submitted',
    },
    revoked: {
      icon: 'cancel',
      bgClass: 'bg-error-container text-on-error-container border-error/30',
      badge: 'Revoked',
    },
    expired: {
      icon: 'schedule',
      bgClass: 'bg-error-container text-on-error-container border-error/30',
      badge: 'Expired',
    },
  }

  const config = stateConfig[claim.state] || stateConfig.submitted

  const handleInspect = () => {
    if (onOpenEvidence) onOpenEvidence(claim)
    if (onToggle) onToggle()
  }

  return (
    <article className="p-md bg-surface-card rounded-xl border border-hairline hover:border-outline transition-colors flex flex-col justify-between gap-md group">
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-md">
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
            <div className="flex items-center gap-2 text-xs text-body-muted mt-1 flex-wrap">
              <span className="font-medium">{claim.methodLabel}</span>
              <span>·</span>
              <span>{date}</span>
              {validity && (
                <>
                  <span>·</span>
                  <span className={new Date(claim.validUntil).getTime() < Date.now() ? 'text-error font-semibold' : 'text-brand-teal'}>
                    {validity}
                  </span>
                </>
              )}
            </div>
          </div>
        </div>

        <div className="flex items-center gap-3 w-full sm:w-auto justify-between sm:justify-end border-t sm:border-t-0 pt-2 sm:pt-0 border-hairline">
          <span className="inline-flex items-center gap-1 text-xs text-body-muted bg-surface px-2.5 py-1 rounded-full border border-hairline">
            <span className="material-symbols-outlined text-sm">attach_file</span>
            {claim.evidenceCount || (claim.evidence ? claim.evidence.length : 0)} evidence item{(claim.evidenceCount === 1 || claim.evidence?.length === 1) ? '' : 's'}
          </span>
          {(onOpenEvidence || onToggle) && (
            <button
              onClick={handleInspect}
              className="text-xs font-button text-button text-brand-teal hover:underline flex items-center gap-0.5 font-bold"
            >
              {isOpen ? 'Close' : 'Inspect'} <span className="material-symbols-outlined text-xs">arrow_forward</span>
            </button>
          )}
        </div>
      </div>

      {isOpen && claim.evidence && claim.evidence.length > 0 && (
        <div className="p-3 bg-surface rounded-lg border border-hairline text-xs flex flex-col gap-2 animate-fadeIn">
          <strong className="text-primary font-semibold">Evidence on record ({claim.evidence.length}):</strong>
          <div className="flex flex-col gap-1">
            {claim.evidence.map((item, idx) => (
              <div key={idx} className="flex justify-between items-center text-on-surface-variant p-1.5 bg-background rounded border border-hairline">
                <span className="font-medium text-primary">{item.label}</span>
                <span className="text-body-muted text-[11px]">{item.kindLabel} · {formatDate(item.issuedOn)}</span>
              </div>
            ))}
          </div>
        </div>
      )}
    </article>
  )
}
