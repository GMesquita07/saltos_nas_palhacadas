export type ContactType = 'EMAIL' | 'PHONE' | 'WHATSAPP' | 'INSTAGRAM' | 'WEBSITE'

export type Contact = {
  id: number
  label: string
  type: ContactType
  value: string
  displayOrder: number
}
