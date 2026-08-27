import { BrandMark } from '../BrandMark/BrandMark'
import styles from './Footer.module.css'

type FooterProps = {
  onCookiesClick: () => void
  onPrivacyClick: () => void
  onTermsClick: () => void
}

export function Footer({ onCookiesClick, onPrivacyClick, onTermsClick }: FooterProps) {
  return (
    <footer className={styles.footer}>
      <div className={styles.inner}>
        <BrandMark compact />
        <p>Eventos que ficam na memória.</p>
        <nav aria-label="Informação legal">
          <button type="button" onClick={onPrivacyClick}>Privacidade</button>
          <button type="button" onClick={onTermsClick}>Termos</button>
          <button type="button" onClick={onCookiesClick}>Cookies</button>
        </nav>
        <a href="mailto:ola@saltosnaspalhacadas.pt">ola@saltosnaspalhacadas.pt</a>
      </div>
    </footer>
  )
}
