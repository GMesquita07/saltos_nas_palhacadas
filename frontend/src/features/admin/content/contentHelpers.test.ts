import assert from 'node:assert/strict'
import test from 'node:test'

import {
  isChronologicalPortfolioOrder,
  portfolioItemToSaveInput,
  portfolioPublicationLabel,
  togglePortfolioPublication,
} from './contentHelpers.ts'
import type { AdminPortfolioItem } from '../../../types/portfolio'

test('portfolio publication labels are explicit', () => {
  assert.equal(portfolioPublicationLabel(true), 'Publicado')
  assert.equal(portfolioPublicationLabel(false), 'Oculto')
})

test('portfolio publication toggles the current state', () => {
  assert.equal(togglePortfolioPublication(item('10', '2026-06-01', true)), false)
  assert.equal(togglePortfolioPublication(item('11', '2026-06-01', false)), true)
})

test('chronological portfolio order expects eventDate DESC and id DESC fallback', () => {
  assert.equal(isChronologicalPortfolioOrder([
    item('12', '2026-06-02', true),
    item('11', '2026-06-01', true),
    item('10', '2026-06-01', true),
  ]), true)

  assert.equal(isChronologicalPortfolioOrder([
    item('10', '2026-06-01', true),
    item('11', '2026-06-01', true),
  ]), false)
})

test('portfolio edit payload preserves fields and can override publication state', () => {
  assert.deepEqual(portfolioItemToSaveInput(item('8', '2026-05-01', true), false), {
    type: 'PHOTO',
    title: 'Atuação',
    location: 'Lisboa',
    eventDate: '2026-05-01',
    mediaUrl: 'https://example.test/media.jpg',
    thumbnailUrl: null,
    published: false,
  })
})

function item(id: string, eventDateIso: string, published: boolean): AdminPortfolioItem {
  return {
    id,
    type: 'Foto',
    title: 'Atuação',
    location: 'Lisboa',
    eventDate: '1 de maio de 2026',
    eventDateIso,
    mediaUrl: 'https://example.test/media.jpg',
    displayOrder: 0,
    published,
  }
}
