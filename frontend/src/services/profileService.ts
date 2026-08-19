import { apiClient } from './apiClient'
import type { Profile } from '../types/profile'

type ApiProfile = { id: number; slug: string; name: string; role: string; description: string; profileImageUrl: string | null }

export async function getProfiles(): Promise<Profile[]> {
  const profiles = await apiClient<ApiProfile[]>('/profiles')
  return profiles.map((profile) => ({ id: profile.slug, slug: profile.slug, name: profile.name, role: profile.role, description: profile.description, imageUrl: profile.profileImageUrl ?? undefined }))
}
