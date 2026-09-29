import assert from 'node:assert/strict'
import test from 'node:test'

import { defaultImageCrop, dragImageCrop, formatImagePosition, imageCropStyle, parseImageCrop } from './imageCrop.ts'

test('uses the same crop anchor for object-position and transform-origin', () => {
  assert.deepEqual(imageCropStyle('50% 100%', 1.14), {
    objectFit: 'cover',
    objectPosition: '50% 100%',
    transform: 'scale(1.14)',
    transformOrigin: '50% 100%',
  })
})

test('round-trips persisted hero background crop values', () => {
  const crop = parseImageCrop('50% 100%', 1.14)

  assert.deepEqual(crop, { x: 50, y: 100, zoom: 1.14 })
  assert.equal(formatImagePosition(crop), '50% 100%')
})

test('uses the same default crop for editors and final renderers', () => {
  assert.deepEqual(defaultImageCrop, { x: 50, y: 50, zoom: 1 })
  assert.deepEqual(parseImageCrop(undefined, undefined), defaultImageCrop)
  assert.deepEqual(imageCropStyle(formatImagePosition(defaultImageCrop), defaultImageCrop.zoom), {
    objectFit: 'cover',
    objectPosition: '50% 50%',
    transform: 'scale(1)',
    transformOrigin: '50% 50%',
  })
})

test('dragging the image moves the persisted crop in the same perceived direction', () => {
  assert.deepEqual(dragImageCrop({ x: 50, y: 50, zoom: 1.4 }, 20, -10, 200, 100), {
    x: 40,
    y: 60,
    zoom: 1.4,
  })

  assert.deepEqual(dragImageCrop({ x: 5, y: 95, zoom: 4 }, 50, -50, 100, 100), {
    x: 0,
    y: 100,
    zoom: 3,
  })
})

test('defaults invalid crop values without leaking unsafe geometry', () => {
  assert.deepEqual(parseImageCrop('left bottom', Number.NaN), { x: 50, y: 50, zoom: 1 })
  assert.equal(formatImagePosition({ x: -20, y: 140 }), '0% 100%')

  assert.deepEqual(imageCropStyle('', 9), {
    objectFit: 'cover',
    objectPosition: '50% 50%',
    transform: 'scale(3)',
    transformOrigin: '50% 50%',
  })
})
