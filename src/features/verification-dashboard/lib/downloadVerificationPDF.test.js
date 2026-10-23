import { describe, it, expect, vi } from 'vitest'
import { downloadVerificationPDF } from './downloadVerificationPDF'

describe('downloadVerificationPDF', () => {
  it('creates and clicks a download link with a valid PDF blob', () => {
    let clicked = false
    let downloadAttr = ''
    let hrefAttr = ''

    const mockAnchor = {
      set href(val) { hrefAttr = val },
      get href() { return hrefAttr },
      set download(val) { downloadAttr = val },
      get download() { return downloadAttr },
      click: () => { clicked = true }
    }

    vi.spyOn(document, 'createElement').mockImplementation((tag) => {
      if (tag === 'a') return mockAnchor
      return document.createElement(tag)
    })

    const appendChildSpy = vi.spyOn(document.body, 'appendChild').mockImplementation(() => {})
    const removeChildSpy = vi.spyOn(document.body, 'removeChild').mockImplementation(() => {})

    const createObjectURLSpy = vi.spyOn(URL, 'createObjectURL').mockReturnValue('blob:test-pdf-url')
    const revokeObjectURLSpy = vi.spyOn(URL, 'revokeObjectURL').mockImplementation(() => {})

    const monarchSupplier = {
      id: 'monarch-industrial',
      legal_name: 'Monarch Industrial Components LLP',
      trade_name: 'Monarch Industrial',
      location: 'Rajkot, Gujarat, IN',
      category: 'Precision machined parts',
      capacity: '35,000 components / month',
      moq: '500 units',
      lead_time: '21 days',
      trust: {
        band: 'basic',
        pillars: [
          ['Identity', 74],
          ['Legal', 57],
          ['Quality', 18],
          ['Capability', 26]
        ]
      },
      claims: [
        {
          label: 'GST registration',
          value: '24AAXFM9104Q1ZN',
          state: 'verified'
        }
      ]
    }

    downloadVerificationPDF(monarchSupplier, 45)

    expect(createObjectURLSpy).toHaveBeenCalled()
    const createdBlob = createObjectURLSpy.mock.calls[0][0]
    expect(createdBlob.type).toBe('application/pdf')
    expect(downloadAttr).toBe('Monarch_Industrial_Verification_Dossier.pdf')
    expect(clicked).toBe(true)
    expect(appendChildSpy).toHaveBeenCalled()
    expect(removeChildSpy).toHaveBeenCalled()
    expect(revokeObjectURLSpy).toHaveBeenCalledWith('blob:test-pdf-url')

    appendChildSpy.mockRestore()
    removeChildSpy.mockRestore()
    createObjectURLSpy.mockRestore()
    revokeObjectURLSpy.mockRestore()
    vi.restoreAllMocks()
  })

  it('handles empty or missing supplier gracefully', () => {
    const createObjectURLSpy = vi.spyOn(URL, 'createObjectURL')
    downloadVerificationPDF(null)
    expect(createObjectURLSpy).not.toHaveBeenCalled()
    createObjectURLSpy.mockRestore()
  })
})
