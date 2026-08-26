import { useEffect, useState } from 'react'
import { supplierRepository } from '../../../shared/api/supplierRepository'
import { TrustBand } from '../../../entities/trust/ui/TrustBand'

export function VerificationDashboard({ onOpenSupplier, onShowToast }) {
  const [activeStep, setActiveStep] = useState(2) // 0: Identity, 1: Watchlists, 2: Financial, 3: ESG
  const [tasks, setTasks] = useState([])
  const [activeTab, setActiveTab] = useState('run') // 'run' or 'queue'
  const [selectedTask, setSelectedTask] = useState(null)
  const [approvedState, setApprovedState] = useState(false)

  const robotMascotImage = 'https://lh3.googleusercontent.com/aida-public/AB6AXuAwzoOzYIQMDjWdJxGUBxuaoKW4vvGta2vTTqt7Z1U2cCYCEIl4mI0nXysZKnvlhATL3OlFL8dHcncrCt3MznFKjG8zwBQy9tLr4jf37D7AdRrl-8IAybYftYSETCzHB0l56zag0XyQ8Okvw9ZPkbJ7YxWVohMhU0vDfxNwaw9BxYqxVB_r1TxS5PZRkNwKwosRndYPLiaQj2I_fJrwT7MoWVOw8LPcHHL2IA0iCIohtgEYp2TPLf6n'

  useEffect(() => {
    supplierRepository.getVerificationQueue().then(setTasks)
  }, [])

  const handleApproveSupplier = () => {
    setApprovedState(true)
    onShowToast?.('Supplier verification run #SV-9921A approved and appended to immutable ledger!')
  }

  const handleDownloadPDF = () => {
    window.print()
    onShowToast?.('Exporting verification dossier as PDF...')
  }

  const handleApproveTask = (taskId) => {
    supplierRepository.approveTask(taskId).then(() => {
      setTasks(tasks.map(t => t.id === taskId ? { ...t, state: 'approved' } : t))
      setSelectedTask(null)
      onShowToast?.(`Task ${taskId} verified and attestation issued!`)
    })
  }

  return (
    <main className="flex-grow w-full max-w-container-max mx-auto px-lg py-8 md:py-12 pb-section">
      {/* Console Tab Switcher */}
      <div className="flex items-center justify-between gap-4 mb-8 border-b border-hairline pb-4 flex-wrap">
        <div>
          <span className="font-label-uppercase text-label-uppercase text-brand-teal bg-brand-mint/20 px-2 py-0.5 rounded uppercase font-bold">
            Verifier Console
          </span>
          <h1 className="font-display-md text-display-md text-primary mt-1 font-bold">
            Verification Engine &amp; Review
          </h1>
        </div>

        <div className="flex gap-2 bg-surface-card p-1.5 rounded-xl border border-hairline">
          <button
            onClick={() => setActiveTab('run')}
            className={`font-button text-button px-4 py-2 rounded-lg transition-colors font-semibold flex items-center gap-1.5 ${
              activeTab === 'run'
                ? 'bg-primary text-on-primary font-bold shadow'
                : 'text-primary hover:bg-surface-variant'
            }`}
          >
            <span className="material-symbols-outlined text-sm">speed</span>
            Live Verification Run
          </button>
          <button
            onClick={() => setActiveTab('queue')}
            className={`font-button text-button px-4 py-2 rounded-lg transition-colors font-semibold flex items-center gap-1.5 ${
              activeTab === 'queue'
                ? 'bg-primary text-on-primary font-bold shadow'
                : 'text-primary hover:bg-surface-variant'
            }`}
          >
            <span className="material-symbols-outlined text-sm">checklist</span>
            Evidence Queue ({tasks.filter(t => t.state !== 'approved').length})
          </button>
        </div>
      </div>

      {activeTab === 'run' ? (
        <div className="flex flex-col gap-8">
          {/* Header & Actions */}
          <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-md">
            <div>
              <div className="flex items-center gap-2 mb-1 flex-wrap">
                <span className="font-label-uppercase text-label-uppercase text-brand-teal bg-brand-mint/25 px-2.5 py-1 rounded font-bold border border-brand-teal/20">
                  Verification Run ID: #SV-9921A
                </span>
                {approvedState && (
                  <span className="font-label-uppercase text-label-uppercase bg-brand-teal text-on-primary px-2.5 py-1 rounded font-bold flex items-center gap-1">
                    <span className="material-symbols-outlined text-xs">check</span>
                    Approved &amp; Certified
                  </span>
                )}
              </div>
              <h2 className="font-display-md text-display-md text-primary font-bold">
                Acme Corp Global
              </h2>
              <p className="font-body-md text-body-md text-on-surface-variant mt-1">
                Initiated by Sarah Jenkins (Senior Lead Verifier) on Aug 24, 2026
              </p>
            </div>

            <div className="flex gap-xs flex-wrap">
              <button
                onClick={handleDownloadPDF}
                className="font-button text-button bg-surface-card border border-hairline text-primary rounded-lg px-4 py-2.5 hover:border-primary transition-colors flex items-center gap-1 font-semibold"
              >
                <span className="material-symbols-outlined text-base">picture_as_pdf</span>
                Download PDF
              </button>
              <button
                disabled={approvedState}
                onClick={handleApproveSupplier}
                className={`font-button text-button rounded-lg px-5 py-2.5 transition-opacity flex items-center gap-1.5 font-bold shadow ${
                  approvedState
                    ? 'bg-brand-mint text-brand-teal opacity-90 cursor-default'
                    : 'bg-brand-teal text-on-primary hover:opacity-90'
                }`}
              >
                <span className="material-symbols-outlined text-[18px]" data-fill="true">check_circle</span>
                {approvedState ? 'Supplier Approved ✓' : 'Approve Supplier'}
              </button>
            </div>
          </div>

          {/* Progress Stepper */}
          <div className="bg-surface-card border border-hairline rounded-xl p-lg flex flex-col md:flex-row justify-between items-center relative shadow-sm">
            <div className="hidden md:block absolute top-1/2 left-lg right-lg h-0.5 bg-hairline -z-0 -translate-y-1/2"></div>
            
            {/* Step 1: Identity */}
            <button
              onClick={() => setActiveStep(0)}
              className={`flex flex-col items-center z-10 bg-surface-card px-md mb-md md:mb-0 cursor-pointer rounded-lg p-2 transition-all ${
                activeStep === 0 ? 'ring-2 ring-primary' : ''
              }`}
            >
              <div className="w-9 h-9 rounded-full bg-brand-teal text-on-primary flex items-center justify-center mb-xs shadow-sm">
                <span className="material-symbols-outlined text-base" data-fill="true">check</span>
              </div>
              <span className="font-button text-button text-primary font-bold">1. Identity Check</span>
              <span className="text-[11px] text-brand-teal font-semibold">100% Complete</span>
            </button>

            {/* Step 2: Sanctions */}
            <button
              onClick={() => setActiveStep(1)}
              className={`flex flex-col items-center z-10 bg-surface-card px-md mb-md md:mb-0 cursor-pointer rounded-lg p-2 transition-all ${
                activeStep === 1 ? 'ring-2 ring-primary' : ''
              }`}
            >
              <div className="w-9 h-9 rounded-full bg-brand-teal text-on-primary flex items-center justify-center mb-xs shadow-sm">
                <span className="material-symbols-outlined text-base" data-fill="true">check</span>
              </div>
              <span className="font-button text-button text-primary font-bold">2. Sanctions &amp; Watchlist</span>
              <span className="text-[11px] text-brand-teal font-semibold">0 Hits · Cleared</span>
            </button>

            {/* Step 3: Financial Health */}
            <button
              onClick={() => setActiveStep(2)}
              className={`flex flex-col items-center z-10 bg-surface-card px-md mb-md md:mb-0 cursor-pointer rounded-lg p-2 transition-all ${
                activeStep === 2 ? 'ring-2 ring-primary' : ''
              }`}
            >
              <div className="w-9 h-9 rounded-full bg-brand-ochre text-primary flex items-center justify-center mb-xs border-2 border-primary shadow-sm">
                <span className="material-symbols-outlined text-base" data-fill="false">hourglass_empty</span>
              </div>
              <span className="font-button text-button text-primary font-bold">3. Financial Health</span>
              <span className="text-[11px] text-brand-ochre font-bold">In Review (D&amp;B)</span>
            </button>

            {/* Step 4: ESG Compliance */}
            <button
              onClick={() => setActiveStep(3)}
              className={`flex flex-col items-center z-10 bg-surface-card px-md cursor-pointer rounded-lg p-2 transition-all ${
                activeStep === 3 ? 'ring-2 ring-primary' : 'opacity-70'
              }`}
            >
              <div className="w-9 h-9 rounded-full bg-surface border border-hairline text-on-surface-variant flex items-center justify-center mb-xs font-bold text-sm">
                4
              </div>
              <span className="font-button text-button text-on-surface-variant font-medium">4. ESG Compliance</span>
              <span className="text-[11px] text-body-muted">Ecovadis Sync</span>
            </button>
          </div>

          {/* Saturated Dashboard Bento Grid */}
          <div className="grid grid-cols-1 md:grid-cols-3 gap-lg">
            {/* Risk Score Card (Saturated Brand Teal) */}
            <div className="bg-brand-teal rounded-xl p-xl text-on-primary md:col-span-1 flex flex-col justify-between relative overflow-hidden shadow-md border-2 border-primary min-h-[320px]">
              <div className="z-10 relative">
                <h3 className="font-title-lg text-title-lg font-bold mb-xs">Overall Trust Score</h3>
                <p className="font-body-sm text-body-sm text-brand-mint mb-lg">
                  Aggregated multi-point verification ledger.
                </p>
                <div className="flex items-end gap-xs mb-md">
                  <span className="font-display-xl text-display-xl leading-none font-bold">92</span>
                  <span className="font-title-md text-title-md text-brand-mint pb-2 font-bold">/100</span>
                </div>
                <div className="inline-flex items-center gap-1.5 bg-white/15 px-3 py-1.5 rounded-lg text-brand-mint font-button text-button text-xs font-semibold backdrop-blur-sm">
                  <span className="material-symbols-outlined text-sm">trending_up</span>
                  +5 from last quarter audit
                </div>
              </div>

              {/* 3D Mascot Illustration */}
              <div className="absolute -bottom-3 -right-3 w-48 h-48 opacity-95 pointer-events-none">
                <img
                  alt="Mascot thumbs up"
                  className="object-contain w-full h-full drop-shadow-xl"
                  src={robotMascotImage}
                />
              </div>
            </div>

            {/* Bento Right Column (Col 2 & 3) */}
            <div className="md:col-span-2 grid grid-cols-1 sm:grid-cols-2 gap-lg">
              {/* Financials Fragment */}
              <div className="bg-surface-card border border-hairline rounded-xl p-lg flex flex-col hover:border-outline-variant transition-colors shadow-sm">
                <div className="flex justify-between items-start mb-md">
                  <div>
                    <h3 className="font-title-md text-title-md text-primary font-bold">Financial Stability</h3>
                    <p className="font-body-sm text-body-sm text-on-surface-variant">Dun &amp; Bradstreet Integration</p>
                  </div>
                  <span className="material-symbols-outlined text-brand-ochre text-2xl">account_balance</span>
                </div>

                <div className="bg-surface rounded-lg p-md border border-hairline flex-grow flex flex-col justify-center gap-2">
                  <div className="flex justify-between items-center">
                    <span className="font-body-sm text-body-sm text-on-surface-variant font-medium">Credit Risk Class</span>
                    <span className="font-button text-button text-primary bg-brand-mint/30 px-2 py-0.5 rounded text-xs font-bold">
                      Low Risk (Class 2)
                    </span>
                  </div>
                  <div className="w-full bg-surface-variant rounded-full h-2 overflow-hidden">
                    <div className="bg-brand-teal h-2 rounded-full w-[20%]"></div>
                  </div>
                  <div className="flex justify-between items-center pt-2 border-t border-hairline">
                    <span className="font-body-sm text-body-sm text-on-surface-variant font-medium">Payment Promptness</span>
                    <span className="font-button text-button text-primary font-bold">98% On-Time</span>
                  </div>
                </div>
              </div>

              {/* ESG Fragment */}
              <div className="bg-surface-card border border-hairline rounded-xl p-lg flex flex-col hover:border-outline-variant transition-colors shadow-sm">
                <div className="flex justify-between items-start mb-md">
                  <div>
                    <h3 className="font-title-md text-title-md text-primary font-bold">ESG Compliance</h3>
                    <p className="font-body-sm text-body-sm text-on-surface-variant">EcoVadis Data Sync</p>
                  </div>
                  <span className="material-symbols-outlined text-brand-coral text-2xl" data-fill="true">eco</span>
                </div>

                <div className="bg-surface rounded-lg p-md border border-hairline flex-grow flex flex-col gap-2 justify-center text-xs">
                  <div className="flex items-center gap-2 text-primary">
                    <span className="material-symbols-outlined text-brand-teal text-base" data-fill="true">check_circle</span>
                    <span className="font-medium">Carbon Emissions Scope 1 &amp; 2 Reported</span>
                  </div>
                  <div className="flex items-center gap-2 text-primary">
                    <span className="material-symbols-outlined text-brand-teal text-base" data-fill="true">check_circle</span>
                    <span className="font-medium">Labor Rights Audit (Passed 2026)</span>
                  </div>
                  <div className="flex items-center gap-2 text-on-surface-variant">
                    <span className="material-symbols-outlined text-brand-ochre text-base">pending</span>
                    <span>Conflict Minerals Declaration (Under Review)</span>
                  </div>
                </div>
              </div>

              {/* Watchlist Fragment (Spans 2 cols) */}
              <div className="bg-brand-peach rounded-xl p-lg flex flex-col sm:col-span-2 relative overflow-hidden text-primary border border-primary/20 shadow-sm">
                <div className="relative z-10">
                  <div className="flex justify-between items-start mb-md">
                    <div>
                      <h3 className="font-title-md text-title-md font-bold">Global Sanctions &amp; Watchlists</h3>
                      <p className="font-body-sm text-body-sm opacity-85">Checked against 1,200+ international regulatory databases</p>
                    </div>
                    <span className="material-symbols-outlined text-2xl">shield_lock</span>
                  </div>

                  <div className="flex items-center gap-md bg-white/60 p-md rounded-lg backdrop-blur-sm border border-white/40">
                    <div className="w-12 h-12 rounded-full bg-white flex items-center justify-center text-brand-coral flex-shrink-0 shadow-sm">
                      <span className="material-symbols-outlined text-2xl" data-fill="true">gpp_good</span>
                    </div>
                    <div>
                      <span className="font-button text-button text-sm font-bold block">0 Hits Found (Clean Standing)</span>
                      <span className="font-body-sm text-body-sm text-primary/80">
                        Clear across OFAC (US Treasury), UN Security Council, and EU Consolidated Financial Sanctions lists.
                      </span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      ) : (
        /* Verifier Task Review Queue */
        <div className="flex flex-col gap-6">
          <div className="flex justify-between items-center">
            <div>
              <h2 className="font-title-lg text-title-lg text-primary font-bold">Evidence Review Queue</h2>
              <p className="text-body-muted text-sm">
                Trained verifiers review submitted artifacts to produce time-bounded domain attestations.
              </p>
            </div>
            <span className="text-xs bg-brand-ochre/20 text-primary font-bold px-3 py-1 rounded-full border border-brand-ochre/40">
              {tasks.filter(t => t.state === 'in_review').length} In Review · {tasks.filter(t => t.state === 'queued').length} Queued
            </span>
          </div>

          <div className="bg-surface-card border border-hairline rounded-xl overflow-hidden shadow-sm">
            <div className="grid grid-cols-12 gap-3 p-4 bg-surface-strong text-xs font-bold text-body-muted uppercase tracking-wider border-b border-hairline">
              <span className="col-span-4">Supplier &amp; Claim</span>
              <span className="col-span-3">Evidence Artifacts</span>
              <span className="col-span-2">Task State</span>
              <span className="col-span-2">Assigned &amp; SLA</span>
              <span className="col-span-1 text-right">Action</span>
            </div>

            <div className="divide-y divide-hairline">
              {tasks.map((task) => (
                <div key={task.id} className="grid grid-cols-12 gap-3 p-4 items-center hover:bg-surface-variant transition-colors text-sm">
                  <div className="col-span-4 flex flex-col">
                    <span className="font-bold text-primary">{task.supplier?.tradeName || 'Supplier'}</span>
                    <span className="text-xs text-on-surface-variant font-medium">{task.claim_label}</span>
                    <span className="text-[11px] text-body-muted font-mono">{task.id}</span>
                  </div>

                  <div className="col-span-3 flex flex-col text-xs">
                    <span className="text-primary line-clamp-1">{task.evidence_summary}</span>
                    <span className="text-body-muted">{task.evidence_count} file(s) attached</span>
                  </div>

                  <div className="col-span-2">
                    <span className={`inline-flex items-center gap-1 text-xs font-semibold px-2.5 py-1 rounded-full ${
                      task.state === 'approved'
                        ? 'bg-brand-mint text-brand-teal'
                        : task.state === 'in_review'
                        ? 'bg-brand-ochre/25 text-primary border border-brand-ochre/30'
                        : 'bg-surface text-body-muted border border-hairline'
                    }`}>
                      {task.state === 'approved' ? '✓ Approved' : task.state.replace('_', ' ')}
                    </span>
                  </div>

                  <div className="col-span-2 flex flex-col text-xs">
                    <span className="font-semibold text-primary">{task.assigned_to}</span>
                    <span className="text-body-muted">{task.sla}</span>
                  </div>

                  <div className="col-span-1 text-right">
                    {task.state !== 'approved' ? (
                      <button
                        onClick={() => setSelectedTask(task)}
                        className="font-button text-button text-xs bg-primary text-on-primary px-3 py-1.5 rounded-lg hover:opacity-90 transition-opacity font-bold shadow-sm whitespace-nowrap"
                      >
                        Review
                      </button>
                    ) : (
                      <span className="text-xs text-brand-teal font-bold">Done</span>
                    )}
                  </div>
                </div>
              ))}
            </div>
          </div>

          <div className="p-4 bg-brand-mint/20 border border-brand-teal/30 rounded-xl flex items-start gap-3 text-xs text-brand-teal">
            <span className="material-symbols-outlined text-base mt-0.5" data-fill="true">gavel</span>
            <div>
              <strong className="font-bold block">Decision Safeguard Axiom</strong>
              Attestations are never issued optimistically. A submitted decision remains pending until the verifier confirms document integrity against registry check logs.
            </div>
          </div>
        </div>
      )}

      {/* Task Review Modal */}
      {selectedTask && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm animate-fadeIn">
          <div className="w-full max-w-lg bg-surface-card border-2 border-primary rounded-2xl p-6 shadow-2xl">
            <div className="flex justify-between items-start pb-3 border-b border-hairline mb-4">
              <div>
                <span className="font-label-uppercase text-label-uppercase text-brand-teal bg-brand-mint/25 px-2 py-0.5 rounded font-bold">
                  {selectedTask.id}
                </span>
                <h3 className="font-title-lg text-title-lg text-primary font-bold mt-1">
                  {selectedTask.supplier?.tradeName}
                </h3>
              </div>
              <button
                onClick={() => setSelectedTask(null)}
                className="text-primary hover:bg-surface-variant p-1 rounded-lg"
              >
                <span className="material-symbols-outlined">close</span>
              </button>
            </div>

            <div className="flex flex-col gap-4 text-sm mb-6">
              <div>
                <span className="text-xs font-bold text-body-muted block mb-1 uppercase">Claim Asserted</span>
                <p className="font-semibold text-primary">{selectedTask.claim_label}</p>
              </div>

              <div>
                <span className="text-xs font-bold text-body-muted block mb-1 uppercase">Evidence Submitted</span>
                <div className="p-3 bg-surface rounded-lg border border-hairline text-xs">
                  <p className="font-medium text-primary mb-1">{selectedTask.evidence_summary}</p>
                  <div className="text-body-muted flex items-center gap-2">
                    <span>SHA-256 Verified</span>
                    <span>·</span>
                    <span>{selectedTask.evidence_count} attached files</span>
                  </div>
                </div>
              </div>

              <div className="grid grid-cols-2 gap-3 text-xs">
                <div className="p-2.5 bg-surface rounded border border-hairline">
                  <span className="text-body-muted block">Priority</span>
                  <span className="font-bold text-primary">{selectedTask.priority}</span>
                </div>
                <div className="p-2.5 bg-surface rounded border border-hairline">
                  <span className="text-body-muted block">SLA Target</span>
                  <span className="font-bold text-primary">{selectedTask.sla}</span>
                </div>
              </div>
            </div>

            <div className="flex justify-end gap-3 pt-3 border-t border-hairline">
              <button
                onClick={() => setSelectedTask(null)}
                className="font-button text-button px-4 py-2 rounded-lg border border-hairline text-primary hover:bg-surface-variant"
              >
                Cancel
              </button>
              <button
                onClick={() => handleApproveTask(selectedTask.id)}
                className="font-button text-button px-5 py-2 rounded-lg bg-brand-teal text-on-primary hover:opacity-90 font-bold shadow"
              >
                Issue Attestation (Approve)
              </button>
            </div>
          </div>
        </div>
      )}
    </main>
  )
}
