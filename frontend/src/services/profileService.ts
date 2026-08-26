import { apiClient } from './apiClient'
import type { Profile } from '../types/profile'

type ApiProfile = { id: number; slug: string; name: string; role: string; description: string; profileImageUrl: string | null; profileImagePosition: string | null; profileImageZoom: number | null; featuredVideoUrl: string | null; displayOrder?: number | null }

export async function getProfiles(): Promise<Profile[]> {
  const profiles = await apiClient<ApiProfile[]>('/profiles', { cache: 'no-store' })
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
    displayOrder: profile.displayOrder ?? 0,
  }))
}
