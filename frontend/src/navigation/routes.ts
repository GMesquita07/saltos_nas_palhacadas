import type { AuthMode } from '../features/auth/authTypes'

export type AdminPage = 'dashboard' | 'profile' | 'content' | 'bookings' | 'reviews' | 'contacts' | 'materials'

const adminPaths: Record<AdminPage, string> = {
  dashboard: '/admin',
  profile: '/admin/perfis',
  content: '/admin/publicacoes',
  bookings: '/admin/reservas',
  reviews: '/admin/avaliacoes',
  contacts: '/admin/contactos',
  materials: '/admin/materiais',
}

const authPaths: Record<AuthMode, string> = {
  login: '/login',
  register: '/registo',
  forgot: '/recuperar-password',
  reset: '/reset-password',
}

export function adminPath(page: AdminPage) {
  return adminPaths[page]
}

export function adminPageFromPath(pathname: string): AdminPage | null {
  const match = Object.entries(adminPaths).find(([, path]) => pathname === path)
  return match ? match[0] as AdminPage : null
}

export function authModeFromPath(pathname: string): AuthMode | null {
  const match = Object.entries(authPaths).find(([, path]) => pathname === path)
  return match ? match[0] as AuthMode : null
}

export function authPath(mode: AuthMode, resetToken?: string | null) {
  const path = authPaths[mode]
  if (mode !== 'reset' || !resetToken) return path

  const params = new URLSearchParams({ resetToken })
  return `${path}?${params.toString()}`
}

export function profilePath(slug: string) {
  return `/perfis/${encodeURIComponent(slug)}`
}

export function bookingPath(slug?: string | null) {
  return slug ? `/agendar/${encodeURIComponent(slug)}` : '/agendar'
}

export function loginPath(returnTo?: string | null) {
  const safeReturnTo = normalizeReturnTo(returnTo)
  if (!safeReturnTo) return '/login'

  const params = new URLSearchParams({ returnTo: safeReturnTo })
  return `/login?${params.toString()}`
}

export function normalizeReturnTo(value?: string | null) {
  const trimmed = value?.trim()
  if (!trimmed || !isSafeReturnTo(trimmed)) return null
  return trimmed
}

export function isSafeReturnTo(value: string) {
  if (!isSingleSlashInternalPath(value)) return false

  let decoded = value
  for (let attempt = 0; attempt < 3; attempt++) {
    try {
      const nextDecoded = decodeURIComponent(decoded)
      if (nextDecoded === decoded) break
      decoded = nextDecoded
    } catch {
      return false
    }

    if (!isSingleSlashInternalPath(decoded)) return false
  }

  try {
    const baseUrl = 'https://saltos.local'
    const url = new URL(decoded, baseUrl)
    return url.origin === baseUrl && isSingleSlashInternalPath(url.pathname)
  } catch {
    return false
  }
}

function isSingleSlashInternalPath(value: string) {
  return value.startsWith('/')
    && !value.startsWith('//')
    && !value.startsWith('/\\')
    && !value.includes('\\')
}

export function legacyResetRedirect(pathname: string, search: string) {
  if (pathname !== '/') return null

  const resetToken = new URLSearchParams(search).get('resetToken')?.trim()
  return resetToken ? authPath('reset', resetToken) : null
}
