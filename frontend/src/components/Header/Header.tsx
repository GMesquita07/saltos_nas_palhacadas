import { BrandMark } from '../BrandMark/BrandMark'
import styles from './Header.module.css'

type HeaderProps = { onAdminClick: () => void; onBrandClick: () => void; onProfilesClick: () => void }

export function Header({ onAdminClick, onBrandClick, onProfilesClick }: HeaderProps) {
  return <header className={styles.header}><div className={styles.inner}>
    <button className={styles.brandButton} type="button" onClick={onBrandClick}><BrandMark compact /></button>
    <nav aria-label="Navegação principal"><button type="button" onClick={onProfilesClick}>Perfis</button><a href="#contactos">Contactos</a><button type="button" onClick={onAdminClick}>Admin</button></nav>
  </div></header>
}
