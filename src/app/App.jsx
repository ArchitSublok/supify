import { useState } from 'react'
import { Routes, Route, useNavigate, useLocation } from 'react-router-dom'
import { Header } from '../shared/ui/Header'
import { Modal } from '../shared/ui/Modal'
import { Toast } from '../shared/ui/Toast'
import { LandingScreen } from '../features/landing/ui/LandingScreen'
import { SearchScreen } from '../features/discovery/ui/SearchScreen'
import { ComparisonScreen } from '../features/comparison/ui/ComparisonScreen'
import { SupplierProfile } from '../features/supplier-profile/ui/SupplierProfile'
import { VerificationDashboard } from '../features/verification-dashboard/ui/VerificationDashboard'
import { VerificationTaskDetail } from '../features/verification-review/ui/VerificationTaskDetail'
import { OnboardingScreen } from '../features/supplier-onboarding/ui/OnboardingScreen'
import { NotFound } from './NotFound'

export default function App() {
  const navigate = useNavigate()
  const location = useLocation()
  const [toast, setToast] = useState(null)
  const [authModalMode, setAuthModalMode] = useState(null) // null | 'login' | 'signup'
  const [selectedRole, setSelectedRole] = useState('buyer')

  const showToast = (message, type = 'success') => {
    setToast({ message, type })
    setTimeout(() => {
      setToast(null)
    }, 4000)
  }

  const handleNavigate = (path) => {
    navigate(path)
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }

  return (
    <div className="flex flex-col min-h-screen bg-background text-on-background">
      <Header
        currentPath={location.pathname}
        onNavigate={handleNavigate}
        onOpenAuth={(mode) => setAuthModalMode(mode)}
      />

      <Routes>
        <Route
  path="/"
  element={
    <LandingScreen
      onNavigate={handleNavigate}
      onOpenAuth={(mode) => setAuthModalMode(mode)}
      onShowToast={showToast}
    />
  }
/>
        <Route
          path="/search"
          element={<SearchScreen onOpenSupplier={(id) => handleNavigate(`/suppliers/${id}`)} />}
        />
        <Route
          path="/solutions"
          element={<SearchScreen onOpenSupplier={(id) => handleNavigate(`/suppliers/${id}`)} />}
        />
        <Route
          path="/discovery"
          element={<SearchScreen onOpenSupplier={(id) => handleNavigate(`/suppliers/${id}`)} />}
        />
        <Route
          path="/compare"
          element={<ComparisonScreen />}
        />
        <Route
          path="/suppliers/:supplierId"
          element={
            <SupplierProfile
              onBack={() => handleNavigate('/search')}
              onShowToast={showToast}
              onOpenVerification={(id) => handleNavigate(`/verification?supplier=${id}`)}
            />
          }
        />
        <Route
          path="/verification"
          element={
            <VerificationDashboard
              onOpenSupplier={(id) => handleNavigate(`/suppliers/${id}`)}
              onShowToast={showToast}
            />
          }
        />
        <Route
          path="/verify"
          element={
            <VerificationDashboard
              onOpenSupplier={(id) => handleNavigate(`/suppliers/${id}`)}
              onShowToast={showToast}
            />
          }
        />
        <Route
          path="/verifier/queue"
          element={
            <VerificationDashboard
              onOpenSupplier={(id) => handleNavigate(`/suppliers/${id}`)}
              onShowToast={showToast}
            />
          }
        />
        <Route
          path="/verifier/task/:taskId"
          element={<VerificationTaskDetail />}
        />
        <Route
          path="/supplier"
          element={
            <OnboardingScreen
              onShowToast={showToast}
              onNavigate={handleNavigate}
            />
          }
        />
        <Route
          path="/supplier/onboarding"
          element={
            <OnboardingScreen
              onShowToast={showToast}
              onNavigate={handleNavigate}
            />
          }
        />
        <Route path="*" element={<NotFound />} />
      </Routes>

      {/* Toast Notification */}
      {toast && (
        <Toast
          message={toast.message}
          type={toast.type}
          onClose={() => setToast(null)}
        />
      )}

      {/* Auth Modal */}
      <Modal
        isOpen={!!authModalMode}
        onClose={() => setAuthModalMode(null)}
        title={authModalMode === 'login' ? 'Log In to Supify' : 'Create Supify Account'}
      >
               <form
          onSubmit={(e) => {
            e.preventDefault()
            localStorage.setItem('currentUserRole', selectedRole)
            setAuthModalMode(null)
            showToast(`Welcome back to Supify! Logged in as ${selectedRole}.`)
          }}
          className="flex flex-col gap-4 text-sm"
        >
          <div>
            <label className="block text-xs font-bold text-primary mb-1">Work Email</label>
            <input
              required
              type="email"
              placeholder="procurement@enterprise.com"
              className="w-full px-3.5 py-2.5 bg-background border border-hairline rounded-lg focus:outline-none focus:border-primary text-primary"
            />
          </div>
          <div>
            <label className="block text-xs font-bold text-primary mb-1">Password</label>
            <input
              required
              type="password"
              placeholder="••••••••••••"
              className="w-full px-3.5 py-2.5 bg-background border border-hairline rounded-lg focus:outline-none focus:border-primary text-primary"
            />
          </div>
                       <div>
            <label className="block text-xs font-bold text-primary mb-1">Login as</label>
            <select
              value={selectedRole}
              onChange={(e) => setSelectedRole(e.target.value)}
              className="w-full px-3.5 py-2.5 bg-background border border-hairline rounded-lg focus:outline-none focus:border-brand-teal focus:ring-2 focus:ring-brand-teal/20 text-primary font-medium cursor-pointer hover:border-brand-teal/50 transition-colors appearance-none"
              style={{
                backgroundImage:
                  "url(\"data:image/svg+xml;charset=UTF-8,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%23666' stroke-width='2'%3E%3Cpath d='M6 9l6 6 6-6'/%3E%3C/svg%3E\")",
                backgroundRepeat: 'no-repeat',
                backgroundPosition: 'right 12px center',
                backgroundSize: '18px',
              }}
            >
              <option value="buyer">Buyer</option>
              <option value="supplier">Supplier</option>
              <option value="verifier">Verifier</option>
            </select>
          </div>
          <button
            type="submit"
            className="font-button text-button bg-primary text-on-primary py-3 rounded-xl hover:opacity-90 font-bold shadow-md mt-2"
          >
            {authModalMode === 'login' ? 'Log In' : 'Sign Up Free'}
          </button>
        </form>
      </Modal>
    </div>
  )
}