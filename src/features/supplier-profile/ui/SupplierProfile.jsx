import { useEffect, useState } from 'react'
import { ClaimRow } from '../../../entities/claim/ui/ClaimRow'
import { TrustPanel } from '../../../entities/trust/ui/TrustPanel'
import { supplierRepository } from '../../../shared/api/supplierRepository'

export function SupplierProfile({ supplierId, onBack, onShowToast, onOpenVerification }) {
  const [supplier, setSupplier] = useState(null)
  const [selectedClaim, setSelectedClaim] = useState(null)
  const [isShortlisted, setIsShortlisted] = useState(false)

  useEffect(() => {
    supplierRepository.getById(supplierId).then(setSupplier)
  }, [supplierId])

  if (!supplier) {
    return (
      <main className="flex-grow max-w-container-max mx-auto px-lg py-16 flex flex-col items-center justify-center text-center">
        <span className="material-symbols-outlined text-5xl text-brand-teal animate-spin mb-3">
          progress_activity
        </span>
        <p className="text-body-muted font-medium">Retrieving verified supplier ledger...</p>
      </main>
    )
  }

  const handleToggleShortlist = () => {
    setIsShortlisted(!isShortlisted)
    onShowToast?.(
      isShortlisted
        ? `Removed ${supplier.tradeName} from your active shortlist.`
        : `Added ${supplier.tradeName} to your sourcing shortlist!`
    )
  }

  return (
    <main className="flex-grow w-full max-w-container-max mx-auto px-lg py-8 md:py-12 flex flex-col gap-8">
      {/* Back Navigation */}
      <button
        onClick={onBack}
        className="font-button text-button text-primary hover:text-brand-teal transition-colors flex items-center gap-1 w-fit font-semibold"
      >
        <span className="material-symbols-outlined text-sm">arrow_back</span>
        Back to search
      </button>

      {/* Profile Header */}
      <section className="bg-surface-card border border-hairline rounded-2xl p-6 md:p-8 flex flex-col md:flex-row justify-between items-start md:items-center gap-6 shadow-sm">
        <div className="flex items-start md:items-center gap-5">
          <div className="w-16 h-16 md:w-20 md:h-20 rounded-2xl bg-brand-teal text-on-primary flex items-center justify-center font-display-md text-display-md font-bold shadow-sm flex-shrink-0">
            {supplier.avatarInitial || supplier.tradeName?.[0] || 'S'}
          </div>
          <div>
            <div className="flex items-center gap-2 flex-wrap mb-1">
              <span className="font-label-uppercase text-label-uppercase bg-surface text-primary border border-hairline px-2.5 py-0.5 rounded font-bold">
                {supplier.category}
              </span>
              <span className="text-xs text-body-muted font-medium flex items-center gap-1">
                <span className="material-symbols-outlined text-xs">location_on</span>
                {supplier.location}
              </span>
            </div>
            <h1 className="font-display-md text-display-md text-primary font-bold">{supplier.tradeName}</h1>
            <p className="text-xs text-body-muted font-mono mt-0.5">{supplier.legalName}</p>
          </div>
        </div>

        <div className="flex flex-wrap gap-3 w-full md:w-auto">
          <button
            onClick={handleToggleShortlist}
            className={`font-button text-button px-5 py-2.5 rounded-lg border transition-all flex items-center gap-1.5 font-bold shadow-sm ${
              isShortlisted
                ? 'bg-brand-pink text-white border-brand-pink'
                : 'bg-background border-hairline text-primary hover:bg-surface-variant'
            }`}
          >
            <span className="material-symbols-outlined text-base" data-fill={isShortlisted ? 'true' : 'false'}>
              bookmark
            </span>
            {isShortlisted ? 'Shortlisted' : 'Add to Shortlist'}
          </button>
          <button
            onClick={() => onOpenVerification?.(supplier.id)}
            className="font-button text-button bg-primary text-on-primary px-5 py-2.5 rounded-lg hover:opacity-90 transition-opacity font-bold shadow-sm flex items-center gap-1.5"
          >
            <span className="material-symbols-outlined text-base">verified</span>
            View Verification Run
          </button>
        </div>
      </section>

      {/* Main Grid: Details + Trust Panel */}
      <section className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
        {/* Left Column: Facts & Claims */}
        <div className="lg:col-span-8 flex flex-col gap-8">
          {/* Quick Facts Card */}
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 p-5 bg-surface-card border border-hairline rounded-xl shadow-sm">
            <div className="flex flex-col gap-1">
              <span className="text-label-uppercase font-label-uppercase text-body-muted">Production Capacity</span>
              <strong className="font-body-md text-body-md text-primary font-bold">{supplier.capacity}</strong>
            </div>
            <div className="flex flex-col gap-1">
              <span className="text-label-uppercase font-label-uppercase text-body-muted">Minimum Order</span>
              <strong className="font-body-md text-body-md text-primary font-bold">{supplier.moq}</strong>
            </div>
            <div className="flex flex-col gap-1">
              <span className="text-label-uppercase font-label-uppercase text-body-muted">Lead Time</span>
              <strong className="font-body-md text-body-md text-primary font-bold">{supplier.leadTime}</strong>
            </div>
            <div className="flex flex-col gap-1">
              <span className="text-label-uppercase font-label-uppercase text-body-muted">Rating Score</span>
              <div className="flex items-center gap-1">
                <span className="material-symbols-outlined text-brand-ochre text-base" data-fill="true">star</span>
                <strong className="font-body-md text-body-md text-primary font-bold">{supplier.rating}</strong>
                <span className="text-xs text-body-muted">({supplier.reviewsCount})</span>
              </div>
            </div>
          </div>

          {/* Description */}
          <div className="p-6 bg-surface-card border border-hairline rounded-xl">
            <h3 className="font-title-md text-title-md text-primary font-bold mb-2">About this Facility</h3>
            <p className="text-body-md text-on-surface-variant leading-relaxed">{supplier.description}</p>
          </div>

          {/* Claim Ledger Section */}
          <section className="flex flex-col gap-4">
            <div className="flex justify-between items-end gap-4 border-b border-hairline pb-3">
              <div>
                <span className="font-label-uppercase text-label-uppercase text-brand-teal font-bold block mb-1">
                  Verifiable Claim Ledger
                </span>
                <h2 className="font-title-lg text-title-lg text-primary font-bold">
                  What this supplier has asserted
                </h2>
              </div>
              <span className="text-xs text-body-muted hidden sm:inline">
                Every claim shows method, date, and evidence
              </span>
            </div>

            <div className="flex flex-col gap-3">
              {supplier.claims.map((claim) => (
                <ClaimRow
                  key={claim.key}
                  claim={claim}
                  onOpenEvidence={(c) => setSelectedClaim(c)}
                />
              ))}
            </div>
          </section>
        </div>

        {/* Right Column: Trust Panel */}
        <div className="lg:col-span-4 flex flex-col gap-6">
          <TrustPanel trust={supplier.trust} />

          <div className="p-5 bg-surface-card border border-hairline rounded-xl flex flex-col gap-3 text-xs text-on-surface-variant">
            <div className="flex items-center gap-2 font-bold text-primary text-sm">
              <span className="material-symbols-outlined text-base text-brand-teal" data-fill="true">verified_user</span>
              <span>The Asymmetry Razor Guarantee</span>
            </div>
            <p>
              Supify guarantees all verified badges represent active, independent evidence checks. Unverified or self-declared claims are never represented as established facts.
            </p>
          </div>
        </div>
      </section>

      {/* Evidence Inspector Modal */}
      {selectedClaim && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm animate-fadeIn">
          <div className="w-full max-w-lg bg-surface-card border-2 border-primary rounded-2xl p-6 shadow-2xl">
            <div className="flex justify-between items-start pb-3 border-b border-hairline mb-4">
              <div>
                <span className="font-label-uppercase text-label-uppercase text-brand-teal bg-brand-mint/25 px-2 py-0.5 rounded font-bold">
                  Evidence Inspector
                </span>
                <h3 className="font-title-lg text-title-lg text-primary font-bold mt-1">
                  {selectedClaim.label}
                </h3>
              </div>
              <button
                onClick={() => setSelectedClaim(null)}
                className="text-primary hover:bg-surface-variant p-1 rounded-lg"
              >
                <span className="material-symbols-outlined">close</span>
              </button>
            </div>

            <div className="flex flex-col gap-4 text-sm mb-6">
              <div>
                <span className="text-xs font-bold text-body-muted block mb-1 uppercase">Asserted Value</span>
                <p className="font-semibold text-primary p-2.5 bg-surface rounded-lg border border-hairline font-mono text-xs">
                  {selectedClaim.value}
                </p>
              </div>

              <div className="grid grid-cols-2 gap-3 text-xs">
                <div className="p-2.5 bg-surface rounded border border-hairline">
                  <span className="text-body-muted block">Verification Method</span>
                  <span className="font-bold text-primary">{selectedClaim.methodLabel}</span>
                </div>
                <div className="p-2.5 bg-surface rounded border border-hairline">
                  <span className="text-body-muted block">Evidence Items</span>
                  <span className="font-bold text-primary">{selectedClaim.evidenceCount} verified file(s)</span>
                </div>
              </div>

              <div className="p-3 bg-brand-mint/20 rounded-lg border border-brand-teal/30 text-xs text-brand-teal flex flex-col gap-1">
                <div className="flex items-center gap-1 font-bold">
                  <span className="material-symbols-outlined text-sm">enhanced_encryption</span>
                  <span>SHA-256 Ledger Record</span>
                </div>
                <span className="font-mono text-[10px] break-all opacity-80">
                  e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855
                </span>
              </div>
            </div>

            <div className="flex justify-end gap-3 pt-3 border-t border-hairline">
              <button
                onClick={() => setSelectedClaim(null)}
                className="font-button text-button px-5 py-2.5 rounded-lg bg-primary text-on-primary hover:opacity-90 font-bold"
              >
                Close Inspector
              </button>
            </div>
          </div>
        </div>
      )}
    </main>
  )
}
