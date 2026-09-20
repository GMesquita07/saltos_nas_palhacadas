import { BrandMark } from '../BrandMark/BrandMark'
import { NavIcon } from '../NavIcon/NavIcon'
import type { AuthSession } from '../../types/auth'
import { useAuthenticatedMediaUrl } from '../AuthenticatedMedia'
import { CroppedImage } from '../CroppedImage'
import styles from './Header.module.css'

export type AuthenticationMode = 'login' | 'register'

type HeaderProps = {
  session: AuthSession | null
  activeView: string
  isBrandHidden?: boolean
  onAdminClick: () => void
  onProfilesClick: () => void
  onBookingClick: () => void
  onContactsClick: () => void
  onMaterialsClick: () => void
  onFavoritesClick: () => void
  onAccountClick: () => void
  onAuthenticationClick: (mode: AuthenticationMode) => void
  onHomeClick: () => void
  onLogout: () => void
}

export function Header({
  session,
  activeView,
  isBrandHidden = false,
  onAdminClick,
  onProfilesClick,
  onBookingClick,
  onContactsClick,
  onMaterialsClick,
  onFavoritesClick,
  onAccountClick,
  onAuthenticationClick,
  onHomeClick,
  onLogout,
}: HeaderProps) {
  const accountAvatarUrl = useAuthenticatedMediaUrl(session?.profileImageUrl, session?.token)
  const activeClass = (view: string) => activeView === view ? styles.isActive : ''

  return (
    <header className={styles.header}>
      <div className={styles.inner}>
        <button className={styles.brandButton} type="button" onClick={onHomeClick} aria-label="Página inicial">
          <BrandMark compact dockTarget isHidden={isBrandHidden} />
        </button>
        <nav aria-label="Navegação principal">
          <button className={activeClass('profiles')} type="button" onClick={onProfilesClick}><NavIcon name="profiles" />Perfis</button>
          <button className={activeClass('booking')} type="button" onClick={onBookingClick}><NavIcon name="booking" />Agendar</button>
          <button className={activeClass('contacts')} type="button" onClick={onContactsClick}><NavIcon name="contacts" />Contactos</button>
          <button className={activeClass('materials')} type="button" onClick={onMaterialsClick}><NavIcon name="materials" />Materiais</button>
          {session && <button className={activeClass('favorites')} type="button" onClick={onFavoritesClick}><NavIcon name="favorites" />Favoritos</button>}
          {session?.role === 'ADMIN' && <button type="button" onClick={onAdminClick}><NavIcon name="admin" />Admin</button>}
          {session ? (
            <>
              <button className={`${styles.accountButton} ${activeClass('account')}`} type="button" onClick={onAccountClick}>
                {session.profileImageUrl && accountAvatarUrl
                  ? <CroppedImage alt="Foto de perfil" className={styles.accountAvatar} position={session.profileImagePosition} src={accountAvatarUrl} zoom={session.profileImageZoom} />
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
