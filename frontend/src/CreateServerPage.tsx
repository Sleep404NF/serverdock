import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { ApiError, createServer, fetchMe } from './api'
import type { Me, ServerLoader } from './api'
import { LOADER_OPTIONS, MEMORY_OPTIONS, formatMemory } from './serverOptions'
import { navigate } from './navigate'

interface Props {
  onLogout: () => void
}

/**
 * 새 서버 만들기(#/new). 시스템 관리자만 만들 수 있고, 만든 사람이 OWNER가 된다.
 * 만들기만 하고 켜지는 않는다. 처음 켤 때 컨테이너와 월드가 생긴다.
 */
export default function CreateServerPage({ onLogout }: Props) {
  const [me, setMe] = useState<Me | null>(null)
  const [name, setName] = useState('')
  const [gameVersion, setGameVersion] = useState('')
  const [loader, setLoader] = useState<ServerLoader>('VANILLA')
  const [loaderVersion, setLoaderVersion] = useState('')
  const [memoryMb, setMemoryMb] = useState(2048)
  const [port, setPort] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  useEffect(() => {
    fetchMe()
      .then(setMe)
      .catch((e: unknown) => {
        if (e instanceof ApiError && e.status === 401) {
          onLogout()
        }
      })
  }, [onLogout])

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setSubmitting(true)
    setError(null)
    try {
      const server = await createServer({
        name: name.trim(),
        gameVersion: gameVersion.trim(),
        loader,
        loaderVersion: loader === 'VANILLA' || !loaderVersion.trim() ? null : loaderVersion.trim(),
        memoryMb,
        port: port.trim() ? Number(port) : null,
      })
      navigate(`#/servers/${server.id}`)
    } catch (e) {
      if (e instanceof ApiError && e.status === 401) {
        onLogout()
        return
      }
      setError(e instanceof Error ? e.message : '서버를 만들지 못했습니다.')
      setSubmitting(false)
    }
  }

  const selectedLoader = LOADER_OPTIONS.find((option) => option.value === loader)

  return (
    <main className="page">
      <header className="topbar">
        <a href="#/" className="back">
          ← 내 서버
        </a>
      </header>

      {me && me.systemRole !== 'ADMIN' && <p className="muted">서버는 관리자만 만들 수 있습니다.</p>}

      {me?.systemRole === 'ADMIN' && (
        <form className="card form" onSubmit={handleSubmit}>
          <h1>새 서버</h1>

          <label>
            이름
            <input value={name} onChange={(e) => setName(e.target.value)} maxLength={50} required />
            <span className="hint">다른 서버와 겹치지 않는 이름</span>
          </label>

          <div className="form-row">
            <label>
              마인크래프트 버전
              <input
                value={gameVersion}
                onChange={(e) => setGameVersion(e.target.value)}
                placeholder="예: 1.21.4"
                maxLength={32}
                required
              />
            </label>
            <label>
              로더
              <select value={loader} onChange={(e) => setLoader(e.target.value as ServerLoader)}>
                {LOADER_OPTIONS.map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </select>
              {selectedLoader && <span className="hint">{selectedLoader.hint}</span>}
            </label>
          </div>

          {loader !== 'VANILLA' && (
            <label>
              로더 버전 (선택)
              <input
                value={loaderVersion}
                onChange={(e) => setLoaderVersion(e.target.value)}
                placeholder="비우면 그 마인크래프트 버전에 맞는 최신"
                maxLength={32}
              />
            </label>
          )}

          <div className="form-row">
            <label>
              메모리
              <select value={memoryMb} onChange={(e) => setMemoryMb(Number(e.target.value))}>
                {MEMORY_OPTIONS.map((value) => (
                  <option key={value} value={value}>
                    {formatMemory(value)}
                  </option>
                ))}
              </select>
              <span className="hint">바닐라는 2 GB, 모드팩은 6 GB 이상 권장</span>
            </label>
            <label>
              포트 (선택)
              <input
                value={port}
                onChange={(e) => setPort(e.target.value.replace(/\D/g, ''))}
                placeholder="비우면 자동 (25565부터)"
                inputMode="numeric"
                maxLength={5}
              />
            </label>
          </div>

          {error && <p className="error">{error}</p>}
          <div className="actions">
            <button type="submit" disabled={submitting}>
              {submitting ? '만드는 중…' : '서버 만들기'}
            </button>
            <a href="#/" className="small">
              취소
            </a>
          </div>
          <p className="muted small">만들기만 하고 켜지는 않습니다. 처음 켤 때 월드가 만들어집니다.</p>
        </form>
      )}
    </main>
  )
}
