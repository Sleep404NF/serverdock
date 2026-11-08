import { useCallback, useState } from 'react'
import { clearToken, getToken } from './api'
import LoginPage from './LoginPage'
import ServerListPage from './ServerListPage'

/**
 * 확인용 최소 화면. 토큰이 있으면 서버 목록, 없으면 로그인 화면을 보여준다.
 */
export default function App() {
  const [loggedIn, setLoggedIn] = useState(() => getToken() !== null)

  const handleLogout = useCallback(() => {
    clearToken()
    setLoggedIn(false)
  }, [])

  if (!loggedIn) {
    return <LoginPage onLoggedIn={() => setLoggedIn(true)} />
  }

  return <ServerListPage onLogout={handleLogout} />
}
