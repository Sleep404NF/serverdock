const TOKEN_KEY = 'serverdock.token'

export type ServerRole = 'OWNER' | 'ADMIN' | 'MEMBER'
export type ServerLoader = 'VANILLA' | 'FABRIC' | 'FORGE' | 'NEOFORGE'
export type ServerStatus = 'NOT_CREATED' | 'STOPPED' | 'STARTING' | 'RUNNING'

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

export interface ServerStatusResponse {
  serverId: number
  status: ServerStatus
  restartRequired: boolean
}

export interface ConsoleCommandResponse {
  command: string
  output: string
}

export interface Member {
  userId: number
  username: string
  role: ServerRole
  joinedAt: string
}

export type InviteExpiry = 'THIRTY_MINUTES' | 'ONE_HOUR' | 'SIX_HOURS' | 'TWELVE_HOURS' | 'ONE_DAY' | 'SEVEN_DAYS'

export interface Invite {
  id: number
  code: string
  role: ServerRole
  createdBy: string
  expiresAt: string
  /** null이면 횟수 제한 없음 */
  maxUses: number | null
  usedCount: number
  /** 만료되지 않았고 횟수도 남아 있어서 지금 쓸 수 있는지 */
  usable: boolean
  createdAt: string
}

export interface CreateInviteInput {
  role: ServerRole
  expiresIn: InviteExpiry
  maxUses: number | null
}

const ROLE_RANK: Record<ServerRole, number> = { MEMBER: 0, ADMIN: 1, OWNER: 2 }

/**
 * 내 역할이 required 이상인지. 버튼을 보여줄지 정할 때만 쓰고, 실제 권한 검사는 백엔드가 한다.
 */
export function hasRoleAtLeast(role: ServerRole, required: ServerRole) {
  return ROLE_RANK[role] >= ROLE_RANK[required]
}

/** role이 other보다 높은 역할인지. 자기보다 낮은 역할만 내보낼 수 있다. */
export function outranks(role: ServerRole, other: ServerRole) {
  return ROLE_RANK[role] > ROLE_RANK[other]
}

