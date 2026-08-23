import { useRef, type PointerEvent } from 'react'
import { clampPercentage, clampZoom, formatImagePosition, imageCropStyle, type ImageCrop } from './imageCrop'
import styles from './ImageCropEditor.module.css'

type ImageCropEditorProps = {
  alt?: string
  crop: ImageCrop
  description?: string
  shape?: 'landscape' | 'square' | 'circle'
  src: string
  title?: string
  onChange: (crop: ImageCrop) => void
}

export function ImageCropEditor({
  alt = 'Pré-visualização da foto',
  crop,
  description = 'Arrasta a fotografia e ajusta o zoom para escolher o enquadramento.',
  shape = 'square',
  src,
  title = 'Ajustar foto',
  onChange,
}: ImageCropEditorProps) {
  const drag = useRef<{ crop: ImageCrop; startX: number; startY: number } | null>(null)

  function startDrag(event: PointerEvent<HTMLDivElement>) {
    drag.current = { crop, startX: event.clientX, startY: event.clientY }
    event.currentTarget.setPointerCapture(event.pointerId)
  }

  function moveDrag(event: PointerEvent<HTMLDivElement>) {
    if (!drag.current) return

    const bounds = event.currentTarget.getBoundingClientRect()
    const nextX = clampPercentage(drag.current.crop.x - ((event.clientX - drag.current.startX) / bounds.width) * 100)
    const nextY = clampPercentage(drag.current.crop.y - ((event.clientY - drag.current.startY) / bounds.height) * 100)
    onChange({ ...crop, x: nextX, y: nextY })
  }

  function stopDrag(event: PointerEvent<HTMLDivElement>) {
    drag.current = null
    if (event.currentTarget.hasPointerCapture(event.pointerId)) {
      event.currentTarget.releasePointerCapture(event.pointerId)
    }
  }

  return (
    <div className={styles.cropEditor}>
      <div>
        <p className={styles.cropTitle}>{title}</p>
        <p className={styles.cropDescription}>{description}</p>
      </div>
      <div
        className={[styles.cropPreview, styles[shape]].join(' ')}
        onPointerCancel={stopDrag}
        onPointerDown={startDrag}
        onPointerMove={moveDrag}
        onPointerUp={stopDrag}
      >
        <img alt={alt} src={src} style={imageCropStyle(formatImagePosition(crop), crop.zoom)} />
      </div>
      <label className={styles.rangeLabel}>
        Posição horizontal
        <input
          aria-valuetext={crop.x + '%'}
          max="100"
          min="0"
          onChange={(event) => onChange({ ...crop, x: clampPercentage(Number(event.target.value)) })}
          type="range"
          value={crop.x}
        />
        <span>{crop.x}%</span>
      </label>
      <label className={styles.rangeLabel}>
        Posição vertical
        <input
          aria-valuetext={crop.y + '%'}
          max="100"
          min="0"
          onChange={(event) => onChange({ ...crop, y: clampPercentage(Number(event.target.value)) })}
          type="range"
          value={crop.y}
        />
        <span>{crop.y}%</span>
      </label>
      <label className={styles.rangeLabel}>
        Zoom
        <input
          aria-valuetext={crop.zoom.toFixed(2) + 'x'}
          max="3"
          min="1"
          onChange={(event) => onChange({ ...crop, zoom: clampZoom(Number(event.target.value)) })}
          step="0.01"
          type="range"
          value={crop.zoom}
        />
        <span>{crop.zoom.toFixed(2)}x</span>
      </label>
    </div>
  )
}
