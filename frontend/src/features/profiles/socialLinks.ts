import type { ProfileSocialLink } from '../../types/profile'
import type { SocialIconName } from '../../components/SocialIcon/SocialIcon'

export const socialPlatformOptions = [
  'INSTAGRAM',
  'FACEBOOK',
  'TIKTOK',
  'YOUTUBE',
  'SPOTIFY',
  'EMAIL',
  'WEBSITE',
  'OTHER',
] as const

const platformLabels: Record<string, string> = {
  INSTAGRAM: 'Instagram',
  FACEBOOK: 'Facebook',
  TIKTOK: 'TikTok',
  YOUTUBE: 'YouTube',
  SPOTIFY: 'Spotify',
  EMAIL: 'Email',
  WEBSITE: 'Website',
  OTHER: 'Outro',
}

const platformIcons: Record<string, SocialIconName> = {
  INSTAGRAM: 'instagram',
  FACEBOOK: 'facebook',
  TIKTOK: 'tiktok',
  YOUTUBE: 'youtube',
  SPOTIFY: 'spotify',
  EMAIL: 'email',
  WEBSITE: 'website',
  OTHER: 'link',
}

export function socialPlatformLabel(platform: string) {
  const normalized = normalizePlatform(platform)
  return platformLabels[normalized] ?? humanizePlatform(normalized)
}

export function socialPlatformIcon(platform: string): SocialIconName {
  const normalized = normalizePlatform(platform)
  return platformIcons[normalized] ?? 'link'
}

export function socialLinkLabel(link: Pick<ProfileSocialLink, 'platform' | 'label'>) {
  return link.label?.trim() || socialPlatformLabel(link.platform)
}

export function socialLinkHref(link: Pick<ProfileSocialLink, 'platform' | 'url'>) {
  return normalizePlatform(link.platform) === 'EMAIL' ? `mailto:${link.url.trim()}` : link.url.trim()
}

export function isExternalSocialLink(link: Pick<ProfileSocialLink, 'platform'>) {
  return normalizePlatform(link.platform) !== 'EMAIL'
}

export function validateSocialLink(platform: string, url: string) {
  const normalizedPlatform = normalizePlatform(platform)
  const value = url.trim()
  if (!normalizedPlatform) return 'Seleciona a plataforma do link.'
  if (!value) return 'Preenche o URL ou email do link.'

  if (normalizedPlatform === 'EMAIL') {
    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value) ? null : 'Indica um email válido para este link.'
  }

  return isHttpUrl(value) ? null : 'Indica um URL que comece por http:// ou https://.'
}

function normalizePlatform(platform: string) {
  return platform.trim().toUpperCase()
}

function humanizePlatform(platform: string) {
  return platform
    .toLowerCase()
    .split(/[_-]+/)
    .filter(Boolean)
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(' ') || 'Link'
}

function isHttpUrl(value: string) {
  try {
    const url = new URL(value)
    return (url.protocol === 'http:' || url.protocol === 'https:') && Boolean(url.hostname)
  } catch {
    return false
  }
}
