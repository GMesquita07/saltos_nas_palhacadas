export type PortfolioItemType = 'Foto' | 'Vídeo'

export type PortfolioItem = {
  id: string
  type: PortfolioItemType
  title: string
  location: string
  eventDate: string
  eventDateIso: string
  mediaUrl: string
  thumbnailUrl?: string
}
