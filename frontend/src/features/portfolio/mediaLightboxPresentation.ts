export type MediaLightboxPresentation = 'default' | 'artistPortfolio'

export type MediaLightboxPointerEnvironment = {
  maxTouchPoints: number
  hasFinePrimaryPointer: boolean
  hasAnyCoarsePointer: boolean
}

export type MediaDimensions = {
  width: number
  height: number
}

export type MediaOrientation = 'landscape' | 'portrait' | 'square' | 'unknown'

export function shouldUseArtistDesktopPresentation(
  presentation: MediaLightboxPresentation,
  environment: MediaLightboxPointerEnvironment,
) {
  return presentation === 'artistPortfolio'
    && environment.maxTouchPoints === 0
    && environment.hasFinePrimaryPointer
    && !environment.hasAnyCoarsePointer
}

export function mediaDimensionsFromSize(width: number, height: number): MediaDimensions | null {
  return Number.isFinite(width) && Number.isFinite(height) && width > 0 && height > 0
    ? { width, height }
    : null
}

export function mediaOrientation(dimensions: MediaDimensions | null): MediaOrientation {
  if (!dimensions) return 'unknown'

  const ratio = dimensions.width / dimensions.height
  if (ratio > 1.04) return 'landscape'
  if (ratio < 0.96) return 'portrait'
  return 'square'
}
