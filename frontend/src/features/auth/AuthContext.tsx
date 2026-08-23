import { createContext, useCallback, useContext, useEffect, useMemo, useState, type PropsWithChildren } from 'react'
import { getCurrentUser, login as requestLogin, register as requestRegistration, updateCurrentUser, type RegisterCredentials, type UpdateAccountInput } from '../../services/authService'
import { addFavorite, getFavorites, removeFavorite } from '../../services/favoriteService'
import type { AuthSession, AuthUser } from '../../types/auth'
import type { Favorite } from '../../types/favorite'

const sessionStorageKey = 'saltos.auth-session'

type Credentials = {
  email: string
  password: string
}

type AuthContextValue = {
  session: AuthSession | null
  isSessionReady: boolean
  favorites: Favorite[]
  isFavoritesLoading: boolean
  login: (credentials: Credentials) => Promise<AuthSession>
  register: (credentials: RegisterCredentials) => Promise<AuthSession>
  logout: () => void
  refreshFavorites: () => Promise<void>
  toggleFavorite: (portfolioItemId: string) => Promise<void>
  updateAccount: (input: UpdateAccountInput) => Promise<AuthUser>
}

const AuthContext = createContext<AuthContextValue | null>(null)

export function AuthProvider({ children }: PropsWithChildren) {
  const [session, setSession] = useState<AuthSession | null>(readStoredSession)
  const [isSessionReady, setIsSessionReady] = useState(() => !readStoredSession())
  const [favorites, setFavorites] = useState<Favorite[]>([])
  const [isFavoritesLoading, setIsFavoritesLoading] = useState(false)

  const saveSession = useCallback((nextSession: AuthSession) => {
    sessionStorage.setItem(sessionStorageKey, JSON.stringify(nextSession))
    setSession(nextSession)
  }, [])

  const logout = useCallback(() => {
    sessionStorage.removeItem(sessionStorageKey)
    setSession(null)
    setFavorites([])
    setIsFavoritesLoading(false)
  }, [])

  useEffect(() => {
    const storedSession = readStoredSession()

    if (!storedSession) {
      return
    }

    let isCurrent = true
    void getCurrentUser(storedSession.token)
      .then((user) => {
        if (!isCurrent) return

        setSession((current) => {
          if (!current || current.token !== storedSession.token) return current
          const refreshedSession = { ...current, ...user }
          sessionStorage.setItem(sessionStorageKey, JSON.stringify(refreshedSession))
          return refreshedSession
        })
      })
      .catch(() => {
        if (!isCurrent) return

        setSession((current) => {
          if (!current || current.token !== storedSession.token) return current
          sessionStorage.removeItem(sessionStorageKey)
          return null
        })
      })
      .finally(() => {
        if (isCurrent) setIsSessionReady(true)
      })

    return () => {
      isCurrent = false
    }
  }, [])

  const refreshFavorites = useCallback(async () => {
    if (!session) {
      setFavorites([])
      return
    }

    setIsFavoritesLoading(true)
    try {
      setFavorites(await getFavorites(session.token))
    } finally {
      setIsFavoritesLoading(false)
    }
  }, [session])

  useEffect(() => {
    if (!session) return

    let isCurrent = true
    void getFavorites(session.token)
      .then((loadedFavorites) => {
        if (isCurrent) setFavorites(loadedFavorites)
      })
      .catch(() => {
        if (isCurrent) setFavorites([])
      })

    return () => {
      isCurrent = false
    }
  }, [session])

  const login = useCallback(async (credentials: Credentials) => {
    const nextSession = await requestLogin(credentials)
    saveSession(nextSession)
    return nextSession
  }, [saveSession])

  const register = useCallback(async (credentials: RegisterCredentials) => {
    const nextSession = await requestRegistration(credentials)
    saveSession(nextSession)
    return nextSession
  }, [saveSession])

  const updateAccount = useCallback(async (input: UpdateAccountInput) => {
    if (!session) {
      throw new Error('Inicia sessão para editar a tua conta.')
    }

    const nextUser = await updateCurrentUser(input, session.token)
    const nextSession = { ...session, ...nextUser }
    sessionStorage.setItem(sessionStorageKey, JSON.stringify(nextSession))
    setSession(nextSession)
    return nextUser
  }, [session])

  const toggleFavorite = useCallback(async (portfolioItemId: string) => {
    if (!session) {
      throw new Error('Inicia sessão para guardar publicações nos favoritos.')
    }

    const alreadyFavorite = favorites.some((favorite) => favorite.portfolioItemId === portfolioItemId)
    if (alreadyFavorite) {
      await removeFavorite(portfolioItemId, session.token)
    } else {
      await addFavorite(portfolioItemId, session.token)
    }

    await refreshFavorites()
  }, [favorites, refreshFavorites, session])

  const value = useMemo<AuthContextValue>(() => ({
    session,
    isSessionReady,
    favorites,
    isFavoritesLoading,
    login,
    register,
    logout,
    refreshFavorites,
    toggleFavorite,
    updateAccount,
  }), [favorites, isFavoritesLoading, isSessionReady, login, logout, refreshFavorites, register, session, toggleFavorite, updateAccount])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

// eslint-disable-next-line react-refresh/only-export-components
export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) throw new Error('useAuth tem de ser usado dentro de AuthProvider.')
  return context
}

function readStoredSession(): AuthSession | null {
  try {
    const rawSession = sessionStorage.getItem(sessionStorageKey)
    if (!rawSession) return null

    const session = JSON.parse(rawSession) as Partial<AuthSession>
    if (
      typeof session.token !== 'string'
      || typeof session.email !== 'string'
      || (session.role !== 'ADMIN' && session.role !== 'CUSTOMER')
    ) {
      sessionStorage.removeItem(sessionStorageKey)
      return null
    }

    return session as AuthSession
  } catch {
    sessionStorage.removeItem(sessionStorageKey)
    return null
  }
}
