import { apiClient } from './apiClient'
import { createPublicRequestCache } from './publicRequestCache'
import type { Contact } from '../types/contact'

const contactRequestCache = createPublicRequestCache<Contact[]>((requestOptions) => apiClient<Contact[]>('/contacts', requestOptions))

export function getContacts(options: { force?: boolean } = {}) {
  return contactRequestCache.get(options)
}

export function getAdminContacts(token: string) {
  return apiClient<Contact[]>('/admin/contacts', { cache: 'no-store' }, token)
}

export function createContact(input: ContactInput, token: string) {
  return apiClient<Contact>('/admin/contacts', {
    method: 'POST',
    body: JSON.stringify(input),
  }, token)
}

export function updateContact(id: number, input: ContactInput, token: string) {
  return apiClient<Contact>('/admin/contacts/' + id, {
    method: 'PUT',
    body: JSON.stringify(input),
  }, token)
}

export function deleteContact(id: number, token: string) {
  return apiClient<void>('/admin/contacts/' + id, { method: 'DELETE' }, token)
}

export function reorderContacts(contactIds: number[], token: string) {
  return apiClient<Contact[]>('/admin/contacts/order', {
    method: 'PUT',
    body: JSON.stringify({ contactIds }),
  }, token).then((contacts) => {
    contactRequestCache.invalidate()
    return contacts
  })
}

export function invalidateContactsCache() {
  contactRequestCache.invalidate()
}

export type ContactInput = {
  label: string
  type: Contact['type']
  value: string
  visible?: boolean
}
