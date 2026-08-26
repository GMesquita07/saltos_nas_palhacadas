import { apiClient } from './apiClient'
import type { ClientContentMediaType, ClientContentPost, ClientContentStatus, SubmitClientContentInput } from '../types/clientContent'

type ApiClientContentPost = {
  id: number | string
  type: ClientContentMediaType
  title: string
  location: string
  eventDate: string
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
  return {
    id: String(post.id),
    type: post.type === 'VIDEO' ? 'Vídeo' : 'Foto',
    mediaType: post.type,
    title: post.title,
    location: post.location,
    eventDate: new Intl.DateTimeFormat('pt-PT', { dateStyle: 'long' }).format(new Date(`${post.eventDate}T00:00:00`)),
    eventDateIso: post.eventDate,
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
  }
}
