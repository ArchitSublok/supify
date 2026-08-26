import { useEffect, useState } from 'react'
import { SupplierCard } from '../../../entities/supplier/ui/SupplierCard'
import { supplierRepository } from '../../../shared/api/supplierRepository'

export function SearchScreen({ onOpenSupplier }) {
  const [params, setParams] = useState(() => new URLSearchParams(window.location.search))
  const [suppliers, setSuppliers] = useState([])
  const [loading, setLoading] = useState(true)

  const query = params.get('q') || ''
  const band = params.get('band') || ''
  const region = params.get('region') || 'All Regions'
  const industry = params.get('industry') || 'All Industries'
  const sortBy = params.get('sort') || 'relevance'
  const [extraFilter, setExtraFilter] = useState('')

  useEffect(() => {
    setLoading(true)
    supplierRepository
      .search({ query, band, region, industry, sortBy })
      .then((data) => {
        let res = data
        if (extraFilter === 'iso_9001') {
          res = res.filter((s) => s.claims.some((c) => c.key.includes('iso') || c.label.includes('ISO')))
        } else if (extraFilter === 'lead_time_fast') {
          res = res.filter((s) => {
            const num = parseInt(s.leadTime) || 99
            return num <= 14
          })
        }
        setSuppliers(res)
        setLoading(false)
      })
  }, [query, band, region, industry, sortBy, extraFilter])

  function updateParams(next) {
    const updated = new URLSearchParams(params)
    Object.entries(next).forEach(([key, value]) => {
      if (value && value !== 'All Regions' && value !== 'All Industries' && value !== 'relevance') {
        updated.set(key, value)
      } else {
        updated.delete(key)
      }
    })
    const href = `/search${updated.toString() ? `?${updated}` : ''}`
    window.history.pushState({}, '', href)
    setParams(updated)
  }

  const clearAllFilters = () => {
    setExtraFilter('')
    updateParams({ q: '', band: '', region: 'All Regions', industry: 'All Industries', sort: 'relevance' })
  }

  const hasActiveFilters = query || band || (region && region !== 'All Regions') || (industry && industry !== 'All Industries') || extraFilter

  return (
    <main className="flex-grow w-full max-w-container-max mx-auto px-lg py-8 md:py-12 flex flex-col gap-10">
      {/* Header & Subtitle */}
      <section className="flex flex-col gap-lg">
        <div className="flex flex-col md:flex-row gap-md items-start md:items-center justify-between">
          <div>
            <h1 className="font-display-md text-display-md text-primary mb-xs font-bold">Find Suppliers</h1>
            <p className="text-body-muted font-body-md text-body-md">Discover verified partners for your supply chain.</p>
          </div>
          <div className="flex items-center gap-2">
            <span className="text-xs font-semibold text-body-muted">Standing:</span>
            <div className="flex flex-wrap gap-1 bg-surface-card p-1 rounded-xl border border-hairline">
              {[
                { id: '', label: 'All' },
                { id: 'verified', label: 'Verified' },
                { id: 'audited', label: 'Audited' },
                { id: 'basic', label: 'Basic' },
                { id: 'unverified', label: 'Unverified' },
              ].map((b) => (
                <button
                  key={b.id}
                  onClick={() => updateParams({ band: b.id })}
                  className={`text-xs px-3 py-1.5 rounded-lg font-button transition-colors ${
                    band === b.id
                      ? 'bg-primary text-on-primary font-bold shadow-sm'
                      : 'text-primary hover:bg-surface-variant'
                  }`}
                >
                  {b.label}
                </button>
              ))}
            </div>
          </div>
        </div>

        {/* Search & Filter Bar */}
        <div className="bg-surface-card rounded-xl border border-hairline p-lg flex flex-col lg:flex-row gap-md items-end shadow-sm">
          <div className="w-full lg:flex-1">
            <label className="block text-body-sm font-body-sm font-semibold text-primary mb-xs">Search</label>
            <div className="relative">
              <span className="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-body-muted">search</span>
              <input
                className="w-full pl-10 pr-4 py-3 bg-background border border-hairline rounded-lg focus:outline-none focus:border-primary focus:ring-1 focus:ring-primary transition-colors font-body-md text-body-md text-primary placeholder:text-body-muted"
                placeholder="Search by name, material, capability, or keyword..."
                type="text"
                value={query}
                onChange={(e) => updateParams({ q: e.target.value })}
              />
              {query && (
                <button
                  onClick={() => updateParams({ q: '' })}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-body-muted hover:text-primary"
                >
                  <span className="material-symbols-outlined text-sm">close</span>
                </button>
              )}
            </div>
          </div>

          <div className="w-full lg:w-48">
            <label className="block text-body-sm font-body-sm font-semibold text-primary mb-xs">Region</label>
            <select
              className="w-full px-4 py-3 bg-background border border-hairline rounded-lg focus:outline-none focus:border-primary focus:ring-1 focus:ring-primary transition-colors font-body-md text-body-md text-primary cursor-pointer"
              value={region}
              onChange={(e) => updateParams({ region: e.target.value })}
            >
              <option value="All Regions">All Regions</option>
              <option value="North America">North America</option>
              <option value="Europe">Europe</option>
              <option value="Asia Pacific">Asia Pacific</option>
              <option value="India">India</option>
            </select>
          </div>

          <div className="w-full lg:w-48">
            <label className="block text-body-sm font-body-sm font-semibold text-primary mb-xs">Industry</label>
            <select
              className="w-full px-4 py-3 bg-background border border-hairline rounded-lg focus:outline-none focus:border-primary focus:ring-1 focus:ring-primary transition-colors font-body-md text-body-md text-primary cursor-pointer"
              value={industry}
              onChange={(e) => updateParams({ industry: e.target.value })}
            >
              <option value="All Industries">All Industries</option>
              <option value="Textiles">Textiles</option>
              <option value="Packaging">Packaging</option>
              <option value="Electronics">Electronics</option>
              <option value="Machining">Machining</option>
              <option value="Industrial fasteners">Industrial Fasteners</option>
              <option value="Sheet-metal fabrication">Sheet-metal Fabrication</option>
            </select>
          </div>

          <div className="w-full lg:w-auto">
            <button
              onClick={() => {}}
              className="w-full lg:w-auto inline-flex items-center justify-center gap-xs font-button text-button text-on-primary bg-primary px-6 py-3 rounded-lg hover:opacity-90 transition-opacity whitespace-nowrap shadow-sm"
            >
              <span className="material-symbols-outlined text-sm">filter_list</span>
              Apply Filters
            </button>
          </div>
        </div>

        {/* Active Filter Pills */}
        <div className="flex gap-xs flex-wrap items-center text-xs">
          <span className="text-body-muted font-medium mr-1">Active filters:</span>
          {query && (
            <span className="inline-flex items-center gap-1 px-3 py-1 bg-surface-strong rounded-full text-primary font-medium">
              Query: "{query}"
              <button onClick={() => updateParams({ q: '' })} className="hover:text-error transition-colors">
                <span className="material-symbols-outlined text-xs">close</span>
              </button>
            </span>
          )}
          {region !== 'All Regions' && (
            <span className="inline-flex items-center gap-1 px-3 py-1 bg-surface-strong rounded-full text-primary font-medium">
              Region: {region}
              <button onClick={() => updateParams({ region: 'All Regions' })} className="hover:text-error transition-colors">
                <span className="material-symbols-outlined text-xs">close</span>
              </button>
            </span>
          )}
          {industry !== 'All Industries' && (
            <span className="inline-flex items-center gap-1 px-3 py-1 bg-surface-strong rounded-full text-primary font-medium">
              Industry: {industry}
              <button onClick={() => updateParams({ industry: 'All Industries' })} className="hover:text-error transition-colors">
                <span className="material-symbols-outlined text-xs">close</span>
              </button>
            </span>
          )}
          {band && (
            <span className="inline-flex items-center gap-1 px-3 py-1 bg-surface-strong rounded-full text-primary font-medium">
              Trust: {band}
              <button onClick={() => updateParams({ band: '' })} className="hover:text-error transition-colors">
                <span className="material-symbols-outlined text-xs">close</span>
              </button>
            </span>
          )}
          <button
            onClick={() => setExtraFilter(extraFilter === 'iso_9001' ? '' : 'iso_9001')}
            className={`inline-flex items-center gap-1 px-3 py-1 rounded-full font-medium transition-colors ${
              extraFilter === 'iso_9001' ? 'bg-primary text-on-primary' : 'bg-surface-strong text-primary hover:bg-surface-variant'
            }`}
          >
            <span>ISO 9001</span>
            {extraFilter === 'iso_9001' && <span className="material-symbols-outlined text-xs">check</span>}
          </button>
          <button
            onClick={() => setExtraFilter(extraFilter === 'lead_time_fast' ? '' : 'lead_time_fast')}
            className={`inline-flex items-center gap-1 px-3 py-1 rounded-full font-medium transition-colors ${
              extraFilter === 'lead_time_fast' ? 'bg-primary text-on-primary' : 'bg-surface-strong text-primary hover:bg-surface-variant'
            }`}
          >
            <span>Lead Time &lt; 14 days</span>
            {extraFilter === 'lead_time_fast' && <span className="material-symbols-outlined text-xs">check</span>}
          </button>

          {hasActiveFilters && (
            <button
              onClick={clearAllFilters}
              className="text-brand-coral hover:underline font-semibold ml-2"
            >
              Reset All
            </button>
          )}
        </div>
      </section>

      {/* Results Grid Section */}
      <section>
        <div className="mb-md flex justify-between items-center">
          <p className="text-body-md font-body-md text-body-muted font-medium">
            Showing <strong className="text-primary font-bold">{suppliers.length}</strong> verified suppliers
          </p>
          <div className="flex items-center gap-xs">
            <span className="text-body-sm font-body-sm text-body-muted">Sort by:</span>
            <select
              className="bg-transparent border-none text-primary font-body-sm font-semibold focus:ring-0 cursor-pointer text-sm"
              value={sortBy}
              onChange={(e) => updateParams({ sort: e.target.value })}
            >
              <option value="relevance">Relevance</option>
              <option value="rating">Rating (High to Low)</option>
              <option value="leadTime">Lead Time (Shortest)</option>
              <option value="moq">MOQ (Lowest)</option>
            </select>
          </div>
        </div>

        {loading ? (
          <div className="p-12 text-center bg-surface-card rounded-xl border border-hairline text-body-muted">
            <span className="material-symbols-outlined text-4xl animate-spin mb-2">progress_activity</span>
            <p>Scanning verified supplier records...</p>
          </div>
        ) : suppliers.length > 0 ? (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-lg">
            {suppliers.map((supplier) => (
              <SupplierCard key={supplier.id} supplier={supplier} onOpen={onOpenSupplier} />
            ))}
          </div>
        ) : (
          <div className="p-12 text-center bg-surface-card border-2 border-dashed border-hairline rounded-2xl flex flex-col items-center gap-3">
            <span className="material-symbols-outlined text-5xl text-brand-coral">search_off</span>
            <h3 className="font-title-lg text-title-lg text-primary font-bold">No suppliers match your current filters</h3>
            <p className="text-body-muted max-w-md">
              Try adjusting your query, switching regions, or resetting your filter criteria to see available partners.
            </p>
            <button
              onClick={clearAllFilters}
              className="font-button text-button bg-primary text-on-primary px-5 py-2.5 rounded-lg mt-2 font-bold"
            >
              Clear All Filters
            </button>
          </div>
        )}

        {suppliers.length > 0 && (
          <div className="mt-xl flex justify-center">
            <button
              onClick={() => alert('All verified suppliers for your search criteria are currently loaded!')}
              className="inline-flex items-center justify-center font-button text-button text-primary bg-surface-card border border-hairline px-6 py-3 rounded-lg hover:bg-surface-strong transition-colors shadow-sm"
            >
              Load More Suppliers
            </button>
          </div>
        )}
      </section>
    </main>
  )
}
