export function prefersReducedMotion(mediaQuery?: Pick<MediaQueryList, 'matches'>): boolean {
  if (mediaQuery) return mediaQuery.matches
  return typeof window !== 'undefined'
    && typeof window.matchMedia === 'function'
    && window.matchMedia('(prefers-reduced-motion: reduce)').matches
}

export function scrollBehaviorForMotion(reducedMotion: boolean): ScrollBehavior {
  return reducedMotion ? 'auto' : 'smooth'
}

export function preferredScrollBehavior(): ScrollBehavior {
  return scrollBehaviorForMotion(prefersReducedMotion())
}
