import assert from 'node:assert/strict'
import test from 'node:test'

import {
  dismissNavigationMenuOnEscape,
  mainContentHref,
  mainContentId,
  navigationAriaCurrent,
  shouldMoveFocusToMain,
} from './accessibility.ts'

test('keeps the skip link and main target identifiers aligned', () => {
  assert.equal(mainContentId, 'main-content')
  assert.equal(mainContentHref, '#main-content')
})

test('marks only the active navigation destination as the current page', () => {
  const destinations = ['profiles', 'booking', 'contacts', 'materials', 'favorites', 'admin', 'account']

  for (const activeView of destinations) {
    for (const itemView of destinations) {
      assert.equal(
        navigationAriaCurrent(activeView, itemView),
        activeView === itemView ? 'page' : undefined,
      )
    }
  }
})

test('Escape closes the mobile navigation before restoring focus', () => {
  const events: string[] = []

  const dismissed = dismissNavigationMenuOnEscape(
    'Escape',
    () => events.push('closed'),
    () => events.push('focused'),
  )

  assert.equal(dismissed, true)
  assert.deepEqual(events, ['closed', 'focused'])
})

test('other keys leave the mobile navigation unchanged', () => {
  let called = false

  const dismissed = dismissNavigationMenuOnEscape(
    'Enter',
    () => { called = true },
    () => { called = true },
  )

  assert.equal(dismissed, false)
  assert.equal(called, false)
})

test('does not move focus on the initial route render', () => {
  assert.equal(shouldMoveFocusToMain(null, '/contactos', ''), false)
})

test('moves focus when the pathname changes', () => {
  assert.equal(shouldMoveFocusToMain('/', '/contactos', ''), true)
  assert.equal(shouldMoveFocusToMain('/contactos', '/contactos', ''), false)
})

test('preserves hash navigation instead of moving focus to main', () => {
  assert.equal(shouldMoveFocusToMain('/conta', '/conta', '#account-notifications-title'), false)
  assert.equal(shouldMoveFocusToMain('/', '/conta', '#account-notifications-title'), false)
})
