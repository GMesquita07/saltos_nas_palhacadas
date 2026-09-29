export type PortfolioItemType = 'Foto' | 'Vídeo'
export type MediaType = 'PHOTO' | 'VIDEO'

export type PortfolioItem = {
  id: string
  type: PortfolioItemType
  title: string
  location: string
  eventDate: string
  eventDateIso: string
  mediaUrl: string
  thumbnailUrl?: string
  thumbnailPosition?: string
  thumbnailZoom?: number
}

export type AdminPortfolioItem = PortfolioItem & {
  displayOrder: number
  published: boolean
}
