import { useEffect, useRef } from 'react'
import { createPortal } from 'react-dom'
import type { PortfolioItem } from '../../types/portfolio'
import styles from './MediaLightbox.module.css'

type MediaLightboxProps = {
  item: PortfolioItem
  onClose: () => void
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

const focusableSelector = [
  'a[href]',
  'button:not([disabled])',
  'video[controls]',
  'input:not([disabled])',
  'select:not([disabled])',
  'textarea:not([disabled])',
  '[tabindex]:not([tabindex="-1"])',
].join(',')

export function MediaLightbox({ item, onClose }: MediaLightboxProps) {
  const dialogRef = useRef<HTMLDivElement | null>(null)
  const closeButtonRef = useRef<HTMLButtonElement | null>(null)
  const onCloseRef = useRef(onClose)
  const previouslyFocusedElementRef = useRef<HTMLElement | null>(null)

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
        trapFocus(event, dialogRef.current)
      }
    }

    window.addEventListener('keydown', handleKeyDown)
    return () => {
      window.removeEventListener('keydown', handleKeyDown)
      unlockBodyScroll(scrollLock)
      if (previouslyFocusedElementRef.current && document.contains(previouslyFocusedElementRef.current)) {
        previouslyFocusedElementRef.current.focus({ preventScroll: true })
      }
    }
  }, [])

  return createPortal(
    <div className={styles.backdrop} onMouseDown={onClose}>
      <div
        aria-labelledby="media-lightbox-title"
        aria-modal="true"
        className={styles.dialog}
        ref={dialogRef}
        role="dialog"
        tabIndex={-1}
        onMouseDown={(event) => event.stopPropagation()}
      >
        <button ref={closeButtonRef} className={styles.close} type="button" aria-label={`Fechar ${item.title}`} onClick={onClose}>×</button>
        <div className={styles.media}>
          {item.type === 'Vídeo'
            ? (
              <video controls playsInline preload="metadata" poster={item.thumbnailUrl}>
                <source src={item.mediaUrl} />
              </video>
            )
            : <img src={item.mediaUrl} alt={item.title} decoding="async" />}
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

function trapFocus(event: KeyboardEvent, dialog: HTMLDivElement | null) {
  if (!dialog) return

  const focusableElements = Array.from(dialog.querySelectorAll<HTMLElement>(focusableSelector))
    .filter((element) => element.offsetParent !== null || element === document.activeElement)
  if (focusableElements.length === 0) {
    event.preventDefault()
    dialog.focus({ preventScroll: true })
    return
  }

  const firstElement = focusableElements[0]
  const lastElement = focusableElements[focusableElements.length - 1]
  const activeElement = document.activeElement

  if (event.shiftKey && activeElement === firstElement) {
    event.preventDefault()
    lastElement.focus({ preventScroll: true })
  } else if (!event.shiftKey && activeElement === lastElement) {
    event.preventDefault()
    firstElement.focus({ preventScroll: true })
  }
}
