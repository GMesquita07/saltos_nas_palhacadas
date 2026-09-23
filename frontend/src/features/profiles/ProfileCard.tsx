import { Link } from 'react-router-dom'
import type { Profile } from '../../types/profile'
import { CroppedImage } from '../../components/CroppedImage'
import { NavIcon } from '../../components/NavIcon/NavIcon'
import { profilePath } from '../../navigation/routes'
import styles from './ProfileCard.module.css'

export function ProfileCard({ profile }: { profile: Profile }) {
  const imagePosition = profile.imagePosition ?? '50% 50%'
  const imageZoom = profile.imageZoom ?? 1

  return (
    <Link className={styles.card} to={profilePath(profile.slug)}>
      <span className={styles.portrait}>
        <CroppedImage
          alt={'Foto de perfil de ' + profile.name}
          className={styles.image}
          shape="circle"
          fallback={profile.name.split(' ').map((name) => name[0]).join('').slice(0, 2)}
          position={imagePosition}
          src={profile.imageUrl}
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
