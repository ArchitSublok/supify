import { SupplierRepository } from './contract'
import { toSupplier, toVerificationTask } from '../../entities/supplier/model/adapter'

const SUPPLIERS_URL = '/data/suppliers.json'
const TASKS_URL = '/data/verification-tasks.json'
const SIMULATED_LATENCY_MS = 200

function delay(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

async function fetchJson(url) {
  await delay(SIMULATED_LATENCY_MS)
  const response = await fetch(url)
  if (!response.ok) {
    throw new Error(`Request failed with status ${response.status} for ${url}`)
  }
  return response.json()
}

function findSupplierWire(supplierWires, supplierId) {
  return supplierWires.find((supplier) => supplier.id === supplierId)
}

class FixtureSupplierRepository extends SupplierRepository {
  async search(filters = {}) {
    const supplierWires = await fetchJson(SUPPLIERS_URL)
    const query = (filters.query || '').trim().toLowerCase()
    const band = filters.band || ''
    const region = filters.region || ''
    const industry = filters.industry || ''
    const sortBy = filters.sortBy || 'relevance'

    let rows = supplierWires.filter((supplier) => {
      const claimsStr = (supplier.claims || []).map((c) => c.label + ' ' + c.value).join(' ')
      const haystack = `${supplier.legal_name} ${supplier.trade_name} ${supplier.category} ${supplier.location} ${supplier.description || ''} ${supplier.region || ''} ${claimsStr}`.toLowerCase()

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
    const supplierWires = await fetchJson(SUPPLIERS_URL)
    const wire = findSupplierWire(supplierWires, supplierId)
    return wire ? toSupplier(wire) : null
  }

  async getVerificationQueue() {
    const [taskWires, supplierWires] = await Promise.all([
      fetchJson(TASKS_URL),
      fetchJson(SUPPLIERS_URL),
    ])
    return taskWires.map((taskWire) =>
      toVerificationTask(taskWire, findSupplierWire(supplierWires, taskWire.supplier_id)),
    )
  }

  async getVerificationTask(taskId) {
    const [taskWires, supplierWires] = await Promise.all([
      fetchJson(TASKS_URL),
      fetchJson(SUPPLIERS_URL),
    ])
    const taskWire = taskWires.find((task) => task.id === taskId)
    return taskWire ? toVerificationTask(taskWire, findSupplierWire(supplierWires, taskWire.supplier_id)) : null
  }
}

export const supplierRepository = new FixtureSupplierRepository()
