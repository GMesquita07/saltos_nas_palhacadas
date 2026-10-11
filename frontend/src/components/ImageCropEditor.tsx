import { useRef, type CSSProperties, type PointerEvent, type ReactNode, type Ref } from 'react'
import { clampPercentage, clampZoom, dragImageCrop, formatImagePosition, type ImageCrop } from './imageCrop'
import { CroppedImage } from './CroppedImage'
import { ProfileHeroBackground, profileHeroDesktopAspectRatio } from './ProfileHeroBackground'
import styles from './ImageCropEditor.module.css'

type CropPreviewShape = 'landscape' | 'square' | 'circle'

type ComparisonPreviewRenderProps = {
  alt: string
  imagePosition: string
  src: string
  zoom: number
}

type ComparisonPreview = {
  title: string
  aspectRatio?: string
  shape?: CropPreviewShape
  render?: (props: ComparisonPreviewRenderProps) => ReactNode
}

type ImageCropEditorProps = {
  alt?: string
  crop: ImageCrop
  description?: string
  descriptionId?: string
  initialFocusRef?: Ref<HTMLInputElement>
  aspectRatio?: string
  comparisonPreviews?: ComparisonPreview[]
  previewMode?: 'image' | 'profileHeroBackground'
  shape?: CropPreviewShape
  src: string
  title?: string
  titleId?: string
  onChange: (crop: ImageCrop) => void
}

export function ImageCropEditor({
  alt = 'Pré-visualização da foto',
  crop,
  description = 'Arrasta a fotografia e ajusta o zoom para escolher o enquadramento.',
  descriptionId,
  initialFocusRef,
  aspectRatio,
  comparisonPreviews = [],
  previewMode = 'image',
  shape = 'square',
  src,
  title = 'Ajustar foto',
  titleId,
  onChange,
}: ImageCropEditorProps) {
  const drag = useRef<{ crop: ImageCrop; startX: number; startY: number } | null>(null)
  const imagePosition = formatImagePosition(crop)
  const resolvedAspectRatio = previewMode === 'profileHeroBackground' ? profileHeroDesktopAspectRatio : aspectRatio
  const previewStyle: CSSProperties | undefined = resolvedAspectRatio ? { aspectRatio: resolvedAspectRatio } : undefined

  function startDrag(event: PointerEvent<HTMLDivElement>) {
    drag.current = { crop, startX: event.clientX, startY: event.clientY }
    event.currentTarget.setPointerCapture(event.pointerId)
  }

  function moveDrag(event: PointerEvent<HTMLDivElement>) {
    if (!drag.current) return

    const bounds = event.currentTarget.getBoundingClientRect()
    onChange(dragImageCrop(
      drag.current.crop,
      event.clientX - drag.current.startX,
      event.clientY - drag.current.startY,
      bounds.width,
      bounds.height,
    ))
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
        <h2 className={styles.cropTitle} id={titleId}>{title}</h2>
        <p className={styles.cropDescription} id={descriptionId}>{description}</p>
      </div>
      <div
        className={[styles.cropPreview, styles[shape]].join(' ')}
        style={previewStyle}
        onPointerCancel={stopDrag}
        onPointerDown={startDrag}
        onPointerMove={moveDrag}
        onPointerUp={stopDrag}
      >
        {previewMode === 'profileHeroBackground'
          ? (
            <ProfileHeroBackground
              alt={alt}
              className={styles.heroCropImage}
              position={imagePosition}
              src={src}
              zoom={crop.zoom}
            />
          )
          : (
            <CroppedImage alt={alt} className={styles.cropImage} position={imagePosition} shape={shape === 'circle' ? 'circle' : 'square'} src={src} zoom={crop.zoom} />
          )}
      </div>
      {comparisonPreviews.length > 0 && (
        <section className={styles.comparisonPreviewSection} aria-label="Como vai aparecer">
          <p className={styles.comparisonPreviewHeading}>Como vai aparecer</p>
          <div className={styles.comparisonPreviews}>
            {comparisonPreviews.map((preview) => {
              const previewShape = preview.shape ?? shape
              const renderedPreview = preview.render?.({ alt, imagePosition, src, zoom: crop.zoom })

              return (
                <div className={styles.comparisonPreview} key={preview.title}>
                  <p className={styles.comparisonPreviewTitle}>{preview.title}</p>
                  {renderedPreview
                    ? <div className={styles.comparisonPreviewSurface}>{renderedPreview}</div>
                    : (
                      <div
                        className={[styles.comparisonPreviewFrame, styles[previewShape]].join(' ')}
                        style={{ aspectRatio: preview.aspectRatio ?? '1 / 1' }}
                      >
                        <CroppedImage
                          alt={alt}
                          className={styles.cropImage}
                          position={imagePosition}
                          shape={previewShape === 'circle' ? 'circle' : 'square'}
                          src={src}
                          zoom={crop.zoom}
                        />
                      </div>
                    )}
                </div>
              )
            })}
          </div>
        </section>
      )}
      {previewMode === 'profileHeroBackground' && (
        <div className={styles.heroResultPreview}>
          <p className={styles.heroResultTitle}>Como aparece no perfil</p>
          <div className={styles.heroResultFrame}>
            <ProfileHeroBackground
              ariaHidden
              className={styles.heroResultBackdrop}
              mutedImage
              position={imagePosition}
              showProfileOverlay
              src={src}
              zoom={crop.zoom}
            />
            <div className={styles.heroResultContent} aria-hidden="true">
              <span className={styles.heroResultAvatar} />
              <span className={styles.heroResultCopy}>
                <span />
                <span />
                <span />
              </span>
              <span className={styles.heroResultVideo} />
            </div>
          </div>
        </div>
      )}
      <label className={styles.rangeLabel}>
        Posição horizontal
        <input
          ref={initialFocusRef}
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
