import { useEffect } from 'react'
import { useLocation } from 'react-router-dom'
import type { Profile } from '../types/profile'
import { buildSeoMetadata, siteName } from './seoConfig'

type SeoManagerProps = {
  hasProfilesError: boolean
  isProfilesLoading: boolean
  profiles: Profile[]
}

export function SeoManager({ hasProfilesError, isProfilesLoading, profiles }: SeoManagerProps) {
  const location = useLocation()

  useEffect(() => {
    const metadata = buildSeoMetadata({
      hasProfilesError,
      isProfilesLoading,
      pathname: location.pathname,
      profiles,
    })

    document.title = metadata.title
    setMetaContent('description', metadata.description)
    setMetaContent('robots', metadata.robots)
    setCanonical(metadata.canonicalUrl)

    setMetaProperty('og:type', metadata.ogType)
    setMetaProperty('og:site_name', siteName)
    setMetaProperty('og:locale', 'pt_PT')
    setMetaProperty('og:title', metadata.title)
    setMetaProperty('og:description', metadata.description)
    setMetaProperty('og:url', metadata.canonicalUrl)
    setMetaProperty('og:image', metadata.imageUrl)
    setMetaProperty('og:image:alt', metadata.imageAlt)

    setMetaContent('twitter:card', 'summary_large_image')
    setMetaContent('twitter:title', metadata.title)
    setMetaContent('twitter:description', metadata.description)
    setMetaContent('twitter:image', metadata.imageUrl)
    setMetaContent('twitter:image:alt', metadata.imageAlt)
    setJsonLd(metadata.jsonLd)
  }, [hasProfilesError, isProfilesLoading, location.pathname, profiles])

  return null
}

function setMetaContent(name: string, content: string) {
  const selector = `meta[name="${cssEscape(name)}"]`
  const tag = getOrCreateElement(selector, () => {
    const meta = document.createElement('meta')
    meta.setAttribute('name', name)
    document.head.append(meta)
    return meta
  })
  tag.setAttribute('content', content)
}

function setMetaProperty(property: string, content: string) {
  const selector = `meta[property="${cssEscape(property)}"]`
  const tag = getOrCreateElement(selector, () => {
    const meta = document.createElement('meta')
    meta.setAttribute('property', property)
    document.head.append(meta)
    return meta
  })
  tag.setAttribute('content', content)
}

function setCanonical(href: string) {
  const tag = getOrCreateElement('link[rel="canonical"]', () => {
    const link = document.createElement('link')
    link.setAttribute('rel', 'canonical')
    document.head.append(link)
    return link
  })
  tag.setAttribute('href', href)
}

function setJsonLd(jsonLd: unknown) {
  const tag = getOrCreateElement('script#saltos-json-ld', () => {
    const script = document.createElement('script')
    script.id = 'saltos-json-ld'
    script.type = 'application/ld+json'
    document.head.append(script)
    return script
  })
  tag.textContent = JSON.stringify(jsonLd)
}

function getOrCreateElement<T extends Element>(selector: string, create: () => T): T {
  return document.head.querySelector<T>(selector) ?? create()
}

function cssEscape(value: string) {
  return value.replace(/"/g, '\\"')
}
