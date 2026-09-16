import assert from 'node:assert/strict'
import test from 'node:test'

import { turnstileHeaderName, turnstileHeaders } from './turnstileHeaders.ts'

test('builds Turnstile header from a non-empty token', () => {
  assert.deepEqual(turnstileHeaders(' token-value '), {
    [turnstileHeaderName]: 'token-value',
  })
})

test('omits Turnstile header for missing token', () => {
  assert.deepEqual(turnstileHeaders(), {})
  assert.deepEqual(turnstileHeaders('   '), {})
})
