export type ClientContentMediaType = 'PHOTO' | 'VIDEO'
export type ClientContentDisplayType = 'Foto' | 'Vídeo'
export type ClientContentStatus = 'PENDING' | 'APPROVED' | 'REJECTED'

export type ClientContentPost = {
  id: string
  type: ClientContentDisplayType
  mediaType: ClientContentMediaType
  title: string
  location: string
  eventDate: string
  eventDateIso: string
  caption?: string
  mediaUrl: string
  thumbnailUrl?: string
  status: ClientContentStatus
  createdAt?: string
  moderatedAt?: string
  adminMessage?: string
  profileSlug?: string
  profileName?: string
  submittedByName: string
  submittedByEmail?: string
}

export type SubmitClientContentInput = {
  profileSlug: string
  type: ClientContentMediaType
  title: string
  location: string
  eventDate: string
  caption: string
  mediaUrl: string
  thumbnailUrl: string | null
}
