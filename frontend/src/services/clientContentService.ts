import { apiClient } from './apiClient'
import type { ClientContentMediaType, ClientContentPost, ClientContentPublicIdentity, ClientContentStatus, SubmitClientContentInput } from '../types/clientContent'

type ApiClientContentPost = {
  id: number | string
  type: ClientContentMediaType
  title: string
  location?: string | null
  eventDate?: string | null
  eventMonth?: string | null
  caption?: string | null
  mediaUrl: string
  thumbnailUrl?: string | null
  status: ClientContentStatus
  createdAt?: string | null
  moderatedAt?: string | null
  adminMessage?: string | null
  profileSlug?: string | null
  profileName?: string | null
  submittedByName?: string | null
  submittedByEmail?: string | null
  publicDisplayName?: string | null
  publicIdentity?: ClientContentPublicIdentity | null
  showLocation?: boolean | null
  showEventDate?: boolean | null
  consentVersion?: string | null
  consentedAt?: string | null
}

export function getPublishedClientContent(type?: ClientContentMediaType): Promise<ClientContentPost[]> {
  const query = type ? `?type=${encodeURIComponent(type)}` : ''
  return apiClient<ApiClientContentPost[]>(`/client-posts${query}`).then(mapClientContentPosts)
}

export function getMyClientContent(token: string): Promise<ClientContentPost[]> {
  return apiClient<ApiClientContentPost[]>('/client-posts/mine', {}, token).then(mapClientContentPosts)
}

export function submitClientContent(input: SubmitClientContentInput, token: string): Promise<ClientContentPost> {
  return apiClient<ApiClientContentPost>('/client-posts', {
    method: 'POST',
    body: JSON.stringify(input),
  }, token).then(mapClientContentPost)
}

export function getAdminClientContent(token: string): Promise<ClientContentPost[]> {
  return apiClient<ApiClientContentPost[]>('/admin/client-posts', {}, token).then(mapClientContentPosts)
}

export function moderateClientContent(id: string, input: { status: ClientContentStatus; adminMessage?: string }, token: string): Promise<ClientContentPost> {
  return apiClient<ApiClientContentPost>(`/admin/client-posts/${encodeURIComponent(id)}`, {
    method: 'PUT',
    body: JSON.stringify(input),
  }, token).then(mapClientContentPost)
}

export function deleteClientContent(id: string, token: string): Promise<void> {
  return apiClient<void>(`/admin/client-posts/${encodeURIComponent(id)}`, { method: 'DELETE' }, token)
}

function mapClientContentPosts(posts: ApiClientContentPost[]) {
  return posts.map(mapClientContentPost).sort((first, second) => second.eventDateIso.localeCompare(first.eventDateIso) || second.id.localeCompare(first.id))
}

function mapClientContentPost(post: ApiClientContentPost): ClientContentPost {
  const eventDateIso = post.eventDate ?? (post.eventMonth ? `${post.eventMonth}-01` : post.createdAt?.slice(0, 10) ?? '')

  return {
    id: String(post.id),
    type: post.type === 'VIDEO' ? 'Vídeo' : 'Foto',
    mediaType: post.type,
    title: post.title,
    location: post.location ?? undefined,
    eventDate: formatVisibleEventDate(post.eventDate, post.eventMonth),
    eventDateIso,
    eventMonth: post.eventMonth ?? undefined,
    caption: post.caption ?? undefined,
    mediaUrl: post.mediaUrl,
    thumbnailUrl: post.thumbnailUrl ?? undefined,
    status: post.status,
    createdAt: post.createdAt ?? undefined,
    moderatedAt: post.moderatedAt ?? undefined,
    adminMessage: post.adminMessage ?? undefined,
    profileSlug: post.profileSlug ?? undefined,
    profileName: post.profileName ?? undefined,
    submittedByName: post.submittedByName ?? 'Cliente',
    submittedByEmail: post.submittedByEmail ?? undefined,
    publicDisplayName: post.publicDisplayName ?? undefined,
    showLocation: Boolean(post.showLocation),
    showEventDate: Boolean(post.showEventDate),
    consentVersion: post.consentVersion ?? undefined,
    consentedAt: post.consentedAt ?? undefined,
  }
}

function formatVisibleEventDate(eventDate?: string | null, eventMonth?: string | null) {
  if (eventDate) {
    return new Intl.DateTimeFormat('pt-PT', { dateStyle: 'long' }).format(new Date(`${eventDate}T00:00:00`))
  }
  if (eventMonth) {
    return new Intl.DateTimeFormat('pt-PT', { month: 'long', year: 'numeric' }).format(new Date(`${eventMonth}-01T00:00:00`))
  }
  return undefined
}
