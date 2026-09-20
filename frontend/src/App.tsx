import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'
import { Navigate, Route, Routes, useLocation, useNavigate, useParams, useSearchParams } from 'react-router-dom'
import { Footer } from './components/Footer/Footer'
import { Header, type AuthenticationMode } from './components/Header/Header'
import { CookieConsent } from './components/CookieConsent/CookieConsent'
import { SupportChat } from './components/SupportChat/SupportChat'
import { AccountPage } from './features/auth/AccountPage'
import { AuthPage } from './features/auth/AuthPage'
import { useAuth } from './features/auth/AuthContext'
import type { AuthMode } from './features/auth/authTypes'
import { AdminArea } from './features/admin/AdminArea'
import { BookingPage } from './features/booking/BookingPage'
import { ContactPage } from './features/contacts/ContactPage'
import { FAQPage } from './features/faq/FAQPage'
import { FavoritesPage } from './features/favorites/FavoritesPage'
import { LegalPage } from './features/legal/LegalPage'
import { MaterialsPage } from './features/materials/MaterialsPage'
import { PortfolioPage } from './features/portfolio/PortfolioPage'
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
import { getProfiles } from './services/profileService'
import type { AuthSession } from './types/auth'
import type { Profile } from './types/profile'
import styles from './App.module.css'

type NavigationView = 'profiles' | 'contacts' | 'materials' | 'admin' | 'auth' | 'favorites' | 'account' | 'booking' | 'privacy' | 'terms' | 'cookies' | 'faq'
type SplashPhase = 'playing' | 'docking' | 'done'

