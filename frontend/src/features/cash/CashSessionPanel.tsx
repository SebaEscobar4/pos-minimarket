import { FormEvent, useEffect, useState } from 'react'
import {
  closeCashSession,
  currentCashSession,
  currentCashSummary,
  openCashSession,
  recordManualCashMovement,
  type CashCloseResult,
  type CashMovementCategory,
  type CashSession,
  type CashSummary,
  type ManualCashDirection,
} from './api'

const incomeCategories: Array<[CashMovementCategory, string]> = [
  ['CASH_REPLENISHMENT', 'Refuerzo de efectivo'],
  ['OTHER_INCOME', 'Otro ingreso'],
]

const withdrawalCategories: Array<[CashMovementCategory, string]> = [
  ['SUPPLIER_PAYMENT', 'Pago a proveedor'],
  ['OPERATING_EXPENSE', 'Gasto operativo'],
  ['SAFE_DROP', 'Retiro preventivo'],
  ['OTHER_WITHDRAWAL', 'Otro retiro'],
]

export function CashSessionPanel({ role }: { role: 'ADMIN' | 'SELLER' }) {
  const [session, setSession] = useState<CashSession | null>(null)
  const [summary, setSummary] = useState<CashSummary | null>(null)
  const [closed, setClosed] = useState<CashCloseResult | null>(null)
  const [direction, setDirection] = useState<ManualCashDirection>('INCOME')
  const [loading, setLoading] = useState(true)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')

  useEffect(() => {
    let active = true
    void currentCashSession()
      .then(async (result) => {
        if (!active) return
        setSession(result)
        if (result) setSummary(await currentCashSummary())
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
  }, [])

  async function open(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    await submit(async () => {
      const data = new FormData(event.currentTarget)
      const opened = await openCashSession(Number(fieldValue(data, 'openingAmount')))
      setSession(opened)
      setSummary(await currentCashSummary())
      setSuccess('Caja abierta correctamente.')
    })
  }

  async function recordMovement(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const form = event.currentTarget
    await submit(async () => {
      const data = new FormData(form)
      await recordManualCashMovement(
        direction,
        fieldValue(data, 'category') as CashMovementCategory,
        Number(fieldValue(data, 'amount')),
        fieldValue(data, 'reason'),
      )
      form.reset()
      setSummary(await currentCashSummary())
      setSuccess('Movimiento de caja registrado.')
    })
  }

  async function close(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    await submit(async () => {
      const data = new FormData(event.currentTarget)
      setClosed(await closeCashSession(Number(fieldValue(data, 'countedCash'))))
      setSession(null)
      setSummary(null)
      setSuccess('Caja cerrada correctamente.')
    })
  }

  async function submit(action: () => Promise<void>) {
    setSubmitting(true)
    setError('')
    setSuccess('')
    try {
      await action()
    } catch (reason) {
      setError(messageFrom(reason))
    } finally {
      setSubmitting(false)
    }
  }

  const categories = direction === 'INCOME' ? incomeCategories : withdrawalCategories

  return (
    <section className="cash-card" aria-labelledby="cash-session-title">
      <p className="eyebrow">Caja única</p>
      <h2 id="cash-session-title">Sesión de caja</h2>
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
      {loading ? (
        <p>Consultando caja activa…</p>
      ) : closed ? (
        <ClosureSummary result={closed} onContinue={() => setClosed(null)} />
      ) : session && summary ? (
        <>
          <CashSummaryView summary={summary} />
          <div className="cash-actions-grid">
            {role === 'ADMIN' && (
              <form onSubmit={(event) => void recordMovement(event)}>
                <h3>Movimiento manual</h3>
                <label>
                  Tipo
                  <select
                    aria-label="Tipo de movimiento de caja"
                    value={direction}
                    onChange={(event) => setDirection(event.target.value as ManualCashDirection)}
                  >
                    <option value="INCOME">Ingreso</option>
                    <option value="WITHDRAWAL">Retiro</option>
                  </select>
                </label>
                <label>
                  Categoría
                  <select name="category" required>
                    {categories.map(([value, label]) => (
                      <option key={value} value={value}>
                        {label}
                      </option>
                    ))}
                  </select>
                </label>
                <label>
                  Monto
                  <input name="amount" type="number" min="1" step="1" required />
                </label>
                <label>
                  Motivo obligatorio
                  <input name="reason" maxLength={500} required />
                </label>
                <button type="submit" disabled={submitting}>
                  Registrar movimiento
                </button>
              </form>
            )}
            <form onSubmit={(event) => void close(event)}>
              <h3>Cerrar caja</h3>
              <p>Cuenta el efectivo físico antes de confirmar el cierre.</p>
              <label>
                Efectivo contado
                <input name="countedCash" type="number" min="0" step="1" required />
              </label>
              <button type="submit" disabled={submitting}>
                Cerrar caja
              </button>
            </form>
          </div>
          <MovementTable summary={summary} />
        </>
      ) : (
        <form className="cash-opening-form" onSubmit={(event) => void open(event)}>
          <p>No hay una sesión abierta. Indica el efectivo inicial contado en la caja.</p>
          <label>
            Monto inicial en pesos
            <input
              name="openingAmount"
              type="number"
              min="0"
              max="9007199254740991"
              step="1"
              required
            />
          </label>
          <button type="submit" disabled={submitting}>
            {submitting ? 'Abriendo…' : 'Abrir caja'}
          </button>
        </form>
      )}
    </section>
  )
}

function CashSummaryView({ summary }: { summary: CashSummary }) {
  const values = [
    ['Monto inicial', summary.session.openingAmount],
    ['Ventas en efectivo', summary.totals.cashSales],
    ['Ingresos manuales', summary.totals.manualIncome],
    ['Retiros manuales', -summary.totals.manualWithdrawals],
    ['Devoluciones en efectivo', -summary.totals.cashRefunds],
    ['Ventas con tarjeta', summary.totals.cardSales],
    ['Ventas por transferencia', summary.totals.transferSales],
  ] as const
  return (
    <>
      <div className="cash-session-summary">
        <div>
          <span>Estado</span>
          <strong>Abierta</strong>
        </div>
        <div>
          <span>Efectivo esperado</span>
          <strong>{formatClp(summary.expectedCash)}</strong>
        </div>
        <p>
          Abierta por {summary.session.openedByDisplayName} el{' '}
          {formatDate(summary.session.openedAt)}.
        </p>
      </div>
      <div className="cash-totals-grid">
        {values.map(([label, value]) => (
          <div key={label}>
            <span>{label}</span>
            <strong>{formatClp(value)}</strong>
          </div>
        ))}
      </div>
    </>
  )
}

function MovementTable({ summary }: { summary: CashSummary }) {
  if (summary.movements.items.length === 0) return <p>No hay movimientos de caja registrados.</p>
  return (
    <div className="table-scroll">
      <h3>Movimientos de caja</h3>
      <table>
        <thead>
          <tr>
            <th>Fecha</th>
            <th>Tipo</th>
            <th>Categoría</th>
            <th>Monto</th>
            <th>Motivo</th>
            <th>Responsable</th>
          </tr>
        </thead>
        <tbody>
          {summary.movements.items.map((movement) => (
            <tr key={movement.id}>
              <td>{formatDate(movement.occurredAt)}</td>
              <td>{movement.type === 'MANUAL_INCOME' ? 'Ingreso' : 'Retiro'}</td>
              <td>{categoryLabel(movement.category)}</td>
              <td>{formatClp(movement.amount)}</td>
              <td>{movement.reason ?? movement.reference ?? '—'}</td>
              <td>{movement.actorDisplayName}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}

function ClosureSummary({
  result,
  onContinue,
}: {
  result: CashCloseResult
  onContinue: () => void
}) {
  return (
    <div className="closure-summary">
      <h3>Cierre registrado</h3>
      <p>Efectivo esperado: {formatClp(result.expectedCash)}</p>
      <p>Efectivo contado: {formatClp(result.countedCash)}</p>
      <p>Diferencia conservada: {formatClp(result.difference)}</p>
      <p>
        Cerrada por {result.closedByDisplayName} el {formatDate(result.closedAt)}.
      </p>
      <button type="button" onClick={onContinue}>
        Continuar
      </button>
    </div>
  )
}

function categoryLabel(category: CashMovementCategory | null): string {
  const labels = Object.fromEntries([...incomeCategories, ...withdrawalCategories]) as Record<
    CashMovementCategory,
    string
  >
  return category ? labels[category] : 'Automático'
}

function formatClp(value: number): string {
  return new Intl.NumberFormat('es-CL', {
    style: 'currency',
    currency: 'CLP',
    maximumFractionDigits: 0,
  }).format(value)
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

function messageFrom(reason: unknown): string {
  return reason instanceof Error ? reason.message : 'Ocurrió un error inesperado.'
}
