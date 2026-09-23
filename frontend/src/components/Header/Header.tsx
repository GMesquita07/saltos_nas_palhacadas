import { useEffect, useId, useState } from 'react'
import { BrandMark } from '../BrandMark/BrandMark'
import { NavIcon } from '../NavIcon/NavIcon'
import type { AuthSession } from '../../types/auth'
import { useAuthenticatedMediaUrl } from '../AuthenticatedMedia'
import { CroppedImage } from '../CroppedImage'
import styles from './Header.module.css'

export type AuthenticationMode = 'login' | 'register'

type HeaderProps = {
  session: AuthSession | null
  theme: 'dark' | 'light'
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
  onThemeToggle: () => void
}

export function Header({
  session,
  theme,
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
  onThemeToggle,
}: HeaderProps) {
  const accountAvatarUrl = useAuthenticatedMediaUrl(session?.profileImageUrl, session?.token)
  const activeClass = (view: string) => activeView === view ? styles.isActive : ''
  const [isMenuOpen, setIsMenuOpen] = useState(false)
  const mobileNavId = useId()

  const runAndClose = (action: () => void) => {
    setIsMenuOpen(false)
    action()
  }

  useEffect(() => {
    if (!isMenuOpen) return

    const closeOnEscape = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        setIsMenuOpen(false)
      }
    }

    document.addEventListener('keydown', closeOnEscape)
    return () => document.removeEventListener('keydown', closeOnEscape)
  }, [isMenuOpen])

  return (
    <header className={styles.header}>
      <div className={styles.inner}>
        <button
          className={styles.brandButton}
          type="button"
          onClick={() => runAndClose(onHomeClick)}
          aria-label="Página inicial"
        >
          <BrandMark compact dockTarget isHidden={isBrandHidden} />
        </button>

        <button
          className={styles.menuButton}
          type="button"
          aria-controls={mobileNavId}
          aria-expanded={isMenuOpen}
          aria-label={isMenuOpen ? 'Fechar menu de navegação' : 'Abrir menu de navegação'}
          onClick={() => setIsMenuOpen((open) => !open)}
        >
          <span className={styles.menuIcon} aria-hidden="true">
            <span />
            <span />
            <span />
          </span>
          <span>{isMenuOpen ? 'Fechar' : 'Menu'}</span>
        </button>

        <nav
          id={mobileNavId}
          className={isMenuOpen ? styles.isMenuOpen : undefined}
          aria-label="Navegação principal"
        >
          <button className={activeClass('profiles')} type="button" onClick={() => runAndClose(onProfilesClick)}>
            <NavIcon name="profiles" />Perfis
          </button>
          <button className={activeClass('booking')} type="button" onClick={() => runAndClose(onBookingClick)}>
            <NavIcon name="booking" />Agendar
          </button>
          <button className={activeClass('contacts')} type="button" onClick={() => runAndClose(onContactsClick)}>
            <NavIcon name="contacts" />Contactos
          </button>
          <button className={activeClass('materials')} type="button" onClick={() => runAndClose(onMaterialsClick)}>
            <NavIcon name="materials" />Materiais
          </button>
          {session && (
            <button className={activeClass('favorites')} type="button" onClick={() => runAndClose(onFavoritesClick)}>
              <NavIcon name="favorites" />Favoritos
            </button>
          )}
          {session?.role === 'ADMIN' && (
            <button type="button" onClick={() => runAndClose(onAdminClick)}>
              <NavIcon name="admin" />Admin
            </button>
          )}

          {session ? (
            <>
              <button
                className={`${styles.accountButton} ${activeClass('account')}`}
                type="button"
                onClick={() => runAndClose(onAccountClick)}
              >
                {session.profileImageUrl && accountAvatarUrl
                  ? (
                    <CroppedImage
                      alt="Foto de perfil"
                      className={styles.accountAvatar}
                      position={session.profileImagePosition}
                      src={accountAvatarUrl}
                      zoom={session.profileImageZoom}
                    />
                    )
                  : <NavIcon name="account" />}
                Conta
              </button>
              <button className={styles.logoutButton} type="button" onClick={() => runAndClose(onLogout)}>
                <NavIcon name="logout" />Sair
              </button>
            </>
          ) : (
            <button
              className={styles.authenticationButton}
              type="button"
              onClick={() => runAndClose(() => onAuthenticationClick('login'))}
            >
              <NavIcon name="login" />
              Login / Criar Conta
            </button>
          )}
          <button
            aria-label={theme === 'dark' ? 'Ativar tema claro' : 'Ativar tema escuro'}
            className={styles.themeButton}
            title={theme === 'dark' ? 'Tema claro' : 'Tema escuro'}
            type="button"
            onClick={onThemeToggle}
          >
            {theme === 'dark' ? (
              <svg aria-hidden="true" viewBox="0 0 24 24">
                <circle cx="12" cy="12" r="4" />
                <path d="M12 2v2M12 20v2M4.93 4.93l1.42 1.42M17.65 17.65l1.42 1.42M2 12h2M20 12h2M4.93 19.07l1.42-1.42M17.65 6.35l1.42-1.42" />
              </svg>
            ) : (
              <svg aria-hidden="true" viewBox="0 0 24 24">
                <path d="M20 15.25A8 8 0 0 1 8.75 4 8.25 8.25 0 1 0 20 15.25Z" />
              </svg>
            )}
          </button>
        </nav>
      </div>
    </header>
  )
}
