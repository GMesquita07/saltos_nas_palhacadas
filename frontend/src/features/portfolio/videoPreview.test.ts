import assert from 'node:assert/strict'
import test from 'node:test'

import { videoFirstFrameSource } from './videoPreview.ts'

test('adds a tiny media fragment so video previews decode the first frame', () => {
  assert.equal(videoFirstFrameSource('/content/video.mp4'), '/content/video.mp4#t=0.001')
  assert.equal(
    videoFirstFrameSource('https://cdn.example.test/video.mp4?token=abc'),
    'https://cdn.example.test/video.mp4?token=abc#t=0.001',
  )
})

test('preserves existing fragments and empty values', () => {
  assert.equal(videoFirstFrameSource('/content/video.mp4#t=1'), '/content/video.mp4#t=1')
  assert.equal(videoFirstFrameSource(''), '')
})