/** 백엔드와 같은 순서: 높은 역할 먼저, 같은 역할은 먼저 들어온 순 */
export function sortMembers(members: Member[]) {
  return [...members].sort(
    (a, b) => ROLE_RANK[b.role] - ROLE_RANK[a.role] || a.joinedAt.localeCompare(b.joinedAt),
  )
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

function authHeaders(): Headers {
  const headers = new Headers()
  const token = getToken()
  if (token) {
    headers.set('Authorization', `Bearer ${token}`)
  }
  return headers
}

/**
 * ProblemDetail을 ApiError로 바꾼다. 입력값 오류(errors)가 있으면 "입력값이 올바르지 않습니다." 대신
 * 첫 번째 필드 오류("아이디는 6~20자여야 합니다." 등)를 보여준다.
 */
async function toApiError(response: Response): Promise<ApiError> {
  const problem = await response.json().catch(() => null)
  const fieldErrors: unknown[] = problem?.errors ? Object.values(problem.errors) : []
  const firstFieldError = fieldErrors.find((value): value is string => typeof value === 'string')
  return new ApiError(
    response.status,
    firstFieldError ?? problem?.detail ?? `요청에 실패했습니다. (${response.status})`,
  )
}

/**
 * 모든 API 요청이 거치는 곳. 토큰을 붙이고, 실패하면 ProblemDetail의 detail로 ApiError를 던진다.
 */
async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = authHeaders()
  if (init.body) {
    headers.set('Content-Type', 'application/json')
  }

  const response = await fetch(path, { ...init, headers })

  if (!response.ok) {
    throw await toApiError(response)
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

export async function signup(username: string, password: string) {
  await request('/api/auth/signup', {
    method: 'POST',
    body: JSON.stringify({ username, password }),
  })
}

export function fetchMe() {
  return request<Me>('/api/auth/me')
}

export function fetchServers() {
  return request<Server[]>('/api/servers')
}

export function fetchServer(serverId: number) {
  return request<Server>(`/api/servers/${serverId}`)
}

export function fetchStatus(serverId: number) {
  return request<ServerStatusResponse>(`/api/servers/${serverId}/status`)
}

export type PowerAction = 'start' | 'stop' | 'restart'

export function changePower(serverId: number, action: PowerAction) {
  return request<ServerStatusResponse>(`/api/servers/${serverId}/${action}`, { method: 'POST' })
}

export function sendCommand(serverId: number, command: string) {
  return request<ConsoleCommandResponse>(`/api/servers/${serverId}/console/commands`, {
    method: 'POST',
    body: JSON.stringify({ command }),
  })
}

export function fetchMembers(serverId: number) {
  return request<Member[]>(`/api/servers/${serverId}/members`)
}

export function changeMemberRole(serverId: number, userId: number, role: ServerRole) {
  return request<Member>(`/api/servers/${serverId}/members/${userId}`, {
    method: 'PATCH',
    body: JSON.stringify({ role }),
  })
}

/** 다른 멤버면 내보내기, 나 자신이면 나가기 */
export function removeMember(serverId: number, userId: number) {
  return request<void>(`/api/servers/${serverId}/members/${userId}`, { method: 'DELETE' })
}

export function fetchInvites(serverId: number) {
  return request<Invite[]>(`/api/servers/${serverId}/invites`)
}

export function createInvite(serverId: number, input: CreateInviteInput) {
  return request<Invite>(`/api/servers/${serverId}/invites`, {
    method: 'POST',
    body: JSON.stringify(input),
  })
}

export function revokeInvite(serverId: number, inviteId: number) {
  return request<void>(`/api/servers/${serverId}/invites/${inviteId}`, { method: 'DELETE' })
}

export function acceptInvite(code: string) {
  return request<Server>(`/api/invites/${encodeURIComponent(code)}/accept`, { method: 'POST' })
}

/** 친구에게 보낼 초대 주소. 지금 보고 있는 화면 주소를 기준으로 만든다. */
export function inviteUrl(code: string) {
  return `${window.location.origin}${window.location.pathname}#/invite/${code}`
}

export interface LogStreamHandlers {
  /** 연결이 열렸다. 지난 로그부터 다시 오므로 화면을 비울 때다. */
  onOpen: () => void
  onLog: (line: string) => void
  /** 서버가 꺼져 스트림이 끝났다. */
  onEnd: (message: string) => void
}

/**
 * 실시간 로그(SSE)를 읽는다. EventSource는 Authorization 헤더를 못 붙여서 fetch로 직접 읽는다.
 * 스트림이 끝나면 resolve되고, signal로 중단하면 AbortError로 reject된다.
 */
export async function streamLogs(serverId: number, handlers: LogStreamHandlers, signal: AbortSignal) {
  const response = await fetch(`/api/servers/${serverId}/console/logs`, { headers: authHeaders(), signal })
  if (!response.ok || !response.body) {
    throw await toApiError(response)
  }
  handlers.onOpen()

  const reader = response.body.pipeThrough(new TextDecoderStream()).getReader()
  let buffer = ''
  for (;;) {
    const { value, done } = await reader.read()
    if (done) {
      return
    }
    buffer += value

    let boundary: number
    while ((boundary = buffer.indexOf('\n\n')) >= 0) {
      const block = buffer.slice(0, boundary)
      buffer = buffer.slice(boundary + 2)
      dispatchEvent(block, handlers)
    }
  }
}

/**
 * SSE 이벤트 하나("event:이름" + "data:내용" 줄들)를 해석한다. ":"로 시작하는 줄은 heartbeat라 무시한다.
 */
function dispatchEvent(block: string, handlers: LogStreamHandlers) {
  let name = 'message'
  const data: string[] = []
  for (const line of block.split('\n')) {
    if (line.startsWith('event:')) {
      name = line.slice(6).trim()
    } else if (line.startsWith('data:')) {
      const value = line.slice(5)
      data.push(value.startsWith(' ') ? value.slice(1) : value)
    }
  }
  if (data.length === 0) {
    return
  }

  if (name === 'log') {
    handlers.onLog(data.join('\n'))
  } else if (name === 'end') {
    handlers.onEnd(data.join('\n'))
  }
}
