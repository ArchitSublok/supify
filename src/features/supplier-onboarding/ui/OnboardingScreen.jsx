import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'

const steps = [
  { id: 1, label: 'Organisation', detail: 'Your core company legal identity' },
  { id: 2, label: 'Capabilities', detail: 'What buyers can discover & source' },
  { id: 3, label: 'Claims & Evidence', detail: 'Certifications and registry files' },
  { id: 4, label: 'Review & Publish', detail: 'Submit for verification audit' },
]

export function OnboardingScreen({ onShowToast = null, onNavigate = null }) {
  const navigate = useNavigate()
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
      if (onShowToast) onShowToast(`Step ${currentStep} saved! Moving to Step ${currentStep + 1}.`)
    } else {
      if (onShowToast) onShowToast('Supplier profile submitted! Verification queue review ticket created.')
      if (onNavigate) {
        onNavigate('/search')
      } else {
        navigate('/search')
      }
    }
  }

  const handleBack = () => {
    if (currentStep > 1) setCurrentStep(currentStep - 1)
  }

  return (
    <main className="flex-grow w-full max-w-container-max mx-auto px-lg py-8 md:py-12">
      <div className="mb-8">
        <span className="font-label-uppercase text-label-uppercase text-brand-teal font-bold block mb-1">
          Supplier Workspace
        </span>
        <h1 className="font-display-md text-display-md text-primary font-bold">List Your Manufacturing Facility</h1>
        <p className="text-body-md text-on-surface-variant">
          Complete the 4-step onboarding wizard. Your draft is auto-saved locally in real time.
        </p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">
        {/* Left: Stepper Navigation */}
        <div className="lg:col-span-4 flex flex-col gap-3">
          {steps.map((step) => {
            const isCompleted = currentStep > step.id
            const isActive = currentStep === step.id
            return (
              <div
                key={step.id}
                onClick={() => setCurrentStep(step.id)}
                className={`p-4 rounded-xl border transition-all cursor-pointer flex items-start gap-3.5 ${
                  isActive
                    ? 'bg-surface-card border-2 border-primary shadow-sm'
                    : isCompleted
                    ? 'bg-surface border-hairline hover:bg-surface-card'
                    : 'bg-transparent border-hairline opacity-60'
                }`}
              >
                <div
                  className={`w-7 h-7 rounded-full flex items-center justify-center text-xs font-bold flex-shrink-0 ${
                    isCompleted
                      ? 'bg-brand-mint text-brand-teal font-bold'
                      : isActive
                      ? 'bg-primary text-on-primary'
                      : 'bg-surface-strong text-body-muted'
                  }`}
                >
                  {isCompleted ? '✓' : step.id}
                </div>
                <div className="flex flex-col">
                  <span className={`text-sm font-bold ${isActive ? 'text-primary' : 'text-body-muted'}`}>
                    {step.label}
                  </span>
                  <span className="text-xs text-on-surface-variant">{step.detail}</span>
                </div>
              </div>
            )
          })}
        </div>

        {/* Right: Form Step Area */}
        <div className="lg:col-span-8 bg-surface-card border border-hairline rounded-2xl p-6 md:p-8 shadow-sm">
          {currentStep === 1 && (
            <div className="flex flex-col gap-6 animate-fadeIn">
              <div>
                <span className="font-label-uppercase text-label-uppercase text-brand-teal font-bold block mb-1">
                  Step 1 of 4
                </span>
                <h2 className="font-title-lg text-title-lg text-primary font-bold">
                  Organisation &amp; Legal Identity
                </h2>
                <p className="text-body-sm text-on-surface-variant">
                  Provide your registered corporate entity details.
                </p>
              </div>

              <div className="flex flex-col gap-4">
                <div>
                  <label className="block text-xs font-bold text-primary mb-1">
                    Trade / Display Name *
                  </label>
                  <input
                    type="text"
                    value={formData.tradeName}
                    onChange={(e) => handleChange('tradeName', e.target.value)}
                    className="w-full px-4 py-3 bg-background border border-hairline rounded-lg focus:outline-none focus:border-primary focus:ring-1 focus:ring-primary text-primary text-sm font-medium"
                    placeholder="e.g. Acme Precision Machining"
                  />
                </div>

                <div>
                  <label className="block text-xs font-bold text-primary mb-1">
                    Full Legal Name (as per registration certificate) *
                  </label>
                  <input
                    type="text"
                    value={formData.legalName}
                    onChange={(e) => handleChange('legalName', e.target.value)}
                    className="w-full px-4 py-3 bg-background border border-hairline rounded-lg focus:outline-none focus:border-primary focus:ring-1 focus:ring-primary text-primary text-sm font-medium"
                    placeholder="e.g. Acme Precision Machining Private Limited"
                  />
                </div>

                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div>
                    <label className="block text-xs font-bold text-primary mb-1">
                      Operating City &amp; Region *
                    </label>
                    <input
                      type="text"
                      value={formData.location}
                      onChange={(e) => handleChange('location', e.target.value)}
                      className="w-full px-4 py-3 bg-background border border-hairline rounded-lg focus:outline-none focus:border-primary focus:ring-1 focus:ring-primary text-primary text-sm font-medium"
                      placeholder="e.g. Pune, Maharashtra, IN"
                    />
                  </div>
                  <div>
                    <label className="block text-xs font-bold text-primary mb-1">
                      Corporate Website
                    </label>
                    <input
                      type="url"
                      value={formData.website}
                      onChange={(e) => handleChange('website', e.target.value)}
                      className="w-full px-4 py-3 bg-background border border-hairline rounded-lg focus:outline-none focus:border-primary focus:ring-1 focus:ring-primary text-primary text-sm font-medium"
                      placeholder="https://company.com"
                    />
                  </div>
                </div>
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
                  Specify your primary categories, capacities, and production terms.
                </p>
              </div>

              <div className="flex flex-col gap-4">
                <div>
                  <label className="block text-xs font-bold text-primary mb-1">
                    Primary Sourcing Category *
                  </label>
                  <select
                    value={formData.category}
                    onChange={(e) => handleChange('category', e.target.value)}
                    className="w-full px-4 py-3 bg-background border border-hairline rounded-lg focus:outline-none focus:border-primary focus:ring-1 focus:ring-primary text-primary text-sm font-medium cursor-pointer"
                  >
                    <option value="Sheet-metal fabrication">Sheet-metal Fabrication</option>
                    <option value="Industrial fasteners">Industrial Fasteners</option>
                    <option value="Precision machined parts">Precision Machined Parts</option>
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
