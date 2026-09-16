import { apiClient } from './apiClient'
import { mapPortfolioItem, type ApiPortfolioItem } from './portfolioService'
import type { Favorite } from '../types/favorite'

type ApiFavorite = {
  portfolioItemId: number | string
  profileSlug: string
  item: ApiPortfolioItem
}

export async function getFavorites(token: string): Promise<Favorite[]> {
  const response = await apiClient<ApiFavorite[]>('/favorites', {}, token)
  return response.map((favorite) => ({
    portfolioItemId: String(favorite.portfolioItemId),
    profileSlug: favorite.profileSlug,
    item: mapPortfolioItem(favorite.item),
  }))
}

export function addFavorite(portfolioItemId: string, token: string): Promise<void> {
  return apiClient<void>(`/favorites/${encodeURIComponent(portfolioItemId)}`, { method: 'POST' }, token)
}

export function removeFavorite(portfolioItemId: string, token: string): Promise<void> {
  return apiClient<void>(`/favorites/${encodeURIComponent(portfolioItemId)}`, { method: 'DELETE' }, token)
}
