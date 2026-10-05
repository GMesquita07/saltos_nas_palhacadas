import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState, type PropsWithChildren } from 'react'
import { useAuth } from '../auth/AuthContext'
import {
  getNotifications,
  markAllNotificationsRead,
  markInboxNotificationRead,
  markInboxRead,
  markNotificationRead,
} from '../../services/notificationService'
import type { NotificationInbox } from '../../types/notification'
import { notificationsRefreshEvent } from '../booking/bookingRefresh'
import { isCurrentNotificationRequest, isNotificationStateVisible } from './notificationSession'

const emptyInbox: NotificationInbox = { notifications: [], unreadCount: 0 }

type NotificationContextValue = NotificationInbox & {
  enabled: boolean
  isLoading: boolean
  error: string | null
  refresh: () => Promise<void>
  markRead: (id: string) => Promise<void>
  markAllRead: () => Promise<void>
}

const NotificationContext = createContext<NotificationContextValue | null>(null)

type NotificationState = {
  token: string | null
  inbox: NotificationInbox
  isLoading: boolean
  error: string | null
}

function emptyState(token: string | null = null): NotificationState {
  return { token, inbox: emptyInbox, isLoading: false, error: null }
}

export function NotificationProvider({ children }: PropsWithChildren) {
  const { isSessionReady, session } = useAuth()
  const enabled = Boolean(isSessionReady && session)
  const token = enabled ? session?.token ?? null : null
  const [state, setState] = useState<NotificationState>(() => emptyState())
  const activeTokenRef = useRef<string | null>(null)
  const sessionGenerationRef = useRef(0)
  const visibleState = isNotificationStateVisible(state.token, token) ? state : emptyState(token)

  useEffect(() => {
    activeTokenRef.current = token
    sessionGenerationRef.current += 1
    const requestId = window.setTimeout(() => setState((current) =>
      isNotificationStateVisible(current.token, token) ? current : emptyState(token)), 0)
    return () => window.clearTimeout(requestId)
  }, [token])

  const refresh = useCallback(async () => {
    if (!token) return
    const requestToken = token
    const requestGeneration = sessionGenerationRef.current
    setState((current) => ({
      ...(current.token === requestToken ? current : emptyState(requestToken)),
      isLoading: true,
    }))
    try {
      const nextInbox = await getNotifications(requestToken)
      setState((current) => isCurrentNotificationRequest(
          activeTokenRef.current,
          sessionGenerationRef.current,
          requestToken,
          requestGeneration,
        )
          && current.token === requestToken
        ? { ...current, inbox: nextInbox, error: null }
        : current)
    } catch (reason) {
      setState((current) => isCurrentNotificationRequest(
          activeTokenRef.current,
          sessionGenerationRef.current,
          requestToken,
          requestGeneration,
        )
          && current.token === requestToken
        ? { ...current, error: reason instanceof Error ? reason.message : 'Não foi possível carregar as notificações.' }
        : current)
    } finally {
      setState((current) => isCurrentNotificationRequest(
          activeTokenRef.current,
          sessionGenerationRef.current,
          requestToken,
          requestGeneration,
        )
          && current.token === requestToken
        ? { ...current, isLoading: false }
        : current)
    }
  }, [token])

  useEffect(() => {
    if (!token) return

    void refresh()
    const interval = window.setInterval(() => {
      if (document.visibilityState === 'visible') void refresh()
    }, 5_000)
    const handleFocus = () => { void refresh() }
    const handleExplicitRefresh = () => { void refresh() }
    const handleVisibility = () => {
      if (document.visibilityState === 'visible') void refresh()
    }
    window.addEventListener('focus', handleFocus)
    window.addEventListener(notificationsRefreshEvent, handleExplicitRefresh)
    document.addEventListener('visibilitychange', handleVisibility)
    return () => {
      window.clearInterval(interval)
      window.removeEventListener('focus', handleFocus)
      window.removeEventListener(notificationsRefreshEvent, handleExplicitRefresh)
      document.removeEventListener('visibilitychange', handleVisibility)
    }
  }, [refresh, token])

  const markRead = useCallback(async (id: string) => {
    if (!token) return
    await markNotificationRead(id, token)
    setState((current) => current.token === token
      ? { ...current, inbox: markInboxNotificationRead(current.inbox, id) }
      : current)
  }, [token])

  const markAllRead = useCallback(async () => {
    if (!token) return
    await markAllNotificationsRead(token)
    setState((current) => current.token === token
      ? { ...current, inbox: markInboxRead(current.inbox) }
      : current)
  }, [token])

  const value = useMemo<NotificationContextValue>(() => ({
    ...visibleState.inbox,
    enabled,
    isLoading: visibleState.isLoading,
    error: visibleState.error,
    refresh,
    markRead,
    markAllRead,
  }), [enabled, markAllRead, markRead, refresh, visibleState])

  return <NotificationContext.Provider value={value}>{children}</NotificationContext.Provider>
}

// eslint-disable-next-line react-refresh/only-export-components
export function useNotifications() {
  const context = useContext(NotificationContext)
  if (!context) throw new Error('useNotifications tem de ser usado dentro de NotificationProvider.')
  return context
}
