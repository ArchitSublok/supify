const bandMeta = {
  unverified: {
    icon: 'error_outline',
    label: 'Unverified',
    colorClass: 'text-body-muted bg-surface-container border-hairline',
    badgeClass: 'bg-surface-strong text-primary border-hairline',
  },
  basic: {
    icon: 'hourglass_empty',
    label: 'Basic Standing',
    colorClass: 'text-brand-ochre bg-brand-ochre/15 border-brand-ochre/30',
    badgeClass: 'bg-brand-ochre/20 text-primary border-brand-ochre/30',
  },
  verified: {
    icon: 'verified',
    label: 'Verified',
    colorClass: 'text-brand-teal bg-brand-mint/25 border-brand-teal/30',
    badgeClass: 'bg-brand-teal text-on-primary',
  },
  audited: {
    icon: 'verified_user',
    label: 'Audited & High Trust',
    colorClass: 'text-brand-teal bg-brand-mint/40 border-brand-teal/40',
    badgeClass: 'bg-brand-teal text-on-primary',
  },
}

export function TrustBand({ trust, compact = false, showMethod = true }) {
  if (!trust) return null
  const meta = bandMeta[trust.band] || bandMeta.unverified

  if (compact) {
    return (
      <span className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-semibold tracking-wide border ${meta.colorClass}`}>
        <span className="material-symbols-outlined text-sm font-semibold leading-none" data-fill={trust.band === 'verified' || trust.band === 'audited' ? 'true' : 'false'}>
          {meta.icon}
        </span>
        <span>{meta.label}</span>
      </span>
    )
  }

  return (
    <div className={`flex flex-col gap-1 p-2.5 rounded-lg border ${meta.colorClass}`}>
      <div className="flex items-center gap-1.5 font-semibold text-sm">
        <span className="material-symbols-outlined text-base leading-none" data-fill={trust.band === 'verified' || trust.band === 'audited' ? 'true' : 'false'}>
          {meta.icon}
        </span>
        <span>{meta.label}</span>
      </div>
      {showMethod && (
        <div className="text-xs text-on-surface-variant flex items-center gap-1">
          <span>{trust.methodLabel || 'Self-declared'}</span>
          <span className="opacity-60">·</span>
          <span>{trust.lastChecked || 'Recently checked'}</span>
        </div>
      )}
    </div>
  )
}
