import { FormEvent, useEffect, useState } from 'react'
import {
  listAuditEvents,
  type AuditEvent,
  type AuditEventPage,
  type AuditEventType,
  type AuditFilters,
} from './api'

const EMPTY_PAGE: AuditEventPage = {
  items: [],
  page: 0,
  size: 20,
  totalElements: 0,
  totalPages: 0,
}

const EVENT_TYPES: AuditEventType[] = [
  'SALE_CONFIRMED',
  'SALE_VOIDED',
  'CASH_OPENED',
  'CASH_CLOSED',
  'CASH_SALE',
  'CASH_REFUND',
  'MANUAL_INCOME',
  'MANUAL_WITHDRAWAL',
  'INVENTORY_ENTRY',
  'INVENTORY_ADJUSTMENT_IN',
  'INVENTORY_ADJUSTMENT_OUT',
  'INVENTORY_SALE_OUT',
  'INVENTORY_SALE_REVERSAL',
  'INVENTORY_DAMAGED',
  'INVENTORY_EXPIRED',
  'LOGIN_SUCCEEDED',
  'LOGIN_FAILED',
  'LOGIN_RATE_LIMITED',
  'PASSWORD_CHANGED',
  'LOGOUT_SUCCEEDED',
]

export function AuditWorkspace() {
  const [events, setEvents] = useState<AuditEventPage>(EMPTY_PAGE)
  const [filters, setFilters] = useState<AuditFilters>({})
  const [page, setPage] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    let active = true
    void listAuditEvents({ ...filters, page, size: 20 })
      .then((result) => {
        if (active) setEvents(result)
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
  }, [filters, page])

  function applyFilters(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const data = new FormData(event.currentTarget)
    const type = fieldValue(data, 'type')
    setLoading(true)
    setError('')
    setFilters({
      from: fieldValue(data, 'from') || undefined,
      to: fieldValue(data, 'to') || undefined,
      type: type ? (type as AuditEventType) : undefined,
      reference: fieldValue(data, 'reference') || undefined,
    })
    setPage(0)
  }

  return (
    <section className="catalog-card audit-workspace" aria-labelledby="audit-title">
      <div className="section-heading">
        <div>
          <p className="eyebrow">Control administrativo</p>
          <h2 id="audit-title">{'Auditor\u00eda de operaciones'}</h2>
          <p>
            {'Consulta qui\u00e9n realiz\u00f3 cada operaci\u00f3n cr\u00edtica y su referencia.'}
          </p>
        </div>
        <strong>{events.totalElements} eventos</strong>
      </div>

      <form className="audit-filters" onSubmit={applyFilters}>
        <label>
          Desde
          <input name="from" type="date" />
        </label>
        <label>
          Hasta
          <input name="to" type="date" />
        </label>
        <label>
          Tipo de evento
          <select name="type" defaultValue="">
            <option value="">Todos</option>
            {EVENT_TYPES.map((type) => (
              <option key={type} value={type}>
                {eventTypeLabel(type)}
              </option>
            ))}
          </select>
        </label>
        <label>
          Referencia
          <input name="reference" maxLength={100} placeholder="Ej. V-000001" />
        </label>
        <button type="submit" disabled={loading}>
          Aplicar filtros
        </button>
      </form>

      {error && (
        <p className="error-message" role="alert">
          {error}
        </p>
      )}
      {loading && <p>{'Cargando auditor\u00eda\u2026'}</p>}
      {!loading && !error && events.items.length === 0 && (
        <p>No hay eventos que coincidan con los filtros.</p>
      )}
      {!loading && events.items.length > 0 && <AuditTable events={events.items} />}

      {events.totalPages > 1 && (
        <div className="audit-pagination" aria-label={'Paginaci\u00f3n de auditor\u00eda'}>
          <button
            type="button"
            className="secondary-button"
            disabled={page === 0 || loading}
            onClick={() => {
              setLoading(true)
              setError('')
              setPage((current) => Math.max(0, current - 1))
            }}
          >
            Anterior
          </button>
          <span>
            {'P\u00e1gina'} {events.page + 1} de {events.totalPages}
          </span>
          <button
            type="button"
            className="secondary-button"
            disabled={page + 1 >= events.totalPages || loading}
            onClick={() => {
              setLoading(true)
              setError('')
              setPage((current) => current + 1)
            }}
          >
            Siguiente
          </button>
        </div>
      )}
    </section>
  )
}

function AuditTable({ events }: { events: AuditEvent[] }) {
  return (
    <div className="table-scroll audit-table">
      <table>
        <thead>
          <tr>
            <th>Fecha</th>
            <th>Tipo</th>
            <th>Responsable</th>
            <th>Referencia</th>
            <th>Resumen</th>
          </tr>
        </thead>
        <tbody>
          {events.map((event) => (
            <tr key={event.id}>
              <td>{formatDate(event.occurredAt)}</td>
              <td>{eventTypeLabel(event.type)}</td>
              <td>{event.actorDisplayName}</td>
              <td>{event.reference ?? '\u2014'}</td>
              <td>{event.summary}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}

function eventTypeLabel(type: AuditEventType): string {
  const labels: Record<AuditEventType, string> = {
    LOGIN_SUCCEEDED: 'Inicio de sesi\u00f3n',
    LOGIN_FAILED: 'Ingreso rechazado',
    LOGIN_RATE_LIMITED: 'Ingreso limitado',
    PASSWORD_CHANGED: 'Cambio de contrase\u00f1a',
    LOGOUT_SUCCEEDED: 'Cierre de sesi\u00f3n',
    CASH_OPENED: 'Apertura de caja',
    CASH_CLOSED: 'Cierre de caja',
    CASH_SALE: 'Efectivo por venta',
    MANUAL_INCOME: 'Ingreso manual',
    MANUAL_WITHDRAWAL: 'Retiro manual',
    CASH_REFUND: 'Devoluci\u00f3n en efectivo',
    INVENTORY_ENTRY: 'Entrada de inventario',
    INVENTORY_SALE_OUT: 'Salida por venta',
    INVENTORY_ADJUSTMENT_IN: 'Ajuste positivo',
    INVENTORY_ADJUSTMENT_OUT: 'Ajuste negativo',
    INVENTORY_DAMAGED: 'Producto da\u00f1ado',
    INVENTORY_EXPIRED: 'Producto vencido',
    INVENTORY_SALE_REVERSAL: 'Reposici\u00f3n por anulaci\u00f3n',
    SALE_CONFIRMED: 'Venta confirmada',
    SALE_VOIDED: 'Venta anulada',
  }
  return labels[type]
}

function formatDate(value: string): string {
  return new Intl.DateTimeFormat('es-CL', {
    dateStyle: 'short',
    timeStyle: 'short',
    timeZone: 'America/Santiago',
  }).format(new Date(value))
}

function fieldValue(data: FormData, name: string): string {
  const value = data.get(name)
  return typeof value === 'string' ? value : ''
}

function messageFrom(reason: unknown): string {
  return reason instanceof Error ? reason.message : 'Ocurri\u00f3 un error inesperado.'
}
