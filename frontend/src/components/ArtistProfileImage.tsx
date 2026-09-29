import type { CSSProperties, ImgHTMLAttributes } from 'react'
import { CroppedImage } from './CroppedImage'
import {
  artistProfileImageCropProps,
  artistProfileImageScaleStyle,
  type ArtistProfileImageVariant,
} from './artistProfileImage'
import styles from './ArtistProfileImage.module.css'

type ArtistProfileImageProps = {
  alt?: string
  className?: string
  decoding?: ImgHTMLAttributes<HTMLImageElement>['decoding']
  fallback?: string
  fetchPriority?: 'auto' | 'high' | 'low'
  loading?: ImgHTMLAttributes<HTMLImageElement>['loading']
  position?: string
  previewScale?: number
  src?: string
  variant: ArtistProfileImageVariant
  zoom?: number
}

export function ArtistProfileImage({
  alt,
  className = '',
  decoding,
  fallback,
  fetchPriority,
  loading,
  position,
  previewScale,
  src,
  variant,
  zoom,
}: ArtistProfileImageProps) {
  const cropProps = artistProfileImageCropProps({ alt, fallback, position, src, zoom })
  const scaleStyle = artistProfileImageScaleStyle(variant, previewScale) as CSSProperties | undefined

  return (
    <span className={[styles.root, styles[variant], className].filter(Boolean).join(' ')} style={scaleStyle}>
      <span className={styles.surface}>
        <CroppedImage
          {...cropProps}
          className={styles.frame}
          decoding={decoding}
          fetchPriority={fetchPriority}
          loading={loading}
        />
      </span>
    </span>
  )
}
