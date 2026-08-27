import { useCallback, useEffect, useState } from 'react'
import { Footer } from './components/Footer/Footer'
import { Header, type AuthenticationMode } from './components/Header/Header'
import { SupportChat } from './components/SupportChat/SupportChat'
import { AccountPage } from './features/auth/AccountPage'
import { AuthPage } from './features/auth/AuthPage'
import { useAuth } from './features/auth/AuthContext'
import { AdminArea } from './features/admin/AdminArea'
import { BookingPage } from './features/booking/BookingPage'
import { ClientContentPage } from './features/clientContent/ClientContentPage'
import { ContactPage } from './features/contacts/ContactPage'
import { FavoritesPage } from './features/favorites/FavoritesPage'
import { LegalPage } from './features/legal/LegalPage'
import { MaterialsPage } from './features/materials/MaterialsPage'
import { PortfolioPage } from './features/portfolio/PortfolioPage'
import { ProfileSelector } from './features/profiles/ProfileSelector'
import { SplashScreen } from './features/splash/SplashScreen'
import { getProfiles } from './services/profileService'
import type { AuthSession } from './types/auth'
import type { Profile } from './types/profile'
import styles from './App.module.css'

const SPLASH_DURATION_MS = 2200

type View = 'profiles' | 'contacts' | 'materials' | 'clientContent' | 'admin' | 'auth' | 'favorites' | 'account' | 'booking' | 'privacy' | 'terms' | 'cookies'

