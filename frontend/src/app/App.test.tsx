import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'

vi.mock('../features/catalog/CategoryAdmin', () => ({
  CategoryAdmin: () => <div>Administrar categorias</div>,
}))
vi.mock('../features/catalog/ProductAdmin', () => ({
  ProductAdmin: () => <div>Administrar productos</div>,
}))
vi.mock('../features/catalog/ProductSearch', () => ({
  ProductSearch: () => <div>Consulta comercial</div>,
}))
vi.mock('../features/inventory/InventoryAdmin', () => ({
  InventoryAdmin: ({ onOpenCatalog }: { onOpenCatalog?: () => void }) => (
    <div>
      Inventario administrativo
      <button type="button" onClick={onOpenCatalog}>
        Crear primer producto
      </button>
    </div>
  ),
}))
vi.mock('../features/cash/CashSessionPanel', () => ({
  CashSessionPanel: () => <div>Sesión de caja funcional</div>,
}))
vi.mock('../features/sales/SaleWorkspace', () => ({
  SaleWorkspace: () => <div>Venta manual funcional</div>,
}))
vi.mock('../features/audit/AuditWorkspace', () => ({
  AuditWorkspace: () => <div>{'Auditor\u00eda administrativa'}</div>,
}))
vi.mock('../features/reports/ReportsWorkspace', () => ({
  ReportsWorkspace: () => <div>Reportes administrativos</div>,
}))

import { App } from './App'

const admin = {
  username: 'admin',
  displayName: 'Administración',
  role: 'ADMIN',
  passwordChangeRequired: false,
}

describe('App', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('allows an internal user to log in with CSRF protection', async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(response({}, 401))
      .mockResolvedValueOnce(response({ headerName: 'X-XSRF-TOKEN', token: 'csrf' }))
      .mockResolvedValueOnce(response(admin))
    vi.stubGlobal('fetch', fetchMock)
    const user = userEvent.setup()
    render(<App />)

    await user.type(await screen.findByLabelText('Usuario'), 'admin')
    await user.type(screen.getByLabelText('Contraseña'), 'Definitiva-admin-2026')
    await user.click(screen.getByRole('button', { name: 'Ingresar' }))

    expect(await screen.findByText('Bienvenido, Administración')).toBeInTheDocument()
    expect(screen.getByText('Venta manual funcional')).toBeInTheDocument()
    expect(screen.queryByText('Sesión de caja funcional')).not.toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Caja' }))
    expect(screen.getByText('Sesión de caja funcional')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Inventario' }))
    expect(screen.getByText('Inventario administrativo')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Crear primer producto' }))
    expect(screen.getByText('Administrar categorias')).toBeInTheDocument()
    expect(screen.getByText('Administrar productos')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Auditor\u00eda' }))
    expect(screen.getByText('Auditor\u00eda administrativa')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Reportes' }))
    expect(screen.getByText('Reportes administrativos')).toBeInTheDocument()
    expect(fetchMock).toHaveBeenNthCalledWith(
      3,
      '/api/v1/auth/login',
      expect.objectContaining({
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'X-XSRF-TOKEN': 'csrf',
        },
      }),
    )
  })

  it('shows a safe authentication error without exposing internals', async () => {
    vi.stubGlobal(
      'fetch',
      vi
        .fn()
        .mockResolvedValueOnce(response({}, 401))
        .mockResolvedValueOnce(response({ headerName: 'X-XSRF-TOKEN', token: 'csrf' }))
        .mockResolvedValueOnce(
          response(
            { code: 'authentication-failed', detail: 'Usuario o contraseña inválidos.' },
            401,
          ),
        ),
    )
    const user = userEvent.setup()
    render(<App />)

    await user.type(await screen.findByLabelText('Usuario'), 'admin')
    await user.type(screen.getByLabelText('Contraseña'), 'incorrecta')
    await user.click(screen.getByRole('button', { name: 'Ingresar' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Usuario o contraseña inválidos.')
  })

  it('forces replacement of the bootstrap password and confirms session revocation', async () => {
    const temporaryAdmin = { ...admin, passwordChangeRequired: true }
    vi.stubGlobal(
      'fetch',
      vi
        .fn()
        .mockResolvedValueOnce(response(temporaryAdmin))
        .mockResolvedValueOnce(response({ headerName: 'X-XSRF-TOKEN', token: 'csrf' }))
        .mockResolvedValueOnce(response(undefined, 204)),
    )
    const user = userEvent.setup()
    render(<App />)

    await user.type(await screen.findByLabelText('Contraseña temporal'), 'Temporal-admin-2026')
    await user.type(screen.getByLabelText('Contraseña nueva'), 'Definitiva-admin-2026')
    await user.click(screen.getByRole('button', { name: 'Guardar contraseña' }))

    expect(await screen.findByText('Contraseña cambiada')).toBeInTheDocument()
    expect(screen.getByText(/La sesión temporal fue cerrada/i)).toBeInTheDocument()
  })

  it('shows the authenticated role and returns to login after logout', async () => {
    vi.stubGlobal(
      'fetch',
      vi
        .fn()
        .mockResolvedValueOnce(response({ ...admin, role: 'SELLER', displayName: 'Vendedor' }))
        .mockResolvedValueOnce(response({ headerName: 'X-XSRF-TOKEN', token: 'csrf' }))
        .mockResolvedValueOnce(response(undefined, 204)),
    )
    const user = userEvent.setup()
    render(<App />)

    expect(await screen.findByText('Rol: Vendedor')).toBeInTheDocument()
    expect(screen.queryByText('Inventario administrativo')).not.toBeInTheDocument()
    expect(screen.getByText('Venta manual funcional')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Cerrar sesión' }))
    expect(await screen.findByRole('heading', { name: 'Iniciar sesión' })).toBeInTheDocument()
  })

  it('offers a retry when the backend is unavailable', async () => {
    const fetchMock = vi
      .fn()
      .mockRejectedValueOnce('sin conexión')
      .mockResolvedValueOnce(response({}, 401))
    vi.stubGlobal('fetch', fetchMock)
    const user = userEvent.setup()
    render(<App />)

    expect(await screen.findByRole('alert')).toHaveTextContent('Ocurrió un error inesperado.')
    await user.click(screen.getByRole('button', { name: 'Reintentar' }))
    expect(await screen.findByRole('heading', { name: 'Iniciar sesión' })).toBeInTheDocument()
    await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(2))
  })
})

function response(body: unknown, status = 200): Response {
  return new Response(status === 204 ? null : JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}
