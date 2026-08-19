import styles from './BrandMark.module.css'

type BrandMarkProps = { compact?: boolean }

export function BrandMark({ compact = false }: BrandMarkProps) {
  return (
    <span className={`${styles.brandMark} ${compact ? styles.compact : ''}`} aria-label="Saltos nas Palhaçadas">
      <span className={styles.symbol}>S</span>
      <span className={styles.text}><strong>Saltos</strong><small>nas palhaçadas</small></span>
    </span>
  )
}
