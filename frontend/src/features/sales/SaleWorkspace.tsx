import { FormEvent, useEffect, useMemo, useRef, useState } from 'react'
import { findProductByCode, searchProductsByName, type SaleProduct } from '../catalog/api'
import { ApiError } from '../../shared/api/client'
import { cancelSale, confirmSale, listRecentSales, type PaymentMethod, type Sale } from './api'

type CartLine = SaleProduct & { quantity: number }

export function SaleWorkspace({ role }: { role: 'ADMIN' | 'SELLER' }) {
  const [query, setQuery] = useState('')
  const [results, setResults] = useState<SaleProduct[]>([])
  const [cart, setCart] = useState<CartLine[]>([])
  const [method, setMethod] = useState<PaymentMethod>('CASH')
  const [cashReceived, setCashReceived] = useState('')
  const [confirmed, setConfirmed] = useState<Sale | null>(null)
  const [history, setHistory] = useState<Sale[]>([])
  const [selectedHistory, setSelectedHistory] = useState<Sale | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const pendingIntent = useRef<{ fingerprint: string; key: string } | null>(null)

  const total = useMemo(
    () => cart.reduce((sum, line) => sum + line.salePrice * line.quantity, 0),
    [cart],
  )
  const received = cashReceived === '' ? 0 : Number(cashReceived)
  const cashPayable = method === 'CASH' ? cashPayableFor(total) : total
  const roundingAdjustment = cashPayable - total
  const change = method === 'CASH' ? Math.max(0, received - cashPayable) : 0

  async function loadHistory() {
    if (role !== 'ADMIN') return
    try {
      setHistory((await listRecentSales()).items)
    } catch (reason) {
      setError(messageFrom(reason))
    }
  }

  async function cancelHistorySale(
    saleId: string,
    input: { refundMethod: PaymentMethod; reason: string },
  ) {
    clearMessages()
    setLoading(true)
    try {
      const updated = await cancelSale(saleId, input)
      setHistory((current) => current.map((sale) => (sale.id === updated.id ? updated : sale)))
      setSelectedHistory(updated)
      setSuccess(`Venta ${updated.folio} anulada correctamente.`)
    } catch (reason) {
      setError(messageFrom(reason))
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    if (role !== 'ADMIN') return
    void listRecentSales()
      .then((page) => setHistory(page.items))
      .catch((reason: unknown) => setError(messageFrom(reason)))
  }, [role])

  async function search(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const normalized = query.trim()
    if (!normalized) return
    setLoading(true)
    clearMessages()
    try {
      const exact = await findProductByCode(normalized)
      addProduct(exact)
      setResults([])
      setQuery('')
    } catch (reason) {
      if (!(reason instanceof ApiError) || (reason.status !== 400 && reason.status !== 404)) {
        setError(messageFrom(reason))
        setLoading(false)
        return
      }
      try {
        const matches = await searchProductsByName(normalized)
        setResults(matches.items)
        if (matches.items.length === 0) setError('No se encontraron productos activos.')
      } catch (fallbackReason) {
        setError(messageFrom(fallbackReason))
      }
    } finally {
      setLoading(false)
    }
  }

  function addProduct(product: SaleProduct) {
    clearMessages()
    if (!product.available || product.currentStock < 1) {
      setError(`${product.name} no tiene stock disponible.`)
      return
    }
    setCart((current) => {
      const existing = current.find((line) => line.id === product.id)
      if (!existing) return [...current, { ...product, quantity: 1 }]
      if (existing.quantity >= product.currentStock) {
        setError(`Solo hay ${product.currentStock} unidades disponibles de ${product.name}.`)
        return current
      }
      return current.map((line) =>
        line.id === product.id ? { ...line, quantity: line.quantity + 1 } : line,
      )
    })
  }

  function updateQuantity(productId: string, value: string) {
    const quantity = Number(value)
    setCart((current) =>
      current.map((line) => {
        if (line.id !== productId) return line
        if (!Number.isInteger(quantity) || quantity < 1 || quantity > line.currentStock) {
          setError(`La cantidad de ${line.name} debe estar entre 1 y ${line.currentStock}.`)
          return line
        }
        clearMessages()
        return { ...line, quantity }
      }),
    )
  }

  async function submitSale(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    clearMessages()
    if (cart.length === 0) {
      setError('Agrega al menos un producto antes de cobrar.')
      return
    }
    if (method === 'CASH' && (!Number.isSafeInteger(received) || received < cashPayable)) {
      setError(
        'El efectivo recibido debe ser un monto entero igual o superior al total efectivo redondeado.',
      )
      return
    }

    const lines = cart.map((line) => ({ productId: line.id, quantity: line.quantity }))
    const fingerprint = JSON.stringify({
      lines,
      method,
      received: method === 'CASH' ? received : null,
    })
    if (pendingIntent.current?.fingerprint !== fingerprint) {
      pendingIntent.current = { fingerprint, key: crypto.randomUUID() }
    }

    setLoading(true)
    try {
      const sale = await confirmSale({
        idempotencyKey: pendingIntent.current.key,
        lines,
        payment: {
          method,
          ...(method === 'CASH' ? { cashReceived: received } : {}),
        },
      })
      setConfirmed(sale)
      setCart([])
      setResults([])
      setQuery('')
      setCashReceived('')
      pendingIntent.current = null
      setSuccess(`Venta ${sale.folio} registrada correctamente.`)
      await loadHistory()
    } catch (reason) {
      setError(messageFrom(reason))
    } finally {
      setLoading(false)
    }
  }

  function cancelCart() {
    setCart([])
    setResults([])
    setCashReceived('')
    pendingIntent.current = null
    clearMessages()
  }

  function clearMessages() {
    setError('')
    setSuccess('')
  }

  return (
    <section className="sales-workspace" aria-labelledby="sale-workspace-title">
      <div className="workspace-intro">
        <p className="eyebrow">Venta manual</p>
        <h2 id="sale-workspace-title">Registrar una venta</h2>
        <p>Busca productos, arma la compra y cobra. El backend confirma stock, precio y caja.</p>
      </div>

      {confirmed && <SaleReceipt sale={confirmed} onNewSale={() => setConfirmed(null)} />}

      <div className="sale-layout">
        <section className="catalog-card sale-product-picker" aria-labelledby="sale-search-title">
          <p className="eyebrow">1 · Productos</p>
          <h3 id="sale-search-title">Buscar por código o nombre</h3>
          <form onSubmit={(event) => void search(event)} className="sale-search-form">
            <label htmlFor="sale-search">Código exacto o parte del nombre</label>
            <div className="form-row">
              <input
                id="sale-search"
                value={query}
                onChange={(event) => setQuery(event.target.value)}
                maxLength={150}
                autoFocus
                required
              />
              <button type="submit" disabled={loading}>
                Buscar
              </button>
            </div>
          </form>
          {results.length > 0 && (
            <div className="product-results" aria-live="polite">
              {results.map((product) => (
                <article className="product-result" key={product.id}>
                  <div>
                    <h3>{product.name}</h3>
                    <p>
                      {product.code ?? 'Sin código'} · Stock: {product.currentStock}
                    </p>
                  </div>
                  <div className="product-numbers">
                    <strong>{formatMoney(product.salePrice)}</strong>
                    <button
                      type="button"
                      className="compact-button"
                      disabled={!product.available}
                      onClick={() => addProduct(product)}
                    >
                      Agregar
                    </button>
                  </div>
                </article>
              ))}
            </div>
          )}
        </section>

        <section className="catalog-card sale-cart" aria-labelledby="sale-cart-title">
          <p className="eyebrow">2 · Compra</p>
          <h3 id="sale-cart-title">Productos agregados</h3>
          {cart.length === 0 ? (
            <p className="sale-empty-cart">Todavía no agregaste productos.</p>
          ) : (
            <div className="table-scroll">
              <table>
                <thead>
                  <tr>
                    <th>Producto</th>
                    <th>Cantidad</th>
                    <th>Precio</th>
                    <th>Subtotal</th>
                    <th aria-label="Acciones" />
                  </tr>
                </thead>
                <tbody>
                  {cart.map((line) => (
                    <tr key={line.id}>
                      <td>{line.name}</td>
                      <td>
                        <input
                          className="quantity-input"
                          aria-label={`Cantidad de ${line.name}`}
                          type="number"
                          min="1"
                          max={line.currentStock}
                          value={line.quantity}
                          onChange={(event) => updateQuantity(line.id, event.target.value)}
                        />
                      </td>
                      <td>{formatMoney(line.salePrice)}</td>
                      <td>{formatMoney(line.salePrice * line.quantity)}</td>
                      <td>
                        <button
                          type="button"
                          className="compact-button danger-button"
                          onClick={() =>
                            setCart((current) => current.filter((item) => item.id !== line.id))
                          }
                        >
                          Quitar
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
          <div className="sale-total">
            <span>Total</span>
            <strong>{formatMoney(total)}</strong>
          </div>
        </section>
      </div>

      <section className="cash-card sale-payment" aria-labelledby="sale-payment-title">
        <p className="eyebrow">3 · Cobro</p>
        <h3 id="sale-payment-title">Confirmar pago</h3>
        <form onSubmit={(event) => void submitSale(event)}>
          <div className="field-grid">
            <label>
              Método de pago
              <select
                value={method}
                onChange={(event) => {
                  setMethod(event.target.value as PaymentMethod)
                  setCashReceived('')
                  pendingIntent.current = null
                }}
              >
                <option value="CASH">Efectivo</option>
                <option value="CARD">Tarjeta</option>
                <option value="TRANSFER">Transferencia</option>
              </select>
            </label>
            {method === 'CASH' && (
              <label>
                Paga con
                <input
                  aria-label="Efectivo recibido"
                  type="number"
                  min="0"
                  step="1"
                  value={cashReceived}
                  onChange={(event) => setCashReceived(event.target.value)}
                  required
                />
              </label>
            )}
          </div>
          <div className="payment-summary">
            <div>
              <span>Total de la venta</span>
              <strong>{formatMoney(total)}</strong>
            </div>
            {method === 'CASH' && (
              <>
                <div>
                  <span>Redondeo efectivo</span>
                  <strong>{formatSignedMoney(roundingAdjustment)}</strong>
                </div>
                <div>
                  <span>Total efectivo</span>
                  <strong>{formatMoney(cashPayable)}</strong>
                </div>
                <div>
                  <span>Vuelto</span>
                  <strong>{formatMoney(change)}</strong>
                </div>
              </>
            )}
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
          <div className="sale-actions">
            <button type="submit" disabled={loading || cart.length === 0}>
              {loading ? 'Confirmando…' : 'Confirmar venta'}
            </button>
            <button
              type="button"
              className="secondary-button"
              disabled={cart.length === 0}
              onClick={cancelCart}
            >
              Cancelar compra
            </button>
          </div>
        </form>
      </section>

      {role === 'ADMIN' && (
        <SaleHistory
          sales={history}
          selected={selectedHistory}
          onSelect={setSelectedHistory}
          onRefresh={() => void loadHistory()}
          onCancel={cancelHistorySale}
          loading={loading}
        />
      )}
    </section>
  )
}

function SaleReceipt({ sale, onNewSale }: { sale: Sale; onNewSale: () => void }) {
  return (
    <section className="sale-receipt" aria-labelledby="receipt-title">
      <p className="eyebrow">Venta confirmada</p>
      <div className="section-heading">
        <div>
          <h3 id="receipt-title">Comprobante {sale.folio}</h3>
          <p>
            {formatDate(sale.confirmedAt)} · {paymentLabel(sale.payment.method)}
          </p>
        </div>
        <button type="button" onClick={onNewSale}>
          Nueva venta
        </button>
      </div>
      <ReceiptDetail sale={sale} />
      <p className="receipt-notice">{sale.notice}</p>
    </section>
  )
}

function SaleHistory({
  sales,
  selected,
  onSelect,
  onRefresh,
  onCancel,
  loading,
}: {
  sales: Sale[]
  selected: Sale | null
  onSelect: (sale: Sale | null) => void
  onRefresh: () => void
  onCancel: (
    saleId: string,
    input: { refundMethod: PaymentMethod; reason: string },
  ) => Promise<void>
  loading: boolean
}) {
  return (
    <section className="catalog-card sale-history" aria-labelledby="sale-history-title">
      <div className="section-heading">
        <div>
          <p className="eyebrow">Registro auditable</p>
          <h2 id="sale-history-title">Ventas recientes</h2>
        </div>
        <button type="button" className="secondary-button" onClick={onRefresh}>
          Actualizar
        </button>
      </div>
      {sales.length === 0 ? (
        <p>Aún no hay ventas registradas.</p>
      ) : (
        <div className="table-scroll">
          <table>
            <thead>
              <tr>
                <th>Número</th>
                <th>Fecha</th>
                <th>Productos</th>
                <th>Pago</th>
                <th>Estado</th>
                <th>Total</th>
                <th aria-label="Acciones" />
              </tr>
            </thead>
            <tbody>
              {sales.map((sale) => (
                <tr key={sale.id}>
                  <td>{sale.folio}</td>
                  <td>{formatDate(sale.confirmedAt)}</td>
                  <td>{sale.lines.reduce((sum, line) => sum + line.quantity, 0)}</td>
                  <td>{paymentLabel(sale.payment.method)}</td>
                  <td>{sale.status === 'VOIDED' ? 'Anulada' : 'Confirmada'}</td>
                  <td>{formatMoney(sale.total)}</td>
                  <td>
                    <button
                      type="button"
                      className="compact-button"
                      onClick={() => onSelect(selected?.id === sale.id ? null : sale)}
                    >
                      {selected?.id === sale.id ? 'Ocultar' : 'Ver detalle'}
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      {selected && (
        <div className="history-detail">
          <h3>Detalle {selected.folio}</h3>
          <ReceiptDetail sale={selected} />
          {selected.status === 'CONFIRMED' && (
            <CancellationForm
              key={selected.id}
              sale={selected}
              loading={loading}
              onCancel={onCancel}
            />
          )}
        </div>
      )}
    </section>
  )
}

function CancellationForm({
  sale,
  loading,
  onCancel,
}: {
  sale: Sale
  loading: boolean
  onCancel: (
    saleId: string,
    input: { refundMethod: PaymentMethod; reason: string },
  ) => Promise<void>
}) {
  const [refundMethod, setRefundMethod] = useState<PaymentMethod>(sale.payment.method)
  const [reason, setReason] = useState('')
  const [armed, setArmed] = useState(false)

  if (!armed) {
    return (
      <div className="cancellation-panel">
        <button type="button" className="danger-button" onClick={() => setArmed(true)}>
          Anular venta
        </button>
      </div>
    )
  }

  return (
    <form
      className="cancellation-panel"
      onSubmit={(event) => {
        event.preventDefault()
        void onCancel(sale.id, { refundMethod, reason })
      }}
    >
      <h4>Confirmar anulación</h4>
      <div className="field-grid">
        <label>
          Método de devolución
          <select
            value={refundMethod}
            onChange={(event) => setRefundMethod(event.target.value as PaymentMethod)}
          >
            <option value="CASH">Efectivo</option>
            <option value="CARD">Tarjeta</option>
            <option value="TRANSFER">Transferencia</option>
          </select>
        </label>
        <label>
          Motivo
          <input
            value={reason}
            onChange={(event) => setReason(event.target.value)}
            maxLength={500}
            required
          />
        </label>
      </div>
      <p>Se repondrá el inventario y la venta no podrá reactivarse.</p>
      <div className="sale-actions">
        <button type="submit" className="danger-button" disabled={loading}>
          {loading ? 'Anulando…' : 'Confirmar anulación'}
        </button>
        <button type="button" className="secondary-button" onClick={() => setArmed(false)}>
          Volver
        </button>
      </div>
    </form>
  )
}

function ReceiptDetail({ sale }: { sale: Sale }) {
  return (
    <>
      <div className="table-scroll">
        <table>
          <thead>
            <tr>
              <th>Producto</th>
              <th>Cantidad</th>
              <th>Precio</th>
              <th>Subtotal</th>
            </tr>
          </thead>
          <tbody>
            {sale.lines.map((line) => (
              <tr key={line.productId}>
                <td>
                  {line.productName}
                  <small>{line.productCode ? ` · ${line.productCode}` : ''}</small>
                </td>
                <td>{line.quantity}</td>
                <td>{formatMoney(line.unitSalePrice)}</td>
                <td>{formatMoney(line.subtotal)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <div className="receipt-totals">
        <p>
          Total de la venta: <strong>{formatMoney(sale.total)}</strong>
        </p>
        {sale.payment.method === 'CASH' && (
          <>
            <p>
              Redondeo efectivo:{' '}
              <strong>{formatSignedMoney(sale.payment.roundingAdjustment ?? 0)}</strong> · Total
              efectivo: <strong>{formatMoney(sale.payment.cashPayable ?? sale.total)}</strong>
            </p>
            <p>
              Recibido: <strong>{formatMoney(sale.payment.cashReceived ?? 0)}</strong> · Vuelto:{' '}
              <strong>{formatMoney(sale.payment.change ?? 0)}</strong>
            </p>
          </>
        )}
        {sale.cancellation && (
          <div className="cancellation-summary">
            <strong>Venta anulada</strong>
            <p>
              {formatDate(sale.cancellation.occurredAt)} ·{' '}
              {paymentLabel(sale.cancellation.refundMethod)} · {sale.cancellation.reason}
            </p>
            <p>
              Devolución exacta: {formatMoney(sale.cancellation.amount)}
              {sale.cancellation.refundMethod === 'CASH' && (
                <>
                  {' '}
                  · Efectivo devuelto:{' '}
                  {formatMoney(sale.cancellation.cashPayable ?? sale.cancellation.amount)}
                </>
              )}
            </p>
          </div>
        )}
      </div>
    </>
  )
}

function paymentLabel(method: PaymentMethod): string {
  return { CASH: 'Efectivo', CARD: 'Tarjeta', TRANSFER: 'Transferencia' }[method]
}

function cashPayableFor(exactTotal: number): number {
  const lastDigit = exactTotal % 10
  if (lastDigit >= 1 && lastDigit <= 5) return exactTotal - lastDigit
  if (lastDigit >= 6 && lastDigit <= 9) return exactTotal + (10 - lastDigit)
  return exactTotal
}

function formatSignedMoney(value: number): string {
  if (value === 0) return formatMoney(0)
  return `${value > 0 ? '+' : '-'}${formatMoney(Math.abs(value))}`
}

function formatMoney(value: number): string {
  return new Intl.NumberFormat('es-CL', {
    style: 'currency',
    currency: 'CLP',
    maximumFractionDigits: 0,
  }).format(value)
}

function formatDate(value: string): string {
  return new Intl.DateTimeFormat('es-CL', {
    dateStyle: 'short',
    timeStyle: 'short',
  }).format(new Date(value))
}

function messageFrom(reason: unknown): string {
  return reason instanceof Error ? reason.message : 'Ocurrió un error inesperado.'
}