function App() {
  const { isSessionReady, logout, session } = useAuth()
  const [isSplashVisible, setIsSplashVisible] = useState(true)
  const [selectedProfile, setSelectedProfile] = useState<Profile | null>(null)
  const [bookingProfile, setBookingProfile] = useState<Profile | null>(null)
  const [shouldReturnToBooking, setShouldReturnToBooking] = useState(false)
  const [view, setView] = useState<View>('profiles')
  const [authMode, setAuthMode] = useState<AuthenticationMode>('login')
  const [profiles, setProfiles] = useState<Profile[]>([])
  const [profilesError, setProfilesError] = useState(false)

  const loadProfiles = useCallback(async () => {
    try {
      const result = await getProfiles()
      setProfiles(result)
      setSelectedProfile((current) => refreshProfileReference(current, result))
      setBookingProfile((current) => refreshProfileReference(current, result))
      setProfilesError(false)
    } catch {
      setProfilesError(true)
    }
  }, [])

  useEffect(() => {
    const timer = window.setTimeout(() => setIsSplashVisible(false), SPLASH_DURATION_MS)
    return () => window.clearTimeout(timer)
  }, [])

  useEffect(() => {
    let isCurrent = true

    void getProfiles()
      .then((result) => {
        if (!isCurrent) return
        setProfiles(result)
        setSelectedProfile((current) => refreshProfileReference(current, result))
        setBookingProfile((current) => refreshProfileReference(current, result))
        setProfilesError(false)
      })
      .catch(() => {
        if (isCurrent) setProfilesError(true)
      })

    window.addEventListener('profiles:changed', loadProfiles)
    return () => {
      isCurrent = false
      window.removeEventListener('profiles:changed', loadProfiles)
    }
  }, [loadProfiles])

  function showProfiles() {
    setSelectedProfile(null)
    setShouldReturnToBooking(false)
    setView('profiles')
    void loadProfiles()
  }

  function showContacts() {
    setSelectedProfile(null)
    setShouldReturnToBooking(false)
    setView('contacts')
  }

  function showMaterials() {
    setSelectedProfile(null)
    setShouldReturnToBooking(false)
    setView('materials')
  }

  function showClientContent() {
    setSelectedProfile(null)
    setShouldReturnToBooking(false)
    setView('clientContent')
  }

  function openBooking(profile: Profile | null = null) {
    setSelectedProfile(null)
    setBookingProfile(profile)
    setShouldReturnToBooking(false)
    setView('booking')
  }

  function openAuthentication(mode: AuthenticationMode) {
    setSelectedProfile(null)
    setShouldReturnToBooking(false)
    setAuthMode(mode)
    setView('auth')
  }

  function requireBookingAuthentication() {
    setAuthMode('login')
    setShouldReturnToBooking(true)
    setView('auth')
  }

  function openFavorites() {
    setSelectedProfile(null)
    setShouldReturnToBooking(false)
    setView('favorites')
  }

  function openAccount() {
    setSelectedProfile(null)
    setShouldReturnToBooking(false)
    setView('account')
  }

  function handleAuthenticated(nextSession: AuthSession) {
    setSelectedProfile(null)
    setView(shouldReturnToBooking ? 'booking' : nextSession.role === 'ADMIN' ? 'admin' : 'profiles')
    setShouldReturnToBooking(false)
  }

  function handleLogout() {
    logout()
    showProfiles()
  }

  function openAdmin() {
    if (session?.role !== 'ADMIN') return
    setSelectedProfile(null)
    setShouldReturnToBooking(false)
    setView('admin')
  }

  function openLegal(view: Extract<View, 'privacy' | 'terms' | 'cookies'>) {
    setSelectedProfile(null)
    setShouldReturnToBooking(false)
    setView(view)
  }

  function renderView() {
    if (view === 'auth') {
      return <AuthPage key={authMode} initialMode={authMode} onAuthenticated={handleAuthenticated} onBack={() => shouldReturnToBooking ? openBooking(bookingProfile) : showProfiles()} />
    }

    if (view === 'admin' && session?.role === 'ADMIN') {
      return <AdminArea token={session.token} onExit={showProfiles} />
    }

    if (view === 'contacts') return <ContactPage />
    if (view === 'materials') return <MaterialsPage />
    if (view === 'clientContent') return <ClientContentPage profiles={profiles} onLogin={() => openAuthentication('login')} />
    if (view === 'privacy') return <LegalPage type="privacy" onBack={showProfiles} />
    if (view === 'terms') return <LegalPage type="terms" onBack={showProfiles} />
    if (view === 'cookies') return <LegalPage type="cookies" onBack={showProfiles} />
    if (view === 'booking') return <BookingPage initialProfile={bookingProfile} onBack={showProfiles} onRequireLogin={requireBookingAuthentication} profiles={profiles} />
    if (view === 'favorites' && session) return <FavoritesPage onBack={showProfiles} />
    if (view === 'account' && session) return <AccountPage onExit={showProfiles} onFavoritesClick={openFavorites} />
    if (selectedProfile) return <PortfolioPage profile={selectedProfile} onBack={showProfiles} onBooking={() => openBooking(selectedProfile)} onLogin={() => openAuthentication('login')} />
    if (profilesError) return <p className={styles.feedback}>Não foi possível carregar os perfis. Confirma que a API está a correr.</p>

    return <ProfileSelector profiles={profiles} viewerName={session ? displaySessionName(session) : undefined} onProfileSelect={setSelectedProfile} />
  }

  return (
    <>
      <SplashScreen isVisible={isSplashVisible} />
      <div className={`${styles.application} ${isSplashVisible ? styles.isWaiting : ''}`}>
        <Header
          session={isSessionReady ? session : null}
          onAccountClick={openAccount}
          onAdminClick={openAdmin}
          onAuthenticationClick={openAuthentication}
          onBookingClick={() => openBooking()}
          onClientContentClick={showClientContent}
          onContactsClick={showContacts}
          onFavoritesClick={openFavorites}
          onMaterialsClick={showMaterials}
          onLogout={handleLogout}
          onProfilesClick={showProfiles}
        />
        <main className={styles.main}>
          {!isSessionReady ? <p className={styles.feedback}>A preparar a tua sessão...</p> : renderView()}
        </main>
        <Footer
          onCookiesClick={() => openLegal('cookies')}
          onPrivacyClick={() => openLegal('privacy')}
          onTermsClick={() => openLegal('terms')}
        />
        {!isSplashVisible && <SupportChat />}
      </div>
    </>
  )
}

export default App

function refreshProfileReference(current: Profile | null, profiles: Profile[]) {
  if (!current) return null
  return profiles.find((profile) => profile.slug === current.slug) ?? current
}

function displaySessionName(session: AuthSession) {
  const fullName = [session.firstName, session.lastName].filter(Boolean).join(' ').trim()
  return fullName || session.username || session.email.split('@')[0] || 'user'
}
