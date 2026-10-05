import assert from 'node:assert/strict'
import test from 'node:test'

import { counterProposeBooking, customerCanRespondToCounterProposal, getAvailability, toBooking } from './bookingService.ts'

test('maps complete counter proposals defensively and derives customer actions', () => {
  const booking = toBooking({
    id: 9,
    profileSlug: 'dj-teste',
    profileName: 'DJ Teste',
    eventDate: '2026-11-10',
    startTime: '10:00:00',
    endTime: '12:00:00',
    eventType: 'BIRTHDAY',
    contactName: 'Cliente',
    contactPhone: '912345678',
    description: 'Festa',
    status: 'COUNTER_PROPOSED',
    counterProposal: {
      budget: 500,
      eventDate: '2026-11-11',
      startTime: '14:00:00',
      endTime: '16:00:00',
      proposedBy: 'ADMIN',
    },
  })
  assert.deepEqual(booking.counterProposal, {
    budget: 500,
    eventDate: '2026-11-11',
    startTime: '14:00:00',
    endTime: '16:00:00',
    proposedBy: 'ADMIN',
  })
  assert.equal(customerCanRespondToCounterProposal(booking), true)
  assert.equal(customerCanRespondToCounterProposal({
    ...booking,
    counterProposal: { ...booking.counterProposal!, proposedBy: 'CUSTOMER' },
  }), false)
})

test('sends an authenticated customer counterproposal contract', async (t) => {
  let request: { url: string; init?: RequestInit } | null = null
  t.mock.method(globalThis, 'fetch', async (url, init) => {
    request = { url: String(url), init }
    return new Response(JSON.stringify({
      id: 9,
      profileSlug: 'dj-teste',
      eventDate: '2026-11-10',
      eventType: 'BIRTHDAY',
      contactName: 'Cliente',
      contactPhone: '912345678',
      description: 'Festa',
      status: 'COUNTER_PROPOSED',
      counterProposal: { eventDate: '2026-11-12', proposedBy: 'CUSTOMER' },
    }), { status: 200 })
  })

  await counterProposeBooking('booking/9', { counterEventDate: '2026-11-12', message: 'Alternativa' }, 'token-test')
  assert.ok(request)
  assert.equal(request.url, '/api/v1/bookings/booking%2F9/counter-proposal')
  assert.equal(request.init?.method, 'PUT')
  assert.equal(new Headers(request.init?.headers).get('Authorization'), 'Bearer token-test')
  assert.deepEqual(JSON.parse(String(request.init?.body)), { counterEventDate: '2026-11-12', message: 'Alternativa' })
})

test('maps the final accepted budget for the customer', () => {
  const booking = toBooking({
    id: 12,
    profileSlug: 'dj-teste',
    eventDate: '2026-11-10',
    eventType: 'BIRTHDAY',
    contactName: 'Cliente',
    contactPhone: '912345678',
    description: 'Festa',
    status: 'ACCEPTED',
    budget: 725,
  })

  assert.equal(booking.status, 'ACCEPTED')
  assert.equal(booking.budget, 725)
})

test('keeps canonical counter-proposed slots in public availability', async (t) => {
  t.mock.method(globalThis, 'fetch', async () => new Response(JSON.stringify({
    bookedDates: [],
    slots: [{ date: '2026-11-10', startTime: '10:00:00', endTime: '12:00:00', status: 'COUNTER_PROPOSED' }],
  }), { status: 200 }))

  assert.deepEqual(await getAvailability('dj-teste', '2026-11-01', '2026-11-30'), [{
    date: '2026-11-10',
    startTime: '10:00:00',
    endTime: '12:00:00',
    status: 'COUNTER_PROPOSED',
  }])
})
