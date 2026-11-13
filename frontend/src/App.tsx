import { useCallback, useEffect, useState } from 'react'
import { clearToken, getToken } from './api'
import InvitePage from './InvitePage'
import LoginPage from './LoginPage'
import ServerDetailPage from './ServerDetailPage'
import type { ServerTab } from './ServerDetailPage'
import ServerListPage from './ServerListPage'
import CreateServerPage from './CreateServerPage'
import TrashPage from './TrashPage'
import AccountPage from './AccountPage'

const SERVER_ROUTE = /^#\/servers\/(\d+)(?:\/(members|invites|settings))?$/
const INVITE_ROUTE = /^#\/invite\/([A-Za-z0-9_-]+)$/

/**
 * 주소의 # 뒷부분으로 화면을 고른다. 새로고침해도 보던 화면이 유지된다.
 * - #/servers/3            3번 서버 (콘솔)
 * - #/servers/3/members    3번 서버 멤버
 * - #/servers/3/invites    3번 서버 초대 링크
 * - #/servers/3/settings   3번 서버 설정
 * - #/new                  새 서버 만들기
 * - #/trash                휴지통
 * - #/account              내 계정 (디스코드 연결). 디스코드에서 돌아오면 ?discord=결과 가 붙는다
 * - #/invite/코드           초대 수락
 * - 그 외                   서버 목록
 */
function useHash() {
  const [hash, setHash] = useState(() => window.location.hash)

  useEffect(() => {
    const handleChange = () => setHash(window.location.hash)
    window.addEventListener('hashchange', handleChange)
    return () => window.removeEventListener('hashchange', handleChange)
  }, [])

  return hash
}

export default function App() {
  const [loggedIn, setLoggedIn] = useState(() => getToken() !== null)
  const hash = useHash()

  const handleLogout = useCallback(() => {
    clearToken()
    setLoggedIn(false)
  }, [])

  const inviteMatch = INVITE_ROUTE.exec(hash)

  if (!loggedIn) {
    // 로그인하면 주소는 그대로라서, 초대 링크로 왔다면 로그인 뒤 바로 초대 화면이 나온다.
    return (
      <LoginPage
        onLoggedIn={() => setLoggedIn(true)}
        notice={inviteMatch ? '서버에 초대받았습니다. 로그인하거나 가입하면 참가할 수 있습니다.' : undefined}
      />
    )
  }

  if (inviteMatch) {
    return <InvitePage code={inviteMatch[1]} onLogout={handleLogout} />
  }

  const serverMatch = SERVER_ROUTE.exec(hash)
  if (serverMatch) {
    const serverId = Number(serverMatch[1])
    const tab = (serverMatch[2] ?? 'console') as ServerTab
    return <ServerDetailPage key={serverId} serverId={serverId} tab={tab} onLogout={handleLogout} />
  }

  if (hash === '#/new') {
    return <CreateServerPage onLogout={handleLogout} />
  }
  if (hash === '#/trash') {
    return <TrashPage onLogout={handleLogout} />
  }
  if (hash === '#/account' || hash.startsWith('#/account?')) {
    const result = new URLSearchParams(hash.split('?')[1] ?? '').get('discord')
    return <AccountPage discordResult={result} onLogout={handleLogout} />
  }

  return <ServerListPage onLogout={handleLogout} />
}
