const TOKEN_KEY = 'serverdock.token'

export type ServerRole = 'OWNER' | 'ADMIN' | 'MEMBER'
export type ServerLoader = 'VANILLA' | 'FABRIC' | 'FORGE' | 'NEOFORGE'

export interface Me {
  id: number
  username: string
  systemRole: 'ADMIN' | 'USER'
}

export interface Server {
  id: number
  name: string
  gameVersion: string
  loader: ServerLoader
  loaderVersion: string | null
  memoryMb: number
  port: number
  autoRestart: boolean
  myRole: ServerRole
  createdAt: string
}

/**
 * 백엔드가 보낸 오류. status로 401(로그인 만료) 같은 경우를 구분한다.
 */
export class ApiError extends Error {
  readonly status: number

  constructor(status: number, message: string) {
    super(message)
    this.status = status
  }
}

export function getToken(): string | null {
  try {
    return localStorage.getItem(TOKEN_KEY)
  } catch {
    return null
  }
}

function setToken(token: string) {
  localStorage.setItem(TOKEN_KEY, token)
}

export function clearToken() {
  localStorage.removeItem(TOKEN_KEY)
}

/**
 * 모든 API 요청이 거치는 곳. 토큰을 붙이고, 실패하면 ProblemDetail의 detail로 ApiError를 던진다.
 */
async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers)
  if (init.body) {
    headers.set('Content-Type', 'application/json')
  }
  const token = getToken()
  if (token) {
    headers.set('Authorization', `Bearer ${token}`)
  }

  const response = await fetch(path, { ...init, headers })

  if (!response.ok) {
    const problem = await response.json().catch(() => null)
    throw new ApiError(response.status, problem?.detail ?? `요청에 실패했습니다. (${response.status})`)
  }
  if (response.status === 204) {
    return undefined as T
  }
  return response.json() as Promise<T>
}

export async function login(username: string, password: string) {
  const { accessToken } = await request<{ accessToken: string }>('/api/auth/login', {
    method: 'POST',
    body: JSON.stringify({ username, password }),
  })
  setToken(accessToken)
}

export function fetchMe() {
  return request<Me>('/api/auth/me')
}

export function fetchServers() {
  return request<Server[]>('/api/servers')
}
