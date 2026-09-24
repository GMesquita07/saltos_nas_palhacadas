import { useCallback, useEffect, useRef, useState, type CSSProperties } from 'react'
import styles from './SplashScreen.module.css'

type SplashPhase = 'playing' | 'docking' | 'done'

type SplashScreenProps = {
  phase: SplashPhase
  onDockingEnd: () => void
  onDockingStart: () => void
}

type DockStyle = CSSProperties & {
  '--dock-end-height': string
  '--dock-end-left': string
  '--dock-end-top': string
  '--dock-end-width': string
  '--dock-start-height': string
  '--dock-start-left': string
  '--dock-start-top': string
  '--dock-start-width': string
}

const initialFallbackMs = 4200

export function SplashScreen({ phase, onDockingEnd, onDockingStart }: SplashScreenProps) {
  const hasStartedDocking = useRef(false)
  const [dockStyle, setDockStyle] = useState<DockStyle | null>(null)
  const [fallbackMs, setFallbackMs] = useState(initialFallbackMs)

  const startDocking = useCallback(() => {
    if (hasStartedDocking.current || phase !== 'playing') return
    hasStartedDocking.current = true

    if (prefersReducedMotion()) {
      onDockingEnd()
      return
    }

    const target = document.querySelector<HTMLElement>('[data-splash-logo-target="true"]')
    const targetRect = target?.getBoundingClientRect()

    if (!targetRect || targetRect.width === 0 || targetRect.height === 0) {
      onDockingStart()
      window.setTimeout(onDockingEnd, 120)
      return
    }

    const startWidth = Math.min(Math.max(window.innerWidth * 0.46, 190), 360)
    const startHeight = startWidth

    setDockStyle({
      '--dock-end-height': `${targetRect.height}px`,
      '--dock-end-left': `${targetRect.left}px`,
      '--dock-end-top': `${targetRect.top}px`,
      '--dock-end-width': `${targetRect.width}px`,
      '--dock-start-height': `${startHeight}px`,
      '--dock-start-left': `${(window.innerWidth - startWidth) / 2}px`,
      '--dock-start-top': `${(window.innerHeight - startHeight) / 2}px`,
      '--dock-start-width': `${startWidth}px`,
    })
    onDockingStart()
  }, [onDockingEnd, onDockingStart, phase])

  useEffect(() => {
    if (phase !== 'playing') return undefined

    const timer = window.setTimeout(startDocking, fallbackMs)
    return () => window.clearTimeout(timer)
  }, [fallbackMs, phase, startDocking])

  if (phase === 'done') return null

  return (
    <div
      className={`${styles.splash} ${phase === 'docking' ? styles.docking : ''}`}
      aria-hidden="true"
    >
      <video
        className={styles.logoVideo}
        autoPlay
        muted
        playsInline
        preload="auto"
        onEnded={startDocking}
        onError={startDocking}
        onLoadedMetadata={(event) => {
          const duration = event.currentTarget.duration
          if (Number.isFinite(duration) && duration > 0) {
            setFallbackMs(Math.ceil(duration * 1000) + 650)
          }
        }}
      >
        <source src="/splash-logo.mp4" type="video/mp4" />
      </video>
      {phase === 'docking' && dockStyle && (
        <img
          alt=""
          className={styles.dockLogo}
          decoding="async"
          fetchPriority="high"
          height={384}
          src="/saltos-logo-dock.webp"
          style={dockStyle}
          width={384}
          onAnimationEnd={onDockingEnd}
        />
      )}
    </div>
  )
}

function prefersReducedMotion() {
  return typeof window !== 'undefined' && window.matchMedia('(prefers-reduced-motion: reduce)').matches
}
