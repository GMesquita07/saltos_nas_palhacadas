import { ProfileCard } from './ProfileCard'
import styles from './ProfileSelector.module.css'
import type { Profile } from '../../types/profile'

type ProfileSelectorProps = { profiles: Profile[]; onProfileSelect: (profile: Profile) => void }

export function ProfileSelector({ profiles, onProfileSelect }: ProfileSelectorProps) {
  return <section className={styles.section}><div className={styles.intro}><p className="eyebrow">Portfólio</p><h1>Escolhe um perfil</h1><p>Descobre os eventos e momentos de cada artista.</p></div>{profiles.length === 0 ? <p className={styles.empty}>Ainda não existem perfis publicados.</p> : <div className={styles.cards}>{profiles.map((profile) => <ProfileCard key={profile.id} profile={profile} onSelect={onProfileSelect} />)}</div>}</section>
}
