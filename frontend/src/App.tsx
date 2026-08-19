import { useEffect, useState } from 'react'
import { Footer } from './components/Footer/Footer'
import { Header } from './components/Header/Header'
import { profiles } from './data/profiles'
import { PortfolioPage } from './features/portfolio/PortfolioPage'
import { ProfileSelector } from './features/profiles/ProfileSelector'
import { SplashScreen } from './features/splash/SplashScreen'
import type { Profile } from './types/profile'
import styles from './App.module.css'

const SPLASH_DURATION_MS = 2200

function App() {
  const [isSplashVisible, setIsSplashVisible] = useState(true)
  const [selectedProfile, setSelectedProfile] = useState<Profile | null>(null)

  useEffect(() => {
    const timer = window.setTimeout(() => setIsSplashVisible(false), SPLASH_DURATION_MS)
    return () => window.clearTimeout(timer)
  }, [])

  const showProfiles = () => setSelectedProfile(null)

  return (
    <>
      <SplashScreen isVisible={isSplashVisible} />
      <div className={`${styles.application} ${isSplashVisible ? styles.isWaiting : ''}`}>
        <Header onBrandClick={showProfiles} onProfilesClick={showProfiles} />
        <main className={styles.main}>
          {selectedProfile ? (
            <PortfolioPage profile={selectedProfile} onBack={showProfiles} />
          ) : (
            <ProfileSelector profiles={profiles} onProfileSelect={setSelectedProfile} />
          )}
        </main>
        <Footer />
      </div>
    </>
  )
}

export default App
