import { useEffect, useRef } from 'react'
import type { PortfolioItem } from '../../types/portfolio'
import styles from './MediaLightbox.module.css'

type MediaLightboxProps = {
  item: PortfolioItem
  onClose: () => void
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

    const previousOverflow = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    closeButtonRef.current?.focus()

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
      document.body.style.overflow = previousOverflow
      window.removeEventListener('keydown', handleKeyDown)
      if (previouslyFocusedElementRef.current && document.contains(previouslyFocusedElementRef.current)) {
        previouslyFocusedElementRef.current.focus()
      }
    }
  }, [])

  return (
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
            : <img src={item.mediaUrl} alt={item.title} />}
        </div>
        <div className={styles.caption}>
          <p>{item.location} · {item.eventDate}</p>
          <h2 id="media-lightbox-title">{item.title}</h2>
        </div>
      </div>
    </div>
  )
}

function trapFocus(event: KeyboardEvent, dialog: HTMLDivElement | null) {
  if (!dialog) return

  const focusableElements = Array.from(dialog.querySelectorAll<HTMLElement>(focusableSelector))
    .filter((element) => element.offsetParent !== null || element === document.activeElement)
  if (focusableElements.length === 0) {
    event.preventDefault()
    dialog.focus()
    return
  }

  const firstElement = focusableElements[0]
  const lastElement = focusableElements[focusableElements.length - 1]
  const activeElement = document.activeElement

  if (event.shiftKey && activeElement === firstElement) {
    event.preventDefault()
    lastElement.focus()
  } else if (!event.shiftKey && activeElement === lastElement) {
    event.preventDefault()
    firstElement.focus()
  }
}
