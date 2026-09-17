import type { Review } from '../../types/review'

export function nextReviewPublished(review: Pick<Review, 'published'>) {
  return !review.published
}

export function reviewVisibilityLabel(published: boolean) {
  return published ? 'Pública' : 'Oculta'
}

export function reviewVisibilityNotice(published: boolean) {
  return published ? 'Avaliação publicada.' : 'Avaliação ocultada.'
}

export function updateReviewPublished(reviews: Review[], reviewId: string, published: boolean) {
  return reviews.map((review) => (
    review.id === reviewId ? { ...review, published } : review
  ))
}

export function replaceReview(reviews: Review[], updatedReview: Review) {
  return reviews.map((review) => (
    review.id === updatedReview.id ? updatedReview : review
  ))
}
