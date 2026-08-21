export type BookingStatus = 'PENDING' | 'ACCEPTED' | 'DECLINED' | 'COUNTER_PROPOSED'
export type BookingDecisionStatus = Exclude<BookingStatus, 'PENDING'>
export type BookingCounterProposalDecision = 'ACCEPTED' | 'DECLINED'

export type BookingCounterProposal = {
  budget: number | null
  eventDate: string | null
}

export type BookingProposal = {
  profileSlug: string
  eventDate: string
  eventType: string
  contactName: string
  contactPhone: string
  budget: number
  description: string
  notes: string
}

export type Booking = BookingProposal & {
  id: string
  profileName: string
  status: BookingStatus
  counterProposal: BookingCounterProposal | null
  message: string | null
  createdAt: string
  updatedAt: string | null
}

export type BookingDecision = {
  status: BookingDecisionStatus
  message?: string
  counterBudget?: number
  counterEventDate?: string
}
