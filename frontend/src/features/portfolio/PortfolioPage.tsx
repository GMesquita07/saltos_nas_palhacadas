import { useCallback, useEffect, useMemo, useState } from 'react'
import { preferredScrollBehavior } from '../../accessibility/motion'
import { getPortfolioItems } from '../../services/portfolioService'
import {
  invalidatePortfolioItemsCache,
  portfolioInvalidationMatches,
  portfolioInvalidationStorageKey,
} from '../../services/portfolioInvalidation'
import { ArtistProfileImage } from '../../components/ArtistProfileImage'
import { artistInitials } from '../../components/artistProfileImage'
import { ProfileHeroBackground } from '../../components/ProfileHeroBackground'
import { formatImagePosition, parseImageCrop } from '../../components/imageCrop'
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
  const heroBackgroundCrop = parseImageCrop(
    profile.heroBackgroundImageUrl ? profile.heroBackgroundImagePosition : undefined,
    profile.heroBackgroundImageUrl ? profile.heroBackgroundImageZoom : undefined,
  )
  const heroBackgroundPosition = formatImagePosition(heroBackgroundCrop)

  useEffect(() => {
    let isCurrent = true

    function loadItems(force = false) {
      getPortfolioItems(profile.slug, { force })
        .then((result) => {
          if (isCurrent) {
            setItems(result)
            setHasError(false)
          }
        })
        .catch(() => {
          if (isCurrent) setHasError(true)
        })
    }

    function reloadCurrentPortfolio() {
      invalidatePortfolioItemsCache(profile.slug, { broadcast: false })
      loadItems(true)
    }

    function handlePortfolioChanged(event: Event) {
      const changedSlug = event instanceof CustomEvent ? (event.detail as { slug?: string } | undefined)?.slug : undefined
      if (changedSlug && changedSlug !== profile.slug) return

      reloadCurrentPortfolio()
    }

    function handlePortfolioStorage(event: StorageEvent) {
      if (event.key !== portfolioInvalidationStorageKey) return
      if (!portfolioInvalidationMatches(event.newValue, profile.slug)) return

      reloadCurrentPortfolio()
    }

    function handlePageVisible() {
      if (document.visibilityState !== 'visible') return

      loadItems(true)
    }

    loadItems()
    window.addEventListener('portfolio:changed', handlePortfolioChanged)
    window.addEventListener('storage', handlePortfolioStorage)
    window.addEventListener('focus', handlePageVisible)
    document.addEventListener('visibilitychange', handlePageVisible)

    return () => {
      isCurrent = false
      window.removeEventListener('portfolio:changed', handlePortfolioChanged)
      window.removeEventListener('storage', handlePortfolioStorage)
      window.removeEventListener('focus', handlePageVisible)
      document.removeEventListener('visibilitychange', handlePageVisible)
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

  const stats = useMemo(() => ({
    contents: items.length,
    events: profile.completedEventsCount ?? 0,
  }), [items.length, profile.completedEventsCount])

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
      document.getElementById('portfolio-events')?.scrollIntoView({ behavior: preferredScrollBehavior(), block: 'start' })
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
            <ProfileHeroBackground
              ariaHidden
              className={styles.heroBackdrop}
              mutedImage
              position={heroBackgroundPosition}
              showProfileOverlay
              src={heroBackdrop}
              zoom={heroBackgroundCrop.zoom}
            />
          )}
          <ArtistProfileImage
            alt={'Foto de perfil de ' + profile.name}
            fallback={artistInitials(profile.name)}
            fetchPriority="high"
            loading="eager"
            position={imagePosition}
            src={profile.imageUrl}
            variant="heroResponsive"
            zoom={imageZoom}
          />

          <div className={styles.heroCopy}>
            <p className={styles.role}>{profile.role}</p>
            <h1>{profile.name}</h1>

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
                    allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share"
                    allowFullScreen
                    loading="lazy"
                    key={profile.slug + '-' + featuredVideo.url}
                    src={featuredVideo.url}
                    title={'Vídeo de destaque de ' + profile.name}
                  />
                )
                : (
                  <video key={profile.slug + '-' + featuredVideo.url} autoPlay controls muted playsInline preload="metadata">
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
          <a href="#profile-about">Sobre o artista</a>
          <button type="button" onClick={() => selectPortfolioFilter('Todos')}>Eventos</button>
          <button type="button" onClick={() => selectPortfolioFilter('Foto')}>Fotos</button>
          <button type="button" onClick={() => selectPortfolioFilter('Vídeo')}>Vídeos</button>
          <a href="#reviews-title">Avaliações</a>
        </nav>

        <section className={styles.aboutSection} id="profile-about" aria-labelledby="profile-about-title">
          <div className={styles.aboutHeading}>
            <p className={styles.sectionKicker}>Sobre</p>
            <h2 id="profile-about-title">Sobre o artista</h2>
          </div>
          <p className={styles.aboutCopy}>{profile.description}</p>
        </section>

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
            ? <p className={styles.feedback} role="alert">Não foi possível carregar este portfólio.</p>
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
          <MediaLightbox item={selectedItem} presentation="artistPortfolio" onClose={() => setSelectedItem(null)} />
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
  return youtubeUrl ? { type: 'youtube', url: withAutoplay(youtubeUrl) } : { type: 'video', url }
}

function withAutoplay(value: string) {
  const url = new URL(value)
  url.searchParams.set('autoplay', '1')
  url.searchParams.set('mute', '1')
  url.searchParams.set('playsinline', '1')
  return url.toString()
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
