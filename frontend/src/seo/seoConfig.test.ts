import assert from 'node:assert/strict'
import test from 'node:test'

import type { Profile } from '../types/profile.ts'
import {
  buildProfileDescription,
  buildSeoMetadata,
  canonicalOrigin,
  defaultSocialImageUrl,
  profileSocialImageUrl,
} from './seoConfig.ts'

const baseProfile: Profile = {
  id: 'dj-kidg',
  slug: 'dj-kidg',
  name: 'DJ KidG',
  role: 'DJ',
  description: 'Energia, som e leitura de pista para festas privadas e eventos corporativos.',
  imageUrl: '/content/profiles/dj-kidg/profile.jpeg',
  heroBackgroundImageUrl: '/content/profiles/dj-kidg/hero.jpeg',
  socialLinks: [
    { platform: 'INSTAGRAM', url: 'https://instagram.example/djkidg' },
    { platform: 'EMAIL', url: 'kidg@example.test' },
    { platform: 'WEBSITE', url: 'javascript:alert(1)' },
    { platform: 'FACEBOOK', url: 'http://facebook.example/djkidg' },
    { platform: 'OTHER', url: '/relative-profile' },
  ],
}

test('builds homepage metadata with canonical and indexable robots', () => {
  const metadata = buildSeoMetadata({ pathname: '/', profiles: [] })

  assert.equal(metadata.title, 'Saltos nas Palhaçadas | Animação de Eventos')
  assert.equal(metadata.canonicalUrl, `${canonicalOrigin}/`)
  assert.equal(metadata.robots, 'index,follow')
})

test('builds profile canonical, title and compact description', () => {
  const profile = {
    ...baseProfile,
    description: '  Linha um\n\nlinha dois com uma descrição longa que continua para testar a compactação por palavras sem partir termos de forma estranha no resultado final do snippet público.  ',
  }
  const metadata = buildSeoMetadata({ pathname: '/perfis/dj-kidg', profiles: [profile] })

  assert.equal(metadata.canonicalUrl, `${canonicalOrigin}/perfis/dj-kidg`)
  assert.equal(metadata.title, 'DJ KidG — DJ | Saltos nas Palhaçadas')
  assert.equal(metadata.description.includes('\n'), false)
  assert.equal(metadata.description.length <= 163, true)
  assert.equal(metadata.description.endsWith('...'), true)
})

test('uses profile fallback description when description is empty', () => {
  assert.equal(
    buildProfileDescription({ ...baseProfile, description: '   ' }),
    'DJ KidG, DJ. Consulta o portfólio e disponibilidade para eventos.',
  )
})

test('prioritizes profile social images and makes them absolute', () => {
  assert.equal(profileSocialImageUrl(baseProfile), `${canonicalOrigin}/content/profiles/dj-kidg/hero.jpeg`)
  assert.equal(profileSocialImageUrl({ ...baseProfile, heroBackgroundImageUrl: undefined }), `${canonicalOrigin}/content/profiles/dj-kidg/profile.jpeg`)
  assert.equal(profileSocialImageUrl({ ...baseProfile, heroBackgroundImageUrl: undefined, imageUrl: undefined }), defaultSocialImageUrl)
})

test('marks private, admin and unknown routes as noindex', () => {
  assert.equal(buildSeoMetadata({ pathname: '/login', profiles: [] }).robots, 'noindex,nofollow')
  assert.equal(buildSeoMetadata({ pathname: '/admin/perfis', profiles: [] }).robots, 'noindex,nofollow')
  assert.equal(buildSeoMetadata({ pathname: '/desconhecida', profiles: [] }).robots, 'noindex,nofollow')
})

test('marks artist-specific booking route noindex with /agendar canonical', () => {
  const metadata = buildSeoMetadata({ pathname: '/agendar/dj-kidg', profiles: [baseProfile] })

  assert.equal(metadata.robots, 'noindex,follow')
  assert.equal(metadata.canonicalUrl, `${canonicalOrigin}/agendar`)
})

test('keeps profile loading route indexable until profiles finish loading', () => {
  const loading = buildSeoMetadata({ pathname: '/perfis/dj-kidg', profiles: [], isProfilesLoading: true })
  const notFound = buildSeoMetadata({ pathname: '/perfis/dj-kidg', profiles: [], isProfilesLoading: false })

  assert.equal(loading.robots, 'index,follow')
  assert.equal(notFound.robots, 'noindex,nofollow')
})

test('builds WebSite and Organization JSON-LD for public pages', () => {
  const graph = buildSeoMetadata({ pathname: '/faq', profiles: [] }).jsonLd['@graph']

  assert.equal(graph.some((node) => node['@type'] === 'Organization'), true)
  assert.equal(graph.some((node) => node['@type'] === 'WebSite'), true)
  assert.equal(graph.some((node) => node['@type'] === 'WebPage'), true)
})

test('builds ProfilePage and Person JSON-LD with valid sameAs URLs only', () => {
  const graph = buildSeoMetadata({ pathname: '/perfis/dj-kidg', profiles: [baseProfile] }).jsonLd['@graph']
  const profilePage = graph.find((node) => node['@type'] === 'ProfilePage')
  const person = profilePage?.mainEntity as { sameAs?: string[] } | undefined

  assert.ok(profilePage)
  assert.deepEqual(person?.sameAs, [
    'https://instagram.example/djkidg',
    'http://facebook.example/djkidg',
  ])
})
