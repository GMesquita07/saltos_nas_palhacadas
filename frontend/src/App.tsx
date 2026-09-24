import { lazy, Suspense, useCallback, useEffect, useState, type ReactNode } from 'react'
import { Navigate, Route, Routes, useLocation, useNavigate, useParams, useSearchParams } from 'react-router-dom'
import { Footer } from './components/Footer/Footer'
import { Header, type AuthenticationMode } from './components/Header/Header'
import { CookieConsent } from './components/CookieConsent/CookieConsent'
import { useAuth } from './features/auth/AuthContext'
import type { AuthMode } from './features/auth/authTypes'
import { ProfileSelector } from './features/profiles/ProfileSelector'
import { SplashScreen } from './features/splash/SplashScreen'
import {
  adminPath,
  authPath,
  bookingPath,
  legacyResetRedirect,
  loginPath,
  normalizeReturnTo,
  profilePath,
  type AdminPage,
} from './navigation/routes'
import { routeNeedsProfiles } from './performance/performanceConfig'
import { SeoManager } from './seo/SeoManager'
import { getProfiles, invalidateProfilesCache } from './services/profileService'
import type { AuthSession } from './types/auth'
import type { Profile } from './types/profile'
import styles from './App.module.css'

type NavigationView = 'profiles' | 'contacts' | 'materials' | 'admin' | 'auth' | 'favorites' | 'account' | 'booking' | 'privacy' | 'terms' | 'cookies' | 'faq'
type SplashPhase = 'playing' | 'docking' | 'done'
type ColorTheme = 'dark' | 'light'

const themeStorageKey = 'saltos.theme'

const AccountPage = lazy(() => import('./features/auth/AccountPage').then((module) => ({ default: module.AccountPage })))
const AdminArea = lazy(() => import('./features/admin/AdminArea').then((module) => ({ default: module.AdminArea })))
const AuthPage = lazy(() => import('./features/auth/AuthPage').then((module) => ({ default: module.AuthPage })))
const BookingPage = lazy(() => import('./features/booking/BookingPage').then((module) => ({ default: module.BookingPage })))
const ContactPage = lazy(() => import('./features/contacts/ContactPage').then((module) => ({ default: module.ContactPage })))
const FAQPage = lazy(() => import('./features/faq/FAQPage').then((module) => ({ default: module.FAQPage })))
const FavoritesPage = lazy(() => import('./features/favorites/FavoritesPage').then((module) => ({ default: module.FavoritesPage })))
const LegalPage = lazy(() => import('./features/legal/LegalPage').then((module) => ({ default: module.LegalPage })))
const MaterialsPage = lazy(() => import('./features/materials/MaterialsPage').then((module) => ({ default: module.MaterialsPage })))
const PortfolioPage = lazy(() => import('./features/portfolio/PortfolioPage').then((module) => ({ default: module.PortfolioPage })))
const SupportChat = lazy(() => import('./components/SupportChat/SupportChat').then((module) => ({ default: module.SupportChat })))

function storedTheme(): ColorTheme | null {
  try {
    const value = localStorage.getItem(themeStorageKey)
    return value === 'dark' || value === 'light' ? value : null
  } catch {
    return null
  }
}

function ScrollToTop() {
  const { hash, pathname } = useLocation()

  useEffect(() => {
    const frame = window.requestAnimationFrame(() => {
      if (hash) {
        const rawId = hash.slice(1)
        const decodedId = rawId ? safelyDecodeHash(rawId) : ''
        const target = decodedId ? document.getElementById(decodedId) : null

        if (target) {
          target.scrollIntoView({ block: 'start', behavior: 'auto' })
          return
        }
      }

      window.scrollTo({ top: 0, left: 0, behavior: 'auto' })
    })

    return () => window.cancelAnimationFrame(frame)
  }, [hash, pathname])

  return null
}

function safelyDecodeHash(value: string) {
  try {
    return decodeURIComponent(value)
  } catch {
    return value
  }
}

