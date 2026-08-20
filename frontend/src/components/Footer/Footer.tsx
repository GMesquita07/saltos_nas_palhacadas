import { BrandMark } from '../BrandMark/BrandMark'
import styles from './Footer.module.css'

export function Footer() {
  return <footer className={styles.footer}><div className={styles.inner}><BrandMark compact /><p>Eventos que ficam na memória.</p><a href="mailto:ola@saltosnaspalhacadas.pt">ola@saltosnaspalhacadas.pt</a></div></footer>
}
