import { useCallback, useEffect, useState } from 'react'
import { ApiError, fetchTrash, restoreServer } from './api'
import type { TrashedServer } from './api'
import { formatDateTime, formatRemaining } from './format'
import { navigate } from './navigate'

interface Props {
  onLogout: () => void
}

/**
 * 휴지통(#/trash). 내가 OWNER인 서버 중 지운 것만 보인다.
 * 보관 기간이 지나면 컨테이너와 월드까지 완전히 지워지고 목록에서도 사라진다.
 */
export default function TrashPage({ onLogout }: Props) {
  const [servers, setServers] = useState<TrashedServer[] | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [restoringId, setRestoringId] = useState<number | null>(null)

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
    fetchTrash()
      .then(setServers)
      .catch((e: unknown) => setError(toMessage(e, '휴지통을 불러오지 못했습니다.')))
  }, [toMessage])

  async function handleRestore(server: TrashedServer) {
    setRestoringId(server.id)
    setError(null)
    try {
      await restoreServer(server.id)
      navigate(`#/servers/${server.id}`)
    } catch (e) {
      setError(toMessage(e, '복구하지 못했습니다.'))
      setRestoringId(null)
    }
  }

  return (
    <main className="page">
      <header className="topbar">
        <a href="#/" className="back">
          ← 내 서버
        </a>
      </header>

      <section className="card panel">
        <div className="panel-head">
          <h2>휴지통</h2>
          <p className="muted small">
            지운 서버는 보관 기간 동안 이름과 포트가 그대로 잡혀 있고, 복구하면 월드도 그대로 돌아옵니다.
          </p>
        </div>

        {error && <p className="error">{error}</p>}
        {servers === null && !error && <p className="muted">불러오는 중…</p>}
        {servers?.length === 0 && <p className="muted small">휴지통이 비어 있습니다.</p>}

        {servers && servers.length > 0 && (
          <table className="servers">
            <thead>
              <tr>
                <th>이름</th>
                <th>포트</th>
                <th>지운 시각</th>
                <th>완전 삭제</th>
                <th aria-label="작업" />
              </tr>
            </thead>
            <tbody>
              {servers.map((server) => (
                <tr key={server.id}>
                  <td>{server.name}</td>
                  <td>{server.port}</td>
                  <td className="muted">{formatDateTime(server.deletedAt)}</td>
                  <td title={formatDateTime(server.purgeAt)}>{formatRemaining(server.purgeAt)}</td>
                  <td className="row-actions">
                    <button
                      type="button"
                      className="compact"
                      onClick={() => handleRestore(server)}
                      disabled={restoringId !== null}
                    >
                      {restoringId === server.id ? '복구 중…' : '복구'}
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>
    </main>
  )
}
