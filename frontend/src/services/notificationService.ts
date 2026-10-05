import { apiClient } from './apiClient.ts'
import type { NotificationInbox, UserNotification, UserNotificationType } from '../types/notification'

const notificationTypes = new Set<UserNotificationType>([
  'BOOKING_CREATED',
  'BOOKING_ACCEPTED',
  'BOOKING_DECLINED',
  'BOOKING_COUNTER_PROPOSED',
  'BOOKING_CUSTOMER_COUNTER_PROPOSED',
  'BOOKING_CUSTOMER_COUNTER_ACCEPTED',
  'BOOKING_CUSTOMER_COUNTER_DECLINED',
  'BOOKING_CUSTOMER_CANCELLED',
  'BOOKING_CANCELLED',
  'BOOKING_REMINDER_5_DAYS',
])

export async function getNotifications(token: string): Promise<NotificationInbox> {
  const response = await apiClient<unknown>('/notifications', {}, token)
  return toNotificationInbox(response)
}

export function markNotificationRead(id: string, token: string): Promise<void> {
  return apiClient<void>(`/notifications/${encodeURIComponent(id)}/read`, { method: 'PATCH' }, token)
}

export function markAllNotificationsRead(token: string): Promise<void> {
  return apiClient<void>('/notifications/read-all', { method: 'PATCH' }, token)
}

export function toNotificationInbox(value: unknown): NotificationInbox {
  const source = isRecord(value) ? value : {}
  const notifications = Array.isArray(source.notifications)
    ? source.notifications.map(toNotification).filter((item): item is UserNotification => item !== null)
    : []

  notifications.sort((left, right) => {
    const byDate = Date.parse(right.createdAt) - Date.parse(left.createdAt)
    return Number.isNaN(byDate) || byDate === 0
      ? right.id.localeCompare(left.id, undefined, { numeric: true })
      : byDate
  })

  const unreadCount = typeof source.unreadCount === 'number' && Number.isFinite(source.unreadCount)
    ? Math.max(0, Math.floor(source.unreadCount))
    : notifications.filter((notification) => !notification.read).length

  return { notifications, unreadCount }
}

export function formatNotificationDate(value: string): string {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return 'Data indisponível'
  return new Intl.DateTimeFormat('pt-PT', { dateStyle: 'medium', timeStyle: 'short' }).format(date)
}

export function formatNotificationBadge(count: number): string {
  const safeCount = Number.isFinite(count) ? Math.max(0, Math.floor(count)) : 0
  return safeCount > 99 ? '99+' : String(safeCount)
}

export function markInboxNotificationRead(inbox: NotificationInbox, id: string, readAt = new Date().toISOString()): NotificationInbox {
  let changed = false
  const notifications = inbox.notifications.map((notification) => {
    if (notification.id !== id || notification.read) return notification
    changed = true
    return { ...notification, read: true, readAt }
  })
  return changed
    ? { notifications, unreadCount: Math.max(0, inbox.unreadCount - 1) }
    : inbox
}

export function markInboxRead(inbox: NotificationInbox, readAt = new Date().toISOString()): NotificationInbox {
  return {
    notifications: inbox.notifications.map((notification) => ({
      ...notification,
      read: true,
      readAt: notification.readAt ?? readAt,
    })),
    unreadCount: 0,
  }
}

function toNotification(value: unknown): UserNotification | null {
  if (!isRecord(value)) return null
  const type = typeof value.type === 'string' && notificationTypes.has(value.type as UserNotificationType)
    ? value.type as UserNotificationType
    : null
  if ((typeof value.id !== 'string' && typeof value.id !== 'number')
      || !type
      || typeof value.title !== 'string'
      || typeof value.message !== 'string'
      || typeof value.createdAt !== 'string') {
    return null
  }

  const readAt = typeof value.readAt === 'string' ? value.readAt : null
  return {
    id: String(value.id),
    type,
    title: value.title.trim(),
    message: value.message.trim(),
    bookingId: typeof value.bookingId === 'string' || typeof value.bookingId === 'number' ? String(value.bookingId) : null,
    createdAt: value.createdAt,
    readAt,
    read: typeof value.read === 'boolean' ? value.read : readAt !== null,
  }
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value)
}
