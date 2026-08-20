import { useAuth } from './AuthContext'
import styles from './AccountPage.module.css'

type AccountPageProps = {
  onFavoritesClick: () => void
  onExit: () => void
}

export function AccountPage({ onFavoritesClick, onExit }: AccountPageProps) {
  const { favorites, logout, session } = useAuth()

  if (!session) return null

  return (
    <section className={styles.page}>
      <button className={styles.back} type="button" onClick={onExit}>← Voltar aos perfis</button>
      <div className={styles.content}>
        <p className="eyebrow">A minha conta</p>
        <h1>Olá!</h1>
        <dl className={styles.details}>
          <div><dt>Email</dt><dd>{session.email}</dd></div>
          <div><dt>Tipo de conta</dt><dd>{session.role === 'ADMIN' ? 'Administração' : 'Utilizador'}</dd></div>
        </dl>
        <div className={styles.actions}>
          <button type="button" onClick={onFavoritesClick}>Ver favoritos ({favorites.length})</button>
          <button className={styles.logout} type="button" onClick={() => { logout(); onExit() }}>Terminar sessão</button>
        </div>
      </div>
    </section>
  )
}
