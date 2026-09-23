import { writeFile } from 'node:fs/promises'
import path from 'node:path'

const canonicalOrigin = 'https://www.saltosnaspalhacadas.pt'
const publicPaths = [
  '/',
  '/agendar',
  '/contactos',
  '/materiais',
  '/faq',
  '/privacidade',
  '/termos',
  '/cookies',
]
const timeoutMs = 8000

const profilePaths = await loadProfilePaths()
const urls = uniqueUrls([...publicPaths, ...profilePaths])
const xml = buildSitemapXml(urls)
const sitemapPath = path.resolve('public/sitemap.xml')

await writeFile(sitemapPath, xml)
console.log(`Generated ${path.relative(process.cwd(), sitemapPath)} with ${urls.length} URLs.`)

async function loadProfilePaths() {
  const apiBase = process.env.SEO_API_URL || process.env.VITE_API_URL
  if (!isAbsoluteHttpUrl(apiBase)) {
    warn('SEO_API_URL/VITE_API_URL is not an absolute http(s) URL; generating sitemap with static public URLs only.')
    return []
  }

  try {
    const response = await fetchWithTimeout(apiUrl(apiBase, '/profiles'), timeoutMs)
    if (!response.ok) throw new Error(`GET /profiles failed with HTTP ${response.status}`)

    const profiles = await response.json()
    if (!Array.isArray(profiles)) throw new Error('GET /profiles did not return an array')

    return profiles
      .map((profile) => typeof profile?.slug === 'string' ? profile.slug.trim() : '')
      .filter(isValidSlug)
      .sort((left, right) => left.localeCompare(right, 'pt-PT'))
      .map((slug) => `/perfis/${encodeURIComponent(slug)}`)
  } catch (error) {
    warn(`Could not fetch public profiles for sitemap: ${error instanceof Error ? error.message : String(error)}. Generating static URLs only.`)
    return []
  }
}

async function fetchWithTimeout(url, timeout) {
  const controller = new AbortController()
  const timeoutId = setTimeout(() => controller.abort(), timeout)

  try {
    return await fetch(url, { signal: controller.signal })
  } finally {
    clearTimeout(timeoutId)
  }
}

function apiUrl(base, pathname) {
  const normalizedBase = base.endsWith('/') ? base : `${base}/`
  return new URL(pathname.replace(/^\/+/, ''), normalizedBase).href
}

function uniqueUrls(paths) {
  return [...new Set(paths.map((urlPath) => new URL(urlPath, canonicalOrigin).href))]
}

function buildSitemapXml(urls) {
  const entries = urls
    .map((url) => `  <url>\n    <loc>${escapeXml(url)}</loc>\n  </url>`)
    .join('\n')

  return `<?xml version="1.0" encoding="UTF-8"?>\n<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">\n${entries}\n</urlset>\n`
}

function isValidSlug(slug) {
  return /^[a-z0-9]+(?:-[a-z0-9]+)*$/.test(slug)
}

function isAbsoluteHttpUrl(value) {
  if (!value) return false

  try {
    const url = new URL(value)
    return (url.protocol === 'http:' || url.protocol === 'https:') && Boolean(url.hostname)
  } catch {
    return false
  }
}

function escapeXml(value) {
  return value
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&apos;')
}

function warn(message) {
  console.warn(`[sitemap] ${message}`)
}
