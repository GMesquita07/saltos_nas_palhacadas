import { useEffect, useState } from 'react'
import styles from './CookieConsent.module.css'

type ConsentChoice = 'accepted' | 'rejected'

const storageKey = 'saltos.cookie-consent'

type CookieConsentProps = {
  onManage: () => void
}

export function CookieConsent({ onManage }: CookieConsentProps) {
  const [choice, setChoice] = useState<ConsentChoice | null>(readConsent)

  useEffect(() => {
    function handleConsentUpdate(event: Event) {
      const nextChoice = (event as CustomEvent<{ analytics?: boolean }>).detail?.analytics ? 'accepted' : 'rejected'
      setChoice(nextChoice)
    }

    window.addEventListener('saltos:cookie-consent', handleConsentUpdate)
    return () => window.removeEventListener('saltos:cookie-consent', handleConsentUpdate)
  }, [])

  if (choice) return null

  function saveConsent(nextChoice: ConsentChoice) {
    try {
      localStorage.setItem(storageKey, nextChoice)
      window.dispatchEvent(new CustomEvent('saltos:cookie-consent', { detail: { analytics: nextChoice === 'accepted' } }))
    } catch {
      // Consent still applies for the current page even if browser storage is blocked.
    }
    setChoice(nextChoice)
  }

  return (
    <section aria-label="Preferências de cookies" className={styles.banner} role="dialog">
      <p>Usamos armazenamento necessário para a conta e segurança. Analytics ou cookies opcionais só ficam ativos com consentimento.</p>
      <div className={styles.actions}>
        <button type="button" onClick={() => saveConsent('rejected')}>Rejeitar</button>
        <button type="button" onClick={onManage}>Gerir</button>
        <button className={styles.primary} type="button" onClick={() => saveConsent('accepted')}>Aceitar</button>
      </div>
    </section>
  )
}

function readConsent(): ConsentChoice | null {
  try {
    const value = localStorage.getItem(storageKey)
    return value === 'accepted' || value === 'rejected' ? value : null
  } catch {
    return null
  }
}
