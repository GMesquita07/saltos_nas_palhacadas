import assert from 'node:assert/strict'
import test from 'node:test'

import {
  nextReviewPublished,
  replaceReview,
  reviewVisibilityLabel,
  reviewVisibilityNotice,
  updateReviewPublished,
} from './reviewModeration.ts'
import type { Review } from '../../types/review'

const publicReview = review('1', true)
const hiddenReview = review('2', false)

test('review visibility labels and notices use the current publication state', () => {
  assert.equal(reviewVisibilityLabel(true), 'Pública')
  assert.equal(reviewVisibilityLabel(false), 'Oculta')
  assert.equal(reviewVisibilityNotice(true), 'Avaliação publicada.')
  assert.equal(reviewVisibilityNotice(false), 'Avaliação ocultada.')
})

test('review visibility toggles to the opposite boolean', () => {
  assert.equal(nextReviewPublished(publicReview), false)
  assert.equal(nextReviewPublished(hiddenReview), true)
})

test('review optimistic update changes only the targeted review', () => {
  const updated = updateReviewPublished([publicReview, hiddenReview], hiddenReview.id, true)

  assert.equal(updated[0].published, true)
  assert.equal(updated[1].published, true)
})

test('review rollback can restore the previous published state', () => {
  const optimistic = updateReviewPublished([publicReview], publicReview.id, false)
  const rolledBack = updateReviewPublished(optimistic, publicReview.id, true)

  assert.equal(rolledBack[0].published, true)
})

test('review API response replaces the optimistic local value', () => {
  const apiReview = { ...hiddenReview, title: 'Updated by API', published: true }
  const replaced = replaceReview([publicReview, hiddenReview], apiReview)

  assert.equal(replaced[1].title, 'Updated by API')
  assert.equal(replaced[1].published, true)
})

function review(id: string, published: boolean): Review {
  return {
    id,
    profileSlug: 'artist',
    profileName: 'Artist',
    reviewerName: 'Cliente',
    title: 'Título',
    comment: 'Comentário',
    rating: 5,
    reviewDate: '01/01/2026',
    reviewDateIso: '2026-01-01',
    displayOrder: 0,
    published,
  }
}
