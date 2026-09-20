import { apiClient } from './apiClient'
import { createPublicRequestCache } from './publicRequestCache'
import type { Contact } from '../types/contact'

const contactRequestCache = createPublicRequestCache<Contact[]>((requestOptions) => apiClient<Contact[]>('/contacts', requestOptions))

export function getContacts(options: { force?: boolean } = {}) {
  return contactRequestCache.get(options)
}

export function reorderContacts(contactIds: number[], token: string) {
  return apiClient<Contact[]>('/admin/contacts/order', {
    method: 'PUT',
    body: JSON.stringify({ contactIds }),
  }, token).then((contacts) => {
    contactRequestCache.prime(contacts)
    return contacts
  })
}

export function invalidateContactsCache() {
  contactRequestCache.invalidate()
}
