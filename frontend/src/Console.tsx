import { useCallback, useEffect, useLayoutEffect, useRef, useState } from 'react'
import type { FormEvent, KeyboardEvent } from 'react'
import { ApiError, sendCommand, streamLogs } from './api'
import type { ServerStatus } from './api'

type LineKind = 'log' | 'warn' | 'error' | 'command' | 'output' | 'system'

interface Line {
  id: number
  kind: LineKind
  text: string
}

/** 화면에 남겨둘 최대 줄 수. 오래 켜둬도 브라우저가 무거워지지 않게 한다. */
const MAX_LINES = 1000
const RETRY_DELAY_MS = 3000
const HISTORY_LIMIT = 50

/** 마인크래프트 로그([시간] [스레드/WARN]: ...)와 itzg 실행기 로그(시간\tWARN\t...)의 수준을 읽는다. */
function kindOf(line: string): LineKind {
  if (/\/(ERROR|FATAL)\]|\t(ERROR|FATAL)\t/.test(line)) {
    return 'error'
  }
  if (/\/WARN\]|\tWARN\t/.test(line)) {
    return 'warn'
  }
  return 'log'
}

/** 마인크래프트는 명령어 오류를 두 줄로 보내는데 RCON으로 받으면 줄바꿈이 빠진다. */
function splitOutput(output: string) {
  return output.replace(/(see below for error)(?=\S)/, '$1\n').split('\n')
}

interface Props {
  serverId: number
  status: ServerStatus | null
  canCommand: boolean
  onUnauthorized: () => void
}

export default function Console({ serverId, status, canCommand, onUnauthorized }: Props) {
  const [lines, setLines] = useState<Line[]>([])
  const [connected, setConnected] = useState(false)
  const [retry, setRetry] = useState(0)
  const [command, setCommand] = useState('')
  const [sending, setSending] = useState(false)

  const nextId = useRef(0)
  const logRef = useRef<HTMLDivElement>(null)
  const stickToBottom = useRef(true)
  const history = useRef<string[]>([])
  const historyIndex = useRef(-1)

  const append = useCallback((kind: LineKind, text: string) => {
    const line = { id: nextId.current++, kind, text }
    setLines((prev) => {
      const next = [...prev, line]
      return next.length > MAX_LINES ? next.slice(next.length - MAX_LINES) : next
    })
  }, [])

  const available = status !== null && status !== 'NOT_CREATED'
  const running = status === 'STARTING' || status === 'RUNNING'

  // 서버가 새로 켜질 때마다 bootCount를 올린다. 꺼질 때는 스트림이 end 이벤트로 알아서 끝나므로 그대로 둔다.
  const [prevRunning, setPrevRunning] = useState(running)
  const [bootCount, setBootCount] = useState(0)
  if (running !== prevRunning) {
    setPrevRunning(running)
    if (running) {
      setBootCount((count) => count + 1)
    }
  }

  // 로그 스트림. 서버가 새로 켜지면 다시 연결해서 이번 실행의 로그부터 받는다.
  // 연결이 끊기면(백엔드 재시작 등) 3초 뒤에 다시 연결한다.
  useEffect(() => {
    if (!available) {
      return
    }
    const controller = new AbortController()
    let retryTimer: number | undefined

    streamLogs(
      serverId,
      {
        onOpen: () => {
          setLines([])
          setConnected(true)
        },
        onLog: (line) => append(kindOf(line), line),
        onEnd: (message) => append('system', message),
      },
      controller.signal,
    )
      .then(() => setConnected(false))
      .catch((e: unknown) => {
        if (controller.signal.aborted) {
          return
        }
        setConnected(false)
        if (e instanceof ApiError && e.status === 401) {
          onUnauthorized()
          return
        }
        append('error', '로그 연결이 끊겼습니다. 잠시 뒤 다시 연결합니다.')
        retryTimer = window.setTimeout(() => setRetry((n) => n + 1), RETRY_DELAY_MS)
      })

    return () => {
      controller.abort()
      window.clearTimeout(retryTimer)
    }
  }, [serverId, available, bootCount, retry, append, onUnauthorized])

  // 맨 아래를 보고 있을 때만 새 로그를 따라 내려간다. 위로 올려서 읽는 중이면 그대로 둔다.
  useLayoutEffect(() => {
    const element = logRef.current
    if (element && stickToBottom.current) {
      element.scrollTop = element.scrollHeight
    }
  }, [lines])

  function handleScroll() {
    const element = logRef.current
    if (element) {
      stickToBottom.current = element.scrollHeight - element.scrollTop - element.clientHeight < 24
    }
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const text = command.trim()
    if (!text || sending) {
      return
    }

    history.current = [text, ...history.current.filter((item) => item !== text)].slice(0, HISTORY_LIMIT)
    historyIndex.current = -1
    setCommand('')
    setSending(true)
    stickToBottom.current = true
    append('command', `> ${text}`)

    try {
      const result = await sendCommand(serverId, text)
      for (const outputLine of splitOutput(result.output)) {
        if (outputLine.trim()) {
          append('output', outputLine)
        }
      }
    } catch (e) {
      if (e instanceof ApiError && e.status === 401) {
        onUnauthorized()
        return
      }
      append('error', e instanceof Error ? e.message : '명령어를 보내지 못했습니다.')
    } finally {
      setSending(false)
    }
  }

  // 위/아래 화살표로 보냈던 명령어를 다시 불러온다.
  function handleKeyDown(event: KeyboardEvent<HTMLInputElement>) {
    if (event.key !== 'ArrowUp' && event.key !== 'ArrowDown') {
      return
    }
    const items = history.current
    if (items.length === 0) {
      return
    }
    event.preventDefault()
    const next =
      event.key === 'ArrowUp' ? Math.min(historyIndex.current + 1, items.length - 1) : historyIndex.current - 1
    historyIndex.current = Math.max(next, -1)
    setCommand(historyIndex.current === -1 ? '' : items[historyIndex.current])
  }

  const commandReady = status === 'RUNNING'

  return (
    <section className="card console">
      <div className="console-head">
        <h2>콘솔</h2>
        <span className={`live${connected ? ' on' : ''}`}>{connected ? '실시간' : '연결 안 됨'}</span>
      </div>

      <div className="console-log" ref={logRef} onScroll={handleScroll} role="log">
        {!available && (
          <div className="line system">
            {status === null ? '불러오는 중…' : '서버를 한 번 켜면 여기에 로그가 나옵니다.'}
          </div>
        )}
        {lines.map((line) => (
          <div key={line.id} className={`line ${line.kind}`}>
            {line.text}
          </div>
        ))}
      </div>

      {canCommand ? (
        <form className="console-input" onSubmit={handleSubmit}>
          <input
            value={command}
            onChange={(event) => setCommand(event.target.value)}
            onKeyDown={handleKeyDown}
            placeholder={commandReady ? '명령어 입력 (예: list, say 안녕)' : '서버가 켜진 뒤에 명령어를 보낼 수 있습니다'}
            disabled={!commandReady}
            maxLength={256}
            spellCheck={false}
            autoComplete="off"
            aria-label="콘솔 명령어"
          />
          <button type="submit" disabled={!commandReady || sending || !command.trim()}>
            보내기
          </button>
        </form>
      ) : (
        <p className="muted small">명령어는 ADMIN 이상만 보낼 수 있습니다.</p>
      )}
    </section>
  )
}
