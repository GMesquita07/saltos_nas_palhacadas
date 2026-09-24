export type ImageLoadingPolicy = {
  decoding: 'async'
  fetchPriority?: 'auto' | 'high' | 'low'
  loading?: 'eager' | 'lazy'
}

export function routeNeedsProfiles(pathname: string) {
  const path = normalizePathname(pathname)
  return path === '/'
    || path === '/agendar'
    || /^\/agendar\/[^/]+$/.test(path)
    || /^\/perfis\/[^/]+$/.test(path)
}

export function profileCardImagePolicy(index: number): ImageLoadingPolicy {
  return index === 0
    ? { decoding: 'async', fetchPriority: 'auto', loading: 'eager' }
    : { decoding: 'async', fetchPriority: 'low', loading: 'lazy' }
}

function normalizePathname(pathname: string) {
  if (!pathname || !pathname.startsWith('/')) return '/'
  return pathname.length > 1 ? pathname.replace(/\/+$/, '') : pathname
}
