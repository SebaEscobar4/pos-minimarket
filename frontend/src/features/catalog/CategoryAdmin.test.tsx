import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import {
  createCategory,
  listCategories,
  renameCategory,
  setCategoryStatus,
  type Category,
} from './api'
import { CategoryAdmin } from './CategoryAdmin'

vi.mock('./api', () => ({
  createCategory: vi.fn(),
  listCategories: vi.fn(),
  renameCategory: vi.fn(),
  setCategoryStatus: vi.fn(),
}))

const category: Category = {
  id: 'category-1',
  name: 'Bebidas',
  status: 'ACTIVE',
  createdAt: '2026-08-04T04:00:00Z',
  updatedAt: '2026-08-04T04:00:00Z',
}

describe('CategoryAdmin', () => {
  beforeEach(() => {
    vi.mocked(listCategories).mockReset().mockResolvedValue([category])
    vi.mocked(createCategory).mockReset().mockResolvedValue(category)
    vi.mocked(renameCategory).mockReset().mockResolvedValue(category)
    vi.mocked(setCategoryStatus)
      .mockReset()
      .mockResolvedValue({
        ...category,
        status: 'INACTIVE',
      })
  })

  it('creates, edits, deactivates and filters categories', async () => {
    const user = userEvent.setup()
    render(<CategoryAdmin />)

    expect(await screen.findByText('Bebidas')).toBeInTheDocument()
    const name = screen.getByLabelText('Nombre de la categoría')
    await user.type(name, 'Lácteos')
    await user.click(screen.getByRole('button', { name: 'Crear categoría' }))
    expect(createCategory).toHaveBeenCalledWith('Lácteos')

    await user.click(screen.getByRole('button', { name: 'Editar' }))
    const editedName = screen.getByLabelText('Nuevo nombre de la categoría')
    await user.clear(editedName)
    await user.type(editedName, 'Jugos')
    await user.click(screen.getByRole('button', { name: 'Guardar cambio' }))
    expect(renameCategory).toHaveBeenCalledWith('category-1', 'Jugos')

    await user.click(screen.getByRole('button', { name: 'Desactivar' }))
    expect(setCategoryStatus).toHaveBeenCalledWith('category-1', 'INACTIVE')

    await user.click(screen.getByLabelText('Incluir inactivas'))
    await waitFor(() => expect(listCategories).toHaveBeenLastCalledWith(true))
  })

  it('supports cancelling edits and shows safe request failures', async () => {
    const user = userEvent.setup()
    render(<CategoryAdmin />)

    await screen.findByText('Bebidas')
    await user.click(screen.getByRole('button', { name: 'Editar' }))
    await user.click(screen.getByRole('button', { name: 'Cancelar' }))
    expect(screen.getByLabelText('Nombre de la categoría')).toHaveValue('')

    vi.mocked(createCategory).mockRejectedValueOnce(new Error('Solicitud inválida.'))
    await user.type(screen.getByLabelText('Nombre de la categoría'), 'Nueva')
    await user.click(screen.getByRole('button', { name: 'Crear categoría' }))
    expect(await screen.findByRole('alert')).toHaveTextContent('Solicitud inválida.')
  })
})
