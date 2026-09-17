import { BrandMark } from '../BrandMark/BrandMark'
import styles from './Footer.module.css'

type FooterProps = {
  onFAQClick: () => void
  onCookiesClick: () => void
  onPrivacyClick: () => void
  onTermsClick: () => void
}

const socialLinks = [
  { href: 'https://www.instagram.com/saltosnaspalhacadas', icon: 'instagram', label: 'Instagram' },
  { href: 'https://www.tiktok.com/@saltosnaspalhacadas', icon: 'tiktok', label: 'TikTok' },
  { href: 'https://www.youtube.com/@saltosnaspalhacadas', icon: 'youtube', label: 'YouTube' },
  { href: 'https://www.facebook.com/saltosnaspalhacadas', icon: 'facebook', label: 'Facebook' },
] as const

export function Footer({ onCookiesClick, onFAQClick, onPrivacyClick, onTermsClick }: FooterProps) {
  return (
    <footer className={styles.footer}>
      <div className={styles.inner}>
        <div className={styles.logoArea}>
          <BrandMark compact variant="square" />
        </div>
        <div className={styles.center}>
          <a href="mailto:saltosepalhacadas@gmail.com">saltosepalhacadas@gmail.com</a>
          <div className={styles.socialLinks} aria-label="Redes sociais">
            {socialLinks.map((link) => (
              <a href={link.href} key={link.label} rel="noreferrer" target="_blank" aria-label={link.label}>
                <SocialIcon name={link.icon} />
              </a>
            ))}
          </div>
        </div>
        <nav aria-label="Informação legal">
          <button type="button" onClick={onFAQClick}>FAQ</button>
          <button type="button" onClick={onPrivacyClick}>Privacidade</button>
          <button type="button" onClick={onTermsClick}>Termos</button>
          <button type="button" onClick={onCookiesClick}>Cookies</button>
        </nav>
      </div>
    </footer>
  )
}

function SocialIcon({ name }: { name: typeof socialLinks[number]['icon'] }) {
  if (name === 'instagram') {
    return (
      <svg aria-hidden="true" viewBox="0 0 24 24">
        <rect x="5" y="5" width="14" height="14" rx="4" />
        <circle cx="12" cy="12" r="3.1" />
        <circle cx="16.4" cy="7.6" r="0.7" />
      </svg>
    )
  }

  if (name === 'tiktok') {
    return (
      <svg aria-hidden="true" viewBox="0 0 24 24">
        <path d="M14 4v9.2a4.2 4.2 0 1 1-3.8-4.18" />
        <path d="M14 4c.45 2.9 2.13 4.63 5 5" />
      </svg>
    )
  }

  if (name === 'youtube') {
    return (
      <svg aria-hidden="true" viewBox="0 0 24 24">
        <rect x="4" y="7" width="16" height="10" rx="3" />
        <path d="m10.5 10 4 2-4 2z" />
      </svg>
    )
  }

  return (
    <svg aria-hidden="true" viewBox="0 0 24 24">
      <path d="M14 8h2V5h-2.4A4.1 4.1 0 0 0 9.5 9.1V11H7v3h2.5v6H13v-6h2.5l.5-3h-3V9.3c0-.8.3-1.3 1-1.3Z" />
    </svg>
  )
}
