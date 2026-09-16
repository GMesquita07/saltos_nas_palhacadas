import { useState } from 'react'
import { useAuth } from '../auth/AuthContext'
import type { PortfolioItem } from '../../types/portfolio'
import styles from './PortfolioCard.module.css'

export function PortfolioCard({ item }: { item: PortfolioItem }) {
  const { favorites, session, toggleFavorite } = useAuth()
  const [isToggling, setIsToggling] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const isFavorite = favorites.some((favorite) => favorite.portfolioItemId === item.id)

  async function handleFavorite() {
    if (!session) {
      setError('Inicia sessão para guardar esta publicação nos favoritos.')
      return
    }

    setIsToggling(true)
    setError(null)
    try {
      await toggleFavorite(item.id)
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'Não foi possível atualizar os favoritos.')
    } finally {
      setIsToggling(false)
    }
  }

  return (
    <article className={styles.card}>
      <div className={styles.image}>
        {item.type === 'Vídeo'
          ? <video controls preload="metadata" poster={item.thumbnailUrl}><source src={item.mediaUrl} /></video>
          : <img src={item.mediaUrl} alt={item.title} />}
        <small>{item.type}</small>
        <button
          aria-label={isFavorite ? 'Remover dos favoritos' : 'Adicionar aos favoritos'}
          aria-pressed={isFavorite}
          className={`${styles.favoriteButton} ${isFavorite ? styles.isFavorite : ''}`}
          disabled={isToggling}
          type="button"
          onClick={() => { void handleFavorite() }}
        >
          {isFavorite ? '♥' : '♡'}
        </button>
      </div>
      <div className={styles.details}>
        <p>{item.location} · {item.eventDate}</p>
        <h3>{item.title}</h3>
        {error && <p className={styles.favoriteError} role="status">{error}</p>}
      </div>
    </article>
  )
}
