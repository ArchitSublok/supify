import { Link } from 'react-router-dom'

export function SupplierCard({ supplier, onOpen = null }) {
  const bgHeaderClass = {
    'brand-teal': 'bg-brand-teal',
    'brand-pink': 'bg-brand-pink',
    'brand-ochre': 'bg-brand-ochre',
    'brand-lavender': 'bg-brand-lavender',
    'brand-peach': 'bg-brand-peach',
    'brand-coral': 'bg-brand-coral',
  }[supplier.themeColor] || 'bg-brand-teal'

  const textColorClass = {
    'brand-teal': 'text-brand-teal group-hover:text-brand-teal',
    'brand-pink': 'text-brand-pink group-hover:text-brand-pink',
    'brand-ochre': 'text-brand-ochre group-hover:text-brand-ochre',
    'brand-lavender': 'text-purple-700 group-hover:text-purple-700',
    'brand-peach': 'text-orange-700 group-hover:text-orange-700',
    'brand-coral': 'text-brand-coral group-hover:text-brand-coral',
  }[supplier.themeColor] || 'text-brand-teal group-hover:text-brand-teal'

  const isVerified = supplier.trust?.band === 'verified' || supplier.trust?.band === 'audited'

  // Compute dynamic verification score (from trust score, pillars average, or rating)
  const verificationScore = supplier.trust?.overallScore || (supplier.trust?.pillars?.length
    ? Math.round(supplier.trust.pillars.reduce((sum, p) => sum + (p.score ?? (Array.isArray(p) ? p[1] : 0)), 0) / supplier.trust.pillars.length)
    : Math.round((supplier.rating || 4.5) * 20))

  return (
    <div className="bg-surface-card border border-hairline rounded-2xl overflow-hidden hover:border-outline transition-all duration-200 group flex flex-col h-full shadow-sm hover:shadow-md">
      {/* Saturated Banner Header with tactile pattern */}
      <div className={`h-28 ${bgHeaderClass} p-4 relative overflow-hidden flex-shrink-0 flex items-center justify-between`}>
        <div className="absolute inset-0 opacity-20 bg-[url('data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSI4IiBoZWlnaHQ9IjgiPgo8cmVjdCB3aWR0aD0iOCIgaGVpZ2h0PSI4IiBmaWxsPSIjZmZmIiBmaWxsLW9wYWNpdHk9IjAuMSI+PC9yZWN0Pgo8cGF0aCBkPSJNMCAwTDggOFpNOCAwTDAgOFoiIHN0cm9rZT0iI2ZmZiIHN0cm9rZS13aWR0aD0iMSIgc3Ryb2tlLW9wYWNpdHk9IjAuMiIvPgo8L3N2Zz4=')] pointer-events-none"></div>
        
        {/* Left: Trust Status Badge */}
        <div className="relative z-10">
          {isVerified ? (
            <span className="inline-flex items-center gap-1 px-3 py-1.5 bg-on-primary text-primary rounded-full text-xs font-bold shadow-sm">
              <span className={`material-symbols-outlined text-sm ${textColorClass}`} data-fill="true">
                verified
              </span>
              Verified
            </span>
          ) : (
            <span className="inline-flex items-center gap-1 px-3 py-1.5 bg-surface-card text-primary rounded-full text-xs font-bold border border-hairline shadow-sm">
              <span className="material-symbols-outlined text-sm text-brand-ochre" data-fill="false">
                hourglass_empty
              </span>
              {supplier.trust?.band === 'basic' ? 'Basic Standing' : 'Unverified'}
            </span>
          )}
        </div>

        {/* Right: Verification Rating Box (shows rating instead of initial) */}
        <div className="relative z-10 bg-on-primary/95 backdrop-blur-sm rounded-xl px-3 py-1.5 flex flex-col items-center justify-center shadow-md border border-black/5 min-w-[62px]">
          <div className="flex items-center gap-0.5">
            <span className={`font-display-sm text-base font-extrabold leading-none ${textColorClass}`}>
              {verificationScore}
            </span>
            <span className="text-[10px] text-body-muted font-bold">/100</span>
          </div>
          <span className="text-[9px] font-bold uppercase tracking-wider text-body-muted leading-tight mt-0.5">
            Trust Score
          </span>
        </div>
      </div>

      {/* Card Body with strict uniform alignment */}
      <div className="p-5 flex flex-col flex-1 bg-surface-card">
        {/* Category & Location */}
        <div className="flex items-center justify-between gap-2 text-xs font-semibold text-body-muted uppercase tracking-wider mb-1">
          <span className="truncate">{supplier.category}</span>
          <span className="truncate opacity-80">{supplier.location?.split(',')?.[0] || supplier.location}</span>
        </div>

        {/* Trade Name */}
        <Link 
          to={`/suppliers/${supplier.id}`} 
          onClick={onOpen ? (e) => { e.preventDefault(); onOpen(supplier.id); } : undefined}
          className="block"
        >
          <h3 className={`font-title-lg text-lg font-bold text-primary mb-1 line-clamp-1 transition-colors cursor-pointer ${textColorClass}`}>
            {supplier.tradeName}
          </h3>
        </Link>

        {/* Description clamped uniformly */}
        <p className="text-xs text-body-muted line-clamp-2 min-h-[34px] mb-4 leading-relaxed">
          {supplier.description || 'Verified industrial supplier with structured claims ledger and quality documentation.'}
        </p>

        {/* Facts Grid */}
        <div className="grid grid-cols-2 gap-3 p-3 bg-surface rounded-xl border border-hairline/60 mb-4">
          <div>
            <p className="text-[10px] uppercase font-bold text-body-muted mb-0.5">Lead Time</p>
            <p className="text-xs font-bold text-primary truncate">{supplier.leadTime}</p>
          </div>
          <div>
            <p className="text-[10px] uppercase font-bold text-body-muted mb-0.5">Minimum Order</p>
            <p className="text-xs font-bold text-primary truncate">{supplier.moq}</p>
          </div>
        </div>

        {/* Footer with clean action links aligned to bottom */}
        <div className="mt-auto pt-3 border-t border-hairline flex justify-between items-center text-xs">
          <div className="flex items-center gap-1">
            <span className="material-symbols-outlined text-brand-ochre text-sm" data-fill="true">star</span>
            <span className="font-bold text-primary">{supplier.rating || 4.8}</span>
            <span className="text-[11px] text-body-muted">({supplier.claims?.filter(c => c.state === 'verified').length || 2} verified)</span>
          </div>

          <div className="flex items-center gap-2">
            <Link
              to={`/verification?supplier=${supplier.id}`}
              className="font-button text-[11px] font-bold text-brand-teal bg-brand-mint/20 hover:bg-brand-mint/40 border border-brand-teal/20 px-2.5 py-1 rounded-lg transition-colors flex items-center gap-0.5"
            >
              <span className="material-symbols-outlined text-xs">verified</span>
              Verify
            </Link>
            <Link
              to={`/suppliers/${supplier.id}`}
              onClick={onOpen ? (e) => { e.preventDefault(); onOpen(supplier.id); } : undefined}
              className={`font-button text-[11px] font-bold hover:underline flex items-center gap-0.5 ${textColorClass}`}
            >
              Profile <span className="material-symbols-outlined text-xs">arrow_forward</span>
            </Link>
          </div>
        </div>
      </div>
    </div>
  )
}
