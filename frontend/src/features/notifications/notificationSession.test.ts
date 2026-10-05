import assert from 'node:assert/strict'
import test from 'node:test'

import { isCurrentNotificationRequest, isNotificationStateVisible } from './notificationSession.ts'

test('notification state is never exposed to another session', () => {
  assert.equal(isNotificationStateVisible('token-a', 'token-a'), true)
  assert.equal(isNotificationStateVisible('token-a', 'token-b'), false)
  assert.equal(isNotificationStateVisible('token-a', null), false)
})

test('late notification requests cannot write after logout or session replacement', () => {
  assert.equal(isCurrentNotificationRequest('token-a', 3, 'token-a', 3), true)
  assert.equal(isCurrentNotificationRequest(null, 4, 'token-a', 3), false)
  assert.equal(isCurrentNotificationRequest('token-b', 4, 'token-a', 3), false)
  assert.equal(isCurrentNotificationRequest('token-a', 4, 'token-a', 3), false)
})
