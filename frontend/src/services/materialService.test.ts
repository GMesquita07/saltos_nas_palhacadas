import assert from 'node:assert/strict'
import test from 'node:test'

import { mapMaterial } from './materialMapper.ts'

test('maps material crop fields from API responses', () => {
  assert.deepEqual(mapMaterial({
    id: 1,
    name: 'Máquina de fumo',
    imageUrl: 'https://example.test/fumo.jpg',
    imagePosition: '20% 80%',
    imageZoom: 1.35,
    displayOrder: 2,
  }), {
    id: 1,
    name: 'Máquina de fumo',
    imageUrl: 'https://example.test/fumo.jpg',
    imagePosition: '20% 80%',
    imageZoom: 1.35,
    displayOrder: 2,
  })
})

test('defaults missing material crop fields from older API responses', () => {
  assert.deepEqual(mapMaterial({
    id: 2,
    name: 'Coluna',
    imageUrl: 'https://example.test/coluna.jpg',
    displayOrder: 3,
  }), {
    id: 2,
    name: 'Coluna',
    imageUrl: 'https://example.test/coluna.jpg',
    imagePosition: '50% 50%',
    imageZoom: 1,
    displayOrder: 3,
  })
})
