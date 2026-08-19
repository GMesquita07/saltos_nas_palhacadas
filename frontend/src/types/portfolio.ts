export type PortfolioItemType = 'Foto' | 'Vídeo'

export type PortfolioItem = {
  id: string
  type: PortfolioItemType
  title: string
  location: string
  eventDate: string
  imageUrl?: string
}
