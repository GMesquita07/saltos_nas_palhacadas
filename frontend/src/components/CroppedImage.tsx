import { useState, type ImgHTMLAttributes } from 'react'
import { formatImagePosition, imageCropStyle, parseImageCrop } from './imageCrop'
import styles from './CroppedImage.module.css'

type CroppedImageProps = {
  alt?: string
  decoding?: ImgHTMLAttributes<HTMLImageElement>['decoding']
  className?: string
  fallback?: string
  fetchPriority?: 'auto' | 'high' | 'low'
  loading?: ImgHTMLAttributes<HTMLImageElement>['loading']
  position?: string
  shape?: 'circle' | 'square'
  src?: string
  zoom?: number
}

export function CroppedImage({ alt = '', className = '', decoding = 'async', fallback, fetchPriority, loading, position, shape = 'circle', src, zoom }: CroppedImageProps) {
  const crop = parseImageCrop(position, zoom)
  const imagePosition = formatImagePosition(crop)
  const [failedImageSrc, setFailedImageSrc] = useState<string | undefined>()
  const canRenderImage = Boolean(src && failedImageSrc !== src)

  return (
    <span className={[styles.frame, styles[shape], className].filter(Boolean).join(' ')}>
      {canRenderImage
        ? (
          <img
            key={src + imagePosition + crop.zoom}
            src={src}
            alt={alt}
            decoding={decoding}
            fetchPriority={fetchPriority}
            loading={loading}
            style={imageCropStyle(imagePosition, crop.zoom)}
            onError={() => setFailedImageSrc(src)}
          />
        )
        : <span>{fallback}</span>}
    </span>
  )
}