function App() {
  const { isSessionReady, logout, session } = useAuth()
  const location = useLocation()
  const navigate = useNavigate()
  const [splashPhase, setSplashPhase] = useState<SplashPhase>(() => prefersReducedMotion() ? 'done' : 'playing')
  const [profiles, setProfiles] = useState<Profile[]>([])
  const [profilesError, setProfilesError] = useState(false)
  const [isProfilesLoading, setIsProfilesLoading] = useState(true)

  const loadProfiles = useCallback(async (force = false) => {
    try {
      const result = await getProfiles({ force })
      setProfiles(result)
      setProfilesError(false)
    } catch {
      setProfilesError(true)
    } finally {
      setIsProfilesLoading(false)
    }
  }, [])

  useEffect(() => {
    queueMicrotask(() => {
      void loadProfiles()
    })

    const handleProfilesChanged = () => {
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

  const currentProfile = useMemo(() => {
    const slug = profileSlugFromPath(location.pathname)
    return slug ? profiles.find((profile) => profile.slug === slug) ?? null : null
  }, [location.pathname, profiles])
  const activeView = activeViewFromPath(location.pathname)

  useEffect(() => {
    const metadata = pageMetadata(activeView, currentProfile)
    document.title = metadata.title
    setMetaContent('description', metadata.description)
    setMetaContent('twitter:title', metadata.title)
    setMetaContent('twitter:description', metadata.description)
    setMetaProperty('og:title', metadata.title)
    setMetaProperty('og:description', metadata.description)
  }, [activeView, currentProfile])

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
      <SplashScreen
        phase={splashPhase}
        onDockingEnd={() => setSplashPhase('done')}
        onDockingStart={() => setSplashPhase('docking')}
      />
      <div className={`${styles.application} ${splashPhase === 'playing' ? styles.isWaiting : ''}`}>
        <Header
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
        />
        <main className={styles.main}>
          <Routes>
            <Route
              path="/"
              element={profilesError
                ? <p className={styles.feedback}>Não foi possível carregar os perfis. Confirma que a API está a correr.</p>
                : isProfilesLoading && profiles.length === 0
                  ? <p className={styles.feedback}>A carregar perfis...</p>
                : <ProfileSelector profiles={profiles} viewerName={session && isSessionReady ? displaySessionName(session) : undefined} onProfileSelect={(profile) => navigate(profilePath(profile.slug))} />}
            />
            <Route path="/perfis" element={<Navigate to="/" replace />} />
            <Route
              path="/perfis/:slug"
              element={(
                <ProfileRoute
                  hasError={profilesError}
                  isLoading={isProfilesLoading}
                  profiles={profiles}
                  onBack={goHome}
                  onBooking={(profile) => navigate(bookingPath(profile.slug))}
                  onLogin={requireLogin}
                />
              )}
            />
            <Route
              path="/agendar"
              element={<BookingRoute hasError={profilesError} isLoading={isProfilesLoading} profiles={profiles} onBack={goHome} onRequireLogin={requireLogin} />}
            />
            <Route
              path="/agendar/:slug"
              element={<BookingRoute hasError={profilesError} isLoading={isProfilesLoading} profiles={profiles} onBack={goHome} onRequireLogin={requireLogin} />}
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
        </main>
        <Footer
          onFAQClick={() => navigate('/faq')}
          onCookiesClick={() => navigate('/cookies')}
          onPrivacyClick={() => navigate('/privacidade')}
          onTermsClick={() => navigate('/termos')}
        />
        <CookieConsent onManage={() => navigate('/cookies')} />
        {splashPhase === 'done' && <SupportChat />}
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

function profileSlugFromPath(pathname: string) {
  const match = /^\/perfis\/([^/]+)$/.exec(pathname)
  return match ? decodeURIComponent(match[1]) : null
}

function pageMetadata(view: NavigationView, profile: Profile | null) {
  if (profile) {
    return {
      title: `${profile.name} | Saltos nas Palhaçadas`,
      description: `${profile.name}, ${profile.role}. Consulta o portfólio, avaliações e disponibilidade para eventos.`,
    }
  }

  const defaults = {
    title: 'Saltos nas Palhaçadas | Animação de Eventos',
    description: 'Perfis de artistas, portfólios, materiais disponíveis e pedidos de agendamento.',
  }

  const metadata: Partial<Record<NavigationView, { title: string; description: string }>> = {
    account: { title: 'A minha conta | Saltos nas Palhaçadas', description: 'Dados pessoais, foto, segurança, favoritos e agendamentos da conta.' },
    admin: { title: 'Administração | Saltos nas Palhaçadas', description: 'Backoffice para gerir perfis, conteúdos, contactos, materiais, avaliações e agendamentos.' },
    auth: { title: 'Login e conta | Saltos nas Palhaçadas', description: 'Entrar, criar conta ou recuperar palavra-passe.' },
    booking: { title: 'Agendar evento | Saltos nas Palhaçadas', description: 'Consulta a disponibilidade dos artistas e envia um pedido de agendamento.' },
    contacts: { title: 'Contactos | Saltos nas Palhaçadas', description: 'Contactos para pedidos, reservas e apoio.' },
    cookies: { title: 'Cookies | Saltos nas Palhaçadas', description: 'Informação sobre cookies e tecnologias semelhantes.' },
    favorites: { title: 'Favoritos | Saltos nas Palhaçadas', description: 'Conteúdos guardados como favoritos.' },
    faq: { title: 'FAQ | Saltos nas Palhaçadas', description: 'Perguntas frequentes sobre agendamentos, contas e disponibilidade.' },
    materials: { title: 'Material disponível | Saltos nas Palhaçadas', description: 'Lista de material disponível para eventos.' },
    privacy: { title: 'Privacidade | Saltos nas Palhaçadas', description: 'Informação sobre privacidade e proteção de dados.' },
    terms: { title: 'Termos | Saltos nas Palhaçadas', description: 'Termos de utilização do site Saltos nas Palhaçadas.' },
  }

  return metadata[view] ?? defaults
}

function setMetaContent(name: string, content: string) {
  document.querySelector<HTMLMetaElement>(`meta[name="${name}"]`)?.setAttribute('content', content)
}

function setMetaProperty(property: string, content: string) {
  document.querySelector<HTMLMetaElement>(`meta[property="${property}"]`)?.setAttribute('content', content)
}
