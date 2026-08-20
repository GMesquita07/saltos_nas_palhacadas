import { apiClient } from './apiClient'
import type { Contact } from '../types/contact'

export function getContacts() {
  return apiClient<Contact[]>('/contacts')
}
