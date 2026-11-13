import type { ServerLoader } from './api'

export const LOADER_OPTIONS: { value: ServerLoader; label: string; hint: string }[] = [
  { value: 'VANILLA', label: '바닐라', hint: '모드 없는 기본 서버' },
  { value: 'FABRIC', label: 'Fabric', hint: '가벼운 모드 서버' },
  { value: 'FORGE', label: 'Forge', hint: '큰 모드팩이 많이 쓰는 서버' },
  { value: 'NEOFORGE', label: 'NeoForge', hint: '최신 버전용 Forge 계열' },
]

/** 고를 수 있는 메모리(MB). 지금 값이 목록에 없으면 화면에서 따로 넣어준다. */
export const MEMORY_OPTIONS = [1024, 2048, 3072, 4096, 6144, 8192, 12288, 16384]

export function formatMemory(memoryMb: number) {
  return memoryMb % 1024 === 0 ? `${memoryMb / 1024} GB` : `${memoryMb} MB`
}

export function memoryOptionsWith(current: number) {
  return MEMORY_OPTIONS.includes(current) ? MEMORY_OPTIONS : [...MEMORY_OPTIONS, current].sort((a, b) => a - b)
}
