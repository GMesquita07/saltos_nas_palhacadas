import type { Profile } from '../../types/profile'
import { CroppedImage } from '../../components/CroppedImage'
import styles from './ProfileCard.module.css'

export function ProfileCard({ profile, onSelect }: { profile: Profile; onSelect: (profile: Profile) => void }) {
  const imagePosition = profile.imagePosition ?? '50% 50%'
  const imageZoom = profile.imageZoom ?? 1

  return (
    <button className={styles.card} type="button" onClick={() => onSelect(profile)}>
      <span className={styles.portrait}>
        <CroppedImage
          className={styles.image}
          fallback={profile.name.split(' ').map((name) => name[0]).join('').slice(0, 2)}
          position={imagePosition}
          src={profile.imageUrl}
          zoom={imageZoom}
        />
      </span>
      <span className={styles.details}>
        <small>{profile.role}</small>
        <strong>{profile.name}</strong>
        <span>Ver portfólio <b>→</b></span>
      </span>
    </button>
  )
}
