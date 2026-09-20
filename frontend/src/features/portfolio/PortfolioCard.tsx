import { useState } from 'react'
import { useAuth } from '../auth/AuthContext'
import type { PortfolioItem } from '../../types/portfolio'
import styles from './PortfolioCard.module.css'

export function PortfolioCard({ item, onOpen }: { item: PortfolioItem; onOpen?: (item: PortfolioItem) => void }) {
  const { favorites, isSessionReady, session, toggleFavorite } = useAuth()
  const [isToggling, setIsToggling] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const isFavorite = favorites.some((favorite) => favorite.portfolioItemId === item.id)

  async function handleFavorite() {
    if (!isSessionReady) return

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
        {onOpen ? (
          <button
            className={styles.mediaButton}
            type="button"
            aria-label={`Abrir ${item.title}`}
            onClick={() => onOpen(item)}
          >
            <PortfolioPreview item={item} />
          </button>
        ) : (
          <div className={styles.mediaPreview}>
            <PortfolioPreview item={item} />
          </div>
        )}
        <small>{item.type}</small>
        <button
          aria-label={isFavorite ? 'Remover dos favoritos' : 'Adicionar aos favoritos'}
          aria-pressed={isFavorite}
          className={`${styles.favoriteButton} ${isFavorite ? styles.isFavorite : ''}`}
          disabled={isToggling || !isSessionReady}
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

function PortfolioPreview({ item }: { item: PortfolioItem }) {
  return item.type === 'Vídeo'
    ? (
      <>
        {item.thumbnailUrl
          ? <img src={item.thumbnailUrl} alt="" />
          : <video muted playsInline preload="metadata"><source src={item.mediaUrl} /></video>}
        <span className={styles.playIndicator} aria-hidden="true">▶</span>
      </>
    )
    : <img src={item.mediaUrl} alt={item.title} />
}
