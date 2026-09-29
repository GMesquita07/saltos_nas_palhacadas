type PortfolioInvalidationOptions = {
  broadcast?: boolean
}

const stalePortfolioSlugs = new Set<string>()
const seenPortfolioInvalidations = new Map<string, string>()

export const portfolioInvalidationStorageKey = 'saltos:portfolio:changed'

export function invalidatePortfolioItemsCache(slug: string, options: PortfolioInvalidationOptions = {}) {
  stalePortfolioSlugs.add(slug)
  if (options.broadcast !== false) {
    writePortfolioInvalidation(slug)
  }
}

export function shouldReloadPortfolioItems(slug: string, force = false) {
  return force || stalePortfolioSlugs.has(slug) || hasExternalPortfolioInvalidation(slug)
}

export function markPortfolioItemsLoaded(slug: string) {
  stalePortfolioSlugs.delete(slug)
}

export function portfolioInvalidationMatches(value: string | null, slug: string) {
  const invalidation = parsePortfolioInvalidation(value)
  return invalidation?.slug === slug
}

function hasExternalPortfolioInvalidation(slug: string) {
  const value = readPortfolioInvalidation()
  if (!portfolioInvalidationMatches(value, slug)) return false
  if (seenPortfolioInvalidations.get(slug) === value) return false

  seenPortfolioInvalidations.set(slug, value as string)
  return true
}

function readPortfolioInvalidation() {
  if (typeof window === 'undefined') return null

  try {
    return window.localStorage.getItem(portfolioInvalidationStorageKey)
  } catch {
    return null
  }
}

function writePortfolioInvalidation(slug: string) {
  if (typeof window === 'undefined') return

  try {
    window.localStorage.setItem(portfolioInvalidationStorageKey, JSON.stringify({ slug, updatedAt: Date.now() }))
  } catch {
    // A falha do storage não deve impedir a atualização na janela atual.
  }
}

function parsePortfolioInvalidation(value: string | null): { slug: string } | null {
  if (!value) return null

  try {
    const parsed = JSON.parse(value) as { slug?: unknown }
    return typeof parsed.slug === 'string' ? { slug: parsed.slug } : null
  } catch {
    return null
  }
}
