import assert from 'node:assert/strict'
import test from 'node:test'
import { ApiError } from '../../services/apiClient.ts'
import { authPath } from '../../navigation/routes.ts'
import {
  initialResetTokenValidationState, shouldShowResetPasswordForm, canSubmitResetPassword,
  classifyResetTokenValidationFailure, classifyResetPasswordSubmitFailure, resetRecoveryMode,
  type ResetTokenValidationState,
} from './resetPasswordUx.ts'

test('email reset link starts validating; incomplete links are immediately invalid', () => {
  assert.equal(initialResetTokenValidationState('reset', 'token'), 'validating')
  for (const token of [undefined, null, '', '  ']) {
    assert.equal(initialResetTokenValidationState('reset', token), 'invalid')
  }
  for (const mode of ['login', 'register', 'forgot'] as const) {
    assert.equal(initialResetTokenValidationState(mode), 'idle')
  }
})

test('only a validated token exposes and enables the form', () => {
  for (const state of ['idle', 'validating', 'invalid', 'error'] satisfies ResetTokenValidationState[]) {
    assert.equal(shouldShowResetPasswordForm(state), false)
    assert.equal(canSubmitResetPassword(state, false), false)
  }
  assert.equal(shouldShowResetPasswordForm('valid'), true)
  assert.equal(canSubmitResetPassword('valid', false), true)
  assert.equal(canSubmitResetPassword('valid', true), false)
})

test('validation separates unusable links, infrastructure failures and cancellation', () => {
  assert.equal(classifyResetTokenValidationFailure(new ApiError('generic', 400)), 'invalid')
  for (const reason of [new ApiError('unavailable', 503), new ApiError('rate limit', 429), new TypeError('network'), new DOMException('timeout', 'TimeoutError')]) {
    assert.equal(classifyResetTokenValidationFailure(reason), 'error')
  }
  assert.equal(classifyResetTokenValidationFailure(new DOMException('aborted', 'AbortError')), 'cancelled')
})

test('final POST rejection invalidates the link but field and network errors do not', () => {
  assert.equal(classifyResetPasswordSubmitFailure(new ApiError('generic', 400)), 'invalid')
  assert.equal(classifyResetPasswordSubmitFailure(new ApiError('fields', 400, { newPassword: 'invalid' })), 'error')
  assert.equal(classifyResetPasswordSubmitFailure(new ApiError('unavailable', 503)), 'error')
  assert.equal(classifyResetPasswordSubmitFailure(new TypeError('network')), 'error')
})

test('request-new-link CTA uses the existing recovery route', () => {
  assert.equal(authPath(resetRecoveryMode()), '/recuperar-password')
})
