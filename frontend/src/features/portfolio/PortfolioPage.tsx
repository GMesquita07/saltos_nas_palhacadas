import { useEffect, useMemo, useState } from 'react'
import { getPortfolioItems } from '../../services/portfolioService'
import { imageCropStyle } from '../../components/imageCrop'
import type { Profile } from '../../types/profile'
import type { PortfolioItem, PortfolioItemType } from '../../types/portfolio'
import { PortfolioCard } from './PortfolioCard'
import { ReviewsSection } from '../reviews/ReviewsSection'
import styles from './PortfolioPage.module.css'

type Filter = 'Todos' | PortfolioItemType
type PortfolioPageProps = { profile: Profile; onBack: () => void; onBooking: () => void; onLogin: () => void }

export function PortfolioPage({ profile, onBack, onBooking, onLogin }: PortfolioPageProps) {
  const [filter, setFilter] = useState<Filter>('Todos')
  const [items, setItems] = useState<PortfolioItem[]>([])
  const [hasError, setHasError] = useState(false)
  const featuredVideo = profile.featuredVideoUrl ? resolveFeaturedVideo(profile.featuredVideoUrl) : null
  const imagePosition = profile.imagePosition ?? '50% 50%'
  const imageZoom = profile.imageZoom ?? 1

  useEffect(() => {
    let isCurrent = true
    getPortfolioItems(profile.slug)
      .then((result) => { if (isCurrent) { setItems(result); setHasError(false) } })
      .catch(() => { if (isCurrent) setHasError(true) })
    return () => { isCurrent = false }
  }, [profile.slug])

  const filteredItems = useMemo(() => items.filter((item) => filter === 'Todos' || item.type === filter), [filter, items])
  const groupedItems = useMemo(() => groupPortfolioItems(filteredItems), [filteredItems])

  return (
    <section className={styles.page}>
      <button className={styles.back} type="button" onClick={onBack}>← Todos os perfis</button>
      <header className={`${styles.hero} ${featuredVideo ? styles.heroWithVideo : ''}`}>
        <div className={styles.profileImage}>
          {profile.imageUrl
            ? <img key={profile.imageUrl + imagePosition + imageZoom} src={profile.imageUrl} alt="" style={imageCropStyle(imagePosition, imageZoom)} />
            : profile.name.split(' ').map((name) => name[0]).join('').slice(0, 2)}
        </div>
        <div className={styles.heroCopy}>
          <p className="eyebrow">{profile.role}</p>
          <h1>{profile.name}</h1>
          <p>{profile.description}</p>
          <button className={styles.bookingCta} type="button" onClick={onBooking}>Agendar este artista →</button>
        </div>
        {featuredVideo && (
          <div className={styles.featuredVideo}>
            <div className={styles.videoLabel}>
              <span>Vídeo em destaque</span>
            </div>
            {featuredVideo.type === 'youtube'
              ? (
                <iframe
                  allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share"
                  allowFullScreen
                  src={featuredVideo.url}
                  title={'Vídeo de destaque de ' + profile.name}
                />
              )
              : (
                <video autoPlay controls loop muted playsInline preload="metadata">
                  <source src={featuredVideo.url} />
                </video>
              )}
          </div>
        )}
      </header>
      <div className={styles.portfolioHeader}>
        <div>
          <p className="eyebrow">Portfólio</p>
          <h2>Eventos recentes</h2>
        </div>
        <div className={styles.filters} aria-label="Filtrar conteúdo">
          {(['Todos', 'Vídeo', 'Foto'] as Filter[]).map((option) => (
            <button className={filter === option ? styles.active : ''} key={option} type="button" onClick={() => setFilter(option)}>{option}</button>
          ))}
        </div>
      </div>
      {hasError
        ? <p className={styles.feedback}>Não foi possível carregar este portfólio.</p>
        : filteredItems.length === 0
          ? <p className={styles.feedback}>Ainda não existem conteúdos publicados neste perfil.</p>
          : (
            <div className={styles.monthGroups}>
              {groupedItems.map((group) => (
                <section className={styles.monthGroup} key={group.key}>
                  <h3>{group.label}</h3>
                  <div className={styles.grid}>{group.items.map((item) => <PortfolioCard item={item} key={item.id} />)}</div>
                </section>
              ))}
            </div>
          )}
      <ReviewsSection profile={profile} onLoginClick={onLogin} />
    </section>
  )
}

function groupPortfolioItems(items: PortfolioItem[]) {
  const monthFormatter = new Intl.DateTimeFormat('pt-PT', { month: 'long', year: 'numeric' })
  const groups: { key: string; label: string; items: PortfolioItem[] }[] = []

  items.forEach((item) => {
    const date = new Date(item.eventDateIso + 'T00:00:00')
    const key = Number.isNaN(date.getTime()) ? item.eventDateIso : date.getFullYear() + '-' + String(date.getMonth() + 1).padStart(2, '0')
    const existing = groups.find((group) => group.key === key)

    if (existing) {
      existing.items.push(item)
      return
    }

    const rawLabel = Number.isNaN(date.getTime()) ? item.eventDate : monthFormatter.format(date)
    groups.push({ key, label: capitalize(rawLabel), items: [item] })
  })

  return groups
}

function capitalize(value: string) {
  return value ? value.charAt(0).toUpperCase() + value.slice(1) : value
}

function resolveFeaturedVideo(url: string): { type: 'youtube' | 'video'; url: string } {
  const youtubeUrl = toYoutubeEmbedUrl(url)
  return youtubeUrl ? { type: 'youtube', url: youtubeUrl } : { type: 'video', url }
}

function toYoutubeEmbedUrl(value: string) {
  try {
    const url = new URL(value)
    const host = url.hostname.replace(/^www\./, '')
    let videoId = ''

    if (host === 'youtu.be') {
      videoId = url.pathname.split('/').filter(Boolean)[0] ?? ''
    } else if (host === 'youtube.com' || host === 'm.youtube.com' || host === 'music.youtube.com') {
      if (url.pathname.startsWith('/embed/')) {
        videoId = url.pathname.split('/').filter(Boolean)[1] ?? ''
      } else if (url.pathname.startsWith('/shorts/')) {
        videoId = url.pathname.split('/').filter(Boolean)[1] ?? ''
      } else {
        videoId = url.searchParams.get('v') ?? ''
      }
    }

    if (!/^[a-zA-Z0-9_-]{6,}$/.test(videoId)) return null
    return 'https://www.youtube.com/embed/' + videoId + '?autoplay=1&mute=1&playsinline=1&rel=0'
  } catch {
    return null
  }
}
