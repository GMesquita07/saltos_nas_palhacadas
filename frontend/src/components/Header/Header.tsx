import { BrandMark } from '../BrandMark/BrandMark'
import { NavIcon } from '../NavIcon/NavIcon'
import type { AuthSession } from '../../types/auth'
import { CroppedImage } from '../CroppedImage'
import styles from './Header.module.css'

export type AuthenticationMode = 'login' | 'register'

type HeaderProps = {
  session: AuthSession | null
  onAdminClick: () => void
  onProfilesClick: () => void
  onBookingClick: () => void
  onContactsClick: () => void
  onClientContentClick: () => void
  onFavoritesClick: () => void
  onAccountClick: () => void
  onAuthenticationClick: (mode: AuthenticationMode) => void
  onLogout: () => void
}

export function Header({
  session,
  onAdminClick,
  onProfilesClick,
  onBookingClick,
  onContactsClick,
  onClientContentClick,
  onFavoritesClick,
  onAccountClick,
  onAuthenticationClick,
  onLogout,
}: HeaderProps) {
  return (
    <header className={styles.header}>
      <div className={styles.inner}>
        <button className={styles.brandButton} type="button" onClick={() => window.location.reload()} aria-label="Atualizar página inicial">
          <BrandMark compact />
        </button>
        <nav aria-label="Navegação principal">
          <button type="button" onClick={onProfilesClick}><NavIcon name="profiles" />Perfis</button>
          <button type="button" onClick={onBookingClick}><NavIcon name="booking" />Agendar</button>
          <button type="button" onClick={onContactsClick}><NavIcon name="contacts" />Contactos</button>
          <button type="button" onClick={onClientContentClick}><NavIcon name="clientContent" />Partilhas</button>
          {session && <button type="button" onClick={onFavoritesClick}><NavIcon name="favorites" />Favoritos</button>}
          {session?.role === 'ADMIN' && <button type="button" onClick={onAdminClick}><NavIcon name="admin" />Admin</button>}
          {session ? (
            <>
              <button className={styles.accountButton} type="button" onClick={onAccountClick}>
                {session.profileImageUrl
                  ? <CroppedImage className={styles.accountAvatar} position={session.profileImagePosition} src={session.profileImageUrl} zoom={session.profileImageZoom} />
                  : <NavIcon name="account" />}
                Conta
              </button>
              <button className={styles.logoutButton} type="button" onClick={onLogout}><NavIcon name="logout" />Sair</button>
            </>
          ) : (
            <button className={styles.authenticationButton} type="button" onClick={() => onAuthenticationClick('login')}>
              <NavIcon name="login" />
              Login / Criar Conta
            </button>
          )}
        </nav>
      </div>
    </header>
  )
}
