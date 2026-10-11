import { useCallback, useEffect, useRef, useState } from 'react'
import { createPortal } from 'react-dom'
import type { SyntheticEvent } from 'react'
import { restoreDialogFocus, trapDialogFocus } from '../../accessibility/dialog'
import type { PortfolioItem } from '../../types/portfolio'
import {
  mediaDimensionsFromSize,
  mediaOrientation,
  shouldUseArtistDesktopPresentation,
  type MediaDimensions,
  type MediaLightboxPresentation,
} from './mediaLightboxPresentation'
import { videoFirstFrameSource } from './videoPreview'
import styles from './MediaLightbox.module.css'

type MediaLightboxProps = {
  item: PortfolioItem
  onClose: () => void
  presentation?: MediaLightboxPresentation
}

type BodyScrollLockSnapshot = {
  scrollY: number
  body: {
    left: string
    overflow: string
    overscrollBehavior: string
    position: string
    right: string
    top: string
    width: string
  }
  documentElement: {
    overscrollBehavior: string
  }
}

export function MediaLightbox({ item, onClose, presentation = 'default' }: MediaLightboxProps) {
  const dialogRef = useRef<HTMLDivElement | null>(null)
  const closeButtonRef = useRef<HTMLButtonElement | null>(null)
  const onCloseRef = useRef(onClose)
  const previouslyFocusedElementRef = useRef<HTMLElement | null>(null)
  const [loadedMediaDimensions, setLoadedMediaDimensions] = useState<{ mediaUrl: string; dimensions: MediaDimensions } | null>(null)
  const mediaDimensions = loadedMediaDimensions?.mediaUrl === item.mediaUrl ? loadedMediaDimensions.dimensions : null
  const isArtistDesktopPresentation = isArtistDesktopLightboxPresentation(presentation)
  const mediaOrientationName = mediaOrientation(mediaDimensions)
  const dialogClassName = isArtistDesktopPresentation
    ? `${styles.dialog} ${styles.artistPortfolioDialog}`
    : styles.dialog
  const handleImageLoad = useCallback((event: SyntheticEvent<HTMLImageElement>) => {
    const dimensions = mediaDimensionsFromSize(
      event.currentTarget.naturalWidth,
      event.currentTarget.naturalHeight,
    )
    if (dimensions) setLoadedMediaDimensions({ mediaUrl: item.mediaUrl, dimensions })
  }, [item.mediaUrl])

  const handleVideoMetadata = useCallback((event: SyntheticEvent<HTMLVideoElement>) => {
    const dimensions = mediaDimensionsFromSize(
      event.currentTarget.videoWidth,
      event.currentTarget.videoHeight,
    )
    if (dimensions) setLoadedMediaDimensions({ mediaUrl: item.mediaUrl, dimensions })
  }, [item.mediaUrl])

  useEffect(() => {
    onCloseRef.current = onClose
  }, [onClose])

  useEffect(() => {
    previouslyFocusedElementRef.current = document.activeElement instanceof HTMLElement
      ? document.activeElement
      : null

    const scrollLock = lockBodyScroll()
    closeButtonRef.current?.focus({ preventScroll: true })

    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        event.preventDefault()
        onCloseRef.current()
        return
      }

      if (event.key === 'Tab') {
        trapDialogFocus(event, dialogRef.current)
      }
    }

    window.addEventListener('keydown', handleKeyDown)
    return () => {
      window.removeEventListener('keydown', handleKeyDown)
      unlockBodyScroll(scrollLock)
      restoreDialogFocus(previouslyFocusedElementRef.current)
    }
  }, [])

  return createPortal(
    <div className={styles.backdrop} onMouseDown={onClose}>
      <div
        aria-labelledby="media-lightbox-title"
        aria-modal="true"
        className={dialogClassName}
        data-media-orientation={isArtistDesktopPresentation ? mediaOrientationName : undefined}
        ref={dialogRef}
        role="dialog"
        tabIndex={-1}
        onMouseDown={(event) => event.stopPropagation()}
      >
        <button ref={closeButtonRef} className={styles.close} type="button" aria-label={`Fechar ${item.title}`} onClick={onClose}>×</button>
        <div className={styles.media}>
          {item.type === 'Vídeo'
            ? (
              <video controls playsInline preload="metadata" poster={item.thumbnailUrl} onLoadedMetadata={handleVideoMetadata}>
                <source src={item.thumbnailUrl ? item.mediaUrl : videoFirstFrameSource(item.mediaUrl)} />
              </video>
            )
            : <img src={item.mediaUrl} alt={item.title} decoding="async" onLoad={handleImageLoad} />}
        </div>
        <div className={styles.caption}>
          <p>{item.location} · {item.eventDate}</p>
          <h2 id="media-lightbox-title">{item.title}</h2>
        </div>
      </div>
    </div>,
    document.body,
  )
}


function isArtistDesktopLightboxPresentation(presentation: MediaLightboxPresentation) {
  if (presentation !== 'artistPortfolio') return false
  if (typeof window === 'undefined' || typeof window.matchMedia !== 'function') return false

  return shouldUseArtistDesktopPresentation(presentation, {
    maxTouchPoints: navigator.maxTouchPoints ?? 0,
    hasFinePrimaryPointer: window.matchMedia('(pointer: fine)').matches,
    hasAnyCoarsePointer: window.matchMedia('(any-pointer: coarse)').matches,
  })
}

function lockBodyScroll(): BodyScrollLockSnapshot {
  const body = document.body
  const documentElement = document.documentElement
  const scrollY = window.scrollY
  const snapshot: BodyScrollLockSnapshot = {
    scrollY,
    body: {
      left: body.style.left,
      overflow: body.style.overflow,
      overscrollBehavior: body.style.overscrollBehavior,
      position: body.style.position,
      right: body.style.right,
      top: body.style.top,
      width: body.style.width,
    },
    documentElement: {
      overscrollBehavior: documentElement.style.overscrollBehavior,
    },
  }

  body.style.position = 'fixed'
  body.style.top = `-${scrollY}px`
  body.style.left = '0'
  body.style.right = '0'
  body.style.width = '100%'
  body.style.overflow = 'hidden'
  body.style.overscrollBehavior = 'none'
  documentElement.style.overscrollBehavior = 'none'

  return snapshot
}

function unlockBodyScroll(snapshot: BodyScrollLockSnapshot) {
  const body = document.body
  const documentElement = document.documentElement

  body.style.position = snapshot.body.position
  body.style.top = snapshot.body.top
  body.style.left = snapshot.body.left
  body.style.right = snapshot.body.right
  body.style.width = snapshot.body.width
  body.style.overflow = snapshot.body.overflow
  body.style.overscrollBehavior = snapshot.body.overscrollBehavior
  documentElement.style.overscrollBehavior = snapshot.documentElement.overscrollBehavior

  window.scrollTo({ top: snapshot.scrollY, left: 0, behavior: 'auto' })
}
