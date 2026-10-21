import { createContext, useContext } from 'react'

export const WorkspaceContext = createContext(null)

export function useSupplierWorkspace() {
  const workspace = useContext(WorkspaceContext)
  if (!workspace) throw new Error('useSupplierWorkspace requires a WorkspaceProvider')
  return workspace
}
