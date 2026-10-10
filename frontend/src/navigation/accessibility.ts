export const mainContentId = 'main-content'
export const mainContentHref = `#${mainContentId}`

export function navigationAriaCurrent(activeView: string, itemView: string): 'page' | undefined {
  return activeView === itemView ? 'page' : undefined
}

export function dismissNavigationMenuOnEscape(
  key: string,
  closeMenu: () => void,
  focusMenuButton: () => void,
): boolean {
  if (key !== 'Escape') return false

  closeMenu()
  focusMenuButton()
  return true
}
