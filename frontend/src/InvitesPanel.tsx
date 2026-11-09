import { useCallback, useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { ApiError, createInvite, fetchInvites, inviteUrl, revokeInvite } from './api'
import type { Invite, InviteExpiry, ServerRole } from './api'
import { formatDateTime, formatRemaining } from './format'

const EXPIRY_OPTIONS: { value: InviteExpiry; label: string }[] = [
  { value: 'THIRTY_MINUTES', label: '30분' },
  { value: 'ONE_HOUR', label: '1시간' },
  { value: 'SIX_HOURS', label: '6시간' },
  { value: 'TWELVE_HOURS', label: '12시간' },
  { value: 'ONE_DAY', label: '1일' },
  { value: 'SEVEN_DAYS', label: '7일' },
]

const USES_OPTIONS = [
  { value: 'unlimited', label: '제한 없음' },
  { value: '1', label: '1회' },
  { value: '5', label: '5회' },
  { value: '10', label: '10회' },
  { value: '25', label: '25회' },
]

/**
 * 복사한다. 클립보드 API는 https나 localhost에서만 열려 있어서, 친구가 http://IP로 접속하면 막힌다.
 * 그때는 예전 방식(보이지 않는 입력칸을 선택해서 복사)으로 한 번 더 시도한다.
 */
async function copyText(text: string) {
  try {
    await navigator.clipboard.writeText(text)
    return true
  } catch {
    return copyWithSelection(text)
  }
}

function copyWithSelection(text: string) {
  const textarea = document.createElement('textarea')
  textarea.value = text
  textarea.setAttribute('readonly', '')
  textarea.style.position = 'fixed'
  textarea.style.opacity = '0'
  document.body.appendChild(textarea)
  textarea.select()
  try {
    return document.execCommand('copy')
  } catch {
    return false
  } finally {
    textarea.remove()
  }
}

function usageText(invite: Invite) {
  return invite.maxUses === null ? `${invite.usedCount}회 사용` : `${invite.usedCount} / ${invite.maxUses}회`
}

function stateText(invite: Invite) {
  if (invite.usable) {
    return `${formatRemaining(invite.expiresAt)} 만료`
  }
  return new Date(invite.expiresAt).getTime() <= Date.now() ? '만료됨' : '횟수 다 씀'
}

interface Props {
  serverId: number
  myRole: ServerRole
  onLogout: () => void
}

export default function InvitesPanel({ serverId, myRole, onLogout }: Props) {
  const [invites, setInvites] = useState<Invite[] | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [role, setRole] = useState<ServerRole>('MEMBER')
  const [expiresIn, setExpiresIn] = useState<InviteExpiry>('ONE_DAY')
  const [maxUses, setMaxUses] = useState('unlimited')
  const [creating, setCreating] = useState(false)
  const [created, setCreated] = useState<Invite | null>(null)
  const [copiedId, setCopiedId] = useState<number | null>(null)

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
    fetchInvites(serverId)
      .then(setInvites)
      .catch((e: unknown) => setError(toMessage(e, '초대 링크를 불러오지 못했습니다.')))
  }, [serverId, toMessage])

  async function handleCreate(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setCreating(true)
    setError(null)
    try {
      const invite = await createInvite(serverId, {
        role,
        expiresIn,
        maxUses: maxUses === 'unlimited' ? null : Number(maxUses),
      })
      setCreated(invite)
      setInvites((list) => (list ? [invite, ...list] : [invite]))
    } catch (e) {
      setError(toMessage(e, '초대 링크를 만들지 못했습니다.'))
    } finally {
      setCreating(false)
    }
  }

  async function handleCopy(invite: Invite) {
    if (await copyText(inviteUrl(invite.code))) {
      setCopiedId(invite.id)
      window.setTimeout(() => setCopiedId((id) => (id === invite.id ? null : id)), 2000)
    } else {
      setCreated(invite)
      setError('자동으로 복사하지 못했습니다. 위 주소를 직접 복사해주세요.')
    }
  }

  async function handleRevoke(invite: Invite) {
    setError(null)
    try {
      await revokeInvite(serverId, invite.id)
      setInvites((list) => list && list.filter((item) => item.id !== invite.id))
      setCreated((current) => (current?.id === invite.id ? null : current))
    } catch (e) {
      setError(toMessage(e, '초대 링크를 폐기하지 못했습니다.'))
    }
  }

  return (
    <section className="card panel">
      <div className="panel-head">
        <h2>초대 링크</h2>
        <p className="muted small">링크를 받은 친구가 로그인하거나 가입하면 서버에 참가합니다.</p>
      </div>

      <form className="invite-form" onSubmit={handleCreate}>
        <label>
          역할
          {myRole === 'OWNER' ? (
            <select value={role} onChange={(event) => setRole(event.target.value as ServerRole)}>
              <option value="MEMBER">MEMBER</option>
              <option value="ADMIN">ADMIN</option>
            </select>
          ) : (
            <span className="static-field">MEMBER</span>
          )}
        </label>
        <label>
          만료
          <select value={expiresIn} onChange={(event) => setExpiresIn(event.target.value as InviteExpiry)}>
            {EXPIRY_OPTIONS.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
        </label>
        <label>
          사용 횟수
          <select value={maxUses} onChange={(event) => setMaxUses(event.target.value)}>
            {USES_OPTIONS.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
        </label>
        <button type="submit" disabled={creating}>
          {creating ? '만드는 중…' : '링크 만들기'}
        </button>
      </form>

      {created && (
        <div className="invite-created">
          <input
            readOnly
            value={inviteUrl(created.code)}
            onFocus={(event) => event.target.select()}
            aria-label="초대 링크 주소"
          />
          <button type="button" onClick={() => handleCopy(created)}>
            {copiedId === created.id ? '복사됨' : '복사'}
          </button>
        </div>
      )}

      {error && <p className="error">{error}</p>}
      {invites === null && !error && <p className="muted">불러오는 중…</p>}
      {invites?.length === 0 && <p className="muted small">아직 만든 초대 링크가 없습니다.</p>}

      {invites && invites.length > 0 && (
        <table className="servers">
          <thead>
            <tr>
              <th>코드</th>
              <th>역할</th>
              <th>사용</th>
              <th>상태</th>
              <th>만든 사람</th>
              <th aria-label="작업" />
            </tr>
          </thead>
          <tbody>
            {invites.map((invite) => (
              <tr key={invite.id} className={invite.usable ? undefined : 'dim'}>
                <td className="mono" title={`${formatDateTime(invite.createdAt)}에 만듦`}>
                  {invite.code.slice(0, 8)}…
                </td>
                <td>
                  <span className={`role role-${invite.role.toLowerCase()}`}>{invite.role}</span>
                </td>
                <td>{usageText(invite)}</td>
                <td title={`${formatDateTime(invite.expiresAt)} 만료`}>{stateText(invite)}</td>
                <td className="muted">{invite.createdBy}</td>
                <td className="row-actions">
                  {invite.usable && (
                    <button type="button" className="secondary compact" onClick={() => handleCopy(invite)}>
                      {copiedId === invite.id ? '복사됨' : '복사'}
                    </button>
                  )}
                  <button type="button" className="danger compact" onClick={() => handleRevoke(invite)}>
                    폐기
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </section>
  )
}