function App() {
  const { isSessionReady, logout, session } = useAuth()
  const location = useLocation()
  const navigate = useNavigate()
  const [splashPhase, setSplashPhase] = useState<SplashPhase>(() => prefersReducedMotion() ? 'done' : 'playing')
  const [profiles, setProfiles] = useState<Profile[]>([])
  const [profilesError, setProfilesError] = useState(false)
  const [hasLoadedProfiles, setHasLoadedProfiles] = useState(false)
  const [isProfilesLoading, setIsProfilesLoading] = useState(() => routeNeedsProfiles(location.pathname))
  const [theme, setTheme] = useState<ColorTheme>(() => storedTheme() ?? 'dark')
  const profilesNeeded = routeNeedsProfiles(location.pathname)
  const areProfilesPending = profilesNeeded && !hasLoadedProfiles
  const effectiveProfilesLoading = isProfilesLoading || areProfilesPending

  useEffect(() => {
    document.documentElement.dataset.theme = theme
    document.documentElement.style.colorScheme = theme
  }, [theme])

  const toggleTheme = useCallback(() => {
    setTheme((current) => {
      const next = current === 'dark' ? 'light' : 'dark'
      try {
        localStorage.setItem(themeStorageKey, next)
      } catch {
        // Mantém a preferência apenas nesta sessão.
      }
      return next
    })
  }, [])

  const loadProfiles = useCallback(async (force = false) => {
    try {
      const result = await getProfiles({ force })
      setProfiles(result)
      setProfilesError(false)
    } catch {
      setProfilesError(true)
    } finally {
      setHasLoadedProfiles(true)
      setIsProfilesLoading(false)
    }
  }, [])

  useEffect(() => {
    if (!profilesNeeded) return

    queueMicrotask(() => {
      setIsProfilesLoading(true)
      void loadProfiles()
    })
  }, [loadProfiles, profilesNeeded])

  useEffect(() => {
    const handleProfilesChanged = () => {
      invalidateProfilesCache()
      setHasLoadedProfiles(false)
      if (!routeNeedsProfiles(window.location.pathname)) return
      setIsProfilesLoading(true)
      void loadProfiles(true)
    }

    window.addEventListener('profiles:changed', handleProfilesChanged)
    return () => {
      window.removeEventListener('profiles:changed', handleProfilesChanged)
    }
  }, [loadProfiles])

  useEffect(() => {
    const target = legacyResetRedirect(location.pathname, location.search)
    if (target) navigate(target, { replace: true })
  }, [location.pathname, location.search, navigate])

  const activeView = activeViewFromPath(location.pathname)

  const goHome = useCallback(() => {
    navigate('/')
  }, [navigate])

  const handleAuthenticationClick = useCallback((mode: AuthenticationMode) => {
    navigate(authPath(mode))
  }, [navigate])

  const handleAuthModeChange = useCallback((mode: AuthMode, notice?: string) => {
    navigate(authPath(mode), notice
      ? { replace: mode === 'login', state: { authNotice: notice } }
      : { replace: mode === 'login' })
  }, [navigate])

  const handleAuthenticated = useCallback((nextSession: AuthSession) => {
    const returnTo = normalizeReturnTo(new URLSearchParams(location.search).get('returnTo'))
    navigate(returnTo ?? (nextSession.role === 'ADMIN' ? adminPath('dashboard') : '/'), { replace: true })
  }, [location.search, navigate])

  const handleLogout = useCallback(() => {
    logout()
    navigate('/')
  }, [logout, navigate])

  const requireLogin = useCallback(() => {
    navigate(loginPath(location.pathname + location.search))
  }, [location.pathname, location.search, navigate])

  const renderAdminRoute = useCallback((page: AdminPage) => (
    <RequireAdmin isSessionReady={isSessionReady} session={session}>
      {(adminSession) => (
        <AdminArea
          page={page}
          token={adminSession.token}
          onExit={goHome}
          onPageChange={(nextPage) => navigate(adminPath(nextPage))}
        />
      )}
    </RequireAdmin>
  ), [goHome, isSessionReady, navigate, session])

  return (
    <>
      <SeoManager hasProfilesError={profilesError} isProfilesLoading={effectiveProfilesLoading} profiles={profiles} />
      <ScrollToTop />
      <SplashScreen
        phase={splashPhase}
        onDockingEnd={() => setSplashPhase('done')}
        onDockingStart={() => setSplashPhase('docking')}
      />
      <div className={`${styles.application} ${splashPhase === 'playing' ? styles.isWaiting : ''}`}>
        <Header
          theme={theme}
          activeView={activeView}
          isBrandHidden={splashPhase === 'docking'}
          session={isSessionReady ? session : null}
          onAccountClick={() => navigate('/conta')}
          onAdminClick={() => { if (session?.role === 'ADMIN') navigate(adminPath('dashboard')) }}
          onAuthenticationClick={handleAuthenticationClick}
          onBookingClick={() => navigate(bookingPath())}
          onContactsClick={() => navigate('/contactos')}
          onFavoritesClick={() => navigate('/favoritos')}
          onHomeClick={goHome}
          onLogout={handleLogout}
          onMaterialsClick={() => navigate('/materiais')}
          onProfilesClick={goHome}
          onThemeToggle={toggleTheme}
        />
        <main className={styles.main}>
          <Suspense fallback={<RouteFallback />}>
          <Routes>
            <Route
              path="/"
              element={profilesError
                ? <p className={styles.feedback}>Não foi possível carregar os perfis. Confirma que a API está a correr.</p>
                : effectiveProfilesLoading && profiles.length === 0
                  ? <p className={styles.feedback}>A carregar perfis...</p>
                : <ProfileSelector profiles={profiles} viewerName={session && isSessionReady ? displaySessionName(session) : undefined} />}
            />
            <Route path="/perfis" element={<Navigate to="/" replace />} />
            <Route
              path="/perfis/:slug"
              element={(
                <ProfileRoute
                  hasError={profilesError}
                  isLoading={effectiveProfilesLoading}
                  profiles={profiles}
                  onBack={goHome}
                  onBooking={(profile) => navigate(bookingPath(profile.slug))}
                  onLogin={requireLogin}
                />
              )}
            />
            <Route
              path="/agendar"
              element={<BookingRoute hasError={profilesError} isLoading={effectiveProfilesLoading} profiles={profiles} onBack={goHome} onRequireLogin={requireLogin} />}
            />
            <Route
              path="/agendar/:slug"
              element={<BookingRoute hasError={profilesError} isLoading={effectiveProfilesLoading} profiles={profiles} onBack={goHome} onRequireLogin={requireLogin} />}
            />
            <Route path="/contactos" element={<ContactPage />} />
            <Route path="/materiais" element={<MaterialsPage />} />
            <Route path="/faq" element={<FAQPage onBack={goHome} />} />
            <Route path="/privacidade" element={<LegalPage type="privacy" onBack={goHome} />} />
            <Route path="/termos" element={<LegalPage type="terms" onBack={goHome} />} />
            <Route path="/cookies" element={<LegalPage type="cookies" onBack={goHome} />} />
            <Route path="/login" element={<AuthRoute initialMode="login" onAuthenticated={handleAuthenticated} onBack={goHome} onModeChange={handleAuthModeChange} />} />
            <Route path="/registo" element={<AuthRoute initialMode="register" onAuthenticated={handleAuthenticated} onBack={goHome} onModeChange={handleAuthModeChange} />} />
            <Route path="/recuperar-password" element={<AuthRoute initialMode="forgot" onAuthenticated={handleAuthenticated} onBack={goHome} onModeChange={handleAuthModeChange} />} />
            <Route path="/reset-password" element={<AuthRoute initialMode="reset" onAuthenticated={handleAuthenticated} onBack={goHome} onModeChange={handleAuthModeChange} />} />
            <Route
              path="/conta"
              element={(
                <RequireSession isSessionReady={isSessionReady} session={session}>
                  {() => <AccountPage onBookingsClick={() => navigate(bookingPath())} onExit={goHome} onFavoritesClick={() => navigate('/favoritos')} />}
                </RequireSession>
              )}
            />
            <Route
              path="/favoritos"
              element={(
                <RequireSession isSessionReady={isSessionReady} session={session}>
                  {() => <FavoritesPage onBack={goHome} />}
                </RequireSession>
              )}
            />
            <Route path="/admin" element={renderAdminRoute('dashboard')} />
            <Route path="/admin/perfis" element={renderAdminRoute('profile')} />
            <Route path="/admin/publicacoes" element={renderAdminRoute('content')} />
            <Route path="/admin/reservas" element={renderAdminRoute('bookings')} />
            <Route path="/admin/avaliacoes" element={renderAdminRoute('reviews')} />
            <Route path="/admin/contactos" element={renderAdminRoute('contacts')} />
            <Route path="/admin/materiais" element={renderAdminRoute('materials')} />
            <Route path="*" element={<NotFoundPage onHome={goHome} />} />
          </Routes>
          </Suspense>
        </main>
        <Footer
          onFAQClick={() => navigate('/faq')}
          onCookiesClick={() => navigate('/cookies')}
          onPrivacyClick={() => navigate('/privacidade')}
          onTermsClick={() => navigate('/termos')}
        />
        <CookieConsent onManage={() => navigate('/cookies')} />
        {splashPhase === 'done' && (
          <Suspense fallback={null}>
            <SupportChat />
          </Suspense>
        )}
      </div>
    </>
  )
}

