import { useCallback, useEffect, useState } from 'react'
import { ApiError, fetchMe, startDiscordLink, unlinkDiscord } from './api'
import type { Me } from './api'
import { goToExternal, navigate } from './navigate'

/** 디스코드에서 돌아왔을 때 보여줄 결과 문구 */
const RESULT_MESSAGES: Record<string, { kind: 'success' | 'error'; text: string }> = {
  linked: { kind: 'success', text: '디스코드 계정을 연결했습니다. 이제 디스코드에서 /server 명령어를 쓸 수 있습니다.' },
  cancelled: { kind: 'error', text: '디스코드에서 연결을 취소했습니다.' },
  expired: { kind: 'error', text: '연결 시간이 지났습니다. 다시 시도해주세요.' },
  taken: { kind: 'error', text: '그 디스코드 계정은 이미 다른 ServerDock 계정에 연결되어 있습니다.' },
  failed: { kind: 'error', text: '디스코드 계정 정보를 받지 못했습니다. 다시 시도해주세요.' },
}

interface Props {
  discordResult: string | null
  onLogout: () => void
}

/**
 * 내 계정(#/account). 지금은 디스코드 연결만 있다.
 * 연결하면 디스코드에서 /server 명령어를 쓸 때 이 계정의 권한으로 동작한다.
 */
export default function AccountPage({ discordResult, onLogout }: Props) {
  const [me, setMe] = useState<Me | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

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
    fetchMe()
      .then(setMe)
      .catch((e: unknown) => setError(toMessage(e, '계정 정보를 불러오지 못했습니다.')))
  }, [toMessage])

  async function handleLink() {
    setBusy(true)
    setError(null)
    try {
      goToExternal(await startDiscordLink())
    } catch (e) {
      setError(toMessage(e, '디스코드 연결을 시작하지 못했습니다.'))
      setBusy(false)
    }
  }

  async function handleUnlink() {
    setBusy(true)
    setError(null)
    try {
      await unlinkDiscord()
      setMe(await fetchMe())
      navigate('#/account')
    } catch (e) {
      setError(toMessage(e, '연결을 끊지 못했습니다.'))
    } finally {
      setBusy(false)
    }
  }

  const result = discordResult ? RESULT_MESSAGES[discordResult] : undefined

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

      {result && <p className={`banner ${result.kind}`}>{result.text}</p>}
      {error && <p className="error">{error}</p>}
      {!me && !error && <p className="muted">불러오는 중…</p>}

      {me && (
        <section className="card panel">
          <div className="panel-head">
            <h2>내 계정</h2>
            <p className="muted small">
              {me.username}
              {me.systemRole === 'ADMIN' && ' · 관리자'}
            </p>
          </div>

          <div className="discord-row">
            <div>
              <strong>디스코드</strong>
              <p className="muted small">
                {me.discordLinked
                  ? `${me.discordUsername ?? '디스코드 계정'}과(와) 연결됨`
                  : '연결하면 디스코드에서 /server start, /server status 같은 명령어로 서버를 켜고 상태를 볼 수 있습니다.'}
              </p>
            </div>
            {me.discordLinked ? (
              <button type="button" className="secondary" onClick={handleUnlink} disabled={busy}>
                연결 끊기
              </button>
            ) : (
              <button type="button" className="discord" onClick={handleLink} disabled={busy}>
                {busy ? '이동하는 중…' : '디스코드 연결'}
              </button>
            )}
          </div>
        </section>
      )}
    </main>
  )
}
