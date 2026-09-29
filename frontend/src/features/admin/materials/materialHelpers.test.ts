import assert from 'node:assert/strict'
import test from 'node:test'

import { materialToEditPayload } from './materialHelpers.ts'

test('material edit payload preserves crop fields and trims text fields', () => {
  assert.deepEqual(materialToEditPayload({
    name: '  Máquina de fumo  ',
    imageUrl: '  https://example.test/fumo.jpg  ',
    imagePosition: '25% 75%',
    imageZoom: 1.42,
  }), {
    name: 'Máquina de fumo',
    imageUrl: 'https://example.test/fumo.jpg',
    imagePosition: '25% 75%',
    imageZoom: 1.42,
  })
})

test('material edit payload defaults missing crop values', () => {
  assert.deepEqual(materialToEditPayload({
    name: 'Tripé',
    imageUrl: 'https://example.test/tripe.jpg',
    imagePosition: '',
    imageZoom: undefined as unknown as number,
  }), {
    name: 'Tripé',
    imageUrl: 'https://example.test/tripe.jpg',
    imagePosition: '50% 50%',
    imageZoom: 1,
  })
})
