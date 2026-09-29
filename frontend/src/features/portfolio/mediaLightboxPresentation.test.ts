import assert from 'node:assert/strict'
import test from 'node:test'

import {
  mediaDimensionsFromSize,
  mediaOrientation,
  shouldUseArtistDesktopPresentation,
  type MediaLightboxPointerEnvironment,
} from './mediaLightboxPresentation.ts'

const desktop: MediaLightboxPointerEnvironment = {
  maxTouchPoints: 0,
  hasFinePrimaryPointer: true,
  hasAnyCoarsePointer: false,
}

test('enables artist lightbox desktop presentation only for non-touch desktop pointers', () => {
  assert.equal(shouldUseArtistDesktopPresentation('artistPortfolio', desktop), true)
  assert.equal(shouldUseArtistDesktopPresentation('default', desktop), false)
})

test('keeps artist lightbox original presentation on touch and hybrid devices', () => {
  assert.equal(
    shouldUseArtistDesktopPresentation('artistPortfolio', {
      maxTouchPoints: 5,
      hasFinePrimaryPointer: true,
      hasAnyCoarsePointer: true,
    }),
    false,
  )
  assert.equal(
    shouldUseArtistDesktopPresentation('artistPortfolio', {
      maxTouchPoints: 0,
      hasFinePrimaryPointer: true,
      hasAnyCoarsePointer: true,
    }),
    false,
  )
  assert.equal(
    shouldUseArtistDesktopPresentation('artistPortfolio', {
      maxTouchPoints: 0,
      hasFinePrimaryPointer: false,
      hasAnyCoarsePointer: false,
    }),
    false,
  )
})

test('classifies loaded media dimensions for ratio-aware lightbox sizing', () => {
  assert.deepEqual(mediaDimensionsFromSize(1440, 1800), { width: 1440, height: 1800 })
  assert.equal(mediaOrientation({ width: 1440, height: 1800 }), 'portrait')
  assert.equal(mediaOrientation({ width: 1600, height: 900 }), 'landscape')
  assert.equal(mediaOrientation({ width: 1000, height: 1000 }), 'square')
  assert.equal(mediaOrientation(mediaDimensionsFromSize(0, 900)), 'unknown')
})
