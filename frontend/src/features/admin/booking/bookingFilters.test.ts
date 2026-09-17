import assert from 'node:assert/strict'
import test from 'node:test'

import {
  bookingEmptyMessage,
  bookingFilters,
  defaultBookingFilter,
  toBookingStatusFilter,
} from './bookingFilters.ts'

test('admin bookings default to all statuses', () => {
  assert.equal(defaultBookingFilter, 'ALL')
  assert.deepEqual(bookingFilters[0], { value: 'ALL', label: 'Todos' })
  assert.equal(toBookingStatusFilter(defaultBookingFilter), undefined)
})

test('admin bookings still map specific filters to status requests', () => {
  assert.equal(toBookingStatusFilter('PENDING'), 'PENDING')
  assert.equal(toBookingStatusFilter('ACCEPTED'), 'ACCEPTED')
})

test('admin bookings empty states match the selected filter', () => {
  assert.equal(bookingEmptyMessage('ALL'), 'Ainda não existem agendamentos.')
  assert.equal(bookingEmptyMessage('PENDING'), 'Não há pedidos pendentes neste momento.')
  assert.equal(bookingEmptyMessage('CANCELLED'), 'Não existem agendamentos neste estado.')
})
