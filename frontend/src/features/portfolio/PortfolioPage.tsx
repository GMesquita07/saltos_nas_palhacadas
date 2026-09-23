import { useCallback, useEffect, useMemo, useState } from 'react'
import { getPortfolioItems } from '../../services/portfolioService'
import { CroppedImage } from '../../components/CroppedImage'
import { NavIcon } from '../../components/NavIcon/NavIcon'
import type { Profile } from '../../types/profile'
import type { PortfolioItem, PortfolioItemType } from '../../types/portfolio'
import { PortfolioCard } from './PortfolioCard'
import { MediaLightbox } from './MediaLightbox'
import { ReviewsSection } from '../reviews/ReviewsSection'
import { SocialIcon } from '../../components/SocialIcon/SocialIcon'
import { isExternalSocialLink, socialLinkHref, socialLinkLabel, socialPlatformIcon } from '../profiles/socialLinks'
import styles from './PortfolioPage.module.css'

type Filter = 'Todos' | PortfolioItemType
type PortfolioPageProps = { profile: Profile; onBack: () => void; onBooking: () => void; onLogin: () => void }

export function PortfolioPage({ profile, onBack, onBooking, onLogin }: PortfolioPageProps) {
  const [filter, setFilter] = useState<Filter>('Todos')
  const [items, setItems] = useState<PortfolioItem[]>([])
  const [selectedItem, setSelectedItem] = useState<PortfolioItem | null>(null)
  const [hasError, setHasError] = useState(false)
  const [reviewSummary, setReviewSummary] = useState({ average: 0, count: 0 })

  const featuredVideo = profile.featuredVideoUrl ? resolveFeaturedVideo(profile.featuredVideoUrl) : null
  const imagePosition = profile.imagePosition ?? '50% 50%'
  const imageZoom = profile.imageZoom ?? 1

  useEffect(() => {
    let isCurrent = true

    getPortfolioItems(profile.slug)
      .then((result) => {
        if (isCurrent) {
          setItems(result)
          setHasError(false)
        }
      })
      .catch(() => {
        if (isCurrent) setHasError(true)
      })

    return () => {
      isCurrent = false
    }
  }, [profile.slug])

  const filteredItems = useMemo(
    () => items.filter((item) => filter === 'Todos' || item.type === filter),
    [filter, items],
  )

  const groupedItems = useMemo(
    () => groupPortfolioItems(filteredItems),
    [filteredItems],
  )

  const stats = useMemo(() => {
    const events = new Set(
      items.map((item) => `${item.eventDateIso}|${item.location.trim().toLocaleLowerCase('pt-PT')}`),
    )

    return {
      contents: items.length,
      events: events.size,
    }
  }, [items])

  const heroBackdrop = useMemo(
    () => profile.heroBackgroundImageUrl
      ?? items.find((item) => item.type === 'Foto')?.mediaUrl
      ?? items.find((item) => item.thumbnailUrl)?.thumbnailUrl
      ?? profile.imageUrl
      ?? '',
    [items, profile.heroBackgroundImageUrl, profile.imageUrl],
  )

  const handleReviewSummary = useCallback((summary: { average: number; count: number }) => {
    setReviewSummary(summary)
  }, [])

  function selectPortfolioFilter(nextFilter: Filter) {
    setFilter(nextFilter)
    window.requestAnimationFrame(() => {
      document.getElementById('portfolio-events')?.scrollIntoView({ behavior: 'smooth', block: 'start' })
    })
  }

  return (
    <section className={styles.profileExperience}>
      <div className={styles.page}>
        <button className={styles.back} type="button" onClick={onBack}>
          <NavIcon name="arrow-left" />
          Todos os perfis
        </button>

        <header
          className={`${styles.hero} ${featuredVideo ? styles.heroWithVideo : ''}`}
          id="profile-overview"
        >
          {heroBackdrop && (
            <div
              aria-hidden="true"
              className={styles.heroBackdrop}
              style={{ backgroundImage: `url(${JSON.stringify(heroBackdrop)})` }}
            />
          )}
          <div className={styles.profileImage}>
            <CroppedImage
              alt={'Foto de perfil de ' + profile.name}
              className={styles.profileImageFrame}
              fallback={profile.name.split(' ').map((name) => name[0]).join('').slice(0, 2)}
              position={imagePosition}
              src={profile.imageUrl}
              zoom={imageZoom}
            />
          </div>

          <div className={styles.heroCopy}>
            <p className={styles.role}>{profile.role}</p>
            <h1>{profile.name}</h1>
            <p className={styles.heroDescription}>{profile.description}</p>

            {profile.socialLinks.length > 0 && (
              <div className={styles.socialLinks} aria-label="Links sociais do perfil">
                {profile.socialLinks.map((link) => (
                  <a
                    aria-label={socialLinkLabel(link)}
                    className={styles.socialLink}
                    href={socialLinkHref(link)}
                    key={`${link.platform}-${link.url}`}
                    rel={isExternalSocialLink(link) ? 'noopener noreferrer' : undefined}
                    target={isExternalSocialLink(link) ? '_blank' : undefined}
                    title={socialLinkLabel(link)}
                  >
                    <span aria-hidden="true"><SocialIcon name={socialPlatformIcon(link.platform)} /></span>
                  </a>
                ))}
              </div>
            )}

            <button className={styles.bookingCta} type="button" onClick={onBooking}>
              <NavIcon name="booking" />
              Agendar este artista
            </button>
          </div>

          {featuredVideo && (
            <div className={styles.featuredVideo}>
              <div className={styles.videoLabel}>
                <span className={styles.videoDot} aria-hidden="true" />
                <span>Vídeo em destaque</span>
              </div>

              {featuredVideo.type === 'youtube'
                ? (
                  <iframe
                    allow="accelerometer; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share"
                    allowFullScreen
                    loading="lazy"
                    src={featuredVideo.url}
                    title={'Vídeo de destaque de ' + profile.name}
                  />
                )
                : (
                  <video controls playsInline preload="metadata">
                    <source src={featuredVideo.url} />
                  </video>
                )}
            </div>
          )}

          <div className={styles.heroGlow} aria-hidden="true" />
        </header>

        <section className={styles.profileSummary} aria-label="Resumo do perfil">
          <div>
            <span className={`${styles.statIcon} ${styles.statBlue}`} aria-hidden="true">
              <NavIcon name="camera" />
            </span>
            <p>
              <strong>{stats.contents}</strong>
              <small>Conteúdos</small>
            </p>
          </div>
          <div>
            <span className={`${styles.statIcon} ${styles.statYellow}`} aria-hidden="true">★</span>
            <p>
              <strong>{reviewSummary.count > 0 ? reviewSummary.average.toFixed(1) : '—'}</strong>
              <small>Avaliação média</small>
            </p>
          </div>
          <div>
            <span className={`${styles.statIcon} ${styles.statGreen}`} aria-hidden="true">
              <NavIcon name="calendar-check" />
            </span>
            <p>
              <strong>{stats.events}</strong>
              <small>Eventos realizados</small>
            </p>
          </div>
        </section>

        <nav className={styles.profileNav} aria-label="Secções do perfil">
          <a href="#profile-overview">Visão geral</a>
          <button type="button" onClick={() => selectPortfolioFilter('Todos')}>Eventos</button>
          <button type="button" onClick={() => selectPortfolioFilter('Foto')}>Fotos</button>
          <button type="button" onClick={() => selectPortfolioFilter('Vídeo')}>Vídeos</button>
          <a href="#reviews-title">Avaliações</a>
        </nav>

        <section className={styles.eventsSection} id="portfolio-events">
          <div className={styles.portfolioHeader}>
            <div>
              <p className={styles.sectionKicker}>Portfólio</p>
              <h2>Eventos recentes</h2>
              <p className={styles.sectionIntro}>Os últimos momentos publicados deste artista.</p>
            </div>

            <div className={styles.filters} aria-label="Filtrar conteúdo">
              {(['Todos', 'Vídeo', 'Foto'] as Filter[]).map((option) => (
                <button
                  className={filter === option ? styles.active : ''}
                  key={option}
                  type="button"
                  onClick={() => setFilter(option)}
                >
                  {option}
                </button>
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
                      <div className={styles.grid}>
                        {group.items.map((item) => (
                          <PortfolioCard item={item} key={item.id} onOpen={setSelectedItem} />
                        ))}
                      </div>
                    </section>
                  ))}
                </div>
              )}
        </section>

        <ReviewsSection
          profile={profile}
          onLoginClick={onLogin}
          onSummaryChange={handleReviewSummary}
        />

        {selectedItem && (
          <MediaLightbox item={selectedItem} onClose={() => setSelectedItem(null)} />
        )}
      </div>
    </section>
  )
}

function groupPortfolioItems(items: PortfolioItem[]) {
  const monthFormatter = new Intl.DateTimeFormat('pt-PT', {
    month: 'long',
    year: 'numeric',
  })

  const groups: { key: string; label: string; items: PortfolioItem[] }[] = []

  items.forEach((item) => {
    const date = new Date(item.eventDateIso + 'T00:00:00')
    const key = Number.isNaN(date.getTime())
      ? item.eventDateIso
      : date.getFullYear() + '-' + String(date.getMonth() + 1).padStart(2, '0')

    const existing = groups.find((group) => group.key === key)
    if (existing) {
      existing.items.push(item)
      return
    }

    const rawLabel = Number.isNaN(date.getTime())
      ? item.eventDate
      : monthFormatter.format(date)

    groups.push({
      key,
      label: rawLabel ? rawLabel.charAt(0).toUpperCase() + rawLabel.slice(1) : rawLabel,
      items: [item],
    })
  })

  return groups
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
    return 'https://www.youtube.com/embed/' + videoId + '?playsinline=1&rel=0'
  } catch {
    return null
  }
}
