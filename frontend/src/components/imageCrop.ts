import type { CSSProperties } from 'react'

export type ImageCrop = {
  x: number
  y: number
  zoom: number
}

export function imageCropStyle(position = '50% 50%', zoom = 1): CSSProperties {
  const safePosition = position || '50% 50%'
  return {
    objectPosition: safePosition,
    transform: 'scale(' + clampZoom(zoom) + ')',
    transformOrigin: safePosition,
  }
}

export function parseImageCrop(value?: string, zoom?: number): ImageCrop {
  const match = value?.match(/^(\d{1,3})% (\d{1,3})%$/)
  if (!match) return { x: 50, y: 50, zoom: clampZoom(zoom ?? 1) }

  return {
    x: clampPercentage(Number(match[1])),
    y: clampPercentage(Number(match[2])),
    zoom: clampZoom(zoom ?? 1),
  }
}

export function formatImagePosition(crop: Pick<ImageCrop, 'x' | 'y'>) {
  return clampPercentage(crop.x) + '% ' + clampPercentage(crop.y) + '%'
}

export function clampPercentage(value: number) {
  if (Number.isNaN(value)) return 50
  return Math.min(100, Math.max(0, Math.round(value)))
}

export function clampZoom(value: number) {
  if (Number.isNaN(value)) return 1
  return Math.min(3, Math.max(1, Math.round(value * 100) / 100))
}
