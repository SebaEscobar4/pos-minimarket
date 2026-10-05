import { FormEvent, useEffect, useState } from 'react'
import {
  changePassword,
  currentSession,
  login,
  logout,
  type UserSession,
} from '../features/auth/api'
import { CategoryAdmin } from '../features/catalog/CategoryAdmin'
import { ProductAdmin } from '../features/catalog/ProductAdmin'
import { ProductSearch } from '../features/catalog/ProductSearch'
import { InventoryAdmin } from '../features/inventory/InventoryAdmin'
import { CashSessionPanel } from '../features/cash/CashSessionPanel'
import { SaleWorkspace } from '../features/sales/SaleWorkspace'
import { AuditWorkspace } from '../features/audit/AuditWorkspace'
import { ReportsWorkspace } from '../features/reports/ReportsWorkspace'

export function App() {
  const [loading, setLoading] = useState(true)
  const [session, setSession] = useState<UserSession | null>(null)
  const [error, setError] = useState('')

  async function loadSession() {
    setLoading(true)
    setError('')
    try {
      setSession(await currentSession())
    } catch (reason) {
      setError(messageFrom(reason))
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void currentSession()
      .then(setSession)
      .catch((reason: unknown) => setError(messageFrom(reason)))
      .finally(() => setLoading(false))
  }, [])

  if (loading) {
    return <main className="centered-shell">Comprobando sesión…</main>
  }

  if (error && session === null) {
    return (
      <main className="centered-shell">
        <section className="auth-card" aria-labelledby="connection-title">
          <p className="eyebrow">POS Minimarket</p>
          <h1 id="connection-title">No pudimos conectar</h1>
          <p className="error-message" role="alert">
            {error}
          </p>
          <button type="button" onClick={() => void loadSession()}>
            Reintentar
          </button>
        </section>
      </main>
    )
  }

  if (session === null) {
    return <LoginScreen onAuthenticated={setSession} onError={setError} error={error} />
  }

  if (session.passwordChangeRequired) {
    return <PasswordChangeScreen session={session} onError={setError} error={error} />
  }

  return <AuthenticatedHome session={session} onSignedOut={() => setSession(null)} />
}

type LoginScreenProps = {
  error: string
  onAuthenticated: (session: UserSession) => void
  onError: (message: string) => void
}

function LoginScreen({ error, onAuthenticated, onError }: LoginScreenProps) {
  const [submitting, setSubmitting] = useState(false)

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setSubmitting(true)
    onError('')
    const data = new FormData(event.currentTarget)
    try {
      onAuthenticated(await login(fieldValue(data, 'username'), fieldValue(data, 'password')))
    } catch (reason) {
      onError(messageFrom(reason))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <main className="auth-layout">
      <section className="brand-panel" aria-labelledby="page-title">
        <p className="eyebrow">Caja única · Operación local</p>
        <h1 id="page-title">DONDE LA CHOLA</h1>
        <p>Ventas, caja e inventario con trazabilidad.</p>
      </section>
      <section className="auth-card" aria-labelledby="login-title">
        <p className="eyebrow">Acceso interno</p>
        <h2 id="login-title">Iniciar sesión</h2>
        <form onSubmit={(event) => void submit(event)}>
          <label htmlFor="username">Usuario</label>
          <input id="username" name="username" autoComplete="username" required />
          <label htmlFor="password">Contraseña</label>
          <input
            id="password"
            name="password"
            type="password"
            autoComplete="current-password"
            required
          />
          {error && (
            <p className="error-message" role="alert">
              {error}
            </p>
          )}
          <button type="submit" disabled={submitting}>
            {submitting ? 'Ingresando…' : 'Ingresar'}
          </button>
        </form>
      </section>
    </main>
  )
}

type PasswordChangeProps = {
  session: UserSession
  error: string
  onError: (message: string) => void
}

function PasswordChangeScreen({ session, error, onError }: PasswordChangeProps) {
  const [submitting, setSubmitting] = useState(false)
  const [completed, setCompleted] = useState(false)

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setSubmitting(true)
    onError('')
    const data = new FormData(event.currentTarget)
    try {
      await changePassword(fieldValue(data, 'currentPassword'), fieldValue(data, 'newPassword'))
      setCompleted(true)
    } catch (reason) {
      onError(messageFrom(reason))
    } finally {
      setSubmitting(false)
    }
  }

  if (completed) {
    return (
      <main className="centered-shell">
        <section className="auth-card">
          <p className="eyebrow">Credencial actualizada</p>
          <h1>Contraseña cambiada</h1>
          <p>La sesión temporal fue cerrada. Ingresa nuevamente con tu contraseña nueva.</p>
          <button type="button" onClick={() => window.location.reload()}>
            Volver al ingreso
          </button>
        </section>
      </main>
    )
  }

  return (
    <main className="centered-shell">
      <section className="auth-card" aria-labelledby="password-title">
        <p className="eyebrow">Primer acceso · {session.displayName}</p>
        <h1 id="password-title">Crea tu contraseña definitiva</h1>
        <p>Por seguridad, la credencial temporal no permite operar el POS.</p>
        <form onSubmit={(event) => void submit(event)}>
          <label htmlFor="current-password">Contraseña temporal</label>
          <input
            id="current-password"
            name="currentPassword"
            type="password"
            autoComplete="current-password"
            required
          />
          <label htmlFor="new-password">Contraseña nueva</label>
          <input
            id="new-password"
            name="newPassword"
            type="password"
            autoComplete="new-password"
            minLength={12}
            maxLength={128}
            required
          />
          {error && (
            <p className="error-message" role="alert">
              {error}
            </p>
          )}
          <button type="submit" disabled={submitting}>
            {submitting ? 'Actualizando…' : 'Guardar contraseña'}
          </button>
        </form>
      </section>
    </main>
  )
}

