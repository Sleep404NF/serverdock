import { useEffect, useState } from 'react'
import { ApiError, fetchMe, fetchServers } from './api'
import type { Me, Server } from './api'

interface Props {
  onLogout: () => void
}

export default function ServerListPage({ onLogout }: Props) {
  const [me, setMe] = useState<Me | null>(null)
  const [servers, setServers] = useState<Server[] | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    Promise.all([fetchMe(), fetchServers()])
      .then(([meResult, serverResult]) => {
        setMe(meResult)
        setServers(serverResult)
      })
      .catch((e: unknown) => {
        if (e instanceof ApiError && e.status === 401) {
          onLogout()
          return
        }
        setError(e instanceof Error ? e.message : '서버 목록을 불러오지 못했습니다.')
      })
  }, [onLogout])

  return (
    <main className="page">
      <header className="topbar">
        <h1>내 서버</h1>
        <div className="user">
          {me && (
            <span>
              {me.username}
              {me.systemRole === 'ADMIN' && ' (관리자)'}
            </span>
          )}
          <button type="button" className="secondary" onClick={onLogout}>
            로그아웃
          </button>
        </div>
      </header>

      {error && <p className="error">{error}</p>}
      {servers === null && !error && <p className="muted">불러오는 중…</p>}
      {servers?.length === 0 && <p className="muted">아직 속한 서버가 없습니다.</p>}

      {servers && servers.length > 0 && (
        <table className="servers">
          <thead>
            <tr>
              <th>이름</th>
              <th>버전</th>
              <th>로더</th>
              <th>메모리</th>
              <th>포트</th>
              <th>내 역할</th>
            </tr>
          </thead>
          <tbody>
            {servers.map((server) => (
              <tr key={server.id}>
                <td>{server.name}</td>
                <td>{server.gameVersion}</td>
                <td>{server.loader}</td>
                <td>{server.memoryMb} MB</td>
                <td>{server.port}</td>
                <td>
                  <span className={`role role-${server.myRole.toLowerCase()}`}>{server.myRole}</span>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </main>
  )
}
