import { useEffect, useId, useRef, useState } from 'react'
import { formatNotificationBadge, formatNotificationDate } from '../../services/notificationService'
import { useNotifications } from '../../features/notifications/NotificationContext'
import { requestBookingsRefresh } from '../../features/booking/bookingRefresh'
import styles from './NotificationBell.module.css'

export function NotificationBell({ onBookings, onViewAll }: { onBookings: () => void; onViewAll: () => void }) {
  const { enabled } = useNotifications()
  return enabled ? <EnabledNotificationBell onBookings={onBookings} onViewAll={onViewAll} /> : null
}

function EnabledNotificationBell({ onBookings, onViewAll }: { onBookings: () => void; onViewAll: () => void }) {
  const { error, isLoading, markAllRead, markRead, notifications, unreadCount } = useNotifications()
  const [isOpen, setIsOpen] = useState(false)
  const rootRef = useRef<HTMLDivElement | null>(null)
  const panelId = useId()

  useEffect(() => {
    if (!isOpen) return
    const closeOnEscape = (event: KeyboardEvent) => {
      if (event.key === 'Escape') setIsOpen(false)
    }
    const closeOutside = (event: PointerEvent) => {
      if (!rootRef.current?.contains(event.target as Node)) setIsOpen(false)
    }
    document.addEventListener('keydown', closeOnEscape)
    document.addEventListener('pointerdown', closeOutside)
    return () => {
      document.removeEventListener('keydown', closeOnEscape)
      document.removeEventListener('pointerdown', closeOutside)
    }
  }, [isOpen])

  const visibleNotifications = notifications.slice(0, 5)

  function openBooking(notificationId: string, read: boolean) {
    if (!read) void markRead(notificationId).catch(() => undefined)
    setIsOpen(false)
    requestBookingsRefresh()
    onBookings()
  }

  return (
    <div className={styles.root} ref={rootRef}>
      <button
        aria-controls={panelId}
        aria-expanded={isOpen}
        aria-label={`${unreadCount} notificações não lidas`}
        className={`${styles.trigger} ${unreadCount > 0 ? styles.triggerAlert : ''}`}
        type="button"
        onClick={() => setIsOpen((current) => !current)}
      >
        <svg aria-hidden="true" viewBox="0 0 24 24">
          <path d="M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9M10 21h4" />
        </svg>
        <span>{formatNotificationBadge(unreadCount)}</span>
      </button>

      {isOpen && (
        <section aria-label="Notificações" className={styles.panel} id={panelId}>
          <header><strong>Notificações</strong><span>{formatNotificationBadge(unreadCount)}</span></header>
          {isLoading && notifications.length === 0 ? <p className={styles.state}>A carregar...</p>
            : error && notifications.length === 0 ? <p className={styles.error}>{error}</p>
              : visibleNotifications.length === 0 ? <p className={styles.state}>Não tens notificações.</p>
                : (
                  <ul>
                    {visibleNotifications.map((notification) => (
                      <li className={notification.read ? styles.read : styles.unread} key={notification.id}>
                        <button
                          type="button"
                          onClick={() => notification.bookingId
                            ? openBooking(notification.id, notification.read)
                            : void markRead(notification.id).catch(() => undefined)}
                        >
                          <span className={styles.titleRow}>
                            {!notification.read && <i aria-hidden="true" />}
                            <strong>{notification.title}</strong>
                          </span>
                          <span>{notification.message}</span>
                          {(notification.type === 'BOOKING_CREATED'
                            || notification.type === 'BOOKING_COUNTER_PROPOSED'
                            || notification.type === 'BOOKING_CUSTOMER_COUNTER_PROPOSED')
                            && !notification.read && <em>Requer resposta</em>}
                          <time dateTime={notification.createdAt}>{formatNotificationDate(notification.createdAt)}</time>
                        </button>
                      </li>
                    ))}
                  </ul>
                )}
          <footer>
            <button disabled={unreadCount === 0} type="button" onClick={() => { void markAllRead().catch(() => undefined) }}>Marcar todas como lidas</button>
            <button type="button" onClick={() => { setIsOpen(false); onViewAll() }}>Ver todas as notificações</button>
          </footer>
        </section>
      )}
    </div>
  )
}
