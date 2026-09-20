import { useState } from 'react'
import { PortfolioCard } from '../portfolio/PortfolioCard'
import { MediaLightbox } from '../portfolio/MediaLightbox'
import { useAuth } from '../auth/AuthContext'
import type { PortfolioItem } from '../../types/portfolio'
import styles from './FavoritesPage.module.css'

type FavoritesPageProps = {
  onBack: () => void
}

export function FavoritesPage({ onBack }: FavoritesPageProps) {
  const { favorites, isFavoritesLoading } = useAuth()
  const [selectedItem, setSelectedItem] = useState<PortfolioItem | null>(null)

  return (
    <section className={styles.page}>
      <button className={styles.back} type="button" onClick={onBack}>← Todos os perfis</button>
      <header className={styles.header}>
        <p className="eyebrow">A minha coleção</p>
        <h1>Favoritos</h1>
        <p>As publicações que guardaste para voltar a ver mais tarde.</p>
      </header>
      {isFavoritesLoading ? (
        <p className={styles.feedback}>A carregar favoritos...</p>
      ) : favorites.length === 0 ? (
        <p className={styles.feedback}>Ainda não guardaste nenhuma publicação. Explora os perfis e usa o ícone de favorito.</p>
      ) : (
        <div className={styles.grid}>
          {favorites.map((favorite) => (
            <div key={favorite.portfolioItemId}>
              <p className={styles.profile}>Perfil · {favorite.profileSlug}</p>
              <PortfolioCard item={favorite.item} onOpen={setSelectedItem} />
            </div>
          ))}
        </div>
      )}
      {selectedItem && <MediaLightbox item={selectedItem} onClose={() => setSelectedItem(null)} />}
    </section>
  )
}
