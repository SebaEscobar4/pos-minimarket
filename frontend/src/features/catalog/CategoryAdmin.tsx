import { FormEvent, useCallback, useEffect, useState } from 'react'
import {
  createCategory,
  listCategories,
  renameCategory,
  setCategoryStatus,
  type Category,
} from './api'

export function CategoryAdmin({ onChanged }: { onChanged?: () => void }) {
  const [categories, setCategories] = useState<Category[]>([])
  const [includeInactive, setIncludeInactive] = useState(false)
  const [editing, setEditing] = useState<Category | null>(null)
  const [name, setName] = useState('')
  const [loading, setLoading] = useState(true)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')

  const refresh = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      setCategories(await listCategories(includeInactive))
    } catch (reason) {
      setError(messageFrom(reason))
    } finally {
      setLoading(false)
    }
  }, [includeInactive])

  useEffect(() => {
    let active = true
    void listCategories(includeInactive)
      .then((result) => {
        if (active) setCategories(result)
      })
      .catch((reason: unknown) => {
        if (active) setError(messageFrom(reason))
      })
      .finally(() => {
        if (active) setLoading(false)
      })
    return () => {
      active = false
    }
  }, [includeInactive])

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setSubmitting(true)
    setError('')
    try {
      if (editing) await renameCategory(editing.id, name)
      else await createCategory(name)
      setEditing(null)
      setName('')
      await refresh()
      onChanged?.()
    } catch (reason) {
      setError(messageFrom(reason))
    } finally {
      setSubmitting(false)
    }
  }

  async function changeStatus(category: Category) {
    setError('')
    try {
      await setCategoryStatus(category.id, category.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE')
      await refresh()
      onChanged?.()
    } catch (reason) {
      setError(messageFrom(reason))
    }
  }

  function startEditing(category: Category) {
    setEditing(category)
    setName(category.name)
  }

  function cancelEditing() {
    setEditing(null)
    setName('')
  }

  return (
    <section className="catalog-card" aria-labelledby="category-title">
      <div className="section-heading">
        <div>
          <p className="eyebrow">Catálogo</p>
          <h2 id="category-title">Categorías</h2>
        </div>
        <label className="inline-control">
          <input
            type="checkbox"
            checked={includeInactive}
            onChange={(event) => {
              setLoading(true)
              setIncludeInactive(event.target.checked)
            }}
          />
          Incluir inactivas
        </label>
      </div>

      <form className="category-form" onSubmit={(event) => void submit(event)}>
        <label htmlFor="category-name">
          {editing ? 'Nuevo nombre de la categoría' : 'Nombre de la categoría'}
        </label>
        <div className="form-row">
          <input
            id="category-name"
            value={name}
            onChange={(event) => setName(event.target.value)}
            maxLength={100}
            required
          />
          <button type="submit" disabled={submitting}>
            {editing ? 'Guardar cambio' : 'Crear categoría'}
          </button>
          {editing && (
            <button type="button" className="secondary-button" onClick={cancelEditing}>
              Cancelar
            </button>
          )}
        </div>
      </form>

      {error && (
        <p className="error-message" role="alert">
          {error}
        </p>
      )}

      {loading ? (
        <p role="status">Cargando categorías…</p>
      ) : categories.length === 0 ? (
        <p>No hay categorías para mostrar.</p>
      ) : (
        <div className="table-scroll">
          <table>
            <thead>
              <tr>
                <th scope="col">Nombre</th>
                <th scope="col">Estado</th>
                <th scope="col">Acciones</th>
              </tr>
            </thead>
            <tbody>
              {categories.map((category) => (
                <tr key={category.id}>
                  <td>{category.name}</td>
                  <td>{category.status === 'ACTIVE' ? 'Activa' : 'Inactiva'}</td>
                  <td className="actions-cell">
                    <button
                      type="button"
                      className="secondary-button compact-button"
                      onClick={() => startEditing(category)}
                    >
                      Editar
                    </button>
                    <button
                      type="button"
                      className="secondary-button compact-button"
                      onClick={() => void changeStatus(category)}
                    >
                      {category.status === 'ACTIVE' ? 'Desactivar' : 'Activar'}
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  )
}

function messageFrom(reason: unknown): string {
  return reason instanceof Error ? reason.message : 'Ocurrió un error inesperado.'
}
