import { useState, useEffect } from 'react'

const steps = [
  { id: 1, label: 'Organisation', detail: 'Your core company legal identity' },
  { id: 2, label: 'Capabilities', detail: 'What buyers can discover & source' },
  { id: 3, label: 'Claims & Evidence', detail: 'Certifications and registry files' },
  { id: 4, label: 'Review & Publish', detail: 'Submit for verification audit' },
]

export function OnboardingScreen({ onShowToast, onNavigate }) {
  const [currentStep, setCurrentStep] = useState(1)
  const [formData, setFormData] = useState(() => {
    try {
      const saved = localStorage.getItem('supify_supplier_onboarding_draft')
      if (saved) return JSON.parse(saved)
    } catch (e) {}
    return {
      legalName: 'Narmada Advanced Custom Fabrication Private Limited',
      tradeName: 'Narmada Fabrication',
      location: 'Indore, Madhya Pradesh, India',
      category: 'Sheet-metal fabrication',
      website: 'https://narmadafabrication.example.com',
      capacity: '50,000 units / month',
      moq: '100 units',
      leadTime: '21-28 Days',
      gstNumber: '23AAECA9082G1ZP',
      isoCertUploaded: true,
    }
  })

  const [savedStatus, setSavedStatus] = useState('Draft saved locally')

  useEffect(() => {
    try {
      localStorage.setItem('supify_supplier_onboarding_draft', JSON.stringify(formData))
      setSavedStatus('Draft saved locally just now')
    } catch (e) {}
  }, [formData])

  const handleChange = (field, val) => {
    setFormData((prev) => ({ ...prev, [field]: val }))
  }

  const handleNext = () => {
    if (currentStep < 4) {
      setCurrentStep(currentStep + 1)
      onShowToast?.(`Step ${currentStep} saved! Moving to Step ${currentStep + 1}.`)
    } else {
      onShowToast?.('Supplier profile submitted! Verification queue review ticket created.')
      onNavigate?.('/search')
    }
  }

  const handleBack = () => {
    if (currentStep > 1) setCurrentStep(currentStep - 1)
  }

  return (
    <main className="flex-grow w-full max-w-container-max mx-auto px-lg py-8 md:py-12">
      {/* Intro Header */}
      <section className="mb-8">
        <span className="font-label-uppercase text-label-uppercase text-brand-teal bg-brand-mint/25 px-2.5 py-1 rounded font-bold border border-brand-teal/20">
          Supplier Console · Onboarding
        </span>
        <h1 className="font-display-md text-display-md text-primary font-bold mt-2">
          Build a profile buyers can understand.
        </h1>
        <p className="text-on-surface-variant font-body-md mt-1">
          Your listing can be published before every claim is verified. We show what is established honestly.
        </p>
      </section>

      {/* Onboarding Wizard Shell */}
      <div className="grid grid-cols-1 lg:grid-cols-12 border-2 border-primary rounded-2xl overflow-hidden bg-surface-card shadow-lg">
        {/* Step Navigation Sidebar */}
        <aside className="lg:col-span-4 p-6 bg-surface-strong/60 border-b lg:border-b-0 lg:border-r border-hairline flex flex-col gap-2">
          <span className="text-xs font-bold text-body-muted uppercase tracking-wider mb-2">
            Onboarding Progress
          </span>
          {steps.map((step) => {
            const isActive = currentStep === step.id
            const isCompleted = currentStep > step.id

            return (
              <button
                key={step.id}
                onClick={() => setCurrentStep(step.id)}
                className={`flex items-start gap-3 p-3.5 rounded-xl text-left transition-all ${
                  isActive
                    ? 'bg-primary text-on-primary font-bold shadow'
                    : isCompleted
                    ? 'bg-brand-mint/20 text-brand-teal hover:bg-brand-mint/30'
                    : 'text-body-muted hover:bg-surface-variant'
                }`}
              >
                <div
                  className={`w-7 h-7 rounded-full flex items-center justify-center font-bold text-xs flex-shrink-0 mt-0.5 ${
                    isActive
                      ? 'bg-white text-primary'
                      : isCompleted
                      ? 'bg-brand-teal text-white'
                      : 'bg-surface border border-hairline text-body-muted'
                  }`}
                >
                  {isCompleted ? '✓' : step.id}
                </div>
                <div>
                  <strong className="block text-sm leading-tight">{step.label}</strong>
                  <span className={`text-xs ${isActive ? 'text-white/80' : 'text-body-muted'}`}>
                    {step.detail}
                  </span>
                </div>
              </button>
            )
          })}

          <div className="mt-auto pt-6 border-t border-hairline text-xs text-body-muted">
            <div className="flex items-center gap-1.5 text-primary font-semibold mb-1">
              <span className="material-symbols-outlined text-sm text-brand-teal">lock</span>
              <span>Encrypted Draft Storage</span>
            </div>
            <span>No data lost on disconnect or page reload.</span>
          </div>
        </aside>

        {/* Step Form Body */}
        <div className="lg:col-span-8 p-6 md:p-10 flex flex-col">
          {currentStep === 1 && (
            <div className="flex flex-col gap-6 animate-fadeIn">
              <div>
                <span className="font-label-uppercase text-label-uppercase text-brand-teal font-bold block mb-1">
                  Step 1 of 4
                </span>
                <h2 className="font-title-lg text-title-lg text-primary font-bold">
                  Tell buyers who you are
                </h2>
                <p className="text-body-sm text-on-surface-variant">
                  These details establish the traceable foundation of your public supplier profile.
                </p>
              </div>

              <div className="flex flex-col gap-4">
                <div>
                  <label className="block text-xs font-bold text-primary mb-1">
                    Legal Organisation Name *
                  </label>
                  <input
                    type="text"
                    value={formData.legalName}
                    onChange={(e) => handleChange('legalName', e.target.value)}
                    className="w-full px-4 py-3 bg-background border border-hairline rounded-lg focus:outline-none focus:border-primary focus:ring-1 focus:ring-primary text-primary text-sm font-medium"
                    placeholder="Registered legal corporate entity name"
                  />
                </div>

                <div>
                  <label className="block text-xs font-bold text-primary mb-1">
                    Trading / Brand Name *
                  </label>
                  <input
                    type="text"
                    value={formData.tradeName}
                    onChange={(e) => handleChange('tradeName', e.target.value)}
                    className="w-full px-4 py-3 bg-background border border-hairline rounded-lg focus:outline-none focus:border-primary focus:ring-1 focus:ring-primary text-primary text-sm font-medium"
                    placeholder="Brand name displayed to buyers"
                  />
                </div>

                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div>
                    <label className="block text-xs font-bold text-primary mb-1">
                      Primary Operating Facility Location *
                    </label>
                    <input
                      type="text"
                      value={formData.location}
                      onChange={(e) => handleChange('location', e.target.value)}
                      className="w-full px-4 py-3 bg-background border border-hairline rounded-lg focus:outline-none focus:border-primary focus:ring-1 focus:ring-primary text-primary text-sm font-medium"
                      placeholder="City, State, Country"
                    />
                  </div>
                  <div>
                    <label className="block text-xs font-bold text-primary mb-1">
                      Official Company Website
                    </label>
                    <input
                      type="text"
                      value={formData.website}
                      onChange={(e) => handleChange('website', e.target.value)}
                      className="w-full px-4 py-3 bg-background border border-hairline rounded-lg focus:outline-none focus:border-primary focus:ring-1 focus:ring-primary text-primary text-sm font-medium"
                      placeholder="https://yourcompany.com"
                    />
                  </div>
                </div>
              </div>

              <div className="p-4 bg-brand-peach/25 border-l-4 border-brand-ochre rounded-r-lg text-xs text-primary flex flex-col gap-1">
                <strong className="font-bold">Why we ask for legal name</strong>
                <span>
                  Buyers need a traceable legal identity before they can evaluate any capability claim. You can pause and return anytime.
                </span>
              </div>
            </div>
          )}

          {currentStep === 2 && (
            <div className="flex flex-col gap-6 animate-fadeIn">
              <div>
                <span className="font-label-uppercase text-label-uppercase text-brand-teal font-bold block mb-1">
                  Step 2 of 4
                </span>
                <h2 className="font-title-lg text-title-lg text-primary font-bold">
                  Manufacturing Capabilities
                </h2>
                <p className="text-body-sm text-on-surface-variant">
                  Specify manufacturing output, minimum order limits, and typical turnarounds.
                </p>
              </div>

              <div className="flex flex-col gap-4">
                <div>
                  <label className="block text-xs font-bold text-primary mb-1">
                    Primary Industry / Category *
                  </label>
                  <select
                    value={formData.category}
                    onChange={(e) => handleChange('category', e.target.value)}
                    className="w-full px-4 py-3 bg-background border border-hairline rounded-lg focus:outline-none focus:border-primary focus:ring-1 focus:ring-primary text-primary text-sm font-medium cursor-pointer"
                  >
                    <option value="Sheet-metal fabrication">Sheet-metal fabrication</option>
                    <option value="Industrial fasteners">Industrial fasteners</option>
                    <option value="Precision machined parts">Precision machined parts</option>
                    <option value="Textiles">Textiles</option>
                    <option value="Packaging">Packaging</option>
                    <option value="Electronics">Electronics</option>
                  </select>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div>
                    <label className="block text-xs font-bold text-primary mb-1">
                      Monthly Production Capacity *
                    </label>
                    <input
                      type="text"
                      value={formData.capacity}
                      onChange={(e) => handleChange('capacity', e.target.value)}
                      className="w-full px-4 py-3 bg-background border border-hairline rounded-lg focus:outline-none focus:border-primary focus:ring-1 focus:ring-primary text-primary text-sm font-medium"
                      placeholder="e.g. 50,000 units / month"
                    />
                  </div>
                  <div>
                    <label className="block text-xs font-bold text-primary mb-1">
                      Minimum Order Quantity (MOQ) *
                    </label>
                    <input
                      type="text"
                      value={formData.moq}
                      onChange={(e) => handleChange('moq', e.target.value)}
                      className="w-full px-4 py-3 bg-background border border-hairline rounded-lg focus:outline-none focus:border-primary focus:ring-1 focus:ring-primary text-primary text-sm font-medium"
                      placeholder="e.g. 100 units"
                    />
                  </div>
                </div>

                <div>
                  <label className="block text-xs font-bold text-primary mb-1">
                    Typical Lead Time *
                  </label>
                  <input
                    type="text"
                    value={formData.leadTime}
                    onChange={(e) => handleChange('leadTime', e.target.value)}
                    className="w-full px-4 py-3 bg-background border border-hairline rounded-lg focus:outline-none focus:border-primary focus:ring-1 focus:ring-primary text-primary text-sm font-medium"
                    placeholder="e.g. 14-21 Days"
                  />
                </div>
              </div>
            </div>
          )}

          {currentStep === 3 && (
            <div className="flex flex-col gap-6 animate-fadeIn">
              <div>
                <span className="font-label-uppercase text-label-uppercase text-brand-teal font-bold block mb-1">
                  Step 3 of 4
                </span>
                <h2 className="font-title-lg text-title-lg text-primary font-bold">
                  Claims &amp; Verifiable Evidence
                </h2>
                <p className="text-body-sm text-on-surface-variant">
                  Upload identifiers and documentation for verifier attestation.
                </p>
              </div>

              <div className="flex flex-col gap-4">
                <div>
                  <label className="block text-xs font-bold text-primary mb-1">
                    Tax / Registry Identifier (e.g. GSTIN, VAT, D-U-N-S)
                  </label>
                  <input
                    type="text"
                    value={formData.gstNumber}
                    onChange={(e) => handleChange('gstNumber', e.target.value)}
                    className="w-full px-4 py-3 bg-background border border-hairline rounded-lg focus:outline-none focus:border-primary focus:ring-1 focus:ring-primary text-primary text-sm font-medium font-mono"
                    placeholder="23AAECA9082G1ZP"
                  />
                </div>

                <div className="p-4 bg-surface rounded-xl border-2 border-dashed border-hairline flex flex-col items-center justify-center text-center gap-2">
                  <span className="material-symbols-outlined text-3xl text-brand-teal">upload_file</span>
                  <span className="text-xs font-bold text-primary">Upload ISO / Quality Certification Documents</span>
                  <span className="text-[11px] text-body-muted">PDF, PNG, JPG up to 25MB (SHA-256 hashed on ingest)</span>
                  <button
                    type="button"
                    onClick={() => onShowToast?.('Sample ISO certificate attached to draft!')}
                    className="font-button text-button text-xs bg-surface-card border border-hairline px-3 py-1.5 rounded-lg text-primary hover:bg-surface-strong mt-1 font-semibold"
                  >
                    Select Files
                  </button>
                </div>
              </div>
            </div>
          )}

          {currentStep === 4 && (
            <div className="flex flex-col gap-6 animate-fadeIn">
              <div>
                <span className="font-label-uppercase text-label-uppercase text-brand-teal font-bold block mb-1">
                  Step 4 of 4
                </span>
                <h2 className="font-title-lg text-title-lg text-primary font-bold">
                  Review &amp; Publish
                </h2>
                <p className="text-body-sm text-on-surface-variant">
                  Review your listing before submitting to the verifier review queue.
                </p>
              </div>

              <div className="p-5 bg-surface rounded-xl border border-hairline flex flex-col gap-3 text-xs">
                <div className="flex justify-between border-b border-hairline pb-2">
                  <span className="text-body-muted">Trade Name:</span>
                  <strong className="text-primary">{formData.tradeName}</strong>
                </div>
                <div className="flex justify-between border-b border-hairline pb-2">
                  <span className="text-body-muted">Legal Name:</span>
                  <strong className="text-primary">{formData.legalName}</strong>
                </div>
                <div className="flex justify-between border-b border-hairline pb-2">
                  <span className="text-body-muted">Operating Facility:</span>
                  <strong className="text-primary">{formData.location}</strong>
                </div>
                <div className="flex justify-between border-b border-hairline pb-2">
                  <span className="text-body-muted">Capacity &amp; MOQ:</span>
                  <strong className="text-primary">{formData.capacity} (MOQ: {formData.moq})</strong>
                </div>
                <div className="flex justify-between">
                  <span className="text-body-muted">Registry ID:</span>
                  <strong className="text-primary font-mono">{formData.gstNumber}</strong>
                </div>
              </div>
            </div>
          )}

          {/* Form Actions Footer */}
          <div className="mt-8 pt-6 border-t border-hairline flex items-center justify-between gap-4">
            <span className="text-xs text-brand-teal font-medium flex items-center gap-1">
              <span className="w-2 h-2 rounded-full bg-brand-teal"></span>
              {savedStatus}
            </span>

            <div className="flex gap-3">
              {currentStep > 1 && (
                <button
                  type="button"
                  onClick={handleBack}
                  className="font-button text-button px-5 py-2.5 rounded-lg border border-hairline text-primary hover:bg-surface-variant font-semibold"
                >
                  ← Back
                </button>
              )}
              <button
                type="button"
                onClick={handleNext}
                className="font-button text-button px-6 py-2.5 rounded-lg bg-primary text-on-primary hover:opacity-90 font-bold shadow-md flex items-center gap-1"
              >
                {currentStep === 4 ? 'Publish Listing' : 'Save and Continue →'}
              </button>
            </div>
          </div>
        </div>
      </div>
    </main>
  )
}
