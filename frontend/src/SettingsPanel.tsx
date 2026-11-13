import { useEffect, useRef, useState } from 'react'
import type { FormEvent } from 'react'
import { ApiError, deleteServer, updateServer } from './api'
import type { Server, UpdateServerInput } from './api'
import { formatMemory, memoryOptionsWith } from './serverOptions'
import { navigate } from './navigate'

/** 삭제는 한 번 더 눌러야 실행된다. 이 시간 안에 다시 안 누르면 취소. */
const CONFIRM_TIMEOUT_MS = 5000

interface Props {
  server: Server
  /** 저장이 끝나면 바뀐 서버 정보를 넘긴다. 상세 화면이 상태(재시작 필요 여부)를 다시 읽는다. */
  onSaved: (server: Server) => void
  onLogout: () => void
}

/**
 * 서버 설정 탭. OWNER만 보인다.
 * 버전과 로더는 바꾸면 월드가 깨질 수 있어서 여기서는 못 바꾼다.
 */
export default function SettingsPanel({ server, onSaved, onLogout }: Props) {
  const [name, setName] = useState(server.name)
  const [memoryMb, setMemoryMb] = useState(server.memoryMb)
  const [autoRestart, setAutoRestart] = useState(server.autoRestart)
  const [saving, setSaving] = useState(false)
  const [message, setMessage] = useState<{ kind: 'error' | 'done'; text: string } | null>(null)
  const [confirmingDelete, setConfirmingDelete] = useState(false)
  const [deleting, setDeleting] = useState(false)
  const confirmTimer = useRef<number | undefined>(undefined)

  useEffect(() => () => window.clearTimeout(confirmTimer.current), [])

  const changes: UpdateServerInput = {}
  if (name.trim() !== server.name) {
    changes.name = name.trim()
  }
  if (memoryMb !== server.memoryMb) {
    changes.memoryMb = memoryMb
  }
  if (autoRestart !== server.autoRestart) {
    changes.autoRestart = autoRestart
  }
  const dirty = Object.keys(changes).length > 0

  function toMessage(e: unknown, fallback: string) {
    if (e instanceof ApiError && e.status === 401) {
      onLogout()
      return null
    }
    return e instanceof Error ? e.message : fallback
  }

  async function handleSave(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!dirty) {
      return
    }
    setSaving(true)
    setMessage(null)
    try {
      const updated = await updateServer(server.id, changes)
      onSaved(updated)
      setName(updated.name)
      setMessage({ kind: 'done', text: '저장했습니다.' })
    } catch (e) {
      const text = toMessage(e, '저장하지 못했습니다.')
      setMessage(text ? { kind: 'error', text } : null)
    } finally {
      setSaving(false)
    }
  }

  async function handleDelete() {
    if (!confirmingDelete) {
      setConfirmingDelete(true)
      window.clearTimeout(confirmTimer.current)
      confirmTimer.current = window.setTimeout(() => setConfirmingDelete(false), CONFIRM_TIMEOUT_MS)
      return
    }

    window.clearTimeout(confirmTimer.current)
    setConfirmingDelete(false)
    setDeleting(true)
    try {
      await deleteServer(server.id)
      navigate('#/trash')
    } catch (e) {
      const text = toMessage(e, '서버를 지우지 못했습니다.')
      setMessage(text ? { kind: 'error', text } : null)
      setDeleting(false)
    }
  }

  return (
    <>
      <form className="card panel" onSubmit={handleSave}>
        <div className="panel-head">
          <h2>설정</h2>
          <p className="muted small">메모리와 자동 재시작은 서버를 다시 켤 때 적용됩니다.</p>
        </div>

        <label>
          이름
          <input value={name} onChange={(e) => setName(e.target.value)} maxLength={50} required />
        </label>

        <label>
          메모리
          <select value={memoryMb} onChange={(e) => setMemoryMb(Number(e.target.value))}>
            {memoryOptionsWith(server.memoryMb).map((value) => (
              <option key={value} value={value}>
                {formatMemory(value)}
              </option>
            ))}
          </select>
        </label>

        <label className="checkbox">
          <input type="checkbox" checked={autoRestart} onChange={(e) => setAutoRestart(e.target.checked)} />
          <span>
            자동 재시작
            <span className="hint">서버가 갑자기 꺼지거나 컴퓨터를 다시 켰을 때 알아서 다시 켭니다.</span>
          </span>
        </label>

        <p className="muted small">
          버전 {server.gameVersion} · {server.loader}
          {server.loaderVersion && ` ${server.loaderVersion}`} · 포트 {server.port} — 월드가 깨질 수 있어서 버전과
          로더는 바꿀 수 없습니다.
        </p>

        {/* "저장했습니다"는 다시 고치기 시작하면 숨긴다 */}
        {message && !(message.kind === 'done' && dirty) && (
          <p className={message.kind === 'error' ? 'error' : 'success'}>{message.text}</p>
        )}
        <div className="actions">
          <button type="submit" disabled={!dirty || saving}>
            {saving ? '저장 중…' : '저장'}
          </button>
        </div>
      </form>

      <section className="card panel danger-zone">
        <div className="panel-head">
          <h2>서버 삭제</h2>
          <p className="muted small">
            서버를 끄고 휴지통으로 옮깁니다. 보관 기간(기본 2일) 안에는 휴지통에서 월드째로 복구할 수 있고, 그 뒤에는
            월드까지 완전히 지워집니다.
          </p>
        </div>
        <div className="actions">
          <button
            type="button"
            className={confirmingDelete ? 'danger filled' : 'danger'}
            onClick={handleDelete}
            disabled={deleting}
          >
            {deleting ? '지우는 중…' : confirmingDelete ? '정말 지우기' : '서버 삭제'}
          </button>
        </div>
      </section>
    </>
  )
}
