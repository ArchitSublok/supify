export function LandingScreen({ onNavigate, onOpenAuth, onShowToast }) {
  const warehouseIllustration = 'https://lh3.googleusercontent.com/aida-public/AB6AXuDbm4vH0wBaP6ao5ChikQYoqGwTZiws_fA9GU44Q-ct1ePV_G07-JHfjfdYeyULGDvne2LKXEyeYEZZfKIT4EItm3iTXAf8vusURi67EwF0P-nX8QFRCDF5Mpey1EjMhsRPxaHWupI562Jy9qfFjp7RsJq24tn9hSHvBYOMYsr8c2xr9z6-m4TihrtYnRCp-fuB9942pdGDRFWbJ0IblVGenXxQxRt1oz8pdVbZXhyllfxh4I71pTg7'
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
            <div className="inline-flex items-center gap-2 bg-surface-card border border-hairline px-3.5 py-1.5 rounded-full w-fit">
              <span className="w-2 h-2 rounded-full bg-brand-coral animate-ping"></span>
              <span className="font-label-uppercase text-label-uppercase text-primary">
                Next-Gen Supplier Discovery &amp; Verification
              </span>
            </div>

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

          {/* Hero 3D Illustration artifact */}
          <div className="lg:col-span-5 flex justify-center lg:justify-end relative">
            <div className="absolute inset-0 bg-brand-peach opacity-20 rounded-full blur-3xl -z-10 transform translate-x-10 translate-y-10"></div>
            <img
              alt="3D Warehouse Illustration"
              className="w-full max-w-md object-contain animate-float drop-shadow-2xl"
              src={warehouseIllustration}
            />
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
