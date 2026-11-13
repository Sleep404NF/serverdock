import { useState } from 'react'
import { ApiError, acceptInvite } from './api'
import { navigate } from './navigate'

interface Props {
  code: string
  onLogout: () => void
}

/**
 * 초대 링크(#/invite/코드)로 들어온 화면. 버튼을 눌러야 참가한다.
 * 링크를 열기만 해도 참가되면, 미리보기나 실수로 연 링크로도 멤버가 되어 버린다.
 */
export default function InvitePage({ code, onLogout }: Props) {
  const [joining, setJoining] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function handleJoin() {
    setJoining(true)
    setError(null)
    try {
      const server = await acceptInvite(code)
      navigate(`#/servers/${server.id}`)
    } catch (e) {
      if (e instanceof ApiError && e.status === 401) {
        onLogout()
        return
      }
      setError(e instanceof Error ? e.message : '참가하지 못했습니다.')
      setJoining(false)
    }
  }

  return (
    <main className="page">
      <section className="card login">
        <h1>서버 초대</h1>
        <p className="muted small">초대 링크로 마인크래프트 서버에 참가합니다. 참가하면 서버 목록에 추가됩니다.</p>
        {error && <p className="error">{error}</p>}
        <button type="button" onClick={handleJoin} disabled={joining}>
          {joining ? '참가하는 중…' : '서버에 참가하기'}
        </button>
        <a href="#/" className="small center">
          내 서버 목록으로
        </a>
      </section>
    </main>
  )
}
