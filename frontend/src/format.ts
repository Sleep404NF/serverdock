/**
 * 백엔드 시간(LocalDateTime, 시간대 없음)을 화면용 문자열로 바꾼다.
 * 백엔드와 브라우저가 같은 시간대라고 보고 그대로 읽는다.
 */
export function formatDateTime(value: string) {
  return new Date(value).toLocaleString('ko-KR', {
    month: 'long',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

export function formatDate(value: string) {
  return new Date(value).toLocaleDateString('ko-KR', { year: 'numeric', month: 'long', day: 'numeric' })
}

/** "3시간 뒤", "2일 뒤", "곧" 처럼 남은 시간을 대략 말한다. 반올림해서 1일짜리가 "23시간 뒤"로 보이지 않게 한다. */
export function formatRemaining(value: string, now: number = Date.now()) {
  const minutes = Math.round((new Date(value).getTime() - now) / 60000)
  if (minutes < 1) {
    return '곧'
  }
  if (minutes < 60) {
    return `${minutes}분 뒤`
  }
  const hours = Math.round(minutes / 60)
  if (hours < 24) {
    return `${hours}시간 뒤`
  }
  return `${Math.round(hours / 24)}일 뒤`
}
