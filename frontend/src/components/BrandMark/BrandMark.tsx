import styles from './BrandMark.module.css'

type BrandMarkProps = {
  compact?: boolean
  dockTarget?: boolean
  isHidden?: boolean
  variant?: 'round' | 'square'
}

export function BrandMark({ compact = false, dockTarget = false, isHidden = false, variant = 'round' }: BrandMarkProps) {
  return (
    <span
      className={`${styles.brandMark} ${variant === 'square' ? styles.square : ''} ${compact ? styles.compact : ''} ${isHidden ? styles.hidden : ''}`}
      data-splash-logo-target={dockTarget ? 'true' : undefined}
    >
      <img
        alt="Saltos nas Palhaçadas"
        className={styles.logo}
        decoding="async"
        height={192}
        src={variant === 'square' ? '/saltos-logo-square-192.webp' : '/saltos-logo-round-192.webp'}
        width={192}
      />
    </span>
  )
}
