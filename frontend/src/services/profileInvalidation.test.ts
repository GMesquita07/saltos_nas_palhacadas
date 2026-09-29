import assert from 'node:assert/strict'
import test from 'node:test'

import { invalidatePublicProfiles, markProfilesLoaded, profilesInvalidationMatches, shouldReloadProfiles } from './profileInvalidation.ts'

test('keeps public profiles dirty until a fresh load succeeds', () => {
  markProfilesLoaded()
  assert.equal(shouldReloadProfiles(), false)

  invalidatePublicProfiles({ broadcast: false })
  assert.equal(shouldReloadProfiles(), true)
  assert.equal(shouldReloadProfiles(), true)

  markProfilesLoaded()
  assert.equal(shouldReloadProfiles(), false)
})

test('force reload and storage markers request a fresh profiles read', () => {
  assert.equal(shouldReloadProfiles(true), true)
  assert.equal(profilesInvalidationMatches(JSON.stringify({ updatedAt: 123 })), true)
  assert.equal(profilesInvalidationMatches(JSON.stringify({ slug: 'dj-kidg' })), false)
  assert.equal(profilesInvalidationMatches('{'), false)
  assert.equal(profilesInvalidationMatches(null), false)
})
