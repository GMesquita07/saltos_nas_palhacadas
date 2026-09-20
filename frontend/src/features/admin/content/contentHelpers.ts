import type { AdminPortfolioItem } from '../../../types/portfolio'

export function portfolioPublicationLabel(published: boolean) {
  return published ? 'Publicado' : 'Oculto'
}

export function togglePortfolioPublication(item: Pick<AdminPortfolioItem, 'published'>) {
  return !item.published
}

export function isChronologicalPortfolioOrder(items: Pick<AdminPortfolioItem, 'id' | 'eventDateIso'>[]) {
  return items.every((item, index) => {
    const next = items[index + 1]
    if (!next) return true

    const dateOrder = item.eventDateIso.localeCompare(next.eventDateIso)
    if (dateOrder !== 0) return dateOrder > 0

    return Number(item.id) >= Number(next.id)
  })
}

export function portfolioItemToSaveInput(item: AdminPortfolioItem, published = item.published) {
  return {
    type: item.type === 'Vídeo' ? 'VIDEO' as const : 'PHOTO' as const,
    title: item.title,
    location: item.location,
    eventDate: item.eventDateIso,
    mediaUrl: item.mediaUrl,
    thumbnailUrl: item.thumbnailUrl ?? null,
    published,
  }
}
