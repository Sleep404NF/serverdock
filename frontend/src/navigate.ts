/**
 * 화면 이동. 주소의 # 뒷부분을 바꾸면 App이 hashchange를 듣고 화면을 바꾼다.
 */
export function navigate(hash: string) {
  window.location.hash = hash
}

/** 다른 사이트(디스코드 승인 화면 등)로 이동한다. */
export function goToExternal(url: string) {
  window.location.assign(url)
}
