import assert from 'node:assert/strict'
import test from 'node:test'

import { forgotPassword, resetPassword, validatePasswordResetToken } from './authService.ts'

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

test('prevalidation posts the token in the body and supports cancellation without auth or Turnstile', async (t) => {
  const controller = new AbortController()
  t.mock.method(globalThis, 'fetch', async (url: string, options: RequestInit) => {
    assert.equal(url, '/api/v1/auth/reset-password/validate')
    assert.equal(options.method, 'POST')
    assert.equal(options.signal, controller.signal)
    assert.deepEqual(JSON.parse(options.body as string), { token: 'raw-token' })
    assert.deepEqual(options.headers, { 'Content-Type': 'application/json' })
    return new Response(null, { status: 204 })
  })
  assert.equal(await validatePasswordResetToken('raw-token', { signal: controller.signal }), undefined)
})

test('prevalidation propagates invalid tokens and unavailable backend separately', async (t) => {
  let status = 400
  t.mock.method(globalThis, 'fetch', async () => new Response(null, { status }))
  await assert.rejects(validatePasswordResetToken('invalid'), { status: 400 })
  status = 503
  await assert.rejects(validatePasswordResetToken('unknown'), { status: 503 })
})

test('forgot and final reset retain their existing contracts', async (t) => {
  const calls: { url: string; options: RequestInit }[] = []
  t.mock.method(globalThis, 'fetch', async (url: string, options: RequestInit) => {
    calls.push({ url, options })
    return new Response(null, { status: 204 })
  })
  await forgotPassword('user@example.test', 'challenge')
  await resetPassword('raw-token', 'password-nova')
  assert.equal(calls[0].url, '/api/v1/auth/forgot-password')
  assert.deepEqual(JSON.parse(calls[0].options.body as string), { email: 'user@example.test' })
  assert.equal((calls[0].options.headers as Record<string, string>)['X-Turnstile-Token'], 'challenge')
  assert.equal(calls[1].url, '/api/v1/auth/reset-password')
  assert.equal(calls[1].options.method, 'POST')
  assert.deepEqual(JSON.parse(calls[1].options.body as string), { token: 'raw-token', newPassword: 'password-nova' })
  assert.deepEqual(calls[1].options.headers, { 'Content-Type': 'application/json' })
})
