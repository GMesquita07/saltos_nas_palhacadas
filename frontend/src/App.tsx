import { useEffect, useState } from 'react'
import { Footer } from './components/Footer/Footer'
import { Header, type AuthenticationMode } from './components/Header/Header'
import { AccountPage } from './features/auth/AccountPage'
import { AuthPage } from './features/auth/AuthPage'
import { useAuth } from './features/auth/AuthContext'
import { AdminArea } from './features/admin/AdminArea'
import { ContactPage } from './features/contacts/ContactPage'
import { FavoritesPage } from './features/favorites/FavoritesPage'
import { PortfolioPage } from './features/portfolio/PortfolioPage'
import { ProfileSelector } from './features/profiles/ProfileSelector'
import { SplashScreen } from './features/splash/SplashScreen'
import { getProfiles } from './services/profileService'
import type { AuthSession } from './types/auth'
import type { Profile } from './types/profile'
import styles from './App.module.css'

const SPLASH_DURATION_MS = 2200

type View = 'profiles' | 'contacts' | 'admin' | 'auth' | 'favorites' | 'account'

function App() {
  const { isSessionReady, logout, session } = useAuth()
  const [isSplashVisible, setIsSplashVisible] = useState(true)
  const [selectedProfile, setSelectedProfile] = useState<Profile | null>(null)
  const [view, setView] = useState<View>('profiles')
  const [authMode, setAuthMode] = useState<AuthenticationMode>('login')
  const [profiles, setProfiles] = useState<Profile[]>([])
  const [profilesError, setProfilesError] = useState(false)

  useEffect(() => {
    const timer = window.setTimeout(() => setIsSplashVisible(false), SPLASH_DURATION_MS)
    return () => window.clearTimeout(timer)
  }, [])

  useEffect(() => {
    const loadProfiles = () => getProfiles()
      .then((result) => {
        setProfiles(result)
        setProfilesError(false)
      })
      .catch(() => setProfilesError(true))

    void loadProfiles()
    window.addEventListener('profiles:changed', loadProfiles)
    return () => window.removeEventListener('profiles:changed', loadProfiles)
  }, [])

  function showProfiles() {
    setSelectedProfile(null)
    setView('profiles')
  }

  function showContacts() {
    setSelectedProfile(null)
    setView('contacts')
  }

  function openAuthentication(mode: AuthenticationMode) {
    setSelectedProfile(null)
    setAuthMode(mode)
    setView('auth')
  }

  function openFavorites() {
    setSelectedProfile(null)
    setView('favorites')
  }

  function openAccount() {
    setSelectedProfile(null)
    setView('account')
  }

  function handleAuthenticated(nextSession: AuthSession) {
    setSelectedProfile(null)
    setView(nextSession.role === 'ADMIN' ? 'admin' : 'profiles')
  }

  function handleLogout() {
    logout()
    showProfiles()
  }

  function openAdmin() {
    if (session?.role !== 'ADMIN') return
    setSelectedProfile(null)
    setView('admin')
  }

  function renderView() {
    if (view === 'auth') {
      return <AuthPage key={authMode} initialMode={authMode} onAuthenticated={handleAuthenticated} onBack={showProfiles} />
    }

    if (view === 'admin' && session?.role === 'ADMIN') {
      return <AdminArea token={session.token} onExit={showProfiles} />
    }

    if (view === 'contacts') return <ContactPage />
    if (view === 'favorites' && session) return <FavoritesPage onBack={showProfiles} />
    if (view === 'account' && session) return <AccountPage onExit={showProfiles} onFavoritesClick={openFavorites} />
    if (selectedProfile) return <PortfolioPage profile={selectedProfile} onBack={showProfiles} />
    if (profilesError) return <p className={styles.feedback}>Não foi possível carregar os perfis. Confirma que a API está a correr.</p>

    return <ProfileSelector profiles={profiles} onProfileSelect={setSelectedProfile} />
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
          onContactsClick={showContacts}
          onFavoritesClick={openFavorites}
          onLogout={handleLogout}
          onProfilesClick={showProfiles}
        />
        <main className={styles.main}>
          {!isSessionReady ? <p className={styles.feedback}>A preparar a tua sessão...</p> : renderView()}
        </main>
        <Footer />
      </div>
    </>
  )
}

export default App
