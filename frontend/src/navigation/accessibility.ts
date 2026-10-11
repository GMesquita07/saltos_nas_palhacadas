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

export function shouldMoveFocusToMain(
  previousPathname: string | null,
  pathname: string,
  hash: string,
): boolean {
  return previousPathname !== null && previousPathname !== pathname && hash.length === 0
}

export function hashTargetId(hash: string): string | null {
  const rawId = hash.startsWith('#') ? hash.slice(1) : hash
  if (!rawId) return null

  try {
    return decodeURIComponent(rawId)
  } catch {
    return rawId
  }
}

export function hashTargetsElement(hash: string, targetId: string): boolean {
  return hashTargetId(hash) === targetId
}
