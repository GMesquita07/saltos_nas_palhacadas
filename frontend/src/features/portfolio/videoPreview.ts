const firstFrameMediaFragment = '#t=0.001'

export function videoFirstFrameSource(mediaUrl: string) {
  const trimmed = mediaUrl.trim()
  if (!trimmed || trimmed.includes('#')) return mediaUrl

  return `${trimmed}${firstFrameMediaFragment}`
}
