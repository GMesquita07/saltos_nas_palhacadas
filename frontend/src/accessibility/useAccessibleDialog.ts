import { useEffect, useRef, type RefObject } from 'react'
import {
  dialogFocusableElements,
  dialogKeyboardAction,
  restoreDialogFocus,
  trapDialogFocus,
} from './dialog'

type AccessibleDialogOptions = {
  initialFocusRef?: RefObject<HTMLElement | null>
  isOpen: boolean
  onClose: () => void
}

export function useAccessibleDialog<T extends HTMLElement = HTMLElement>({ initialFocusRef, isOpen, onClose }: AccessibleDialogOptions) {
  const dialogRef = useRef<T | null>(null)
  const onCloseRef = useRef(onClose)

  useEffect(() => {
    onCloseRef.current = onClose
  }, [onClose])

  useEffect(() => {
    if (!isOpen) return

    const previouslyFocusedElement = document.activeElement instanceof HTMLElement
      ? document.activeElement
      : null
    const previousOverflow = document.body.style.overflow
    document.body.style.overflow = 'hidden'

    const dialog = dialogRef.current
    const initialFocus = initialFocusRef?.current ?? (dialog ? dialogFocusableElements(dialog)[0] : null)
    ;(initialFocus ?? dialog)?.focus({ preventScroll: true })

    function handleKeyDown(event: KeyboardEvent) {
      const action = dialogKeyboardAction(event.key)
      if (action === 'close') {
        event.preventDefault()
        onCloseRef.current()
      } else if (action === 'trap') {
        trapDialogFocus(event, dialogRef.current)
      }
    }

    window.addEventListener('keydown', handleKeyDown)
    return () => {
      window.removeEventListener('keydown', handleKeyDown)
      document.body.style.overflow = previousOverflow
      restoreDialogFocus(previouslyFocusedElement)
    }
  }, [initialFocusRef, isOpen])

  return dialogRef
}
