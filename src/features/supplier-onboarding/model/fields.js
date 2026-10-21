export const categories = ['Sheet-metal fabrication', 'Industrial fasteners', 'Precision machined parts', 'Textiles', 'Packaging', 'Electronics']

export const defaultValues = {
  legalName: 'Narmada Advanced Custom Fabrication Private Limited',
  tradeName: 'Narmada Fabrication',
  location: 'Indore, Madhya Pradesh, India',
  category: categories[0],
  website: 'https://narmadafabrication.example.com',
  capacity: '50,000 units / month',
  moq: '100 units',
  leadTime: '21-28 Days',
  gstNumber: '23AAECA9082G1ZP',
  qualityProcess: '',
}

export const fieldGroups = {
  organisation: [
    { name: 'tradeName', label: 'Trade / Display Name', required: true },
    { name: 'legalName', label: 'Full Legal Name', required: true },
    { name: 'location', label: 'Operating City & Region', required: true },
    { name: 'website', label: 'Corporate Website', type: 'url', placeholder: 'https://company.com' },
  ],
  capabilities: [
    { name: 'category', label: 'Primary Sourcing Category', required: true, options: categories },
    { name: 'capacity', label: 'Monthly Production Capacity', required: true, placeholder: '50,000 units / month' },
    { name: 'moq', label: 'Minimum Order Quantity', placeholder: '100 units' },
    { name: 'leadTime', label: 'Typical Lead Time', placeholder: '14-21 Days' },
  ],
  claims: [
    { name: 'gstNumber', label: 'Tax / Registry Identifier', hint: 'For example, GSTIN, VAT, or D-U-N-S. This is a self-declared identifier.' },
    { name: 'qualityProcess', label: 'Quality Management Process', multiline: true, placeholder: 'Describe your inspection and quality controls' },
  ],
}

export const allFields = Object.values(fieldGroups).flat()

export function stepForField(name) {
  return Object.keys(fieldGroups).find((key) => fieldGroups[key].some((field) => field.name === name)) || 'organisation'
}
