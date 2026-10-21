export class SupplierRepository {
  async getWorkspaceDraft(_options) { throw new Error('SupplierRepository.getWorkspaceDraft must be implemented') }
  async saveWorkspaceDraft(_draft, _options) { throw new Error('SupplierRepository.saveWorkspaceDraft must be implemented') }
  async resetWorkspaceDraft(_options) { throw new Error('SupplierRepository.resetWorkspaceDraft must be implemented') }
  async uploadEvidence(_file, _options) { throw new Error('SupplierRepository.uploadEvidence must be implemented') }
  async submitWorkspace(_payload, _options) { throw new Error('SupplierRepository.submitWorkspace must be implemented') }

  async search(_filters) {
    throw new Error('SupplierRepository.search must be implemented')
  }

  async getById(_supplierId) {
    throw new Error('SupplierRepository.getById must be implemented')
  }

  async getVerificationQueue() {
    throw new Error('SupplierRepository.getVerificationQueue must be implemented')
  }

  async getVerificationTask(_taskId) {
    throw new Error('SupplierRepository.getVerificationTask must be implemented')
  }
}
