import { BrandMark } from '../../components/BrandMark/BrandMark'
import styles from './SplashScreen.module.css'

export function SplashScreen({ isVisible }: { isVisible: boolean }) {
  return <div className={`${styles.splash} ${isVisible ? '' : styles.hidden}`} aria-hidden={!isVisible}><BrandMark /></div>
}
