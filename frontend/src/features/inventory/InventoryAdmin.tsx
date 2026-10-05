import { FormEvent, useCallback, useEffect, useState } from 'react'
import {
  getInventorySnapshot,
  listInventoryProducts,
  recordInventoryAdjustment,
  recordInventoryEntry,
  type InventoryMovementType,
  type InventoryProduct,
  type InventorySnapshot,
} from './api'

export function InventoryAdmin({ onOpenCatalog }: { onOpenCatalog?: () => void }) {
  const [products, setProducts] = useState<InventoryProduct[]>([])
  const [productId, setProductId] = useState('')
  const [snapshot, setSnapshot] = useState<InventorySnapshot | null>(null)
  const [loading, setLoading] = useState(true)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')

  const refresh = useCallback(async (selectedProductId: string) => {
    if (!selectedProductId) {
      setSnapshot(null)
      return
    }
    setLoading(true)
    setError('')
    try {
      setSnapshot(await getInventorySnapshot(selectedProductId))
    } catch (reason) {
      setError(messageFrom(reason))
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    let active = true
    void listInventoryProducts()
      .then((result) => {
        if (!active) return
        setProducts(result)
        setProductId(result[0]?.id ?? '')
        if (result.length === 0) setLoading(false)
      })
      .catch((reason: unknown) => {
        if (active) {
          setError(messageFrom(reason))
          setLoading(false)
        }
      })
    return () => {
      active = false
    }
  }, [])

  useEffect(() => {
    if (!productId) return
    let active = true
    void getInventorySnapshot(productId)
      .then((result) => {
        if (active) setSnapshot(result)
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
  }, [productId])

  async function submitEntry(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const form = event.currentTarget
    const data = new FormData(form)
    const reason = fieldValue(data, 'entryReason')
    const reference = fieldValue(data, 'entryReference')
    if (!reason.trim() && !reference.trim()) {
      setError('La entrada requiere un motivo o una referencia.')
      return
    }
    await submitMovement(
      () => recordInventoryEntry(productId, numberValue(data, 'entryQuantity'), reason, reference),
      'Entrada registrada.',
      form,
    )
  }

  async function submitAdjustment(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const form = event.currentTarget
    const data = new FormData(form)
    await submitMovement(
      () =>
        recordInventoryAdjustment(
          productId,
          fieldValue(data, 'adjustmentDirection') as 'POSITIVE' | 'NEGATIVE',
          numberValue(data, 'adjustmentQuantity'),
          fieldValue(data, 'adjustmentReason'),
        ),
      'Ajuste registrado.',
      form,
    )
  }

  async function submitPhysicalCount(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const form = event.currentTarget
    const data = new FormData(form)
    if (!snapshot) return

    const countedBalance = numberValue(data, 'countedBalance')
    const difference = countedBalance - snapshot.currentBalance
    if (Math.abs(difference) > 1_000_000) {
      setSuccess('')
      setError('La diferencia del conteo no puede superar 1.000.000 de unidades.')
      return
    }
    if (difference === 0) {
      setError('')
      setSuccess('El saldo ya coincide con el conteo f\u00edsico.')
      return
    }

    await submitMovement(
      () =>
        recordInventoryAdjustment(
          productId,
          difference > 0 ? 'POSITIVE' : 'NEGATIVE',
          Math.abs(difference),
          fieldValue(data, 'physicalCountReason'),
        ),
      'Inventario actualizado seg\u00fan el conteo f\u00edsico.',
      form,
    )
  }

  async function submitMovement(
    action: () => Promise<unknown>,
    message: string,
    form: HTMLFormElement,
  ) {
    setSubmitting(true)
    setError('')
    setSuccess('')
    try {
      await action()
      form.reset()
      setSuccess(message)
      await refresh(productId)
    } catch (reason) {
      setError(messageFrom(reason))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <section className="catalog-card inventory-workspace" aria-labelledby="inventory-admin-title">
      <div className="section-heading">
        <div>
          <p className="eyebrow">Administración</p>
          <h2 id="inventory-admin-title">Actualizar inventario</h2>
          <p>
            Registra mercader&iacute;a recibida o corrige el saldo a partir de un conteo
            f&iacute;sico.
          </p>
        </div>
        <label>
          Producto
          <select
            aria-label="Producto de inventario"
            value={productId}
            onChange={(event) => {
              setSuccess('')
              setError('')
              setLoading(true)
              setProductId(event.target.value)
            }}
          >
            {products.length === 0 && <option value="">No hay productos</option>}
            {products.map((product) => (
              <option key={product.id} value={product.id}>
                {product.name} · {product.code ?? 'Sin código'}
                {product.status === 'INACTIVE' ? ' · Inactivo' : ''}
              </option>
            ))}
          </select>
        </label>
      </div>

      {error && (
        <p className="error-message" role="alert">
          {error}
        </p>
      )}
      {success && (
        <p className="success-message" role="status">
          {success}
        </p>
      )}

      {!loading && products.length === 0 && (
        <div className="inventory-empty-state">
          <h3>Primero crea un producto</h3>
          <p>
            Cada producto comienza con stock cero. Despu&eacute;s podr&aacute;s registrar
            aqu&iacute; su primera entrada.
          </p>
          {onOpenCatalog && (
            <button type="button" onClick={onOpenCatalog}>
              Ir a Productos
            </button>
          )}
        </div>
      )}

      {snapshot && (
        <>
          <div className="inventory-summary">
            <div>
              <span>Saldo actual</span>
              <strong>{snapshot.currentBalance}</strong>
            </div>
            <p>
              {snapshot.productName} · versión {snapshot.version}
            </p>
          </div>

          <div className="inventory-forms">
            <form onSubmit={(event) => void submitEntry(event)}>
              <h3>Registrar entrada</h3>
              <label>
                Cantidad
                <input
                  aria-label="Cantidad de entrada"
                  name="entryQuantity"
                  type="number"
                  min="1"
                  max="1000000"
                  required
                />
              </label>
              <label>
                Motivo
                <input name="entryReason" maxLength={500} />
              </label>
              <label>
                Referencia
                <input name="entryReference" maxLength={100} />
              </label>
              <p className="form-help">Indica al menos un motivo o una referencia.</p>
              <button type="submit" disabled={submitting || !productId}>
                Registrar entrada
              </button>
            </form>

            <form onSubmit={(event) => void submitAdjustment(event)}>
              <h3>Registrar ajuste</h3>
              <label>
                Dirección
                <select name="adjustmentDirection" defaultValue="POSITIVE" required>
                  <option value="POSITIVE">Aumentar saldo</option>
                  <option value="NEGATIVE">Disminuir saldo</option>
                </select>
              </label>
              <label>
                Cantidad
                <input
                  aria-label="Cantidad del ajuste"
                  name="adjustmentQuantity"
                  type="number"
                  min="1"
                  max="1000000"
                  required
                />
              </label>
              <label>
                Motivo obligatorio
                <input name="adjustmentReason" maxLength={500} required />
              </label>
              <button type="submit" disabled={submitting || !productId}>
                Registrar ajuste
              </button>
            </form>

            <form onSubmit={(event) => void submitPhysicalCount(event)}>
              <h3>Corregir por conteo f&iacute;sico</h3>
              <p className="form-help">
                Escribe cu&aacute;ntas unidades existen realmente. El sistema registrar&aacute;
                solamente la diferencia.
              </p>
              <label>
                Stock contado
                <input
                  aria-label="Stock contado"
                  name="countedBalance"
                  type="number"
                  min="0"
                  max="100000000"
                  required
                />
              </label>
              <label>
                Motivo del conteo
                <input
                  aria-label="Motivo del conteo fisico"
                  name="physicalCountReason"
                  maxLength={500}
                  required
                />
              </label>
              <button type="submit" disabled={submitting || !productId}>
                Actualizar al stock contado
              </button>
            </form>
          </div>

          <h3>Historial inmutable</h3>
          {snapshot.movements.items.length === 0 ? (
            <p>No hay movimientos registrados.</p>
          ) : (
            <div className="table-scroll">
              <table>
                <thead>
                  <tr>
                    <th>Fecha</th>
                    <th>Tipo</th>
                    <th>Cambio</th>
                    <th>Saldo</th>
                    <th>Evidencia</th>
                    <th>Responsable</th>
                  </tr>
                </thead>
                <tbody>
                  {snapshot.movements.items.map((movement) => (
                    <tr key={movement.id}>
                      <td>{formatDate(movement.occurredAt)}</td>
                      <td>{movementLabel(movement.type)}</td>
                      <td className={movement.delta > 0 ? 'positive-delta' : 'negative-delta'}>
                        {movement.delta > 0 ? '+' : ''}
                        {movement.delta}
                      </td>
                      <td>
                        {movement.previousBalance} → {movement.resultingBalance}
                      </td>
                      <td>{movement.reason ?? movement.reference ?? '—'}</td>
                      <td>{movement.actorDisplayName}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </>
      )}
      {loading && <p>Cargando inventario…</p>}
    </section>
  )
}

function movementLabel(type: InventoryMovementType): string {
  const labels: Record<InventoryMovementType, string> = {
    ENTRY: 'Entrada',
    SALE_OUT: 'Venta',
    ADJUSTMENT_IN: 'Ajuste positivo',
    ADJUSTMENT_OUT: 'Ajuste negativo',
    DAMAGED: 'Dañado',
    EXPIRED: 'Vencido',
    SALE_REVERSAL: 'Reverso de venta',
  }
  return labels[type]
}

function formatDate(value: string): string {
  return new Intl.DateTimeFormat('es-CL', { dateStyle: 'short', timeStyle: 'short' }).format(
    new Date(value),
  )
}

function fieldValue(data: FormData, name: string): string {
  const value = data.get(name)
  return typeof value === 'string' ? value : ''
}

function numberValue(data: FormData, name: string): number {
  return Number(fieldValue(data, name))
}

function messageFrom(reason: unknown): string {
  return reason instanceof Error ? reason.message : 'Ocurrió un error inesperado.'
}
