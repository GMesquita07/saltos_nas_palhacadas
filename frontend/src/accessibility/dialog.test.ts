import assert from 'node:assert/strict'
import test from 'node:test'

import {
  canRestoreDialogFocus,
  dialogKeyboardAction,
  dialogTabTarget,
} from './dialog.ts'

test('maps Escape to close and leaves unrelated keys alone', () => {
  assert.equal(dialogKeyboardAction('Escape'), 'close')
  assert.equal(dialogKeyboardAction('Enter'), 'ignore')
})

test('maps Tab to focus trapping', () => {
  assert.equal(dialogKeyboardAction('Tab'), 'trap')
})

test('wraps Tab from the last item to the first', () => {
  assert.equal(dialogTabTarget(3, 2, false), 0)
})

test('wraps Shift+Tab from the first item to the last', () => {
  assert.equal(dialogTabTarget(3, 0, true), 2)
})

test('focuses the dialog when it contains no focusable elements', () => {
  assert.equal(dialogTabTarget(0, -1, false), 'dialog')
})

test('brings focus back inside when focus starts outside the dialog', () => {
  assert.equal(dialogTabTarget(3, -1, false), 0)
  assert.equal(dialogTabTarget(3, -1, true), 2)
})

test('restores focus only while the previous trigger still exists', () => {
  assert.equal(canRestoreDialogFocus(true), true)
  assert.equal(canRestoreDialogFocus(false), false)
})
