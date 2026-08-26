export function Toast({ message, type = 'success', onClose }) {
  if (!message) return null

  const icons = {
    success: 'check_circle',
    info: 'info',
    warning: 'warning',
    error: 'error',
  }

  const bgStyles = {
    success: 'bg-brand-teal text-on-primary border-brand-teal',
    info: 'bg-primary text-on-primary border-primary',
    warning: 'bg-brand-ochre text-primary border-primary',
    error: 'bg-error text-on-error border-error',
  }[type] || 'bg-brand-teal text-on-primary'

  return (
    <div className="fixed bottom-6 right-6 z-50 animate-bounce-short">
      <div className={`flex items-center gap-3 px-4 py-3 rounded-xl border-2 shadow-xl ${bgStyles}`}>
        <span className="material-symbols-outlined text-xl" data-fill="true">
          {icons[type] || 'info'}
        </span>
        <span className="font-button text-button text-sm">{message}</span>
        {onClose && (
          <button onClick={onClose} className="opacity-80 hover:opacity-100 ml-2">
            <span className="material-symbols-outlined text-sm">close</span>
          </button>
        )}
      </div>
    </div>
  )
}
