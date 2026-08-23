import { apiClient } from './apiClient'
import type { Review } from '../types/review'

type ApiReview = {
  id: number
  profileSlug: string | null
  profileName: string | null
  reviewerName: string
  submittedByEmail: string | null
  title: string
  comment: string
  rating: number
  reviewDate: string
  displayOrder: number
  published: boolean
}

const dateFormatter = new Intl.DateTimeFormat('pt-PT')

export type SubmitReviewInput = {
  reviewerName: string
  title: string
  comment: string
  rating: number
}

export async function getProfileReviews(profileSlug: string): Promise<Review[]> {
  const reviews = await apiClient<ApiReview[]>('/profiles/' + encodeURIComponent(profileSlug) + '/reviews', { cache: 'no-store' })
  return reviews.map(toReview)
}

export async function submitProfileReview(profileSlug: string, input: SubmitReviewInput, token: string): Promise<Review> {
  const review = await apiClient<ApiReview>('/profiles/' + encodeURIComponent(profileSlug) + '/reviews', {
    method: 'POST',
    body: JSON.stringify(input),
  }, token)
  return toReview(review)
}

export async function getAdminReviews(token: string): Promise<Review[]> {
  const reviews = await apiClient<ApiReview[]>('/admin/reviews', { cache: 'no-store' }, token)
  return reviews.map(toReview)
}

export async function moderateReview(reviewId: string, input: { published: boolean }, token: string): Promise<Review> {
  const review = await apiClient<ApiReview>('/admin/reviews/' + encodeURIComponent(reviewId), {
    method: 'PUT',
    body: JSON.stringify(input),
  }, token)
  return toReview(review)
}

function toReview(review: ApiReview): Review {
  return {
    id: String(review.id),
    profileSlug: review.profileSlug ?? undefined,
    profileName: review.profileName ?? undefined,
    reviewerName: review.reviewerName,
    submittedByEmail: review.submittedByEmail ?? undefined,
    title: review.title,
    comment: review.comment,
    rating: review.rating,
    reviewDate: formatDate(review.reviewDate),
    reviewDateIso: review.reviewDate,
    displayOrder: review.displayOrder,
    published: review.published,
  }
}

function formatDate(value: string) {
  const date = new Date(value + 'T00:00:00')
  return Number.isNaN(date.getTime()) ? value : dateFormatter.format(date)
}
