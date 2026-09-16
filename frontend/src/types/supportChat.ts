export type SupportChatResponse = {
  answer: string
  suggestions: string[]
}

export type SupportChatMessage = {
  id: string
  role: 'assistant' | 'user'
  text: string
}
