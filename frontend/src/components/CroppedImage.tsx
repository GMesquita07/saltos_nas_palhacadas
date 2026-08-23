import { formatImagePosition, imageCropStyle, parseImageCrop } from './imageCrop'
import styles from './CroppedImage.module.css'

type CroppedImageProps = {
  alt?: string
  className?: string
  fallback?: string
  position?: string
  shape?: 'circle' | 'square'
  src?: string
  zoom?: number
}

export function CroppedImage({ alt = '', className = '', fallback, position, shape = 'circle', src, zoom }: CroppedImageProps) {
  const crop = parseImageCrop(position, zoom)
  const imagePosition = formatImagePosition(crop)

  return (
    <span className={[styles.frame, styles[shape], className].filter(Boolean).join(' ')}>
      {src
        ? <img key={src + imagePosition + crop.zoom} src={src} alt={alt} style={imageCropStyle(imagePosition, crop.zoom)} />
        : <span>{fallback}</span>}
    </span>
  )
}
