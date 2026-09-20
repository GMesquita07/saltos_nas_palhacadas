import { BrandMark } from '../BrandMark/BrandMark'
import { SocialIcon, type SocialIconName } from '../SocialIcon/SocialIcon'
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
] satisfies { href: string; icon: SocialIconName; label: string }[]

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
