import { apiClient } from './apiClient'
import type { Profile, ProfileSocialLink } from '../types/profile'

export type AdminManagedProfile = Profile & {
  notificationEmail?: string | null
}

export type ApiProfileResponse = {
  id: number
  slug: string
  name: string
  role: string
  description: string
  profileImageUrl: string | null
  profileImagePosition: string | null
  profileImageZoom: number | null
  featuredVideoUrl: string | null
  notificationEmail: string | null
  displayOrder: number | null
  socialLinks: ApiProfileSocialLinkResponse[] | null
}

export type ApiProfileSocialLinkResponse = {
  id: number
  platform: string
  label: string | null
  url: string
  displayOrder: number
}

export type ProfileSocialLinkInput = {
  platform: string
  label: string | null
  url: string
}

export type SaveProfileInput = {
  name: string
  role: string
  description: string
  notificationEmail: string | null
  profileImageUrl: string | null
  profileImagePosition: string
  profileImageZoom: number
  featuredVideoUrl: string | null
  socialLinks: ProfileSocialLinkInput[]
}

export type CreateProfileInput = SaveProfileInput & {
  slug: string
}

export async function getAdminProfiles(token: string) {
  return (await apiClient<ApiProfileResponse[]>('/admin/profiles', {}, token)).map(toProfile)
}

export async function createAdminProfile(input: CreateProfileInput, token: string) {
  return toProfile(await apiClient<ApiProfileResponse>('/admin/profiles', {
    method: 'POST',
    body: JSON.stringify(input),
  }, token))
}

export async function updateAdminProfile(slug: string, input: SaveProfileInput, token: string) {
  return toProfile(await apiClient<ApiProfileResponse>('/admin/profiles/' + encodeURIComponent(slug), {
    method: 'PUT',
    body: JSON.stringify(input),
  }, token))
}

export function deleteAdminProfile(slug: string, token: string) {
  return apiClient<void>('/admin/profiles/' + encodeURIComponent(slug), { method: 'DELETE' }, token)
}

export async function reorderAdminProfiles(profileSlugs: string[], token: string) {
  return (await apiClient<ApiProfileResponse[]>('/admin/profiles/order', {
    method: 'PUT',
    body: JSON.stringify({ profileSlugs }),
  }, token)).map(toProfile)
}

export function toProfile(profile: ApiProfileResponse): AdminManagedProfile {
  return {
    id: profile.slug,
    slug: profile.slug,
    name: profile.name,
    role: profile.role,
    description: profile.description,
    imageUrl: profile.profileImageUrl ?? undefined,
    imagePosition: profile.profileImagePosition ?? '50% 50%',
    imageZoom: profile.profileImageZoom ?? 1,
    featuredVideoUrl: profile.featuredVideoUrl ?? undefined,
    notificationEmail: profile.notificationEmail ?? '',
    displayOrder: profile.displayOrder ?? 0,
    socialLinks: mapSocialLinks(profile.socialLinks),
  }
}

function mapSocialLinks(links: ApiProfileSocialLinkResponse[] | null | undefined): ProfileSocialLink[] {
  return (links ?? []).map((link) => ({
    id: link.id,
    platform: link.platform,
    label: link.label,
    url: link.url,
    displayOrder: link.displayOrder,
  }))
}
