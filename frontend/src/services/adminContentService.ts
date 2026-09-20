import { apiClient } from './apiClient'
import { mapPortfolioItem, type ApiPortfolioItem } from './portfolioService'
import type { AdminPortfolioItem, MediaType } from '../types/portfolio'

type ApiAdminPortfolioItem = ApiPortfolioItem & {
  displayOrder: number
  published: boolean
}

export type SavePortfolioItemInput = {
  type: MediaType
  title: string
  location: string
  eventDate: string
  mediaUrl: string
  thumbnailUrl: string | null
  published: boolean
}

export async function getAdminPortfolioItems(slug: string, token: string): Promise<AdminPortfolioItem[]> {
  const items = await apiClient<ApiAdminPortfolioItem[]>(`/admin/profiles/${encodeURIComponent(slug)}/portfolio`, { cache: 'no-store' }, token)
  return items.map(mapAdminPortfolioItem)
}

export async function createAdminPortfolioItem(slug: string, input: SavePortfolioItemInput, token: string) {
  return mapAdminPortfolioItem(await apiClient<ApiAdminPortfolioItem>(`/admin/profiles/${encodeURIComponent(slug)}/portfolio`, {
    method: 'POST',
    body: JSON.stringify(input),
  }, token))
}

export async function updateAdminPortfolioItem(slug: string, itemId: string, input: SavePortfolioItemInput, token: string) {
  return mapAdminPortfolioItem(await apiClient<ApiAdminPortfolioItem>(`/admin/profiles/${encodeURIComponent(slug)}/portfolio/${encodeURIComponent(itemId)}`, {
    method: 'PUT',
    body: JSON.stringify(input),
  }, token))
}

export function deleteAdminPortfolioItem(slug: string, itemId: string, token: string) {
  return apiClient<void>(`/admin/profiles/${encodeURIComponent(slug)}/portfolio/${encodeURIComponent(itemId)}`, {
    method: 'DELETE',
  }, token)
}

function mapAdminPortfolioItem(item: ApiAdminPortfolioItem): AdminPortfolioItem {
  return {
    ...mapPortfolioItem(item),
    displayOrder: item.displayOrder,
    published: item.published,
  }
}