function AuthenticatedHome({
  session,
  onSignedOut,
}: {
  session: UserSession
  onSignedOut: () => void
}) {
  const [error, setError] = useState('')
  const [section, setSection] = useState<WorkspaceSection>('SALES')
  const [catalogRevision, setCatalogRevision] = useState(0)

  const sections: { id: WorkspaceSection; label: string }[] =
    session.role === 'ADMIN'
      ? [
          { id: 'SALES', label: 'Ventas' },
          { id: 'INVENTORY', label: 'Inventario' },
          { id: 'CATALOG', label: 'Productos' },
          { id: 'CASH', label: 'Caja' },
          { id: 'AUDIT', label: 'Auditor\u00eda' },
          { id: 'REPORTS', label: 'Reportes' },
          { id: 'SEARCH', label: 'Consulta' },
        ]
      : [
          { id: 'SALES', label: 'Ventas' },
          { id: 'CASH', label: 'Caja' },
          { id: 'SEARCH', label: 'Consulta' },
        ]

  async function signOut() {
    setError('')
    try {
      await logout()
      onSignedOut()
    } catch (reason) {
      setError(messageFrom(reason))
    }
  }

  return (
    <main className="dashboard-shell">
      <header className="topbar">
        <div>
          <p className="eyebrow">Donde La Chola</p>
          <h1>Bienvenido, {session.displayName}</h1>
          <p>Rol: {session.role === 'ADMIN' ? 'Administrador' : 'Vendedor'}</p>
        </div>
        <button type="button" className="secondary-button" onClick={() => void signOut()}>
          Cerrar sesión
        </button>
      </header>
      {error && (
        <p className="error-message" role="alert">
          {error}
        </p>
      )}
      <section className="status-card" aria-labelledby="status-title">
        <p className="eyebrow">Donde La Chola</p>
        <h2 id="status-title">Ventas, inventario y caja disponibles</h2>
        <p>Registra compras completas y consulta cada comprobante interno desde Ventas.</p>
      </section>
      <nav className="workspace-nav" aria-label="Secciones principales">
        {sections.map((item) => (
          <button
            key={item.id}
            type="button"
            className={section === item.id ? 'workspace-nav-button active' : 'workspace-nav-button'}
            aria-current={section === item.id ? 'page' : undefined}
            onClick={() => setSection(item.id)}
          >
            {item.label}
          </button>
        ))}
      </nav>

      {section === 'SALES' && <SaleWorkspace role={session.role} />}
      {section === 'CASH' && <CashSessionPanel role={session.role} />}
      {section === 'AUDIT' && session.role === 'ADMIN' && <AuditWorkspace />}
      {section === 'REPORTS' && session.role === 'ADMIN' && <ReportsWorkspace />}
      {section === 'SEARCH' && <ProductSearch />}
      {section === 'CATALOG' && session.role === 'ADMIN' && (
        <>
          <section className="workspace-intro" aria-labelledby="catalog-workspace-title">
            <p className="eyebrow">Paso previo</p>
            <h2 id="catalog-workspace-title">Productos</h2>
            <p>
              Crea primero una categor&iacute;a y luego el producto que controlar&aacute;s en
              inventario.
            </p>
          </section>
          <CategoryAdmin onChanged={() => setCatalogRevision((revision) => revision + 1)} />
          <ProductAdmin refreshToken={catalogRevision} />
        </>
      )}
      {section === 'INVENTORY' && session.role === 'ADMIN' && (
        <InventoryAdmin onOpenCatalog={() => setSection('CATALOG')} />
      )}
    </main>
  )
}

type WorkspaceSection = 'SALES' | 'CASH' | 'CATALOG' | 'INVENTORY' | 'AUDIT' | 'REPORTS' | 'SEARCH'

function messageFrom(reason: unknown): string {
  return reason instanceof Error ? reason.message : 'Ocurrió un error inesperado.'
}

function fieldValue(data: FormData, name: string): string {
  const value = data.get(name)
  return typeof value === 'string' ? value : ''
}
