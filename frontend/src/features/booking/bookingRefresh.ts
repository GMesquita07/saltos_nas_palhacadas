export const bookingsRefreshEvent = 'bookings:refresh'
export const bookingSyncIntervalMs = 5_000

export function requestBookingsRefresh(target: EventTarget = window) {
  target.dispatchEvent(new Event(bookingsRefreshEvent))
}

export const notificationsRefreshEvent = 'notifications:refresh'

export function requestNotificationsRefresh(target: EventTarget = window) {
  target.dispatchEvent(new Event(notificationsRefreshEvent))
}
