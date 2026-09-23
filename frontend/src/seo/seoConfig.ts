import type { Profile } from '../types/profile'

export const canonicalOrigin = 'https://www.saltosnaspalhacadas.pt'
export const siteName = 'Saltos nas Palhaçadas'
export const defaultSocialImageUrl = `${canonicalOrigin}/saltos_logo.jpeg`
const profileDescriptionMaxLength = 160

export type SeoMetadata = {
  title: string
  description: string
  robots: 'index,follow' | 'noindex,follow' | 'noindex,nofollow'
  canonicalUrl: string
  ogType: 'website' | 'profile'
  imageUrl: string
  imageAlt: string
  jsonLd: JsonLdGraph
}

export type JsonLdGraph = {
  '@context': 'https://schema.org'
  '@graph': JsonLdNode[]
}

type JsonLdNode = Record<string, unknown>

type SeoInput = {
  hasProfilesError?: boolean
  isProfilesLoading?: boolean
  pathname: string
  profiles: Profile[]
}

type StaticRouteMetadata = {
  title: string
  description: string
  path: string
}

const publicRoutes: Record<string, StaticRouteMetadata> = {
  '/': {
    title: `${siteName} | Animação de Eventos`,
    description: 'Artistas, portfólios e pedidos de agendamento para criar eventos com ritmo, energia e personalidade.',
    path: '/',
  },
  '/agendar': {
    title: `Agendar evento | ${siteName}`,
    description: 'Consulta os artistas disponíveis e envia um pedido de agendamento para o teu evento.',
    path: '/agendar',
  },
  '/contactos': {
    title: `Contactos | ${siteName}`,
    description: 'Fala com a Saltos nas Palhaçadas para pedidos, reservas, parcerias ou apoio.',
    path: '/contactos',
  },
  '/materiais': {
    title: `Material disponível | ${siteName}`,
    description: 'Consulta o material disponível para apoiar animação, música e experiências em eventos.',
    path: '/materiais',
  },
  '/faq': {
    title: `Perguntas frequentes | ${siteName}`,
    description: 'Respostas sobre agendamentos, perfis, contas, disponibilidade e funcionamento da plataforma.',
    path: '/faq',
  },
  '/privacidade': {
    title: `Privacidade | ${siteName}`,
    description: 'Informação sobre tratamento de dados pessoais, direitos dos titulares e contactos de privacidade.',
    path: '/privacidade',
  },
  '/termos': {
    title: `Termos de utilização | ${siteName}`,
    description: 'Condições de utilização do site Saltos nas Palhaçadas e dos serviços associados.',
    path: '/termos',
  },
  '/cookies': {
    title: `Cookies | ${siteName}`,
    description: 'Informação sobre cookies e tecnologias semelhantes usados no site Saltos nas Palhaçadas.',
    path: '/cookies',
  },
}

const privateRouteMetadata = {
  title: `Área reservada | ${siteName}`,
  description: 'Área reservada da plataforma Saltos nas Palhaçadas.',
}

export function buildSeoMetadata({
  hasProfilesError = false,
  isProfilesLoading = false,
  pathname,
  profiles,
}: SeoInput): SeoMetadata {
  const normalizedPathname = normalizePathname(pathname)
  const profileSlug = profileSlugFromPath(normalizedPathname)

  if (profileSlug) {
    const profile = profiles.find((item) => item.slug === profileSlug) ?? null
    if (profile) return buildProfileSeo(profile)

    const canonicalUrl = absoluteSiteUrl(profilePath(profileSlug))
    if (isProfilesLoading && !hasProfilesError) {
      return buildPageSeo({
        title: `${siteName} | Perfil de artista`,
        description: 'Perfil público de artista na Saltos nas Palhaçadas.',
        path: profilePath(profileSlug),
        robots: 'index,follow',
        canonicalUrl,
      })
    }

    return buildPageSeo({
      title: `Perfil não encontrado | ${siteName}`,
      description: 'O perfil indicado não existe ou deixou de estar disponível.',
      path: profilePath(profileSlug),
      robots: 'noindex,nofollow',
      canonicalUrl,
    })
  }

  if (bookingProfileSlugFromPath(normalizedPathname)) {
    return buildPageSeo({
      ...publicRoutes['/agendar'],
      robots: 'noindex,follow',
      canonicalUrl: absoluteSiteUrl('/agendar'),
    })
  }

  const publicRoute = publicRoutes[normalizedPathname]
  if (publicRoute) return buildPageSeo({ ...publicRoute, robots: 'index,follow' })

  if (isPrivateRoute(normalizedPathname)) {
    return buildPageSeo({
      ...privateRouteMetadata,
      path: normalizedPathname,
      robots: 'noindex,nofollow',
    })
  }

  return buildPageSeo({
    title: `Página não encontrada | ${siteName}`,
    description: 'O endereço indicado não existe ou deixou de estar disponível.',
    path: normalizedPathname,
    robots: 'noindex,nofollow',
  })
}

export function buildProfileTitle(profile: Pick<Profile, 'name' | 'role'>) {
  return `${profile.name} — ${profile.role} | ${siteName}`
}

export function buildProfileDescription(profile: Pick<Profile, 'description' | 'name' | 'role'>) {
  const normalizedDescription = normalizeWhitespace(profile.description)
  if (normalizedDescription) return truncateAtWord(normalizedDescription, profileDescriptionMaxLength)

  return `${profile.name}, ${profile.role}. Consulta o portfólio e disponibilidade para eventos.`
}

