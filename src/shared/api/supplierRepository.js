import { SupplierRepository } from './contract'
import { supplierWires, verificationTaskWires } from './fixtures'
import { toSupplier } from '../../entities/supplier/model/adapter'

class FixtureSupplierRepository extends SupplierRepository {
  async search(filters = {}) {
    const query = (filters.query || '').trim().toLowerCase()
    const band = filters.band || ''
    const region = filters.region || ''
    const industry = filters.industry || ''
    const sortBy = filters.sortBy || 'relevance'

    let rows = supplierWires.filter((supplier) => {
      const haystack = `${supplier.legal_name} ${supplier.trade_name} ${supplier.category} ${supplier.location} ${supplier.description} ${supplier.region} ${supplier.claims.map(c => c.label + ' ' + c.value).join(' ')}`.toLowerCase()
      
      const matchesQuery = !query || haystack.includes(query)
      const matchesBand = !band || supplier.trust.band === band
      const matchesRegion = !region || region === 'All Regions' || supplier.region === region || supplier.location.toLowerCase().includes(region.toLowerCase())
      const matchesIndustry = !industry || industry === 'All Industries' || supplier.category.toLowerCase().includes(industry.toLowerCase())

      return matchesQuery && matchesBand && matchesRegion && matchesIndustry
    })

    if (sortBy === 'rating') {
      rows.sort((a, b) => (b.rating || 0) - (a.rating || 0))
    } else if (sortBy === 'leadTime') {
      rows.sort((a, b) => {
        const getDays = (str) => parseInt(str) || 999
        return getDays(a.lead_time) - getDays(b.lead_time)
      })
    } else if (sortBy === 'moq') {
      rows.sort((a, b) => {
        const getNum = (str) => parseInt((str || '').replace(/[^0-9]/g, '')) || 999999
        return getNum(a.moq) - getNum(b.moq)
      })
    }

    return rows.map(toSupplier)
  }

  async getById(supplierId) {
    const wire = supplierWires.find((supplier) => supplier.id === supplierId)
    return wire ? toSupplier(wire) : null
  }

  async getVerificationQueue() {
    return verificationTaskWires.map((task) => ({
      ...task,
      supplier: toSupplier(supplierWires.find((supplier) => supplier.id === task.supplier_id)),
    }))
  }

  async approveTask(taskId) {
    const task = verificationTaskWires.find(t => t.id === taskId)
    if (task) {
      task.state = 'approved'
      return { success: true, taskId, state: 'approved' }
    }
    return { success: false }
  }
}

// Replace only this export when the database/API is ready.
export const supplierRepository = new FixtureSupplierRepository()
