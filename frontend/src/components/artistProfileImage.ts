export type ArtistProfileImageVariant = 'card' | 'heroDesktop' | 'heroMobile' | 'heroResponsive'

export type ArtistProfileImageCropInput = {
  alt?: string
  fallback?: string
  position?: string
  src?: string
  zoom?: number
}

export type ArtistProfileImageCropProps = {
  alt: string
  fallback?: string
  position: string
  shape: 'circle'
  src?: string
  zoom: number
}

export const artistProfileImageBaseSizes: Record<ArtistProfileImageVariant, number> = {
  card: 190,
  heroDesktop: 152,
  heroMobile: 82,
  heroResponsive: 152,
}

export function artistInitials(name: string) {
  return name
    .split(' ')
    .map((part) => part.trim()[0])
    .filter(Boolean)
    .join('')
    .slice(0, 2)
}

export function artistProfileImageCropProps(input: ArtistProfileImageCropInput): ArtistProfileImageCropProps {
  return {
    alt: input.alt ?? '',
    fallback: input.fallback,
    position: input.position ?? '50% 50%',
    shape: 'circle',
    src: input.src,
    zoom: input.zoom ?? 1,
  }
}

export function artistProfileImageScaleStyle(variant: ArtistProfileImageVariant, previewScale?: number) {
  if (!previewScale || previewScale === 1) return undefined

  return {
    '--artist-profile-image-layout-size': `${roundSize(artistProfileImageBaseSizes[variant] * previewScale)}px`,
    '--artist-profile-image-scale': String(previewScale),
  }
}

function roundSize(value: number) {
  return Math.round(value * 1000) / 1000
}
