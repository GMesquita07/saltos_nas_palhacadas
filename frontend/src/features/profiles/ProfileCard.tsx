import type { Profile } from '../../types/profile'
import styles from './ProfileCard.module.css'

export function ProfileCard({ profile, onSelect }: { profile: Profile; onSelect: (profile: Profile) => void }) {
  return <button className={styles.card} type="button" onClick={() => onSelect(profile)}><span className={styles.image}>{profile.imageUrl ? <img src={profile.imageUrl} alt="" /> : <span>{profile.name.split(' ').map((name) => name[0]).join('').slice(0, 2)}</span>}</span><span className={styles.details}><small>{profile.role}</small><strong>{profile.name}</strong><span>Ver portfólio <b>→</b></span></span></button>
}
