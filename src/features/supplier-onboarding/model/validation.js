import { allFields, fieldGroups, categories } from './fields.js'

export function validateField(name, value) {
  const field = allFields.find((entry) => entry.name === name)
  const text = (value || '').trim()
  if (field?.required && !text) return `${field.label} is required.`
  if (text.length > 1000) return 'Please use 1,000 characters or fewer.'
  if (name === 'category' && text && !categories.includes(text)) return 'Choose a category from the list.'
  if (name === 'website' && text) {
    try {
      const url = new URL(text)
      if (!['https:', 'http:'].includes(url.protocol) || !url.hostname) throw new Error()
    } catch {
      return 'Enter a complete website URL starting with https:// or http://.'
    }
  }
  return ''
}

export function validateValues(values, steps = Object.keys(fieldGroups)) {
  const errors = {}
  for (const key of steps) {
    for (const { name } of fieldGroups[key] || []) {
      const error = validateField(name, values[name])
      if (error) errors[name] = error
    }
  }
  return errors
}

export function validateEvidence(file) {
  if (!['application/pdf', 'image/png', 'image/jpeg'].includes(file.type)) return 'Choose a PDF, PNG, or JPEG file.'
  if (file.size > 25 * 1024 * 1024) return 'Files must be 25 MB or smaller.'
  if (file.size === 0) return 'The selected file is empty.'
  return ''
}
