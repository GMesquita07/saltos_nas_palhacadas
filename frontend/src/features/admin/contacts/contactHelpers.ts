import type { Contact } from '../../../types/contact'

export function contactVisibilityLabel(visible: boolean | undefined) {
  return visible ?? true ? 'Visível' : 'Oculto'
}

export function nextContactVisible(contact: Pick<Contact, 'visible'>) {
  return !(contact.visible ?? true)
}

export function contactToInput(contact: Contact, visible = contact.visible ?? true) {
  return {
    label: contact.label,
    type: contact.type,
    value: contact.value,
    visible,
  }
}
