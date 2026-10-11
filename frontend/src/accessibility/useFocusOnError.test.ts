import assert from 'node:assert/strict'
import test from 'node:test'

import { shouldFocusError } from './useFocusOnError.ts'

test('focuses an actionable form error summary', () => {
  assert.equal(shouldFocusError('Indica o teu email.'), true)
})

test('does not focus an absent or empty error summary', () => {
  assert.equal(shouldFocusError(null), false)
  assert.equal(shouldFocusError('  '), false)
})