export function profileSocialImageUrl(profile: Pick<Profile, 'heroBackgroundImageUrl' | 'imageUrl'>) {
  return toAbsoluteHttpUrl(profile.heroBackgroundImageUrl)
    ?? toAbsoluteHttpUrl(profile.imageUrl)
    ?? defaultSocialImageUrl
}

export function toAbsoluteHttpUrl(value?: string | null) {
  const trimmed = value?.trim()
  if (!trimmed) return null

  try {
    const url = new URL(trimmed, canonicalOrigin)
    return url.protocol === 'http:' || url.protocol === 'https:' ? url.href : null
  } catch {
    return null
  }
}

function buildProfileSeo(profile: Profile): SeoMetadata {
  const description = buildProfileDescription(profile)
  const imageUrl = profileSocialImageUrl(profile)
  const canonicalUrl = absoluteSiteUrl(profilePath(profile.slug))
  const person = buildPersonJsonLd(profile, imageUrl, description)

  return {
    title: buildProfileTitle(profile),
    description,
    robots: 'index,follow',
    canonicalUrl,
    ogType: 'profile',
    imageUrl,
    imageAlt: `${profile.name}, ${profile.role}`,
    jsonLd: {
      '@context': 'https://schema.org',
      '@graph': [
        organizationJsonLd(),
        websiteJsonLd(),
        {
          '@type': 'ProfilePage',
          '@id': `${canonicalUrl}#profile-page`,
          url: canonicalUrl,
          name: buildProfileTitle(profile),
          description,
          isPartOf: { '@id': `${canonicalOrigin}/#website` },
          mainEntity: person,
        },
      ],
    },
  }
}

function buildPageSeo({
  canonicalUrl,
  description,
  path,
  robots,
  title,
}: StaticRouteMetadata & { canonicalUrl?: string; robots: SeoMetadata['robots'] }): SeoMetadata {
  const resolvedCanonicalUrl = canonicalUrl ?? absoluteSiteUrl(path)

  return {
    title,
    description,
    robots,
    canonicalUrl: resolvedCanonicalUrl,
    ogType: 'website',
    imageUrl: defaultSocialImageUrl,
    imageAlt: `${siteName}, animação de eventos`,
    jsonLd: {
      '@context': 'https://schema.org',
      '@graph': [
        organizationJsonLd(),
        websiteJsonLd(),
        {
          '@type': 'WebPage',
          '@id': `${resolvedCanonicalUrl}#webpage`,
          url: resolvedCanonicalUrl,
          name: title,
          description,
          isPartOf: { '@id': `${canonicalOrigin}/#website` },
        },
      ],
    },
  }
}

function buildPersonJsonLd(profile: Profile, imageUrl: string, description: string): JsonLdNode {
  const sameAs = validSocialUrls(profile.socialLinks)
  const person: JsonLdNode = {
    '@type': 'Person',
    name: profile.name,
    jobTitle: profile.role,
    description,
    image: imageUrl,
  }

  if (sameAs.length > 0) person.sameAs = sameAs

  return person
}

function organizationJsonLd(): JsonLdNode {
  return {
    '@type': 'Organization',
    '@id': `${canonicalOrigin}/#organization`,
    name: siteName,
    url: `${canonicalOrigin}/`,
    logo: defaultSocialImageUrl,
  }
}

function websiteJsonLd(): JsonLdNode {
  return {
    '@type': 'WebSite',
    '@id': `${canonicalOrigin}/#website`,
    name: siteName,
    url: `${canonicalOrigin}/`,
    publisher: { '@id': `${canonicalOrigin}/#organization` },
    inLanguage: 'pt-PT',
  }
}

function validSocialUrls(links: Profile['socialLinks']) {
  return links
    .map((link) => toStrictHttpUrl(link.url))
    .filter((url): url is string => Boolean(url))
}

function toStrictHttpUrl(value?: string | null) {
  const trimmed = value?.trim()
  if (!trimmed) return null

  try {
    const url = new URL(trimmed)
    return url.protocol === 'http:' || url.protocol === 'https:' ? url.href : null
  } catch {
    return null
  }
}

function isPrivateRoute(pathname: string) {
  return pathname === '/admin'
    || pathname.startsWith('/admin/')
    || pathname === '/conta'
    || pathname === '/favoritos'
    || pathname === '/login'
    || pathname === '/registo'
    || pathname === '/recuperar-password'
    || pathname === '/reset-password'
}

function absoluteSiteUrl(pathname: string) {
  return new URL(pathname, canonicalOrigin).href
}

function profilePath(slug: string) {
  return `/perfis/${encodeURIComponent(slug)}`
}

function profileSlugFromPath(pathname: string) {
  const match = /^\/perfis\/([^/]+)$/.exec(pathname)
  return match ? safeDecodeURIComponent(match[1]) : null
}

function bookingProfileSlugFromPath(pathname: string) {
  const match = /^\/agendar\/([^/]+)$/.exec(pathname)
  return match ? safeDecodeURIComponent(match[1]) : null
}

function normalizePathname(pathname: string) {
  if (!pathname || !pathname.startsWith('/')) return '/'
  return pathname.length > 1 ? pathname.replace(/\/+$/, '') : pathname
}

function safeDecodeURIComponent(value: string) {
  try {
    return decodeURIComponent(value)
  } catch {
    return value
  }
}

function normalizeWhitespace(value?: string | null) {
  return value?.replace(/\s+/g, ' ').trim() ?? ''
}

function truncateAtWord(value: string, maxLength: number) {
  if (value.length <= maxLength) return value

  const boundary = value.lastIndexOf(' ', maxLength - 1)
  const cutAt = boundary >= Math.round(maxLength * 0.7) ? boundary : maxLength
  return `${value.slice(0, cutAt).trimEnd()}...`
}
