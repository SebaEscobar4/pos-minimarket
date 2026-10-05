import { FormEvent, useEffect, useState } from 'react'
import {
  getCashReport,
  getInventoryReport,
  getSalesReport,
  type CashReport,
  type InventoryReport,
  type PaymentMethod,
  type SalesReport,
} from './api'

type ReportSection = 'SALES' | 'INVENTORY' | 'CASH'
type DateRange = { from: string; to: string }

export function ReportsWorkspace() {
  const initial = initialRange()
  const [draft, setDraft] = useState(initial)
  const [range, setRange] = useState(initial)
  const [revision, setRevision] = useState(0)
  const [section, setSection] = useState<ReportSection>('SALES')
  const [method, setMethod] = useState<PaymentMethod | ''>('')
  const [sales, setSales] = useState<SalesReport | null>(null)
  const [inventory, setInventory] = useState<InventoryReport | null>(null)
  const [cash, setCash] = useState<CashReport | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    let active = true
    const request =
      section === 'SALES'
        ? getSalesReport({ ...range, method: method || undefined }).then((value) => {
            if (active) setSales(value)
          })
        : section === 'INVENTORY'
          ? getInventoryReport({ ...range, page: 0, size: 20 }).then((value) => {
              if (active) setInventory(value)
            })
          : getCashReport({ ...range, page: 0, size: 20 }).then((value) => {
              if (active) setCash(value)
            })
    void request
      .catch((reason: unknown) => {
        if (active) setError(messageFrom(reason))
      })
      .finally(() => {
        if (active) setLoading(false)
      })
    return () => {
      active = false
    }
  }, [method, range, revision, section])

  function apply(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setLoading(true)
    setError('')
    setRange(draft)
    setRevision((value) => value + 1)
  }

  function selectSection(next: ReportSection) {
    if (next === section) return
    setLoading(true)
    setError('')
    setSection(next)
  }

  function selectMethod(next: PaymentMethod | '') {
    if (next === method) return
    setLoading(true)
    setError('')
    setMethod(next)
  }

  return (
    <section className="catalog-card report-workspace" aria-labelledby="reports-title">
      <div className="section-heading">
        <div>
          <p className="eyebrow">Control del negocio</p>
          <h2 id="reports-title">Reportes operacionales</h2>
          <p>Totales conciliables construidos desde ventas, movimientos y cierres registrados.</p>
        </div>
      </div>

      <form className="report-filters" onSubmit={apply}>
        <label htmlFor="report-from">Desde</label>
        <input
          id="report-from"
          type="date"
          value={draft.from}
          onChange={(event) => setDraft({ ...draft, from: event.target.value })}
          required
        />
        <label htmlFor="report-to">Hasta</label>
        <input
          id="report-to"
          type="date"
          value={draft.to}
          onChange={(event) => setDraft({ ...draft, to: event.target.value })}
          required
        />
        <button type="submit">Actualizar reportes</button>
      </form>

      <nav className="report-tabs" aria-label="Tipos de reporte">
        <ReportTab active={section === 'SALES'} onClick={() => selectSection('SALES')}>
          Ventas
        </ReportTab>
        <ReportTab active={section === 'INVENTORY'} onClick={() => selectSection('INVENTORY')}>
          Inventario
        </ReportTab>
        <ReportTab active={section === 'CASH'} onClick={() => selectSection('CASH')}>
          Caja
        </ReportTab>
      </nav>

      {error && (
        <p className="error-message" role="alert">
          {error}
        </p>
      )}
      {loading && <p>{'Cargando reporte\u2026'}</p>}
      {!loading && section === 'SALES' && sales && (
        <SalesView report={sales} method={method} onMethod={selectMethod} />
      )}
      {!loading && section === 'INVENTORY' && inventory && <InventoryView report={inventory} />}
      {!loading && section === 'CASH' && cash && <CashView report={cash} />}
    </section>
  )
}

function ReportTab({
  active,
  onClick,
  children,
}: {
  active: boolean
  onClick: () => void
  children: string
}) {
  return (
    <button
      type="button"
      className={active ? 'workspace-nav-button active' : 'workspace-nav-button'}
      aria-current={active ? 'page' : undefined}
      onClick={onClick}
    >
      {children}
    </button>
  )
}

