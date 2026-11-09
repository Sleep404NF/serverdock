import { useCallback, useEffect, useState } from 'react'
import { ApiError, changePower, fetchServer, fetchStatus, hasRoleAtLeast } from './api'
import type { PowerAction, Server, ServerStatus, ServerStatusResponse } from './api'
import Console from './Console'
import InvitesPanel from './InvitesPanel'
import MembersPanel from './MembersPanel'

export type ServerTab = 'console' | 'members' | 'invites'

const STATUS_LABEL: Record<ServerStatus, string> = {
  NOT_CREATED: '아직 안 켬',
  STOPPED: '꺼짐',
  STARTING: '켜는 중',
  RUNNING: '켜짐',
}

/** 켜는 중에는 자주, 그 외에는 가끔 상태를 다시 읽는다. */
const POLL_FAST_MS = 3000
const POLL_SLOW_MS = 10000

interface Props {
  serverId: number
  tab: ServerTab
  onLogout: () => void
}

export default function ServerDetailPage({ serverId, tab, onLogout }: Props) {
  const [server, setServer] = useState<Server | null>(null)
  const [status, setStatus] = useState<ServerStatusResponse | null>(null)
  const [pageError, setPageError] = useState<string | null>(null)
  const [statusError, setStatusError] = useState<string | null>(null)
  const [actionError, setActionError] = useState<string | null>(null)
  const [busy, setBusy] = useState<PowerAction | null>(null)

  /** 401이면 로그아웃하고, 아니면 화면에 보여줄 메시지를 돌려준다. */
  const toMessage = useCallback(
    (e: unknown, fallback: string) => {
      if (e instanceof ApiError && e.status === 401) {
        onLogout()
        return null
      }
      return e instanceof Error ? e.message : fallback
    },
    [onLogout],
  )

  useEffect(() => {
    fetchServer(serverId)
      .then(setServer)
      .catch((e: unknown) => setPageError(toMessage(e, '서버를 불러오지 못했습니다.')))
  }, [serverId, toMessage])

  const loaded = server !== null
  const currentStatus = status?.status ?? null

  useEffect(() => {
    if (!loaded) {
      return
    }
    let cancelled = false
    const load = () => {
      fetchStatus(serverId)
        .then((result) => {
          if (!cancelled) {
            setStatus(result)
            setStatusError(null)
          }
        })
        .catch((e: unknown) => {
          if (!cancelled) {
            setStatusError(toMessage(e, '상태를 불러오지 못했습니다.'))
          }
        })
    }
    load()
    const timer = window.setInterval(load, currentStatus === 'STARTING' ? POLL_FAST_MS : POLL_SLOW_MS)
    return () => {
      cancelled = true
      window.clearInterval(timer)
    }
  }, [serverId, loaded, currentStatus, toMessage])

  async function runAction(action: PowerAction) {
    setBusy(action)
    setActionError(null)
    try {
      setStatus(await changePower(serverId, action))
    } catch (e) {
      setActionError(toMessage(e, '요청에 실패했습니다.'))
    } finally {
      setBusy(null)
    }
  }

  const running = currentStatus === 'STARTING' || currentStatus === 'RUNNING'
  const canManage = server !== null && hasRoleAtLeast(server.myRole, 'ADMIN')

  return (
    <main className="page">
      <header className="topbar">
        <a href="#/" className="back">
          ← 내 서버
        </a>
        <button type="button" className="secondary" onClick={onLogout}>
          로그아웃
        </button>
      </header>

      {pageError && <p className="error">{pageError}</p>}
      {!server && !pageError && <p className="muted">불러오는 중…</p>}

      {server && (
        <>
          <section className="card server-head">
            <div className="server-title">
              <h1>{server.name}</h1>
              {status && (
                <span className={`status status-${status.status.toLowerCase()}`}>{STATUS_LABEL[status.status]}</span>
              )}
            </div>

            <p className="meta">
              {server.gameVersion} · {server.loader}
              {server.loaderVersion && ` ${server.loaderVersion}`} · {server.memoryMb} MB · 포트 {server.port}
              <span className={`role role-${server.myRole.toLowerCase()}`}>{server.myRole}</span>
            </p>

            <div className="actions">
              <button type="button" onClick={() => runAction('start')} disabled={!status || running || busy !== null}>
                {busy === 'start' ? '켜는 중…' : '시작'}
              </button>
              {canManage && (
                <>
                  <button
                    type="button"
                    className="secondary"
                    onClick={() => runAction('restart')}
                    disabled={!running || busy !== null}
                  >
                    {busy === 'restart' ? '재시작 중…' : '재시작'}
                  </button>
                  <button
                    type="button"
                    className="danger"
                    onClick={() => runAction('stop')}
                    disabled={!running || busy !== null}
                  >
                    {busy === 'stop' ? '끄는 중…' : '중지'}
                  </button>
                </>
              )}
            </div>

            {status?.restartRequired && (
              <p className="notice">
                {running ? '설정이 바뀌었습니다. 재시작하면 적용됩니다.' : '설정이 바뀌었습니다. 다음에 켤 때 적용됩니다.'}
              </p>
            )}
            {actionError && <p className="error">{actionError}</p>}
            {statusError && <p className="error">{statusError}</p>}
          </section>

          <nav className="tabs" aria-label="서버 메뉴">
            <a href={`#/servers/${serverId}`} aria-current={tab === 'console' ? 'page' : undefined}>
              콘솔
            </a>
            <a href={`#/servers/${serverId}/members`} aria-current={tab === 'members' ? 'page' : undefined}>
              멤버
            </a>
            {canManage && (
              <a href={`#/servers/${serverId}/invites`} aria-current={tab === 'invites' ? 'page' : undefined}>
                초대 링크
              </a>
            )}
          </nav>

          {/* 콘솔은 다른 탭에 가 있어도 연결과 명령어 기록을 유지하도록 숨기기만 한다. */}
          <div hidden={tab !== 'console'}>
            <Console
              serverId={serverId}
              status={currentStatus}
              canCommand={canManage}
              visible={tab === 'console'}
              onUnauthorized={onLogout}
            />
          </div>
          {tab === 'members' && <MembersPanel serverId={serverId} myRole={server.myRole} onLogout={onLogout} />}
          {tab === 'invites' &&
            (canManage ? (
              <InvitesPanel serverId={serverId} myRole={server.myRole} onLogout={onLogout} />
            ) : (
              <p className="muted">초대 링크는 ADMIN 이상만 볼 수 있습니다.</p>
            ))}
        </>
      )}
    </main>
  )
}
