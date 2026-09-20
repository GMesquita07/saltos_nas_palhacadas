import assert from 'node:assert/strict'
import test from 'node:test'

import { contactToInput, contactVisibilityLabel, nextContactVisible } from './contactHelpers.ts'
import type { Contact } from '../../../types/contact'

test('contact visibility defaults to visible when missing', () => {
  assert.equal(contactVisibilityLabel(undefined), 'Visível')
  assert.equal(nextContactVisible(contact(undefined)), false)
})

test('contact visibility labels hidden contacts', () => {
  assert.equal(contactVisibilityLabel(false), 'Oculto')
  assert.equal(nextContactVisible(contact(false)), true)
})

test('contact input preserves editable fields and visibility', () => {
  assert.deepEqual(contactToInput(contact(false)), {
    label: 'Reservas',
    type: 'EMAIL',
    value: 'ola@example.test',
    visible: false,
  })
})

function contact(visible: boolean | undefined): Contact {
  return {
    id: 1,
    label: 'Reservas',
    type: 'EMAIL',
    value: 'ola@example.test',
    displayOrder: 0,
    visible,
  }
}
