import { afterEach, describe, expect, it, vi } from 'vitest'
import { ApiError, changePassword, currentSession, login, logout } from './api'

describe('authentication API', () => {
  afterEach(() => vi.unstubAllGlobals())

  it('distinguishes an anonymous request from an active session', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValueOnce(response({}, 401)))
    await expect(currentSession()).resolves.toBeNull()

    const session = {
      username: 'admin',
      displayName: 'Administración',
      role: 'ADMIN',
      passwordChangeRequired: false,
    }
    vi.stubGlobal('fetch', vi.fn().mockResolvedValueOnce(response(session)))
    await expect(currentSession()).resolves.toEqual(session)
  })

  it('sends credentials and the server-provided CSRF header on login', async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(response({ headerName: 'X-XSRF-TOKEN', token: 'token-1' }))
      .mockResolvedValueOnce(
        response({
          username: 'admin',
          displayName: 'Administración',
          role: 'ADMIN',
          passwordChangeRequired: true,
        }),
      )
    vi.stubGlobal('fetch', fetchMock)

    await login('admin', 'Temporal-admin-2026')

    expect(fetchMock).toHaveBeenNthCalledWith(
      2,
      '/api/v1/auth/login',
      expect.objectContaining({
        body: JSON.stringify({ username: 'admin', password: 'Temporal-admin-2026' }),
        credentials: 'same-origin',
        headers: {
          'Content-Type': 'application/json',
          'X-XSRF-TOKEN': 'token-1',
        },
      }),
    )
  })

  it('supports password changes and logout responses without bodies', async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(response({ headerName: 'X-CSRF', token: 'one' }))
      .mockResolvedValueOnce(response(undefined, 204))
      .mockResolvedValueOnce(response({ headerName: 'X-CSRF', token: 'two' }))
      .mockResolvedValueOnce(response(undefined, 204))
    vi.stubGlobal('fetch', fetchMock)

    await expect(changePassword('actual-segura', 'nueva-segura-2026')).resolves.toBeUndefined()
    await expect(logout()).resolves.toBeUndefined()
    expect(fetchMock).toHaveBeenNthCalledWith(
      4,
      '/api/v1/auth/logout',
      expect.objectContaining({ method: 'POST', headers: { 'X-CSRF': 'two' } }),
    )
  })

  it('uses structured problems and a safe fallback for malformed errors', async () => {
    vi.stubGlobal(
      'fetch',
      vi
        .fn()
        .mockResolvedValueOnce(
          response({ code: 'authentication-failed', detail: 'Credencial inválida.' }, 403),
        ),
    )
    await expect(currentSession()).rejects.toMatchObject({
      status: 403,
      code: 'authentication-failed',
      message: 'Credencial inválida.',
    })

    vi.stubGlobal('fetch', vi.fn().mockResolvedValueOnce(new Response('not-json', { status: 503 })))
    await expect(currentSession()).rejects.toMatchObject(
      new ApiError(503, 'request-failed', 'No fue posible completar la operación.'),
    )
  })
})

function response(body: unknown, status = 200): Response {
  return new Response(status === 204 ? null : JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}
