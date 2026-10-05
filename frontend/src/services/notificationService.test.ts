import assert from 'node:assert/strict'
import test from 'node:test'

import {
  formatNotificationDate,
  formatNotificationBadge,
  getNotifications,
  markAllNotificationsRead,
  markNotificationRead,
  markInboxNotificationRead,
  markInboxRead,
  toNotificationInbox,
} from './notificationService.ts'

test('maps, filters and orders notification responses defensively', () => {
  const inbox = toNotificationInbox({
    notifications: [
      { id: 2, type: 'BOOKING_ACCEPTED', title: ' Aceite ', message: ' Confirmado ', bookingId: 10, createdAt: '2026-10-05T09:00:00Z', readAt: null, read: false },
      { id: 3, type: 'UNKNOWN', title: 'Ignorar', message: 'Ignorar', createdAt: '2026-10-05T11:00:00Z' },
      { id: 1, type: 'BOOKING_CANCELLED', title: 'Cancelado', message: 'Cancelado', bookingId: null, createdAt: '2026-10-05T10:00:00Z', readAt: '2026-10-05T10:05:00Z' },
    ],
    unreadCount: 4.9,
  })

  assert.deepEqual(inbox.notifications.map((notification) => notification.id), ['1', '2'])
  assert.equal(inbox.notifications[0].read, true)
  assert.equal(inbox.notifications[1].title, 'Aceite')
  assert.equal(inbox.notifications[1].bookingId, '10')
  assert.equal(inbox.unreadCount, 4)
})

test('falls back to mapped unread items when count is absent', () => {
  const inbox = toNotificationInbox({
    notifications: [
      { id: 1, type: 'BOOKING_DECLINED', title: 'Pedido', message: 'Mensagem', createdAt: '2026-10-05T09:00:00Z', read: false },
      { id: 2, type: 'BOOKING_COUNTER_PROPOSED', title: 'Proposta', message: 'Mensagem', createdAt: '2026-10-05T10:00:00Z', readAt: '2026-10-05T10:10:00Z' },
    ],
  })
  assert.equal(inbox.unreadCount, 1)
})

test('notification API uses authenticated GET and PATCH contracts', async (t) => {
  const requests: Array<{ url: string; init?: RequestInit }> = []
  t.mock.method(globalThis, 'fetch', async (url, init) => {
    requests.push({ url: String(url), init })
    if (!init?.method) {
      return new Response(JSON.stringify({ notifications: [], unreadCount: 0 }), { status: 200 })
    }
    return new Response(null, { status: 204 })
  })

  await getNotifications('token-test')
  await markNotificationRead('notification/1', 'token-test')
  await markAllNotificationsRead('token-test')

  assert.deepEqual(requests.map((request) => [request.url, request.init?.method ?? 'GET']), [
    ['/api/v1/notifications', 'GET'],
    ['/api/v1/notifications/notification%2F1/read', 'PATCH'],
    ['/api/v1/notifications/read-all', 'PATCH'],
  ])
  assert.ok(requests.every((request) => new Headers(request.init?.headers).get('Authorization') === 'Bearer token-test'))
})

test('formats notification timestamps in pt-PT and handles invalid values', () => {
  assert.match(formatNotificationDate('2026-10-05T10:30:00Z'), /2026/)
  assert.equal(formatNotificationDate('invalid'), 'Data indisponível')
})

test('supports reminder/admin-action notifications and compact bell counts', () => {
  const inbox = toNotificationInbox({
    notifications: [
      { id: 7, type: 'BOOKING_REMINDER_5_DAYS', title: 'Lembrete', message: 'Evento próximo', createdAt: '2026-10-05T10:30:00Z' },
      { id: 8, type: 'BOOKING_CUSTOMER_COUNTER_PROPOSED', title: 'Contraproposta', message: 'Requer decisão', createdAt: '2026-10-05T10:31:00Z' },
      { id: 9, type: 'BOOKING_CREATED', title: 'Novo pedido', message: 'Requer análise', createdAt: '2026-10-05T10:32:00Z' },
      { id: 10, type: 'BOOKING_CUSTOMER_COUNTER_ACCEPTED', title: 'Aceite', message: 'Cliente aceitou', createdAt: '2026-10-05T10:33:00Z' },
      { id: 11, type: 'BOOKING_CUSTOMER_COUNTER_DECLINED', title: 'Recusada', message: 'Cliente recusou', createdAt: '2026-10-05T10:34:00Z' },
      { id: 12, type: 'BOOKING_CUSTOMER_CANCELLED', title: 'Cancelado', message: 'Cliente cancelou', createdAt: '2026-10-05T10:35:00Z' },
    ],
  })
  assert.deepEqual(
    inbox.notifications.slice(0, 5).map((notification) => notification.type),
    [
      'BOOKING_CUSTOMER_CANCELLED',
      'BOOKING_CUSTOMER_COUNTER_DECLINED',
      'BOOKING_CUSTOMER_COUNTER_ACCEPTED',
      'BOOKING_CREATED',
      'BOOKING_CUSTOMER_COUNTER_PROPOSED',
    ],
  )
  assert.equal(inbox.notifications.at(-1)?.type, 'BOOKING_REMINDER_5_DAYS')
  assert.deepEqual([0, 1, 99, 100].map(formatNotificationBadge), ['0', '1', '99', '99+'])
})

test('read helpers update counts idempotently', () => {
  const inbox = toNotificationInbox({
    notifications: [
      { id: 1, type: 'BOOKING_ACCEPTED', title: 'Aceite', message: 'Ok', createdAt: '2026-10-05T10:00:00Z', read: false },
      { id: 2, type: 'BOOKING_DECLINED', title: 'Recusado', message: 'Não', createdAt: '2026-10-05T09:00:00Z', read: false },
    ],
    unreadCount: 2,
  })
  const oneRead = markInboxNotificationRead(inbox, '1', '2026-10-05T11:00:00Z')
  assert.equal(oneRead.unreadCount, 1)
  assert.equal(markInboxNotificationRead(oneRead, '1').unreadCount, 1)
  assert.equal(markInboxRead(oneRead).unreadCount, 0)
})
