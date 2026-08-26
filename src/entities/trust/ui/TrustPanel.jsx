import { TrustBand } from './TrustBand'

function formatValidity(value) {
  if (!value) return 'Pending full audit'
  const isExpired = new Date(value).getTime() < Date.now()
  const formatter = new Intl.DateTimeFormat('en-US', { day: 'numeric', month: 'short', year: 'numeric' })
  return (isExpired ? 'Expired ' : 'Valid until ') + formatter.format(new Date(value))
}

export function TrustPanel({ trust }) {
  if (!trust) return null

  return (
    <div className="bg-brand-teal text-on-primary rounded-xl p-lg flex flex-col gap-md shadow-md border-2 border-primary">
      <div className="flex justify-between items-start border-b border-white/15 pb-md">
        <div>
          <span className="font-label-uppercase text-label-uppercase text-brand-mint tracking-wider block mb-1">
            Trust Standing
          </span>
          <TrustBand trust={trust} />
        </div>
        <div className="text-right">
          <span className="text-xs text-brand-mint block">Validity</span>
          <span className={`text-xs font-semibold ${trust.validUntil && new Date(trust.validUntil).getTime() < Date.now() ? 'text-brand-peach' : 'text-white'}`}>
            {formatValidity(trust.validUntil)}
          </span>
        </div>
      </div>

      <div>
        <div className="flex justify-between items-center mb-2">
          <span className="text-xs font-semibold text-brand-mint uppercase tracking-wider">
            Verification Pillars
          </span>
          {trust.overallScore ? (
            <span className="text-xs font-bold bg-white/10 px-2 py-0.5 rounded text-white">
              {trust.overallScore}/100 Score
            </span>
          ) : null}
        </div>

        <div className="flex flex-col gap-3">
          {(trust.pillars || []).map((pillar) => (
            <div key={pillar.name} className="flex flex-col gap-1">
              <div className="flex justify-between text-xs">
                <span className="text-white/90 font-medium">{pillar.name}</span>
                <span className="text-brand-mint font-semibold">{pillar.score}%</span>
              </div>
              <div className="w-full bg-white/20 rounded-full h-1.5 overflow-hidden">
                <div
                  className="bg-brand-mint h-1.5 rounded-full transition-all duration-500"
                  style={{ width: `${pillar.score}%` }}
                />
              </div>
            </div>
          ))}
        </div>
      </div>

      {trust.gatesApplied && trust.gatesApplied.length > 0 && (
        <div className="bg-brand-peach text-primary p-3 rounded-lg flex items-start gap-2 border border-primary/20">
          <span className="material-symbols-outlined text-base text-primary mt-0.5">warning</span>
          <div className="flex flex-col gap-0.5 text-xs">
            <strong className="font-semibold">Trust Gate Applied</strong>
            <span>{trust.gatesApplied.join(' · ')}</span>
          </div>
        </div>
      )}
    </div>
  )
}
