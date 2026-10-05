import { mutateJson, readJson } from '../../shared/api/client'

export { ApiError } from '../../shared/api/client'

export type UserSession = {
  username: string
  displayName: string
  role: 'ADMIN' | 'SELLER'
  passwordChangeRequired: boolean
}

export async function currentSession(): Promise<UserSession | null> {
  const response = await fetch('/api/v1/auth/session', { credentials: 'same-origin' })
  if (response.status === 401) return null
  return readJson<UserSession>(response)
}

export async function login(username: string, password: string): Promise<UserSession> {
  return mutateJson<UserSession>('/api/v1/auth/login', 'POST', { username, password })
}

export async function changePassword(currentPassword: string, newPassword: string): Promise<void> {
  await mutateJson('/api/v1/auth/password', 'POST', { currentPassword, newPassword })
}

export async function logout(): Promise<void> {
  await mutateJson('/api/v1/auth/logout', 'POST')
}
