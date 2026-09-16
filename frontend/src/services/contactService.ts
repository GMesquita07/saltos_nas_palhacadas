import { apiClient } from './apiClient'
import type { Contact } from '../types/contact'

export function getContacts() {
  return apiClient<Contact[]>('/contacts', { cache: 'no-store' })
}

export function reorderContacts(contactIds: number[], token: string) {
  return apiClient<Contact[]>('/admin/contacts/order', {
    method: 'PUT',
    body: JSON.stringify({ contactIds }),
  }, token)
}
