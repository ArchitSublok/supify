export function Modal({ isOpen, onClose, title, children, maxWidth = 'max-w-lg' }) {
  if (!isOpen) return null

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/40 backdrop-blur-sm animate-fadeIn">
      <div className="fixed inset-0" onClick={onClose}></div>
      <div className={`relative z-10 w-full ${maxWidth} bg-surface-card border-2 border-primary rounded-2xl p-lg shadow-2xl overflow-hidden max-h-[90vh] flex flex-col`}>
        <div className="flex justify-between items-center pb-md border-b border-hairline mb-md">
          <h3 className="font-title-lg text-title-lg text-primary font-bold">{title}</h3>
          <button
            onClick={onClose}
            className="w-8 h-8 rounded-full flex items-center justify-center text-primary hover:bg-surface-variant transition-colors"
          >
            <span className="material-symbols-outlined text-lg">close</span>
          </button>
        </div>
        <div className="overflow-y-auto flex-grow pr-1">
          {children}
        </div>
      </div>
    </div>
  )
}
