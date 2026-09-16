import assert from 'node:assert/strict'
import test from 'node:test'

import { canSubmitProtectedAuth, protectedAuthRequiresSiteKey, turnstileActionForMode } from './turnstileAuth.ts'

test('maps public auth modes to distinct Turnstile actions', () => {
  assert.equal(turnstileActionForMode('login'), 'login')
  assert.equal(turnstileActionForMode('register'), 'register')
  assert.equal(turnstileActionForMode('forgot'), 'forgot_password')
  assert.equal(turnstileActionForMode('reset'), null)
})

test('allows local development without a Turnstile site key', () => {
  assert.equal(protectedAuthRequiresSiteKey('login', false, ''), false)
  assert.equal(canSubmitProtectedAuth('login', '', null, false), true)
})

test('blocks protected auth in production without a Turnstile site key', () => {
  assert.equal(protectedAuthRequiresSiteKey('register', true, ''), true)
  assert.equal(canSubmitProtectedAuth('register', '', null, true), false)
})

test('requires a token when a Turnstile site key is configured', () => {
  assert.equal(canSubmitProtectedAuth('forgot', 'site-key', null, true), false)
  assert.equal(canSubmitProtectedAuth('forgot', 'site-key', 'token', true), true)
})

test('does not require Turnstile for reset-password mode', () => {
  assert.equal(protectedAuthRequiresSiteKey('reset', true, ''), false)
  assert.equal(canSubmitProtectedAuth('reset', '', null, true), true)
})
