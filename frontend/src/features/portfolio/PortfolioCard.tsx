import type { PortfolioItem } from '../../types/portfolio'
import styles from './PortfolioCard.module.css'

export function PortfolioCard({ item }: { item: PortfolioItem }) {
  return <article className={styles.card}><div className={styles.image}>{item.type === 'Vídeo' ? <video controls preload="metadata" poster={item.thumbnailUrl}><source src={item.mediaUrl} /></video> : <img src={item.mediaUrl} alt={item.title} />}<small>{item.type}</small></div><div className={styles.details}><p>{item.location} · {item.eventDate}</p><h3>{item.title}</h3></div></article>
}
