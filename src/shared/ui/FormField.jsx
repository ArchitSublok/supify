import { useId } from 'react'

export function FormField({ name, label, error, hint, options, multiline, id: suppliedId, ...props }) {
  const generatedId = useId()
  const id = suppliedId || `field-${name || generatedId}`
  const description = [hint && `${id}-hint`, error && `${id}-error`].filter(Boolean).join(' ') || undefined
  const inputProps = { ...props, id, name, 'aria-invalid': Boolean(error), 'aria-describedby': description, className: 'workspace-input' }
  return (
    <div className="space-y-2 min-w-0">
      <label htmlFor={id} className="block text-sm font-semibold">{label}{props.required && <span aria-hidden="true"> *</span>}</label>
      {options ? <select {...inputProps}>{options.map((option) => <option key={option} value={option}>{option}</option>)}</select>
        : multiline ? <textarea {...inputProps} rows={4} /> : <input {...inputProps} />}
      {hint && <p id={`${id}-hint`} className="text-xs text-body-muted">{hint}</p>}
      {error && <p id={`${id}-error`} className="text-sm text-error">{error}</p>}
    </div>
  )
}
