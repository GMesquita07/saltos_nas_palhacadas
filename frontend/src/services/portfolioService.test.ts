import assert from 'node:assert/strict'
import test from 'node:test'

import { portfolioInvalidationMatches } from './portfolioInvalidation.ts'

test('matches portfolio invalidation marker for the same slug', () => {
  assert.equal(portfolioInvalidationMatches(JSON.stringify({ slug: 'dj-kidg', updatedAt: 123 }), 'dj-kidg'), true)
})

test('ignores invalidation markers for other slugs or malformed values', () => {
  assert.equal(portfolioInvalidationMatches(JSON.stringify({ slug: 'outro-artista', updatedAt: 123 }), 'dj-kidg'), false)
  assert.equal(portfolioInvalidationMatches('{', 'dj-kidg'), false)
  assert.equal(portfolioInvalidationMatches(null, 'dj-kidg'), false)
})
