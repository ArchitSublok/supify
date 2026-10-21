import { act, fireEvent, render, renderHook, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { createMemoryRouter, Link, Navigate, Outlet, RouterProvider } from 'react-router-dom'
import { WorkspaceLayout } from './WorkspaceLayout'
import { WorkspaceDashboard } from './WorkspaceDashboard'
import { OnboardingScreen } from './OnboardingScreen'
import { OnboardingStep } from './OnboardingStep'
import { PortfolioScreen, PortfolioAbout, PortfolioCapabilities, PortfolioEvidence } from './PortfolioScreen'
import { WorkspaceProvider } from '../model/WorkspaceContext'
import { useSupplierWorkspace } from '../model/useSupplierWorkspace.js'
import { defaultValues } from '../model/fields.js'
import { createWorkspaceRepository, DRAFT_KEY } from '../../../shared/api/workspaceRepository.js'
import App from '../../../app/App'

function renderWorkspace(repository, initial = '/supplier/onboarding/organisation') {
  const router = createMemoryRouter([{ element: <><Link to="/outside">Leave workspace</Link><Outlet /></>, children: [
    { path: '/outside', element: <h1>Outside workspace</h1> },
    { path: '/supplier', element: <WorkspaceLayout repository={repository} />, children: [
      { index: true, element: <WorkspaceDashboard /> },
      { path: 'onboarding', element: <OnboardingScreen />, children: [
        { index: true, element: <Navigate to="organisation" replace /> },
        { path: ':step', element: <OnboardingStep /> },
      ] },
      { path: 'portfolio', element: <PortfolioScreen />, children: [
        { index: true, element: <PortfolioAbout /> },
        { path: 'capabilities', element: <PortfolioCapabilities /> },
        { path: 'evidence', element: <PortfolioEvidence /> },
      ] },
    ] },
  ] }], { initialEntries: [initial] })
  return { router, ...render(<RouterProvider router={router} />) }
}

async function savedRepository(values = defaultValues) {
  const repository = createWorkspaceRepository({ latency: 0 })
  await repository.saveWorkspaceDraft({ values, attachments: [], receipt: null }, { revision: 0 })
  return repository
}

describe('routed workspace', () => {
  it('validates stepper navigation, focuses errors, and supports Enter submission', async () => {
    const user = userEvent.setup()
    const { router } = renderWorkspace(await savedRepository())
    let input = await screen.findByLabelText(/Trade \/ Display Name/)
    await user.clear(input)
    await user.click(screen.getByRole('link', { name: 'Step 4: Review' }))
    await waitFor(() => expect(router.state.location.pathname).toBe('/supplier/onboarding/organisation'))
    input = screen.getByLabelText(/Trade \/ Display Name/)
    expect(input).toHaveFocus()
    expect(input).toHaveAttribute('aria-invalid', 'true')
    expect(screen.getByRole('alert')).toHaveTextContent('Trade / Display Name is required')
    await user.type(input, 'Acme Manufacturing')
    expect(input).toHaveAttribute('aria-invalid', 'false')
    await user.type(input, '{Enter}')
    await screen.findByRole('heading', { name: 'Describe what you can supply' })
    expect(router.state.location.pathname).toBe('/supplier/onboarding/capabilities')
    await user.click(screen.getByRole('button', { name: 'Back', exact: true }))
    expect(await screen.findByLabelText(/Trade \/ Display Name/)).toHaveValue('Acme Manufacturing')
  })

  it('guards deep links and restores incomplete drafts without losing fields', async () => {
    const { router } = renderWorkspace(await savedRepository({ ...defaultValues, legalName: '', tradeName: 'Saved company' }), '/supplier/onboarding/review')
    await screen.findByLabelText('Full Legal Name *')
    expect(router.state.location.pathname).toBe('/supplier/onboarding/organisation')
    expect(screen.getByLabelText(/Trade \/ Display Name/)).toHaveValue('Saved company')
  })

  it('shares edits across portfolio routes and keeps checklist state across navigation', async () => {
    const user = userEvent.setup()
    renderWorkspace(await savedRepository())
    const input = await screen.findByLabelText(/Trade \/ Display Name/)
    fireEvent.change(input, { target: { value: 'Acme Precision' } })
    await user.click(screen.getByRole('link', { name: /Portfolio preview/ }))
    expect(await screen.findByRole('heading', { name: 'Acme Precision' })).toBeVisible()
    await user.click(screen.getByRole('link', { name: 'Capabilities', exact: true }))
    expect(await screen.findByRole('heading', { name: 'Manufacturing capabilities' })).toBeVisible()
    await user.click(screen.getByRole('link', { name: /Overview/ }))
    await user.type(screen.getByLabelText('Add a preparation task'), 'Request a current certificate')
    await user.click(screen.getByRole('button', { name: 'Add task' }))
    await user.click(screen.getByRole('checkbox', { name: 'Request a current certificate' }))
    await user.click(screen.getByRole('button', { name: 'Done', exact: true }))
    expect(screen.getByRole('checkbox', { name: 'Request a current certificate' })).toBeChecked()
    await user.click(screen.getByRole('link', { name: /Portfolio preview/ }))
    await user.click(screen.getByRole('link', { name: /Overview/ }))
    expect(screen.getByRole('checkbox', { name: 'Request a current certificate' })).toBeChecked()
  })

  it('shows loading and a recoverable load failure, and backs up a damaged draft on reset', async () => {
    const repository = createWorkspaceRepository({ latency: 10 })
    localStorage.setItem(DRAFT_KEY, 'broken-json')
    renderWorkspace(repository)
    expect(screen.getByRole('status', { name: 'Loading supplier workspace' })).toBeVisible()
    expect(await screen.findByRole('heading', { name: "We couldn't restore your draft" })).toBeVisible()
    expect(localStorage.getItem(DRAFT_KEY)).toBe('broken-json')
    await userEvent.click(screen.getByRole('button', { name: 'Back up and start fresh' }))
    expect(await screen.findByLabelText(/Trade \/ Display Name/)).toHaveValue(defaultValues.tradeName)
    expect(Object.keys(localStorage).some((key) => key.startsWith(`${DRAFT_KEY}_backup_`))).toBe(true)
  })

  it('keeps edited values on save failure and can retry saving before leaving', async () => {
    const repository = await savedRepository()
    repository.configureDemo({ failNext: 'save' })
    const { router } = renderWorkspace(repository)
    const input = await screen.findByLabelText(/Trade \/ Display Name/)
    fireEvent.change(input, { target: { value: 'Still here' } })
    await userEvent.click(screen.getByRole('button', { name: 'Save draft', exact: true }))
    expect(await screen.findByText(/Demo save failed/)).toBeVisible()
    expect(input).toHaveValue('Still here')
    await userEvent.click(screen.getByRole('link', { name: 'Leave workspace' }))
    const dialog = screen.getByRole('dialog')
    expect(dialog).toBeVisible()
    await userEvent.click(within(dialog).getByRole('button', { name: 'Save and leave' }))
    await screen.findByRole('heading', { name: 'Outside workspace' })
    expect(router.state.location.pathname).toBe('/outside')
    expect((await repository.getWorkspaceDraft()).values.tradeName).toBe('Still here')
  })

  it('retries failed uploads and prevents submission until incomplete evidence is removed', async () => {
    const repository = await savedRepository()
    repository.configureDemo({ failNext: 'upload' })
    const user = userEvent.setup({ applyAccept: false })
    renderWorkspace(repository, '/supplier/onboarding/claims')
    const picker = await screen.findByLabelText('Select evidence files (optional)')
    await user.upload(picker, new File(['certificate'], 'quality.pdf', { type: 'application/pdf' }))
    expect(await screen.findByText(/Demo upload failed/)).toBeVisible()
    await user.click(screen.getByRole('button', { name: 'Retry quality.pdf' }))
    expect(await screen.findByText('Demo evidence attached')).toBeVisible()
    await user.upload(picker, new File(['wrong file'], 'notes.txt', { type: 'text/plain' }))
    expect(await screen.findByText('Choose a PDF, PNG, or JPEG file.')).toBeVisible()
    await user.click(screen.getByRole('link', { name: 'Step 4: Review' }))
    expect(await screen.findByRole('button', { name: 'Submit demo listing' })).toBeDisabled()
    await user.click(screen.getByRole('button', { name: 'Edit claims' }))
    await user.click(screen.getByRole('button', { name: 'Remove notes.txt' }))
    await user.click(screen.getByRole('link', { name: 'Step 4: Review' }))
    expect(await screen.findByRole('button', { name: 'Submit demo listing' })).toBeEnabled()
  })

  it('prevents duplicate submits, retains values on failure, and reuses the retry identity', async () => {
    const repository = await savedRepository()
    repository.configureDemo({ failNext: 'submit' })
    const submitSpy = vi.spyOn(repository, 'submitWorkspace')
    renderWorkspace(repository, '/supplier/onboarding/review')
    let button = await screen.findByRole('button', { name: 'Submit demo listing' })
    act(() => { fireEvent.click(button); fireEvent.click(button) })
    expect(await screen.findByText(/Demo submit failed/)).toBeVisible()
    expect(submitSpy).toHaveBeenCalledTimes(1)
    expect(screen.getByText(defaultValues.legalName)).toBeVisible()
    button = screen.getByRole('button', { name: 'Submit demo listing' })
    await userEvent.click(button)
    expect(await screen.findByRole('heading', { name: 'Demo listing received' })).toBeVisible()
    expect(submitSpy).toHaveBeenCalledTimes(2)
    expect(submitSpy.mock.calls[0][1].idempotencyKey).toBe(submitSpy.mock.calls[1][1].idempotencyKey)
    expect(screen.getByRole('button', { name: 'Demo submitted' })).toBeDisabled()
    await waitFor(async () => expect((await repository.getWorkspaceDraft()).receipt?.status).toBe('demo-received'))
  })
})

it('serializes slow saves and coalesces edits made while a save is in flight', async () => {
  let finishFirst
  const repository = {
    getWorkspaceDraft: vi.fn().mockResolvedValue({ values: defaultValues, attachments: [], revision: 0 }),
    saveWorkspaceDraft: vi.fn()
      .mockImplementationOnce(() => new Promise((resolve) => { finishFirst = resolve }))
      .mockImplementation(async (_, { revision }) => ({ revision, savedAt: '2026-10-21T09:00:00Z' })),
  }
  const { result } = renderHook(() => useSupplierWorkspace(), {
    wrapper: ({ children }) => <WorkspaceProvider repository={repository}>{children}</WorkspaceProvider>,
  })
  await waitFor(() => expect(result.current.state.load.status).toBe('success'))
  act(() => result.current.dispatch({ type: 'FIELD_CHANGED', name: 'tradeName', value: 'First edit' }))
  let saving
  act(() => { saving = result.current.saveDraft() })
  act(() => {
    result.current.dispatch({ type: 'FIELD_CHANGED', name: 'tradeName', value: 'Second edit' })
    result.current.dispatch({ type: 'FIELD_CHANGED', name: 'tradeName', value: 'Latest edit' })
  })
  expect(repository.saveWorkspaceDraft).toHaveBeenCalledTimes(1)
  await act(async () => { finishFirst({ revision: 1, savedAt: '2026-10-21T09:00:00Z' }); await saving })
  expect(repository.saveWorkspaceDraft).toHaveBeenCalledTimes(2)
  expect(repository.saveWorkspaceDraft.mock.calls[1][0].values.tradeName).toBe('Latest edit')
  expect(result.current.isDirty).toBe(false)
})

it('cancels pending saves and loads when the provider unmounts', async () => {
  const repository = createWorkspaceRepository({ latency: 0 })
  const saveSpy = vi.spyOn(repository, 'saveWorkspaceDraft')
  const { result, unmount } = renderHook(() => useSupplierWorkspace(), {
    wrapper: ({ children }) => <WorkspaceProvider repository={repository}>{children}</WorkspaceProvider>,
  })
  await waitFor(() => expect(result.current.state.load.status).toBe('success'))
  repository.configureDemo({ delayMs: 1000 })
  let saving
  act(() => { saving = result.current.saveDraft() })
  unmount()
  expect(saveSpy.mock.calls[0][1].signal.aborted).toBe(true)
  expect(await saving).toBe(false)
})

it('the actual App routes render the dashboard and redirect the onboarding alias', async () => {
  const router = createMemoryRouter([{ path: '*', element: <App /> }], { initialEntries: ['/supplier'] })
  render(<RouterProvider router={router} />)
  expect(await screen.findByRole('heading', { name: 'Before you submit' })).toBeVisible()
  await act(async () => { await router.navigate('/supplier/onboarding') })
  expect(await screen.findByLabelText(/Trade \/ Display Name/)).toHaveValue(defaultValues.tradeName)
  expect(router.state.location.pathname).toBe('/supplier/onboarding/organisation')
})
