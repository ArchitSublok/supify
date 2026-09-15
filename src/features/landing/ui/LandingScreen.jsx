// Illustrative hero artifact — a bento summary of the trust model. The method and
// pillar counts are structural facts about the product; the band, claim row and
// expiry are illustrative sample values.
const HERO_ALT =
  'Illustration of the Supify trust model. A supplier at trust band Verified, with the identity ' +
  'gate passed. Five evidence rungs, from self-declared to site-verified. Seven scored pillars. ' +
  'A sample claim: GST registration, confirmed by registry check four days ago. A factory audit ' +
  'expiring in twelve days, showing that trust decays over time.'

export function LandingScreen({ onNavigate, onOpenAuth, onShowToast }) {
  const handleVerificationClick = () => {
    const currentUserRole = localStorage.getItem('currentUserRole')
    if (!currentUserRole) {
      onShowToast?.('Please log in first to access the Verification Engine.', 'error')
      onOpenAuth?.('login')
      return
    }
    onNavigate('/verification')
  }
  return (
    <main className="flex-grow w-full">
      {/* Hero Section */}
      <section className="max-w-container-max mx-auto px-lg py-12 md:py-section">
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-xl items-center">
          <div className="lg:col-span-7 flex flex-col gap-lg">
            <h1 className="font-display-xl-mobile text-display-xl-mobile md:font-display-xl md:text-display-xl text-primary max-w-[800px] leading-tight">
              Source with{' '}
              <span className="text-brand-coral relative inline-block">
                confidence
                <svg
                  className="absolute w-full h-full -z-10 scale-125 opacity-30 bottom-0 left-0"
                  preserveAspectRatio="none"
                  viewBox="0 0 100 100"
                >
                  <path d="M0,50 Q25,10 50,50 T100,50" fill="none" stroke="currentColor" strokeWidth="8"></path>
                </svg>
              </span>
              .
            </h1>

            <p className="font-title-lg text-title-lg text-on-surface-variant max-w-2xl">
              The B2B platform that turns chaotic supplier discovery into a verified, smooth, and predictable process.
              Build your supply chain with clarity and zero blind trust.
            </p>

            <div className="flex flex-wrap gap-md mt-2">
              <button
                onClick={() => onNavigate('/search')}
                className="font-button text-button bg-primary text-on-primary px-xl py-md rounded-xl hover:opacity-90 transition-all inline-flex items-center justify-center h-12 gap-2 shadow-md hover:-translate-y-0.5 font-bold"
              >
                <span>Start Sourcing Free</span>
                <span className="material-symbols-outlined text-sm">arrow_forward</span>
              </button>
              <button
                onClick={handleVerificationClick}
                className="font-button text-button bg-surface-card text-primary border border-hairline px-xl py-md rounded-xl hover:bg-surface-variant transition-colors inline-flex items-center justify-center h-12 font-semibold"
              >
                Verification Engine
              </button>
            </div>

            {/* Micro proof points */}
            <div className="flex items-center gap-6 pt-4 text-xs text-body-muted font-medium flex-wrap">
              <div className="flex items-center gap-1.5">
                <span className="material-symbols-outlined text-brand-teal text-base" data-fill="true">check_circle</span>
                <span>Time-bounded attestations</span>
              </div>
              <div className="flex items-center gap-1.5">
                <span className="material-symbols-outlined text-brand-teal text-base" data-fill="true">check_circle</span>
                <span>Third-party API checks</span>
              </div>
              <div className="flex items-center gap-1.5">
                <span className="material-symbols-outlined text-brand-teal text-base" data-fill="true">check_circle</span>
                <span>Inspectable evidence</span>
              </div>
            </div>
          </div>

          {/* Hero artifact — bento summary of the trust model */}
          <div className="lg:col-span-5 flex justify-center lg:justify-end relative">
            <div className="absolute inset-0 bg-brand-peach opacity-20 rounded-full blur-3xl -z-10 transform translate-x-10 translate-y-10"></div>

            <div
              role="img"
              aria-label={HERO_ALT}
              className="w-full max-w-md grid grid-cols-3 gap-2.5 animate-float"
            >
              {/* Trust band */}
              <div className="col-span-2 bg-brand-teal border-2 border-primary rounded-xl p-4 shadow-[4px_4px_0px_0px_rgba(0,0,0,1)] flex flex-col justify-between min-h-[118px]">
                <span className="font-label-uppercase text-label-uppercase text-[9px] font-bold text-on-primary/70">
                  Trust band
                </span>
                <div>
                  <div className="flex items-center gap-1.5 mb-0.5">
                    <span className="material-symbols-outlined text-on-primary text-xl" data-fill="true">verified</span>
                    <span className="font-display-sm text-display-sm text-on-primary font-bold text-2xl">Verified</span>
                  </div>
                  <div className="font-body-sm text-body-sm text-[10px] text-on-primary/80">
                    Identity gate passed
                  </div>
                </div>
              </div>

              {/* Evidence rungs */}
              <div className="bg-brand-coral border-2 border-primary rounded-xl p-3 shadow-[4px_4px_0px_0px_rgba(0,0,0,1)] flex flex-col justify-between">
                <span className="font-label-uppercase text-label-uppercase text-[9px] font-bold text-primary/70">
                  Methods
                </span>
                <div>
                  <div className="font-display-sm text-display-sm text-primary font-bold text-3xl leading-none">5</div>
                  <div className="font-body-sm text-body-sm text-[9px] text-primary/80 mt-0.5">evidence rungs</div>
                </div>
              </div>

              {/* Pillars */}
              <div className="bg-brand-lavender border-2 border-primary rounded-xl p-3 shadow-[4px_4px_0px_0px_rgba(0,0,0,1)] flex flex-col justify-between">
                <span className="font-label-uppercase text-label-uppercase text-[9px] font-bold text-primary/70">
                  Pillars
                </span>
                <div>
                  <div className="font-display-sm text-display-sm text-primary font-bold text-3xl leading-none">7</div>
                  <div className="font-body-sm text-body-sm text-[9px] text-primary/70 mt-0.5">scored areas</div>
                </div>
              </div>

              {/* Sample claim — method and recency, per A2 */}
              <div className="col-span-2 bg-surface-bright border-2 border-primary rounded-xl p-3 shadow-[4px_4px_0px_0px_rgba(0,0,0,1)] flex items-center">
                <div className="flex items-center gap-2 w-full">
                  <span className="material-symbols-outlined text-brand-teal text-base shrink-0" data-fill="true">check_circle</span>
                  <div className="min-w-0">
                    <div className="font-button text-button text-[10px] font-bold text-primary truncate">GST Registration</div>
                    <div className="font-body-sm text-body-sm text-[9px] text-body-muted">Registry check · 4 days ago</div>
                  </div>
                </div>
              </div>

              {/* Decay — per A3 */}
              <div className="col-span-3 bg-brand-peach border-2 border-primary rounded-xl p-3.5 shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]">
                <div className="flex items-center justify-between gap-2 mb-2">
                  <div className="flex items-center gap-1.5">
                    <span className="material-symbols-outlined text-primary text-sm" data-fill="true">schedule</span>
                    <span className="font-button text-button text-[10px] font-bold text-primary">
                      Trust decays over time
                    </span>
                  </div>
                  <span className="font-body-sm text-body-sm text-[9px] text-primary/70">Audit · 12 days left</span>
                </div>
                <div className="w-full bg-primary/15 rounded-full h-1.5 overflow-hidden" aria-hidden="true">
                  <div className="bg-brand-coral h-1.5 rounded-full w-[22%]"></div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Saturated Feature Cards Grid */}
      <section className="max-w-container-max mx-auto px-lg py-12 md:py-section">
        <div className="text-center mb-xl">
          <h2 className="font-display-lg text-display-lg text-primary mb-md">Built for modern teams.</h2>
          <p className="font-title-md text-title-md text-on-surface-variant max-w-2xl mx-auto">
            Everything you need to discover, verify, and manage suppliers in one joyful workspace.
          </p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-lg">
          {/* Pink Card: Smart Discovery */}
          <div
            onClick={() => onNavigate('/search')}
            className="bg-brand-pink text-on-primary rounded-xl p-xl flex flex-col gap-lg shadow-[4px_4px_0px_0px_rgba(0,0,0,1)] hover:-translate-y-1 hover:shadow-[6px_6px_0px_0px_rgba(0,0,0,1)] transition-all duration-200 border-2 border-primary group cursor-pointer h-full"
          >
            <div className="flex items-center justify-between">
              <h3 className="font-title-lg text-title-lg font-bold">Smart Discovery</h3>
              <span className="material-symbols-outlined text-4xl" data-fill="true">search</span>
            </div>
            <p className="font-body-md text-body-md opacity-95 flex-grow">
              Instantly match with top-tier suppliers using our multi-attribute search. Cut down weeks of research into minutes.
            </p>

            {/* UI Fragment Mockup */}
            <div className="bg-on-primary text-primary p-md rounded-lg mt-md shadow-sm border border-primary/20">
              <div className="flex items-center gap-3 mb-2">
                <div className="w-8 h-8 rounded-full bg-brand-coral/20 flex items-center justify-center">
                  <span className="material-symbols-outlined text-sm text-brand-coral" data-fill="true">factory</span>
                </div>
                <div>
                  <div className="font-button text-button text-xs font-bold">Acme Corp Global</div>
                  <div className="font-body-sm text-body-sm text-body-muted text-[10px]">98% Match Score · Aerospace</div>
                </div>
              </div>
              <div className="w-full bg-surface-variant rounded-full h-1.5 mt-2 overflow-hidden">
                <div className="bg-brand-pink h-1.5 rounded-full w-[98%]"></div>
              </div>
            </div>
          </div>

          {/* Teal Card: Verification */}
          <div
              onClick={handleVerificationClick}
            className="bg-brand-teal text-on-primary rounded-xl p-xl flex flex-col gap-lg shadow-[4px_4px_0px_0px_rgba(0,0,0,1)] hover:-translate-y-1 hover:shadow-[6px_6px_0px_0px_rgba(0,0,0,1)] transition-all duration-200 border-2 border-primary group cursor-pointer h-full"
          >
            <div className="flex items-center justify-between">
              <h3 className="font-title-lg text-title-lg font-bold">Verification</h3>
              <span className="material-symbols-outlined text-4xl" data-fill="true">verified</span>
            </div>
            <p className="font-body-md text-body-md opacity-95 flex-grow">
              Source with confidence. Every supplier undergoes rigorous, multi-point background checks and registry syncs.
            </p>

            {/* UI Fragment Mockup */}
            <div className="bg-on-primary text-primary p-md rounded-lg mt-md shadow-sm border border-primary/20 flex flex-col gap-2">
              <div className="flex items-center gap-2">
                <span className="material-symbols-outlined text-brand-teal text-base" data-fill="true">check_circle</span>
                <span className="font-body-sm text-body-sm font-medium">Financial Health (D&amp;B Live Sync)</span>
              </div>
              <div className="flex items-center gap-2">
                <span className="material-symbols-outlined text-brand-teal text-base" data-fill="true">check_circle</span>
                <span className="font-body-sm text-body-sm font-medium">Facility &amp; Quality Audits</span>
              </div>
            </div>
          </div>

          {/* Lavender Card: Compliance */}
          <div
            onClick={() => onNavigate('/verification')}
            className="bg-brand-lavender text-primary rounded-xl p-xl flex flex-col gap-lg shadow-[4px_4px_0px_0px_rgba(0,0,0,1)] hover:-translate-y-1 hover:shadow-[6px_6px_0px_0px_rgba(0,0,0,1)] transition-all duration-200 border-2 border-primary group cursor-pointer h-full lg:col-span-1 md:col-span-2"
          >
            <div className="flex items-center justify-between">
              <h3 className="font-title-lg text-title-lg font-bold">Compliance</h3>
              <span className="material-symbols-outlined text-4xl" data-fill="true">gavel</span>
            </div>
            <p className="font-body-md text-body-md opacity-90 flex-grow">
              Stay ahead of regulations. Automated document collection and real-time alerts keep your supply chain compliant.
            </p>

            {/* UI Fragment Mockup */}
            <div className="bg-surface-bright text-primary p-md rounded-lg mt-md shadow-sm border border-primary/20">
              <div className="flex justify-between items-center mb-2 pb-2 border-b border-hairline">
                <span className="font-button text-button text-xs font-bold">ISO 9001 Cert</span>
                <span className="font-label-uppercase text-label-uppercase text-brand-teal text-[10px] bg-brand-teal/10 px-2 py-0.5 rounded font-bold">
                  Valid
                </span>
              </div>
              <div className="flex justify-between items-center">
                <span className="font-button text-button text-xs font-bold">ESG EcoVadis</span>
                <span className="font-label-uppercase text-label-uppercase text-brand-ochre text-[10px] bg-brand-ochre/25 px-2 py-0.5 rounded font-bold">
                  Gold Medal
                </span>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Pre-Footer Illustrated CTA Band */}
      <section className="max-w-container-max mx-auto px-lg py-12">
        <div className="bg-surface-card border-2 border-primary rounded-2xl p-8 md:p-14 shadow-lg flex flex-col md:flex-row items-center justify-between gap-8 relative overflow-hidden">
          <div className="max-w-xl z-10">
            <span className="font-label-uppercase text-label-uppercase text-brand-teal font-bold block mb-2">
              Ready to upgrade your supply chain?
            </span>
            <h2 className="font-display-md text-display-md text-primary mb-3">
              Turn your growth ideas into reality today.
            </h2>
            <p className="text-on-surface-variant text-body-md mb-6">
              Join hundreds of procurement leaders who discover, verify, and source from qualified suppliers with full transparency.
            </p>
            <div className="flex flex-wrap gap-4">
              <button
                onClick={() => onNavigate('/search')}
                className="font-button text-button bg-primary text-on-primary px-6 py-3 rounded-xl hover:opacity-90 transition-opacity font-bold shadow"
              >
                Explore Suppliers Now
              </button>
              <button
                   onClick={handleVerificationClick}
                className="font-button text-button bg-surface border border-hairline text-primary px-6 py-3 rounded-xl hover:bg-surface-variant transition-colors font-semibold"
              >
                View Verification Engine
              </button>
            </div>
          </div>

          <div className="z-10 flex flex-col items-center justify-center p-6 bg-brand-peach/30 rounded-xl border border-primary/20 text-center min-w-[240px]">
            <span className="material-symbols-outlined text-4xl text-brand-coral mb-2" data-fill="true">shield</span>
            <div className="font-display-sm text-display-sm font-bold text-primary">100%</div>
            <div className="text-xs text-primary font-semibold">Evidence-Backed Ledger</div>
          </div>
        </div>
      </section>
    </main>
  )
}
