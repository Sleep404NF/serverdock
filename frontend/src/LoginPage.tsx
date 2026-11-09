import { useState } from 'react'
import type { FormEvent } from 'react'
import { login, signup } from './api'

interface Props {
  onLoggedIn: () => void
  /** 초대 링크로 들어왔을 때 보여줄 안내 */
  notice?: string
}

type Mode = 'login' | 'signup'

export default function LoginPage({ onLoggedIn, notice }: Props) {
  const [mode, setMode] = useState<Mode>('login')
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  const isSignup = mode === 'signup'

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setSubmitting(true)
    setError(null)
    try {
      if (isSignup) {
        await signup(username, password)
      }
      await login(username, password)
      onLoggedIn()
    } catch (e) {
      setError(e instanceof Error ? e.message : isSignup ? '가입에 실패했습니다.' : '로그인에 실패했습니다.')
    } finally {
      setSubmitting(false)
    }
  }

  function switchMode() {
    setMode(isSignup ? 'login' : 'signup')
    setError(null)
  }

  return (
    <main className="page">
      <form className="card login" onSubmit={handleSubmit}>
        <h1>{isSignup ? 'ServerDock 가입' : 'ServerDock'}</h1>
        {notice && <p className="notice">{notice}</p>}
        <label>
          아이디
          <input
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            autoComplete="username"
            required
          />
          {isSignup && <span className="hint">6~20자, 영문·숫자·밑줄(_)</span>}
        </label>
        <label>
          비밀번호
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            autoComplete={isSignup ? 'new-password' : 'current-password'}
            required
          />
          {isSignup && <span className="hint">8자 이상</span>}
        </label>
        {error && <p className="error">{error}</p>}
        <button type="submit" disabled={submitting}>
          {isSignup ? (submitting ? '가입 중…' : '가입하고 시작하기') : submitting ? '로그인 중…' : '로그인'}
        </button>
        <button type="button" className="link" onClick={switchMode}>
          {isSignup ? '이미 계정이 있나요? 로그인' : '계정이 없나요? 가입하기'}
        </button>
      </form>
    </main>
  )
}
