import { apiClient } from './apiClient'
import { markPortfolioItemsLoaded, shouldReloadPortfolioItems } from './portfolioInvalidation'
import type { PortfolioItem } from '../types/portfolio'

type PortfolioRequestOptions = {
  force?: boolean
}

export type ApiPortfolioItem = {
  id: number | string
  type: 'PHOTO' | 'VIDEO'
  title: string
  location: string
  eventDate: string
  mediaUrl: string
  thumbnailUrl: string | null
  thumbnailPosition: string | null
  thumbnailZoom: number | null
}

export async function getPortfolioItems(slug: string, options: PortfolioRequestOptions = {}): Promise<PortfolioItem[]> {
  const shouldReload = shouldReloadPortfolioItems(slug, options.force)
  const path = `/profiles/${encodeURIComponent(slug)}/portfolio${shouldReload ? `?refresh=${Date.now()}` : ''}`
  const items = await apiClient<ApiPortfolioItem[]>(
    path,
    shouldReload ? { cache: 'reload' } : {},
  )
  markPortfolioItemsLoaded(slug)
  return items.map(mapPortfolioItem).sort((first, second) => second.eventDateIso.localeCompare(first.eventDateIso) || second.id.localeCompare(first.id))
}

export function mapPortfolioItem(item: ApiPortfolioItem): PortfolioItem {
  return {
    id: String(item.id),
    type: item.type === 'PHOTO' ? 'Foto' : 'Vídeo',
    title: item.title,
    location: item.location,
    eventDate: new Intl.DateTimeFormat('pt-PT', { dateStyle: 'long' }).format(new Date(`${item.eventDate}T00:00:00`)),
    eventDateIso: item.eventDate,
    mediaUrl: item.mediaUrl,
    thumbnailUrl: item.thumbnailUrl ?? undefined,
    thumbnailPosition: item.thumbnailPosition ?? '50% 50%',
    thumbnailZoom: item.thumbnailZoom ?? 1,
  }
}
