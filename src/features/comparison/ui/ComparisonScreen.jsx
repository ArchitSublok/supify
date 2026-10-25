import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { TrustBand } from '../../../entities/trust/ui/TrustBand'
import { supplierRepository } from '../../../shared/api/supplierRepository'

export function ComparisonScreen() {
  const [searchParams, setSearchParams] = useSearchParams()
  const [suppliers, setSuppliers] = useState(null)
  const [hasError, setHasError] = useState(false)
  const [retryToken, setRetryToken] = useState(0)
  const [filterQuery, setFilterQuery] = useState('')

  useEffect(() => {
    let cancelled = false
    async function loadSuppliers() {
      setHasError(false)
      try {
        const results = await supplierRepository.search({})
        if (!cancelled) setSuppliers(results)
      } catch {
        if (!cancelled) setHasError(true)
      }
    }
    loadSuppliers()
    return () => {
      cancelled = true
    }
  }, [retryToken])

  const idsParam = searchParams.get('ids') || ''

  const selectedIds = useMemo(
    () => new Set(idsParam.split(',').filter(Boolean)),
    [idsParam],
  )

  const selected = useMemo(
    () => (suppliers || []).filter((supplier) => selectedIds.has(supplier.id)),
    [suppliers, selectedIds],
  )

  const filteredSuppliers = useMemo(() => {
    if (!suppliers) return []
    if (!filterQuery.trim()) return suppliers
    const q = filterQuery.toLowerCase()
    return suppliers.filter(
      (s) =>
        s.tradeName.toLowerCase().includes(q) ||
        s.category.toLowerCase().includes(q) ||
        s.location.toLowerCase().includes(q),
    )
  }, [suppliers, filterQuery])

  const toggleId = useCallback(
    (supplierId) => {
      const next = new Set(selectedIds)
      if (next.has(supplierId)) next.delete(supplierId)
      else next.add(supplierId)
      const updated = new URLSearchParams(searchParams)
      if (next.size > 0) updated.set('ids', [...next].join(','))
      else updated.delete('ids')
      setSearchParams(updated)
    },
    [searchParams, selectedIds, setSearchParams],
  )

  const selectTopTwo = () => {
    if (!suppliers || suppliers.length === 0) return
    const topTwo = suppliers.slice(0, 2).map((s) => s.id)
    const updated = new URLSearchParams(searchParams)
    updated.set('ids', topTwo.join(','))
    setSearchParams(updated)
  }

  const clearSelection = () => {
    const updated = new URLSearchParams(searchParams)
    updated.delete('ids')
    setSearchParams(updated)
  }

  if (hasError) {
    return (
      <main className="flex-grow w-full max-w-container-max mx-auto px-lg py-12">
        <div className="bg-surface-card border border-error/30 rounded-2xl p-8 text-center max-w-md mx-auto shadow-sm">
          <span className="material-symbols-outlined text-4xl text-error mb-3">warning</span>
          <h2 className="font-title-lg text-lg font-bold text-primary mb-2">Could Not Load Suppliers</h2>
          <p className="text-body-muted text-sm mb-6">Failed to retrieve candidate suppliers for comparison.</p>
          <button
            onClick={() => setRetryToken((t) => t + 1)}
            className="px-5 py-2.5 bg-primary text-on-primary rounded-xl font-bold text-sm hover:opacity-90 transition-opacity"
          >
            Try Again
          </button>
        </div>
      </main>
    )
  }

  return (
    <main className="flex-grow w-full max-w-container-max mx-auto px-lg py-8 md:py-12 flex flex-col gap-6">
      {/* Top Header & Breadcrumb */}
      <div>
        <Link
          to="/search"
          className="inline-flex items-center gap-1 text-xs font-bold text-brand-teal hover:underline mb-3 uppercase tracking-wider"
        >
          <span className="material-symbols-outlined text-sm">arrow_back</span>
          Back to Supplier Discovery
        </Link>
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div>
            <div className="flex items-center gap-2.5">
              <h1 className="font-display-md text-2xl md:text-3xl font-bold text-primary">
                Supplier Comparison Matrix
              </h1>
              <span className="px-3 py-1 bg-brand-mint/30 text-brand-teal text-xs font-bold rounded-full border border-brand-teal/20">
                {selected.length} candidate{selected.length === 1 ? '' : 's'} selected
              </span>
            </div>
            <p className="text-body-muted text-sm mt-1">
              Compare verified manufacturers side by side on trust standing, lead times, minimum order quantities, and audit claims.
            </p>
          </div>
          <div className="flex items-center gap-2">
            {selected.length > 0 && (
              <button
                onClick={clearSelection}
                className="px-3 py-2 text-xs font-bold text-body-muted hover:text-error bg-surface-card border border-hairline rounded-lg transition-colors"
              >
                Clear Selection
              </button>
            )}
            {selected.length < 2 && (
              <button
                onClick={selectTopTwo}
                className="px-4 py-2 text-xs font-bold text-on-primary bg-primary rounded-lg hover:opacity-90 transition-opacity shadow-sm flex items-center gap-1"
              >
                <span className="material-symbols-outlined text-sm">compare_arrows</span>
                Compare Top 2
              </button>
            )}
          </div>
        </div>
      </div>

      {/* Main Grid: Candidates Sidebar + Comparison Matrix */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
        {/* Left Column: Candidate Picker */}
        <aside className="lg:col-span-4 xl:col-span-3 bg-surface-card border border-hairline rounded-2xl p-5 shadow-sm flex flex-col gap-4">
          <div className="flex items-center justify-between">
            <h2 className="text-sm font-bold text-primary">Candidates</h2>
            <span className="text-xs text-body-muted">
              {selected.length} / {suppliers?.length || 0}
            </span>
          </div>

          {/* Quick Filter */}
          <div className="relative">
            <span className="material-symbols-outlined absolute left-2.5 top-2.5 text-sm text-body-muted">
              search
            </span>
            <input
              type="text"
              placeholder="Filter candidates..."
              value={filterQuery}
              onChange={(e) => setFilterQuery(e.target.value)}
              className="w-full pl-8 pr-3 py-1.5 text-xs bg-background border border-hairline rounded-lg text-primary focus:outline-none focus:border-brand-teal"
            />
          </div>

          {/* Candidate Checkboxes List */}
          <div className="flex flex-col gap-1.5 max-h-[500px] overflow-y-auto pr-1">
            {suppliers === null && (
              <p className="text-xs text-body-muted py-4 text-center animate-pulse">Loading candidates…</p>
            )}
            {filteredSuppliers.map((supplier) => {
              const isChecked = selectedIds.has(supplier.id)
              return (
                <label
                  key={supplier.id}
                  className={`p-3 rounded-xl border transition-all cursor-pointer flex items-start gap-3 text-xs ${
                    isChecked
                      ? 'bg-brand-mint/15 border-brand-teal/40 shadow-2xs'
                      : 'bg-surface border-hairline/70 hover:bg-surface-strong'
                  }`}
                >
                  <input
                    type="checkbox"
                    checked={isChecked}
                    onChange={() => toggleId(supplier.id)}
                    className="mt-0.5 accent-brand-teal w-4 h-4 rounded cursor-pointer shrink-0"
                  />
                  <div className="flex-1 min-w-0">
                    <p className="font-bold text-primary truncate">{supplier.tradeName}</p>
                    <p className="text-[11px] text-body-muted truncate mb-1.5">{supplier.category}</p>
                    <TrustBand trust={supplier.trust} compact />
                  </div>
                </label>
              )
            })}
          </div>
        </aside>

        {/* Right Column: Comparison Table View */}
        <section className="lg:col-span-8 xl:col-span-9 flex flex-col gap-4">
          {suppliers === null ? (
            <div className="bg-surface-card border border-hairline rounded-2xl p-12 text-center shadow-sm">
              <span className="material-symbols-outlined text-4xl text-brand-teal animate-spin mb-3">
                progress_activity
              </span>
              <p className="text-sm font-bold text-primary">Loading comparison matrix…</p>
            </div>
          ) : selected.length === 0 ? (
            /* Empty State */
            <div className="bg-surface-card border-2 border-dashed border-hairline rounded-2xl p-12 text-center shadow-sm flex flex-col items-center justify-center">
              <div className="w-14 h-14 rounded-2xl bg-brand-mint/30 text-brand-teal flex items-center justify-center mb-4">
                <span className="material-symbols-outlined text-3xl">compare_arrows</span>
              </div>
              <h2 className="font-title-lg text-lg font-bold text-primary mb-1">
                No Suppliers Selected for Comparison
              </h2>
              <p className="text-body-muted text-sm max-w-md mb-6 leading-relaxed">
                Check two or more suppliers from the candidate list on the left to compare their lead times, trust ratings, and compliance documentation side by side.
              </p>
              <button
                onClick={selectTopTwo}
                className="px-5 py-2.5 bg-primary text-on-primary font-bold text-xs rounded-xl shadow-sm hover:opacity-90 transition-opacity flex items-center gap-1.5"
              >
                <span className="material-symbols-outlined text-sm">compare_arrows</span>
                Compare First 2 Suppliers
              </button>
            </div>
          ) : (
            <>
              {/* Informative Banner when only 1 candidate is selected */}
              {selected.length === 1 && (
                <div className="bg-brand-mint/20 border border-brand-teal/30 rounded-xl p-3.5 flex items-center justify-between text-xs text-primary shadow-2xs">
                  <div className="flex items-center gap-2">
                    <span className="material-symbols-outlined text-base text-brand-teal">info</span>
                    <span>
                      Viewing <strong>{selected[0].tradeName}</strong>. Check at least 1 more candidate on the left to see side-by-side differences.
                    </span>
                  </div>
                  <button
                    onClick={selectTopTwo}
                    className="font-bold text-brand-teal hover:underline shrink-0 ml-3"
                  >
                    + Add Another
                  </button>
                </div>
              )}

              {/* Responsive Comparison Table */}
              <div className="bg-surface-card border border-hairline rounded-2xl shadow-sm overflow-hidden">
                <div className="overflow-x-auto">
                  <table className="w-full text-left border-collapse min-w-[550px]">
                    {/* Header: Candidate Columns */}
                    <thead>
                      <tr className="border-b border-hairline bg-surface">
                        <th className="p-4 text-xs font-bold text-body-muted uppercase tracking-wider w-44 sticky left-0 bg-surface z-10 border-r border-hairline">
                          Candidate
                        </th>
                        {selected.map((supplier) => (
                          <th key={supplier.id} className="p-4 min-w-[200px] border-r border-hairline last:border-r-0 align-top">
                            <div className="flex justify-between items-start gap-2">
                              <Link
                                to={`/suppliers/${supplier.id}`}
                                className="font-title-lg text-sm font-bold text-primary hover:text-brand-teal transition-colors line-clamp-1"
                              >
                                {supplier.tradeName}
                              </Link>
                              <button
                                onClick={() => toggleId(supplier.id)}
                                className="text-body-muted hover:text-error p-1 rounded transition-colors"
                                title="Remove candidate"
                              >
                                <span className="material-symbols-outlined text-sm">close</span>
                              </button>
                            </div>
                            <p className="text-[11px] text-body-muted truncate mt-0.5">{supplier.location}</p>
                            <div className="mt-2">
                              <TrustBand trust={supplier.trust} compact />
                            </div>
                          </th>
                        ))}
                      </tr>
                    </thead>

                    {/* Table Body: Technical Specifications & Commercials */}
                    <tbody className="divide-y divide-hairline text-xs">
                      {/* Trust & Rating */}
                      <tr className="hover:bg-surface/50 transition-colors">
                        <td className="p-4 font-bold text-primary sticky left-0 bg-surface-card z-10 border-r border-hairline">
                          Trust Band &amp; Rating
                        </td>
                        {selected.map((s) => (
                          <td key={s.id} className="p-4 border-r border-hairline last:border-r-0">
                            <div className="flex items-center gap-1.5 font-bold text-primary">
                              <span className="material-symbols-outlined text-brand-ochre text-sm" data-fill="true">star</span>
                              <span>{s.rating || 4.8} / 5</span>
                              <span className="text-body-muted font-normal text-[11px]">({s.reviewsCount || 12} reviews)</span>
                            </div>
                            <span className="text-[11px] text-body-muted mt-1 block">
                              Method: {s.trust?.methodLabel || 'Document audit'}
                            </span>
                          </td>
                        ))}
                      </tr>

                      {/* Manufacturing Category */}
                      <tr className="bg-surface/25 hover:bg-surface/50 transition-colors">
                        <td className="p-4 font-bold text-primary sticky left-0 bg-surface-card z-10 border-r border-hairline">
                          Core Category
                        </td>
                        {selected.map((s) => (
                          <td key={s.id} className="p-4 font-semibold text-primary border-r border-hairline last:border-r-0">
                            {s.category}
                          </td>
                        ))}
                      </tr>

                      {/* Location & Region */}
                      <tr className="hover:bg-surface/50 transition-colors">
                        <td className="p-4 font-bold text-primary sticky left-0 bg-surface-card z-10 border-r border-hairline">
                          Operating Location
                        </td>
                        {selected.map((s) => (
                          <td key={s.id} className="p-4 text-primary border-r border-hairline last:border-r-0">
                            <span className="font-medium">{s.location}</span>
                            <span className="block text-[11px] text-body-muted uppercase tracking-wider mt-0.5">
                              Region: {s.region || 'Domestic / Global'}
                            </span>
                          </td>
                        ))}
                      </tr>

                      {/* Lead Time */}
                      <tr className="bg-surface/25 hover:bg-surface/50 transition-colors">
                        <td className="p-4 font-bold text-primary sticky left-0 bg-surface-card z-10 border-r border-hairline">
                          Production Lead Time
                        </td>
                        {selected.map((s) => (
                          <td key={s.id} className="p-4 font-bold text-primary border-r border-hairline last:border-r-0">
                            <span className="px-2.5 py-1 bg-surface-strong rounded-lg inline-block">
                              {s.leadTime}
                            </span>
                          </td>
                        ))}
                      </tr>

                      {/* Minimum Order Quantity (MOQ) */}
                      <tr className="hover:bg-surface/50 transition-colors">
                        <td className="p-4 font-bold text-primary sticky left-0 bg-surface-card z-10 border-r border-hairline">
                          Minimum Order (MOQ)
                        </td>
                        {selected.map((s) => (
                          <td key={s.id} className="p-4 font-bold text-primary border-r border-hairline last:border-r-0">
                            {s.moq}
                          </td>
                        ))}
                      </tr>

                      {/* Capacity */}
                      <tr className="bg-surface/25 hover:bg-surface/50 transition-colors">
                        <td className="p-4 font-bold text-primary sticky left-0 bg-surface-card z-10 border-r border-hairline">
                          Monthly Capacity
                        </td>
                        {selected.map((s) => (
                          <td key={s.id} className="p-4 text-primary font-medium border-r border-hairline last:border-r-0">
                            {s.capacity}
                          </td>
                        ))}
                      </tr>

                      {/* Verified Claims & Compliance */}
                      <tr className="hover:bg-surface/50 transition-colors">
                        <td className="p-4 font-bold text-primary sticky left-0 bg-surface-card z-10 border-r border-hairline">
                          Verified Claims
                        </td>
                        {selected.map((s) => {
                          const verified = (s.claims || []).filter((c) => c.state === 'verified')
                          return (
                            <td key={s.id} className="p-4 border-r border-hairline last:border-r-0">
                              <span className="font-bold text-brand-teal block mb-1.5">
                                {verified.length} of {s.claims?.length || 0} claims verified
                              </span>
                              <div className="flex flex-wrap gap-1">
                                {verified.slice(0, 3).map((c) => (
                                  <span
                                    key={c.key}
                                    className="px-2 py-0.5 bg-brand-mint/25 text-brand-teal font-semibold rounded text-[10px] border border-brand-teal/20"
                                  >
                                    {c.label}
                                  </span>
                                ))}
                              </div>
                            </td>
                          )
                        })}
                      </tr>

                      {/* Actions */}
                      <tr className="bg-surface/40">
                        <td className="p-4 font-bold text-primary sticky left-0 bg-surface-card z-10 border-r border-hairline">
                          Actions
                        </td>
                        {selected.map((s) => (
                          <td key={s.id} className="p-4 border-r border-hairline last:border-r-0">
                            <div className="flex flex-col gap-1.5">
                              <Link
                                to={`/verification?supplier=${s.id}`}
                                className="px-3 py-1.5 bg-brand-mint/25 text-brand-teal font-bold rounded-lg text-center hover:bg-brand-mint/40 transition-colors flex items-center justify-center gap-1 border border-brand-teal/20"
                              >
                                <span className="material-symbols-outlined text-xs">verified</span>
                                Audit Dossier
                              </Link>
                              <Link
                                to={`/suppliers/${s.id}`}
                                className="px-3 py-1.5 bg-primary text-on-primary font-bold rounded-lg text-center hover:opacity-90 transition-opacity flex items-center justify-center gap-1"
                              >
                                Profile
                                <span className="material-symbols-outlined text-xs">arrow_forward</span>
                              </Link>
                            </div>
                          </td>
                        ))}
                      </tr>
                    </tbody>
                  </table>
                </div>
              </div>
            </>
          )}
        </section>
      </div>
    </main>
  )
}
