import { useEffect, useState } from 'react'
import { Header } from '../shared/ui/Header'
import { Modal } from '../shared/ui/Modal'
import { Toast } from '../shared/ui/Toast'
import { LandingScreen } from '../features/landing/ui/LandingScreen'
import { SearchScreen } from '../features/discovery/ui/SearchScreen'
import { SupplierProfile } from '../features/supplier-profile/ui/SupplierProfile'
import { VerificationDashboard } from '../features/verification-dashboard/ui/VerificationDashboard'
import { OnboardingScreen } from '../features/supplier-onboarding/ui/OnboardingScreen'

function getPath() {
  return window.location.pathname || '/'
}

export default function App() {
  const [path, setPath] = useState(getPath)
  const [toast, setToast] = useState(null)
  const [authModalMode, setAuthModalMode] = useState(null) // null | 'login' | 'signup'

  useEffect(() => {
    const sync = () => setPath(getPath())
    window.addEventListener('popstate', sync)
    return () => window.removeEventListener('popstate', sync)
  }, [])

  const navigate = (nextPath) => {
    window.history.pushState({}, '', nextPath)
    setPath(nextPath)
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }

  const showToast = (message, type = 'success') => {
    setToast({ message, type })
    setTimeout(() => {
      setToast(null)
    }, 4000)
  }

  let content
  if (path.startsWith('/suppliers/')) {
    const supplierId = path.split('/')[2]
    content = (
      <SupplierProfile
        supplierId={supplierId}
        onBack={() => navigate('/search')}
        onShowToast={showToast}
        onOpenVerification={() => navigate('/verification')}
      />
    )
  } else if (path.startsWith('/verification') || path.startsWith('/verify')) {
    content = (
      <VerificationDashboard
        onOpenSupplier={(id) => navigate(`/suppliers/${id}`)}
        onShowToast={showToast}
      />
    )
  } else if (path.startsWith('/supplier')) {
    content = (
      <OnboardingScreen
        onShowToast={showToast}
        onNavigate={navigate}
      />
    )
  } else if (path.startsWith('/search') || path.startsWith('/solutions') || path.startsWith('/discovery')) {
    content = (
      <SearchScreen
        onOpenSupplier={(id) => navigate(`/suppliers/${id}`)}
      />
    )
  } else {
    // Default to Landing Page
    content = (
      <LandingScreen
        onNavigate={navigate}
      />
    )
  }

  return (
    <div className="flex flex-col min-h-screen bg-background text-on-background">
      <Header
        currentPath={path}
        onNavigate={navigate}
        onOpenAuth={(mode) => setAuthModalMode(mode)}
      />

      {content}

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
            setAuthModalMode(null)
            showToast(`Welcome back to Supify!`)
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
