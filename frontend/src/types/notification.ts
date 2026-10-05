export type UserNotificationType =
  | 'BOOKING_CREATED'
  | 'BOOKING_ACCEPTED'
  | 'BOOKING_DECLINED'
  | 'BOOKING_COUNTER_PROPOSED'
  | 'BOOKING_CUSTOMER_COUNTER_PROPOSED'
  | 'BOOKING_CUSTOMER_COUNTER_ACCEPTED'
  | 'BOOKING_CUSTOMER_COUNTER_DECLINED'
  | 'BOOKING_CUSTOMER_CANCELLED'
  | 'BOOKING_CANCELLED'
  | 'BOOKING_REMINDER_5_DAYS'

export type UserNotification = {
  id: string
  type: UserNotificationType
  title: string
  message: string
  bookingId: string | null
  createdAt: string
  readAt: string | null
  read: boolean
}

export type NotificationInbox = {
  notifications: UserNotification[]
  unreadCount: number
}
