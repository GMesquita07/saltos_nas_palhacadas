import { apiClient } from './apiClient'
import { createPublicRequestCache } from './publicRequestCache'
import type { Profile, ProfileSocialLink } from '../types/profile'

type ApiProfile = { id: number; slug: string; name: string; role: string; description: string; profileImageUrl: string | null; profileImagePosition: string | null; profileImageZoom: number | null; featuredVideoUrl: string | null; heroBackgroundImageUrl: string | null; displayOrder?: number | null; socialLinks?: ApiProfileSocialLink[] | null }
type ApiProfileSocialLink = { id: number; platform: string; label: string | null; url: string; displayOrder: number }

const profileRequestCache = createPublicRequestCache<Profile[]>(loadProfiles)

export async function getProfiles(options: { force?: boolean } = {}): Promise<Profile[]> {
  return profileRequestCache.get(options)
}

export function invalidateProfilesCache() {
  profileRequestCache.invalidate()
}

async function loadProfiles(requestOptions: RequestInit = {}): Promise<Profile[]> {
  const profiles = await apiClient<ApiProfile[]>('/profiles', requestOptions)
  return profiles.map((profile) => ({
    id: profile.slug,
    slug: profile.slug,
    name: profile.name,
    role: profile.role,
    description: profile.description,
    imageUrl: profile.profileImageUrl ?? undefined,
    imagePosition: profile.profileImagePosition ?? '50% 50%',
    imageZoom: profile.profileImageZoom ?? 1,
    featuredVideoUrl: profile.featuredVideoUrl ?? undefined,
    heroBackgroundImageUrl: profile.heroBackgroundImageUrl ?? undefined,
    displayOrder: profile.displayOrder ?? 0,
    socialLinks: mapSocialLinks(profile.socialLinks),
  }))
}

function mapSocialLinks(links: ApiProfileSocialLink[] | null | undefined): ProfileSocialLink[] {
  return (links ?? []).map((link) => ({
    id: link.id,
    platform: link.platform,
    label: link.label,
    url: link.url,
    displayOrder: link.displayOrder,
  }))
}
