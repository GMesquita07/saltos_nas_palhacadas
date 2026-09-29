import { Link } from 'react-router-dom'
import type { Profile } from '../../types/profile'
import { ArtistProfileImage } from '../../components/ArtistProfileImage'
import { artistInitials } from '../../components/artistProfileImage'
import { NavIcon } from '../../components/NavIcon/NavIcon'
import { profilePath } from '../../navigation/routes'
import type { ImageLoadingPolicy } from '../../performance/performanceConfig'
import styles from './ProfileCard.module.css'

export function ProfileCard({ imageLoading, profile }: { imageLoading?: ImageLoadingPolicy; profile: Profile }) {
  const imagePosition = profile.imagePosition ?? '50% 50%'
  const imageZoom = profile.imageZoom ?? 1

  return (
    <Link className={styles.card} to={profilePath(profile.slug)}>
      <span className={styles.portrait}>
        <ArtistProfileImage
          alt={'Foto de perfil de ' + profile.name}
          decoding={imageLoading?.decoding}
          fallback={artistInitials(profile.name)}
          fetchPriority={imageLoading?.fetchPriority}
          loading={imageLoading?.loading}
          position={imagePosition}
          src={profile.imageUrl}
          variant="card"
          zoom={imageZoom}
        />
      </span>
      <span className={styles.details}>
        <small>{profile.role}</small>
        <strong>{profile.name}</strong>
        <span>Ver portfólio <b><NavIcon name="arrow-right" /></b></span>
      </span>
    </Link>
  )
}
