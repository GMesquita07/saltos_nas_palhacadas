import assert from 'node:assert/strict'
import test from 'node:test'

import {
  bookingsRefreshEvent,
  notificationsRefreshEvent,
  requestBookingsRefresh,
  requestNotificationsRefresh,
} from './bookingRefresh.ts'

test('dispatches explicit booking and notification refresh events once', () => {
  const target = new EventTarget()
  const received: string[] = []
  target.addEventListener(bookingsRefreshEvent, (event) => received.push(event.type))
  target.addEventListener(notificationsRefreshEvent, (event) => received.push(event.type))

  requestBookingsRefresh(target)
  requestNotificationsRefresh(target)

  assert.deepEqual(received, ['bookings:refresh', 'notifications:refresh'])
})
