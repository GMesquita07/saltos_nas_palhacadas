export type BookingStatus = 'PENDING' | 'ACCEPTED' | 'DECLINED' | 'COUNTER_PROPOSED' | 'CANCELLED'
export type BookingDecisionStatus = Exclude<BookingStatus, 'PENDING'>
export type BookingCounterProposalDecision = 'ACCEPTED' | 'DECLINED'

export type BookingCounterProposal = {
  budget: number | null
  eventDate: string | null
}

export type BookingProposal = {
  profileSlug: string
  eventDate: string
  startTime: string | null
  endTime: string | null
  eventType: string
  customEventType: string | null
  weddingCoupleNames: string | null
  location: string
  contactName: string
  contactEmail: string
  contactPhone: string
  description: string
  notes: string
}

export type Booking = BookingProposal & {
  id: string
  profileName: string
  budget: number | null
  status: BookingStatus
  counterProposal: BookingCounterProposal | null
  message: string | null
  createdAt: string
  updatedAt: string | null
}

export type BookingDecision = {
  status: BookingDecisionStatus
  message?: string
  eventDate?: string
  startTime?: string | null
  endTime?: string | null
  agreedBudget?: number
  counterBudget?: number
  counterEventDate?: string
}

export type AvailabilitySlot = {
  date: string
  startTime: string | null
  endTime: string | null
  status: Extract<BookingStatus, 'PENDING' | 'ACCEPTED'>
}