export default App

function ProfileRoute({
  hasError,
  isLoading,
  onBack,
  onBooking,
  onLogin,
  profiles,
}: {
  hasError: boolean
  isLoading: boolean
  onBack: () => void
  onBooking: (profile: Profile) => void
  onLogin: () => void
  profiles: Profile[]
}) {
  const { slug = '' } = useParams()
  const profile = profiles.find((item) => item.slug === slug) ?? null

  if (hasError) return <p className={styles.feedback}>Não foi possível carregar este perfil.</p>
  if (!profile && isLoading) return <p className={styles.feedback}>A carregar perfil...</p>
  if (!profile) return <NotFoundPage onHome={onBack} title="Perfil não encontrado" />

  return <PortfolioPage profile={profile} onBack={onBack} onBooking={() => onBooking(profile)} onLogin={onLogin} />
}

function BookingRoute({
  hasError,
  isLoading,
  onBack,
  onRequireLogin,
  profiles,
}: {
  hasError: boolean
  isLoading: boolean
  onBack: () => void
  onRequireLogin: () => void
  profiles: Profile[]
}) {
  const { slug } = useParams()
  const navigate = useNavigate()
  const initialProfile = slug ? profiles.find((profile) => profile.slug === slug) ?? null : null
  const handleBack = slug ? () => navigate(profilePath(slug)) : onBack

  if (hasError) return <p className={styles.feedback}>Não foi possível carregar os perfis para agendamento.</p>
  if (slug && !initialProfile && isLoading) return <p className={styles.feedback}>A carregar agendamento...</p>
  if (slug && !initialProfile) return <NotFoundPage onHome={onBack} title="Perfil para agendamento não encontrado" />

  return <BookingPage key={slug ?? 'all'} initialProfile={initialProfile} onBack={handleBack} onRequireLogin={onRequireLogin} profiles={profiles} />
}

