export type ProfileSocialLink = {
  id?: number | string
  platform: string
  label?: string | null
  url: string
  displayOrder?: number
}

export type Profile = {
  id: string
  slug: string
  name: string
  role: string
  description: string
  imageUrl?: string
  imagePosition?: string
  imageZoom?: number
  featuredVideoUrl?: string
  heroBackgroundImageUrl?: string
  displayOrder?: number
  socialLinks: ProfileSocialLink[]
}
