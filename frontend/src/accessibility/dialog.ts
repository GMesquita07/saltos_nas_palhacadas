export const dialogFocusableSelector = [
  'a[href]',
  'button:not([disabled])',
  'video[controls]',
  'input:not([disabled])',
  'select:not([disabled])',
  'textarea:not([disabled])',
  '[tabindex]:not([tabindex="-1"])',
].join(',')

export type DialogKeyboardAction = 'close' | 'trap' | 'ignore'

export function dialogKeyboardAction(key: string): DialogKeyboardAction {
  if (key === 'Escape') return 'close'
  if (key === 'Tab') return 'trap'
  return 'ignore'
}

export function dialogTabTarget(
  focusableCount: number,
  activeIndex: number,
  shiftKey: boolean,
): number | 'dialog' | null {
  if (focusableCount === 0) return 'dialog'
  if (activeIndex < 0) return shiftKey ? focusableCount - 1 : 0
  if (shiftKey && activeIndex === 0) return focusableCount - 1
  if (!shiftKey && activeIndex === focusableCount - 1) return 0
  return null
}

export function canRestoreDialogFocus(isConnected: boolean): boolean {
  return isConnected
}

export function dialogFocusableElements(dialog: HTMLElement): HTMLElement[] {
  return Array.from(dialog.querySelectorAll<HTMLElement>(dialogFocusableSelector))
    .filter((element) => element.getClientRects().length > 0 || element === document.activeElement)
}

export function trapDialogFocus(event: KeyboardEvent, dialog: HTMLElement | null) {
  if (!dialog) return

  const focusableElements = dialogFocusableElements(dialog)
  const activeIndex = focusableElements.indexOf(document.activeElement as HTMLElement)
  const target = dialogTabTarget(focusableElements.length, activeIndex, event.shiftKey)

  if (target === null) return

  event.preventDefault()
  if (target === 'dialog') {
    dialog.focus({ preventScroll: true })
    return
  }

  focusableElements[target]?.focus({ preventScroll: true })
}

export function restoreDialogFocus(element: HTMLElement | null) {
  if (element && canRestoreDialogFocus(element.isConnected)) {
    element.focus({ preventScroll: true })
  }
}
