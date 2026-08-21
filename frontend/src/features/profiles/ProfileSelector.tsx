import { ProfileCard } from './ProfileCard'
import styles from './ProfileSelector.module.css'
import type { Profile } from '../../types/profile'

type ProfileSelectorProps = { profiles: Profile[]; onProfileSelect: (profile: Profile) => void }

export function ProfileSelector({ profiles, onProfileSelect }: ProfileSelectorProps) {
  return <section className={styles.section}><div className={styles.intro}><p className="eyebrow">Portfólio</p><h1>Bem-vindo!</h1><p>Aqui, cada evento ganha ritmo, energia e personalidade. 🎧✨ Explora os perfis, descobre diferentes experiências e encontra a animação certa para transformar o teu momento numa memória inesquecível.</p></div>{profiles.length === 0 ? <p className={styles.empty}>Ainda não existem perfis publicados.</p> : <div className={styles.cards}>{profiles.map((profile) => <ProfileCard key={profile.id} profile={profile} onSelect={onProfileSelect} />)}</div>}</section>
}
