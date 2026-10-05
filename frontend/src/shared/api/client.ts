type CsrfToken = {
  headerName: string
  token: string
}

type ProblemResponse = {
  detail?: string
  code?: string
}

export class ApiError extends Error {
  constructor(
    public readonly status: number,
    public readonly code: string,
    message: string,
  ) {
    super(message)
    this.name = 'ApiError'
  }
}

export async function mutateJson<T>(
  path: string,
  method: 'POST' | 'PUT' | 'PATCH',
  body?: unknown,
): Promise<T> {
  const csrf = await readJson<CsrfToken>(
    await fetch('/api/v1/auth/csrf', { credentials: 'same-origin' }),
  )
  const response = await fetch(path, {
    method,
    credentials: 'same-origin',
    headers: {
      [csrf.headerName]: csrf.token,
      ...(body === undefined ? {} : { 'Content-Type': 'application/json' }),
    },
    ...(body === undefined ? {} : { body: JSON.stringify(body) }),
  })
  if (response.status === 204) return undefined as T
  return readJson<T>(response)
}

export async function readJson<T>(response: Response): Promise<T> {
  if (response.ok) return (await response.json()) as T

  let problem: ProblemResponse = {}
  try {
    problem = (await response.json()) as ProblemResponse
  } catch {
    // La respuesta segura de respaldo evita mostrar HTML o detalles internos.
  }
  throw new ApiError(
    response.status,
    problem.code ?? 'request-failed',
    problem.detail ?? 'No fue posible completar la operación.',
  )
}
