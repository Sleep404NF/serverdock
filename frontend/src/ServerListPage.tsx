import { useEffect, useState } from 'react'
import { ApiError, fetchMe, fetchServers, fetchTrash } from './api'
import type { Me, Server } from './api'
import { formatMemory } from './serverOptions'

interface Props {
  onLogout: () => void
}

export default function ServerListPage({ onLogout }: Props) {
  const [me, setMe] = useState<Me | null>(null)
  const [servers, setServers] = useState<Server[] | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [trashCount, setTrashCount] = useState(0)

  useEffect(() => {
    // 휴지통은 못 읽어도 목록에는 지장이 없어서 조용히 넘어간다.
    fetchTrash()
      .then((trash) => setTrashCount(trash.length))
      .catch(() => setTrashCount(0))
  }, [])

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
            <a href="#/account" className="account-link">
              {me.username}
              {me.systemRole === 'ADMIN' && ' (관리자)'}
            </a>
          )}
          <button type="button" className="secondary" onClick={onLogout}>
            로그아웃
          </button>
        </div>
      </header>

      {(me?.systemRole === 'ADMIN' || trashCount > 0) && (
        <div className="list-actions">
          {me?.systemRole === 'ADMIN' && (
            <a href="#/new" className="button">
              + 새 서버
            </a>
          )}
          {trashCount > 0 && (
            <a href="#/trash" className="small">
              휴지통 ({trashCount})
            </a>
          )}
        </div>
      )}

      {error && <p className="error">{error}</p>}
      {servers === null && !error && <p className="muted">불러오는 중…</p>}
      {servers?.length === 0 && (
        <p className="muted">
          {me?.systemRole === 'ADMIN'
            ? '아직 서버가 없습니다. 새 서버를 만들어 보세요.'
            : '아직 속한 서버가 없습니다. 친구에게 초대 링크를 받아 참가할 수 있습니다.'}
        </p>
      )}

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
                <td>
                  <a href={`#/servers/${server.id}`}>{server.name}</a>
                </td>
                <td>{server.gameVersion}</td>
                <td>{server.loader}</td>
                <td>{formatMemory(server.memoryMb)}</td>
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
