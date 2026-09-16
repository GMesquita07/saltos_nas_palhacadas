export type ClientContentMediaType = 'PHOTO' | 'VIDEO'
export type ClientContentDisplayType = 'Foto' | 'Vídeo'
export type ClientContentStatus = 'PENDING' | 'APPROVED' | 'REJECTED'
export type ClientContentPublicIdentity = 'ANONYMOUS' | 'USERNAME' | 'CUSTOM'

export type ClientContentPost = {
  id: string
  type: ClientContentDisplayType
  mediaType: ClientContentMediaType
  title: string
  location?: string
  eventDate?: string
  eventDateIso: string
  eventMonth?: string
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
  publicDisplayName?: string
  showLocation: boolean
  showEventDate: boolean
  consentVersion?: string
  consentedAt?: string
}

export type SubmitClientContentInput = {
  profileSlug: string
  type: ClientContentMediaType
  mediaId: string
  thumbnailId: string | null
  title: string
  location: string
  eventDate: string
  caption: string
  publicIdentity: ClientContentPublicIdentity
  customDisplayName: string | null
  showLocation: boolean
  showEventDate: boolean
  consentToPublish: boolean
}
