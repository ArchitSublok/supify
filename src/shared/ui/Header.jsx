import { useState } from 'react'

export function Header({ currentPath, onNavigate, onOpenAuth }) {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)

  const navItems = [
    { label: 'Platform', path: '/', isActive: currentPath === '/' || currentPath === '/platform' },
    { label: 'Solutions', path: '/search', isActive: currentPath.startsWith('/search') || currentPath.startsWith('/solutions') || currentPath.startsWith('/suppliers') },
    { label: 'Verification', path: '/verification', isActive: currentPath.startsWith('/verification') || currentPath.startsWith('/verify') },
    { label: 'Supplier Workspace', path: '/supplier/onboarding', isActive: currentPath.startsWith('/supplier') },
  ]

  const handleNav = (path) => {
    setMobileMenuOpen(false)
    onNavigate(path)
  }

  return (
    <header className="bg-background sticky top-0 w-full z-50 border-b border-hairline transition-all duration-200 ease-in-out backdrop-blur-md bg-background/95">
      <div className="flex justify-between items-center px-lg py-md max-w-container-max mx-auto">
        {/* Brand & Nav */}
        <div className="flex items-center gap-xl">
          <button
            onClick={() => handleNav('/')}
            className="font-display-sm text-display-sm font-bold text-primary hover:opacity-80 transition-opacity flex items-center gap-1.5"
          >
            <span className="w-3 h-3 rounded-full bg-brand-coral inline-block"></span>
            Supify
          </button>

          <nav className="hidden lg:flex items-center gap-lg">
            {navItems.map((item) => (
              <button
                key={item.label}
                onClick={() => handleNav(item.path)}
                className={`font-button text-button transition-all duration-150 py-1 ${
                  item.isActive
                    ? 'text-primary font-bold border-b-2 border-primary pb-0.5'
                    : 'text-on-surface-variant hover:text-primary hover:opacity-90'
                }`}
              >
                {item.label}
              </button>
            ))}
          </nav>
        </div>

        {/* Right CTA Cluster */}
        <div className="hidden md:flex items-center gap-md">
          <button
            onClick={() => onOpenAuth?.('login')}
            className="font-button text-button text-primary bg-surface-card border border-hairline px-5 py-2.5 rounded-lg hover:bg-surface-strong transition-colors"
          >
            Log In
          </button>
          <button
            onClick={() => handleNav('/search')}
            className="font-button text-button text-on-primary bg-primary px-5 py-2.5 rounded-lg hover:opacity-90 transition-opacity flex items-center gap-1 shadow-sm"
          >
            <span>Get Started</span>
            <span className="material-symbols-outlined text-sm">arrow_forward</span>
          </button>
        </div>

        {/* Mobile menu button */}
        <button
          onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
          className="lg:hidden p-2 text-primary rounded-lg hover:bg-surface-variant transition-colors"
          aria-label="Toggle navigation menu"
        >
          <span className="material-symbols-outlined text-2xl">
            {mobileMenuOpen ? 'close' : 'menu'}
          </span>
        </button>
      </div>

      {/* Mobile Drawer */}
      {mobileMenuOpen && (
        <div className="lg:hidden border-t border-hairline bg-surface-card px-lg py-md shadow-lg flex flex-col gap-sm animate-fadeIn">
          {navItems.map((item) => (
            <button
              key={item.label}
              onClick={() => handleNav(item.path)}
              className={`text-left font-button text-button py-2.5 px-3 rounded-lg transition-colors ${
                item.isActive
                  ? 'bg-primary text-on-primary font-bold'
                  : 'text-primary hover:bg-surface-variant'
              }`}
            >
              {item.label}
            </button>
          ))}
          <div className="pt-sm mt-xs border-t border-hairline flex flex-col gap-xs">
            <button
              onClick={() => {
                setMobileMenuOpen(false)
                onOpenAuth?.('login')
              }}
              className="w-full text-center font-button text-button py-2.5 bg-background border border-hairline rounded-lg text-primary"
            >
              Log In
            </button>
            <button
              onClick={() => handleNav('/search')}
              className="w-full text-center font-button text-button py-2.5 bg-primary text-on-primary rounded-lg font-bold"
            >
              Get Started
            </button>
          </div>
        </div>
      )}
    </header>
  )
}
