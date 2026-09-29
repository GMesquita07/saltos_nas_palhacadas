import type { ImgHTMLAttributes } from 'react'
import { imageCropStyle } from './imageCrop'
import styles from './ProfileHeroBackground.module.css'

export const profileHeroDesktopAspectRatio = '1240 / 390'

type ProfileHeroBackgroundProps = {
  alt?: string
  ariaHidden?: boolean
  className?: string
  decoding?: ImgHTMLAttributes<HTMLImageElement>['decoding']
  loading?: ImgHTMLAttributes<HTMLImageElement>['loading']
  mutedImage?: boolean
  position?: string
  showProfileOverlay?: boolean
  src: string
  zoom?: number
}

export function ProfileHeroBackground({
  alt = '',
  ariaHidden,
  className = '',
  decoding = 'async',
  loading,
  mutedImage = false,
  position = '50% 50%',
  showProfileOverlay = false,
  src,
  zoom = 1,
}: ProfileHeroBackgroundProps) {
  return (
    <div aria-hidden={ariaHidden} className={[styles.root, className].filter(Boolean).join(' ')}>
      <img
        alt={alt}
        className={[styles.image, mutedImage ? styles.mutedImage : ''].filter(Boolean).join(' ')}
        decoding={decoding}
        loading={loading}
        src={src}
        style={imageCropStyle(position, zoom)}
      />
      {showProfileOverlay && <span className={styles.profileOverlay} aria-hidden="true" />}
    </div>
  )
}
