export type Review = {
  id: string
  profileSlug?: string
  profileName?: string
  reviewerName: string
  submittedByEmail?: string
  title: string
  comment: string
  rating: number
  reviewDate: string
  reviewDateIso: string
  displayOrder: number
  published: boolean
}
