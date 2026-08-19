import type { PortfolioItem } from '../../types/portfolio'
import styles from './PortfolioCard.module.css'

export function PortfolioCard({ item }: { item: PortfolioItem }) {
  return <article className={styles.card}><div className={styles.image}>{item.imageUrl ? <img src={item.imageUrl} alt="" /> : <span>{item.type === 'Vídeo' ? '▶' : '○'}</span>}<small>{item.type}</small></div><div className={styles.details}><p>{item.location} · {item.eventDate}</p><h3>{item.title}</h3></div></article>
}
