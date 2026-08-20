import { useEffect, useMemo, useState } from 'react'
import { getPortfolioItems } from '../../services/portfolioService'
import type { Profile } from '../../types/profile'
import type { PortfolioItem, PortfolioItemType } from '../../types/portfolio'
import { PortfolioCard } from './PortfolioCard'
import styles from './PortfolioPage.module.css'

type Filter = 'Todos' | PortfolioItemType
type PortfolioPageProps = { profile: Profile; onBack: () => void }

export function PortfolioPage({ profile, onBack }: PortfolioPageProps) {
  const [filter, setFilter] = useState<Filter>('Todos')
  const [items, setItems] = useState<PortfolioItem[]>([])
  const [hasError, setHasError] = useState(false)

  useEffect(() => {
    let isCurrent = true
    getPortfolioItems(profile.slug)
      .then((result) => { if (isCurrent) { setItems(result); setHasError(false) } })
      .catch(() => { if (isCurrent) setHasError(true) })
    return () => { isCurrent = false }
  }, [profile.slug])

  const filteredItems = useMemo(() => items.filter((item) => filter === 'Todos' || item.type === filter), [filter, items])

  return <section className={styles.page}>
    <button className={styles.back} type="button" onClick={onBack}>← Todos os perfis</button>
    <header className={styles.hero}><div className={styles.profileImage}>{profile.imageUrl ? <img src={profile.imageUrl} alt="" style={{ objectPosition: profile.imagePosition ?? '50% 50%' }} /> : profile.name.split(' ').map((name) => name[0]).join('').slice(0, 2)}</div><div><p className="eyebrow">{profile.role}</p><h1>{profile.name}</h1><p>{profile.description}</p></div></header>
    <div className={styles.portfolioHeader}><div><p className="eyebrow">Portfólio</p><h2>Eventos recentes</h2></div><div className={styles.filters} aria-label="Filtrar conteúdo">{(['Todos', 'Vídeo', 'Foto'] as Filter[]).map((option) => <button className={filter === option ? styles.active : ''} key={option} type="button" onClick={() => setFilter(option)}>{option}</button>)}</div></div>
    {hasError ? <p className={styles.feedback}>Não foi possível carregar este portfólio.</p> : filteredItems.length === 0 ? <p className={styles.feedback}>Ainda não existem conteúdos publicados neste perfil.</p> : <div className={styles.grid}>{filteredItems.map((item) => <PortfolioCard item={item} key={item.id} />)}</div>}
  </section>
}
