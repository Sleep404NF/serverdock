import { useCallback, useEffect, useState } from 'react'
import { clearToken, getToken } from './api'
import LoginPage from './LoginPage'
import ServerDetailPage from './ServerDetailPage'
import ServerListPage from './ServerListPage'

const SERVER_ROUTE = /^#\/servers\/(\d+)$/

/**
 * 주소의 # 뒷부분으로 화면을 고른다. #/servers/3이면 3번 서버 화면, 그 외에는 서버 목록.
 * 새로고침해도 보던 화면이 유지된다.
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

  if (!loggedIn) {
    return <LoginPage onLoggedIn={() => setLoggedIn(true)} />
  }

  const serverMatch = SERVER_ROUTE.exec(hash)
  if (serverMatch) {
    const serverId = Number(serverMatch[1])
    return <ServerDetailPage key={serverId} serverId={serverId} onLogout={handleLogout} />
  }

  return <ServerListPage onLogout={handleLogout} />
}
