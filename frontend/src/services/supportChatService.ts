import { apiClient } from './apiClient'
import type { SupportChatResponse } from '../types/supportChat'

export function askSupportChat(message: string) {
  return apiClient<SupportChatResponse>('/support-chat', {
    method: 'POST',
    body: JSON.stringify({ message }),
  })
}