function AuthRoute({
  initialMode,
  onAuthenticated,
  onBack,
  onModeChange,
}: {
  initialMode: AuthMode
  onAuthenticated: (session: AuthSession) => void
  onBack: () => void
  onModeChange: (mode: AuthMode, notice?: string) => void
}) {
  const location = useLocation()
  const [searchParams] = useSearchParams()
  const resetToken = initialMode === 'reset' ? searchParams.get('resetToken') : null

  return (
    <AuthPage
      key={`${initialMode}-${resetToken ?? ''}`}
      initialMode={initialMode}
      initialNotice={authNoticeFromState(location.state)}
      resetToken={resetToken}
      onAuthenticated={onAuthenticated}
      onBack={onBack}
      onModeChange={onModeChange}
    />
  )
}

function RequireSession({
  children,
  isSessionReady,
  session,
}: {
  children: (session: AuthSession) => ReactNode
  isSessionReady: boolean
  session: AuthSession | null
}) {
  const location = useLocation()

  if (!isSessionReady) return <p className={styles.feedback}>A validar sessão...</p>
  if (!session) return <Navigate to={loginPath(location.pathname + location.search)} replace />

  return children(session)
}

function authNoticeFromState(state: unknown) {
  if (!state || typeof state !== 'object' || !('authNotice' in state)) return null

  const notice = (state as { authNotice?: unknown }).authNotice
  return typeof notice === 'string' ? notice : null
}

function RequireAdmin({
  children,
  isSessionReady,
  session,
}: {
  children: (session: AuthSession) => ReactNode
  isSessionReady: boolean
  session: AuthSession | null
}) {
  const location = useLocation()

  if (!isSessionReady) return <p className={styles.feedback}>A validar sessão...</p>
  if (!session) return <Navigate to={loginPath(location.pathname + location.search)} replace />
  if (session.role !== 'ADMIN') return <Navigate to="/" replace />

  return children(session)
}

function NotFoundPage({ onHome, title = 'Página não encontrada' }: { onHome: () => void; title?: string }) {
  return (
    <section>
      <h1>{title}</h1>
      <p className={styles.feedback}>O endereço que procuras não existe ou deixou de estar disponível.</p>
      <button type="button" onClick={onHome}>Voltar ao início</button>
    </section>
  )
}

function RouteFallback() {
  return <p className={styles.feedback}>A carregar...</p>
}

function displaySessionName(session: AuthSession) {
  const fullName = [session.firstName, session.lastName].filter(Boolean).join(' ').trim()
  return fullName || session.username || session.email.split('@')[0] || 'user'
}

function prefersReducedMotion() {
  return typeof window !== 'undefined' && window.matchMedia('(prefers-reduced-motion: reduce)').matches
}

function activeViewFromPath(pathname: string): NavigationView {
  if (pathname.startsWith('/admin')) return 'admin'
  if (pathname.startsWith('/agendar')) return 'booking'
  if (pathname === '/contactos') return 'contacts'
  if (pathname === '/materiais') return 'materials'
  if (pathname === '/faq') return 'faq'
  if (pathname === '/privacidade') return 'privacy'
  if (pathname === '/termos') return 'terms'
  if (pathname === '/cookies') return 'cookies'
  if (pathname === '/favoritos') return 'favorites'
  if (pathname === '/conta') return 'account'
  if (['/login', '/registo', '/recuperar-password', '/reset-password'].includes(pathname)) return 'auth'
  return 'profiles'
}
