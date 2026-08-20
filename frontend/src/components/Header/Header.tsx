import { BrandMark } from '../BrandMark/BrandMark'
import styles from './Header.module.css'

type HeaderProps = { onAdminClick: () => void; onProfilesClick: () => void; onContactsClick: () => void }

export function Header({ onAdminClick, onProfilesClick, onContactsClick }: HeaderProps) {
  return <header className={styles.header}><div className={styles.inner}>
    <button className={styles.brandButton} type="button" onClick={() => window.location.reload()} aria-label="Atualizar página inicial"><BrandMark compact /></button>
    <nav aria-label="Navegação principal"><button type="button" onClick={onProfilesClick}>Perfis</button><button type="button" onClick={onContactsClick}>Contactos</button><button type="button" onClick={onAdminClick}>Admin</button></nav>
  </div></header>
}
