import { useCallback, useEffect, useRef, useState } from 'react'
import { ApiError, changeMemberRole, fetchMe, fetchMembers, outranks, removeMember, sortMembers } from './api'
import type { Me, Member, ServerRole } from './api'
import { formatDate } from './format'

/** 내보내기/나가기는 한 번 더 눌러야 실행된다. 이 시간 안에 다시 안 누르면 취소. */
const CONFIRM_TIMEOUT_MS = 4000

interface Props {
  serverId: number
  myRole: ServerRole
  onLogout: () => void
}

export default function MembersPanel({ serverId, myRole, onLogout }: Props) {
  const [me, setMe] = useState<Me | null>(null)
  const [members, setMembers] = useState<Member[] | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [pendingUserId, setPendingUserId] = useState<number | null>(null)
  const [confirmingUserId, setConfirmingUserId] = useState<number | null>(null)
  const confirmTimer = useRef<number | undefined>(undefined)

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
    Promise.all([fetchMe(), fetchMembers(serverId)])
      .then(([meResult, memberResult]) => {
        setMe(meResult)
        setMembers(memberResult)
      })
      .catch((e: unknown) => setError(toMessage(e, '멤버를 불러오지 못했습니다.')))
  }, [serverId, toMessage])

  useEffect(() => () => window.clearTimeout(confirmTimer.current), [])

  async function handleRoleChange(member: Member, role: ServerRole) {
    setPendingUserId(member.userId)
    setError(null)
    try {
      const updated = await changeMemberRole(serverId, member.userId, role)
      setMembers((list) => list && sortMembers(list.map((item) => (item.userId === updated.userId ? updated : item))))
    } catch (e) {
      setError(toMessage(e, '역할을 바꾸지 못했습니다.'))
    } finally {
      setPendingUserId(null)
    }
  }

  async function handleRemove(member: Member, isMe: boolean) {
    if (confirmingUserId !== member.userId) {
      setConfirmingUserId(member.userId)
      window.clearTimeout(confirmTimer.current)
      confirmTimer.current = window.setTimeout(() => setConfirmingUserId(null), CONFIRM_TIMEOUT_MS)
      return
    }

    window.clearTimeout(confirmTimer.current)
    setConfirmingUserId(null)
    setPendingUserId(member.userId)
    setError(null)
    try {
      await removeMember(serverId, member.userId)
      if (isMe) {
        window.location.hash = '#/'
        return
      }
      setMembers((list) => list && list.filter((item) => item.userId !== member.userId))
    } catch (e) {
      setError(toMessage(e, isMe ? '서버를 나가지 못했습니다.' : '멤버를 내보내지 못했습니다.'))
    } finally {
      setPendingUserId(null)
    }
  }

  return (
    <section className="card panel">
      <div className="panel-head">
        <h2>멤버{members && ` ${members.length}명`}</h2>
        <p className="muted small">
          {myRole === 'OWNER'
            ? '역할을 바꾸거나 멤버를 내보낼 수 있습니다.'
            : myRole === 'ADMIN'
              ? 'MEMBER를 내보낼 수 있습니다.'
              : '멤버 관리는 ADMIN 이상만 할 수 있습니다.'}
        </p>
      </div>

      {error && <p className="error">{error}</p>}
      {members === null && !error && <p className="muted">불러오는 중…</p>}

      {members && me && (
        <table className="servers">
          <thead>
            <tr>
              <th>아이디</th>
              <th>역할</th>
              <th>참가일</th>
              <th aria-label="작업" />
            </tr>
          </thead>
          <tbody>
            {members.map((member) => {
              const isMe = member.userId === me.id
              const canChangeRole = myRole === 'OWNER' && member.role !== 'OWNER'
              const canKick = !isMe && outranks(myRole, member.role)
              const canLeave = isMe && member.role !== 'OWNER'
              const pending = pendingUserId === member.userId
              const confirming = confirmingUserId === member.userId

              return (
                <tr key={member.userId}>
                  <td>
                    {member.username}
                    {isMe && <span className="muted"> (나)</span>}
                  </td>
                  <td>
                    {canChangeRole ? (
                      <select
                        value={member.role}
                        onChange={(event) => handleRoleChange(member, event.target.value as ServerRole)}
                        disabled={pending}
                        aria-label={`${member.username} 역할`}
                      >
                        <option value="ADMIN">ADMIN</option>
                        <option value="MEMBER">MEMBER</option>
                      </select>
                    ) : (
                      <span className={`role role-${member.role.toLowerCase()}`}>{member.role}</span>
                    )}
                  </td>
                  <td className="muted">{formatDate(member.joinedAt)}</td>
                  <td className="row-actions">
                    {(canKick || canLeave) && (
                      <button
                        type="button"
                        className={confirming ? 'danger filled compact' : 'danger compact'}
                        onClick={() => handleRemove(member, isMe)}
                        disabled={pending}
                      >
                        {canLeave ? (confirming ? '정말 나가기' : '서버 나가기') : confirming ? '정말 내보내기' : '내보내기'}
                      </button>
                    )}
                  </td>
                </tr>
              )
            })}
          </tbody>
        </table>
      )}
    </section>
  )
}
