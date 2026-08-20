import { apiClient } from './apiClient'
import type { PortfolioItem } from '../types/portfolio'

type ApiPortfolioItem = { id: number; type: 'PHOTO' | 'VIDEO'; title: string; location: string; eventDate: string; mediaUrl: string; thumbnailUrl: string | null }

export async function getPortfolioItems(slug: string): Promise<PortfolioItem[]> {
  const items = await apiClient<ApiPortfolioItem[]>(`/profiles/${slug}/portfolio`)
  return items.map((item) => ({ id: String(item.id), type: item.type === 'PHOTO' ? 'Foto' : 'Vídeo', title: item.title, location: item.location, eventDate: new Intl.DateTimeFormat('pt-PT', { dateStyle: 'long' }).format(new Date(`${item.eventDate}T00:00:00`)), mediaUrl: item.mediaUrl, thumbnailUrl: item.thumbnailUrl ?? undefined }))
}
