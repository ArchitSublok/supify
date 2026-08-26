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

  return (
    <div className="bg-background border border-hairline rounded-xl overflow-hidden hover:border-outline transition-all duration-200 group flex flex-col h-full shadow-sm hover:shadow-md">
      {/* Saturated Banner Header with tactile pattern */}
      <div className={`h-32 ${bgHeaderClass} p-md relative overflow-hidden flex-shrink-0`}>
        <div className="absolute inset-0 opacity-20 bg-[url('data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSI4IiBoZWlnaHQ9IjgiPgo8cmVjdCB3aWR0aD0iOCIgaGVpZ2h0PSI4IiBmaWxsPSIjZmZmIiBmaWxsLW9wYWNpdHk9IjAuMSI+PC9yZWN0Pgo8cGF0aCBkPSJNMCAwTDggOFpNOCAwTDAgOFoiIHN0cm9rZT0iI2ZmZiIHN0cm9rZS13aWR0aD0iMSIgc3Ryb2tlLW9wYWNpdHk9IjAuMiIvPgo8L3N2Zz4=')]"></div>
        <div className="relative z-10 flex justify-between items-start">
          {isVerified ? (
            <span className="inline-flex items-center gap-1 px-2.5 py-1 bg-on-primary text-primary rounded-full text-label-uppercase font-label-uppercase shadow-sm">
              <span className={`material-symbols-outlined text-sm ${textColorClass}`} data-fill="true">
                verified
              </span>
              Verified
            </span>
          ) : (
            <span className="inline-flex items-center gap-1 px-2.5 py-1 bg-surface-card text-primary rounded-full text-label-uppercase font-label-uppercase border border-hairline shadow-sm">
              <span className="material-symbols-outlined text-sm text-brand-ochre" data-fill="false">
                hourglass_empty
              </span>
              {supplier.trust?.band === 'basic' ? 'Basic Standing' : 'Pending Verification'}
            </span>
          )}
          <div className="w-12 h-12 bg-on-primary rounded-lg flex items-center justify-center shadow-sm">
            <span className={`font-display-sm text-display-sm font-bold ${textColorClass}`}>
              {supplier.avatarInitial || supplier.tradeName?.[0] || 'S'}
            </span>
          </div>
        </div>
      </div>

      {/* Card Body */}
      <div className="p-lg flex flex-col flex-grow bg-surface-card rounded-b-xl">
        <div className="mb-auto">
          <div className="flex items-center justify-between gap-2 mb-1">
            <span className="text-xs font-semibold text-body-muted uppercase tracking-wider">
              {supplier.category} · {supplier.location?.split(',')?.[0] || supplier.location}
            </span>
          </div>
          <Link to={`/suppliers/${supplier.id}`} onClick={onOpen ? (e) => { e.preventDefault(); onOpen(supplier.id); } : undefined}>
            <h3 className={`font-title-lg text-title-lg text-primary mb-xs transition-colors cursor-pointer ${textColorClass}`}>
              {supplier.tradeName}
            </h3>
          </Link>
          <p className="text-body-sm font-body-sm text-body-muted line-clamp-2 mb-md">
            {supplier.description}
          </p>
        </div>

        {/* Facts Grid */}
        <div className="grid grid-cols-2 gap-md py-md border-t border-hairline mt-md">
          <div>
            <p className="text-label-uppercase font-label-uppercase text-body-muted mb-1">Lead Time</p>
            <p className="font-body-md text-body-md font-semibold text-primary">{supplier.leadTime}</p>
          </div>
          <div>
            <p className="text-label-uppercase font-label-uppercase text-body-muted mb-1">MOQ</p>
            <p className="font-body-md text-body-md font-semibold text-primary">{supplier.moq}</p>
          </div>
        </div>

        {/* Footer */}
        <div className="mt-md pt-md border-t border-hairline flex justify-between items-center">
          <div className="flex items-center gap-1">
            <span className="material-symbols-outlined text-brand-ochre text-base" data-fill="true">star</span>
            <span className="font-body-sm text-body-sm font-semibold text-primary">{supplier.rating || 4.8}</span>
            <span className="text-body-sm text-body-sm text-body-muted">({supplier.reviewsCount || 42})</span>
          </div>
          <Link
            to={`/suppliers/${supplier.id}`}
            onClick={onOpen ? (e) => { e.preventDefault(); onOpen(supplier.id); } : undefined}
            className={`font-button text-button font-semibold hover:underline flex items-center gap-1 ${textColorClass}`}
          >
            View Profile <span className="material-symbols-outlined text-sm">arrow_forward</span>
          </Link>
        </div>
      </div>
    </div>
  )
}
