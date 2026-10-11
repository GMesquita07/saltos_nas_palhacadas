import assert from 'node:assert/strict'
import test from 'node:test'

import { prefersReducedMotion, scrollBehaviorForMotion } from './motion.ts'

test('reads the reduced-motion media query state', () => {
  assert.equal(prefersReducedMotion({ matches: true }), true)
  assert.equal(prefersReducedMotion({ matches: false }), false)
})

test('avoids smooth scrolling when reduced motion is requested', () => {
  assert.equal(scrollBehaviorForMotion(true), 'auto')
  assert.equal(scrollBehaviorForMotion(false), 'smooth')
})