function SalesView({
  report,
  method,
  onMethod,
}: {
  report: SalesReport
  method: PaymentMethod | ''
  onMethod: (method: PaymentMethod | '') => void
}) {
  return (
    <div className="report-content">
      <label htmlFor="report-method">Medio de pago</label>
      <select
        id="report-method"
        value={method}
        onChange={(event) => onMethod(event.target.value as PaymentMethod | '')}
      >
        <option value="">Todos</option>
        <option value="CASH">Efectivo</option>
        <option value="CARD">Tarjeta</option>
        <option value="TRANSFER">Transferencia</option>
      </select>
      <div className="report-metrics">
        <Metric
          label="Ventas registradas"
          value={`${report.recordedSales} / ${money(report.recordedAmount)}`}
        />
        <Metric
          label="Anulaciones"
          value={`${report.voidedSales} / ${money(report.voidedAmount)}`}
        />
        <Metric label="Venta neta" value={`${report.netSales} / ${money(report.netAmount)}`} />
        <Metric label="Margen bruto estimado" value={money(report.estimatedGrossProfit)} />
      </div>
      <h3>Conciliación por medio de pago</h3>
      <div className="table-scroll">
        <table>
          <thead>
            <tr>
              <th>Medio</th>
              <th>Registrado</th>
              <th>Anulado</th>
              <th>Neto</th>
            </tr>
          </thead>
          <tbody>
            {report.payments.map((row) => (
              <tr key={row.method}>
                <td>{paymentLabel(row.method)}</td>
                <td>
                  {row.recordedSales} / {money(row.recordedAmount)}
                </td>
                <td>
                  {row.voidedSales} / {money(row.voidedAmount)}
                </td>
                <td>
                  {row.netSales} / {money(row.netAmount)}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <h3>Productos más vendidos</h3>
      {report.topProducts.length === 0 ? (
        <p>No hay ventas netas en el período.</p>
      ) : (
        <div className="table-scroll">
          <table>
            <thead>
              <tr>
                <th>Producto</th>
                <th>Código</th>
                <th>Unidades</th>
                <th>Ingreso</th>
              </tr>
            </thead>
            <tbody>
              {report.topProducts.map((row) => (
                <tr key={row.productId}>
                  <td>{row.name}</td>
                  <td>{row.code ?? '\u2014'}</td>
                  <td>{row.quantity}</td>
                  <td>{money(row.revenue)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}

function InventoryView({ report }: { report: InventoryReport }) {
  return (
    <div className="report-content">
      <h3>Stock bajo</h3>
      {report.lowStock.length === 0 ? (
        <p>No hay productos bajo su stock mínimo.</p>
      ) : (
        <div className="table-scroll">
          <table>
            <thead>
              <tr>
                <th>Producto</th>
                <th>Actual</th>
                <th>Mínimo</th>
                <th>Faltante</th>
              </tr>
            </thead>
            <tbody>
              {report.lowStock.map((row) => (
                <tr key={row.productId}>
                  <td>{row.name}</td>
                  <td>{row.quantity}</td>
                  <td>{row.minimumStock}</td>
                  <td>{row.shortage}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      <h3>Resumen de movimientos</h3>
      <div className="report-metrics">
        {report.totals.map((row) => (
          <Metric
            key={row.type}
            label={movementLabel(row.type)}
            value={`${row.movements} mov. / ${signed(row.netDelta)}`}
          />
        ))}
      </div>
      <h3>Libro de inventario</h3>
      <div className="table-scroll">
        <table>
          <thead>
            <tr>
              <th>Fecha</th>
              <th>Producto</th>
              <th>Tipo</th>
              <th>Cambio</th>
              <th>Saldo</th>
              <th>Referencia</th>
            </tr>
          </thead>
          <tbody>
            {report.movements.items.map((row) => (
              <tr key={row.id}>
                <td>{dateTime(row.occurredAt)}</td>
                <td>{row.productName}</td>
                <td>{movementLabel(row.type)}</td>
                <td>{signed(row.delta)}</td>
                <td>
                  {row.previousBalance} → {row.resultingBalance}
                </td>
                <td>{row.reference ?? row.reason ?? '\u2014'}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}

function CashView({ report }: { report: CashReport }) {
  return (
    <div className="report-content">
      <div className="report-metrics">
        <Metric label="Aperturas" value={String(report.openedSessions)} />
        <Metric label="Cierres" value={String(report.closedSessions)} />
        <Metric label="Efectivo esperado" value={money(report.expectedCash)} />
        <Metric label="Diferencia acumulada" value={money(report.difference)} />
      </div>
      <h3>Historial de caja</h3>
      <div className="table-scroll">
        <table>
          <thead>
            <tr>
              <th>Apertura</th>
              <th>Estado</th>
              <th>Monto inicial</th>
              <th>Esperado</th>
              <th>Contado</th>
              <th>Diferencia</th>
            </tr>
          </thead>
          <tbody>
            {report.sessions.items.map((row) => (
              <tr key={row.id}>
                <td>{dateTime(row.openedAt)}</td>
                <td>{row.status === 'OPEN' ? 'Abierta' : 'Cerrada'}</td>
                <td>{money(row.openingAmount)}</td>
                <td>{nullableMoney(row.expectedCash)}</td>
                <td>{nullableMoney(row.countedCash)}</td>
                <td>{nullableMoney(row.difference)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}

function Metric({ label, value }: { label: string; value: string }) {
  return (
    <article className="report-metric">
      <span>{label}</span>
      <strong>{value}</strong>
    </article>
  )
}

function initialRange(): DateRange {
  const to = new Date()
  const from = new Date(to)
  from.setDate(from.getDate() - 29)
  return { from: localDate(from), to: localDate(to) }
}

function localDate(value: Date): string {
  const year = value.getFullYear()
  const month = String(value.getMonth() + 1).padStart(2, '0')
  const day = String(value.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function money(value: number): string {
  return new Intl.NumberFormat('es-CL', {
    style: 'currency',
    currency: 'CLP',
    maximumFractionDigits: 2,
  }).format(value)
}

function nullableMoney(value: number | null): string {
  return value === null ? '\u2014' : money(value)
}
function signed(value: number): string {
  return value > 0 ? `+${value}` : String(value)
}
function dateTime(value: string): string {
  return new Intl.DateTimeFormat('es-CL', { dateStyle: 'short', timeStyle: 'short' }).format(
    new Date(value),
  )
}
function paymentLabel(value: PaymentMethod): string {
  return { CASH: 'Efectivo', CARD: 'Tarjeta', TRANSFER: 'Transferencia' }[value]
}
function movementLabel(value: string): string {
  return (
    (
      {
        ENTRY: 'Entrada',
        SALE_OUT: 'Venta',
        ADJUSTMENT_IN: 'Ajuste positivo',
        ADJUSTMENT_OUT: 'Ajuste negativo',
        DAMAGED: 'Dañado',
        EXPIRED: 'Vencido',
        SALE_REVERSAL: 'Reposición por anulación',
      } as Record<string, string>
    )[value] ?? value
  )
}
function messageFrom(reason: unknown): string {
  return reason instanceof Error ? reason.message : 'Ocurri\u00f3 un error inesperado.'
}
