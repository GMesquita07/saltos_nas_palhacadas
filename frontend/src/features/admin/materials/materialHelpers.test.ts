import assert from 'node:assert/strict'
import test from 'node:test'

import { materialToEditPayload } from './materialHelpers.ts'

test('material edit payload contains only editable fields', () => {
  assert.deepEqual(materialToEditPayload({
    name: '  Máquina de fumo  ',
    imageUrl: '  https://example.test/fumo.jpg  ',
  }), {
    name: 'Máquina de fumo',
    imageUrl: 'https://example.test/fumo.jpg',
  })
})
