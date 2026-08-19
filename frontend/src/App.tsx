import { useEffect, useState } from 'react'
import { Footer } from './components/Footer/Footer'
import { Header } from './components/Header/Header'
import { AdminArea } from './features/admin/AdminArea'
import { getProfiles } from './services/profileService'
import { PortfolioPage } from './features/portfolio/PortfolioPage'
import { ProfileSelector } from './features/profiles/ProfileSelector'
import { SplashScreen } from './features/splash/SplashScreen'
import type { Profile } from './types/profile'
import styles from './App.module.css'

const SPLASH_DURATION_MS = 2200

function App() {
  const [isSplashVisible, setIsSplashVisible] = useState(true)
  const [selectedProfile, setSelectedProfile] = useState<Profile | null>(null)
  const [isAdminOpen, setIsAdminOpen] = useState(false)
  const [profiles, setProfiles] = useState<Profile[]>([])
  const [profilesError, setProfilesError] = useState(false)

  useEffect(() => {
    const timer = window.setTimeout(() => setIsSplashVisible(false), SPLASH_DURATION_MS)
    return () => window.clearTimeout(timer)
  }, [])

  useEffect(() => {
    const loadProfiles = () => getProfiles().then((result) => { setProfiles(result); setProfilesError(false) }).catch(() => setProfilesError(true))
    loadProfiles()
    window.addEventListener('profiles:changed', loadProfiles)
    return () => window.removeEventListener('profiles:changed', loadProfiles)
  }, [])

  const showProfiles = () => { setSelectedProfile(null); setIsAdminOpen(false) }

  return (
    <>
      <SplashScreen isVisible={isSplashVisible} />
      <div className={`${styles.application} ${isSplashVisible ? styles.isWaiting : ''}`}>
        <Header onAdminClick={() => setIsAdminOpen(true)} onBrandClick={showProfiles} onProfilesClick={showProfiles} />
        <main className={styles.main}>
          {isAdminOpen ? <AdminArea onExit={showProfiles} /> : selectedProfile ? (
            <PortfolioPage profile={selectedProfile} onBack={showProfiles} />
          ) : profilesError ? <p className={styles.feedback}>Não foi possível carregar os perfis. Confirma que a API está a correr.</p>
            : <ProfileSelector profiles={profiles} onProfileSelect={setSelectedProfile} />}
        </main>
        <Footer />
      </div>
    </>
  )
}

export default App
