import { useEffect, useRef } from 'react'

export function shouldFocusError(message: string | null): boolean {
  return Boolean(message?.trim())
}

export function useFocusOnError(message: string | null) {
  const errorRef = useRef<HTMLParagraphElement | null>(null)

  useEffect(() => {
    if (shouldFocusError(message)) {
      errorRef.current?.focus()
    }
  }, [message])

  return errorRef
}
