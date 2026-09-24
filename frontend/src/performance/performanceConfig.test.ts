import assert from 'node:assert/strict'
import test from 'node:test'

import { profileCardImagePolicy, routeNeedsProfiles } from './performanceConfig.ts'

test('only loads profiles on routes that need public profile data', () => {
  assert.equal(routeNeedsProfiles('/'), true)
  assert.equal(routeNeedsProfiles('/perfis/dj-kidg'), true)
  assert.equal(routeNeedsProfiles('/agendar'), true)
  assert.equal(routeNeedsProfiles('/agendar/dj-kidg'), true)
  assert.equal(routeNeedsProfiles('/contactos'), false)
  assert.equal(routeNeedsProfiles('/materiais'), false)
  assert.equal(routeNeedsProfiles('/faq'), false)
  assert.equal(routeNeedsProfiles('/privacidade'), false)
  assert.equal(routeNeedsProfiles('/termos'), false)
  assert.equal(routeNeedsProfiles('/cookies'), false)
  assert.equal(routeNeedsProfiles('/login'), false)
  assert.equal(routeNeedsProfiles('/admin/perfis'), false)
})

test('keeps homepage profile images below the splash LCP priority', () => {
  assert.deepEqual(profileCardImagePolicy(0), { decoding: 'async', fetchPriority: 'auto', loading: 'eager' })
  assert.deepEqual(profileCardImagePolicy(1), { decoding: 'async', fetchPriority: 'low', loading: 'lazy' })
  assert.deepEqual(profileCardImagePolicy(4), { decoding: 'async', fetchPriority: 'low', loading: 'lazy' })
})
