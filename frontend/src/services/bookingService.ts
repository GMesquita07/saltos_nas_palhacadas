import { apiClient } from './apiClient'
import type {
  AvailabilitySlot,
  Booking,
  BookingCounterProposal,
  BookingCounterProposalDecision,
  BookingDecision,
  BookingProposal,
  BookingStatus,
} from '../types/booking'

type ApiBooking = {
  id: string | number
  profileSlug: string
  profileName?: string | null
  eventDate: string
  startTime?: string | null
  endTime?: string | null
  eventType: string
  customEventType?: string | null
  weddingCoupleNames?: string | null
  location?: string | null
  contactName: string
  contactEmail?: string | null
  contactPhone: string
  budget?: number | null
  description: string
  notes?: string | null
  status: string
  counterProposal?: { budget?: number | null; eventDate?: string | null } | null
  message?: string | null
  createdAt?: string | null
  updatedAt?: string | null
}

type ApiAvailabilitySlot = {
  date?: string | null
  startTime?: string | null
  endTime?: string | null
  status?: string | null
}

type AvailabilityResponse = string[] | { bookedDates?: unknown; slots?: unknown }

export async function getBookedDates(profileSlug: string, from: string, to: string): Promise<string[]> {
  const response = await apiClient<AvailabilityResponse>(`/profiles/${encodeURIComponent(profileSlug)}/availability?from=${encodeURIComponent(from)}&to=${encodeURIComponent(to)}`)
  const values = Array.isArray(response) ? response : response.bookedDates

  if (!Array.isArray(values)) return []

  return values.filter((value): value is string => typeof value === 'string')
}

export async function getAvailability(profileSlug: string, from: string, to: string): Promise<AvailabilitySlot[]> {
  const response = await apiClient<AvailabilityResponse>(`/profiles/${encodeURIComponent(profileSlug)}/availability?from=${encodeURIComponent(from)}&to=${encodeURIComponent(to)}`)
  if (Array.isArray(response)) {
    return response
      .filter((value): value is string => typeof value === 'string')
      .map((date) => ({ date, startTime: null, endTime: null, status: 'ACCEPTED' }))
  }

  const slots = Array.isArray(response.slots) ? response.slots : []
  const parsedSlots = slots
    .map((slot) => toAvailabilitySlot(slot))
    .filter((slot): slot is AvailabilitySlot => slot !== null)

  if (parsedSlots.length > 0) return parsedSlots

  const bookedDates = Array.isArray(response.bookedDates) ? response.bookedDates : []
  return bookedDates
    .filter((value): value is string => typeof value === 'string')
    .map((date) => ({ date, startTime: null, endTime: null, status: 'ACCEPTED' }))
}

export async function createBooking(proposal: BookingProposal, token: string): Promise<Booking> {
  const response = await apiClient<ApiBooking>('/bookings', {
    method: 'POST',
    body: JSON.stringify(proposal),
  }, token)

  return toBooking(response)
}

export async function getMyBookings(token: string): Promise<Booking[]> {
  const response = await apiClient<ApiBooking[]>('/bookings/mine', {}, token)
  return response.map(toBooking)
}

export async function getAdminBookings(token: string, status?: BookingStatus): Promise<Booking[]> {
  const query = status ? `?status=${encodeURIComponent(status)}` : ''
  const response = await apiClient<ApiBooking[]>(`/admin/bookings${query}`, {}, token)
  return response.map(toBooking)
}

export async function decideBooking(id: string, decision: BookingDecision, token: string): Promise<Booking> {
  const response = await apiClient<ApiBooking>(`/admin/bookings/${encodeURIComponent(id)}/decision`, {
    method: 'PUT',
    body: JSON.stringify(decision),
  }, token)
  return toBooking(response)
}

export async function respondToCounterProposal(
  id: string,
  decision: BookingCounterProposalDecision,
  token: string,
): Promise<Booking> {
  const response = await apiClient<ApiBooking>(`/bookings/${encodeURIComponent(id)}/counter-proposal/decision`, {
    method: 'PUT',
    body: JSON.stringify({ decision }),
  }, token)

  return toBooking(response)
}

function toBooking(booking: ApiBooking): Booking {
  return {
    id: String(booking.id),
    profileSlug: booking.profileSlug,
    profileName: booking.profileName?.trim() || booking.profileSlug,
    eventDate: booking.eventDate,
    startTime: booking.startTime ?? null,
    endTime: booking.endTime ?? null,
    eventType: booking.eventType,
    customEventType: booking.customEventType ?? null,
    weddingCoupleNames: booking.weddingCoupleNames ?? null,
    location: booking.location ?? '',
    contactName: booking.contactName,
    contactEmail: booking.contactEmail ?? '',
    contactPhone: booking.contactPhone,
    budget: typeof booking.budget === 'number' ? booking.budget : null,
    description: booking.description,
    notes: booking.notes ?? '',
    status: toBookingStatus(booking.status),
    counterProposal: toCounterProposal(booking.counterProposal),
    message: booking.message ?? null,
    createdAt: booking.createdAt ?? '',
    updatedAt: booking.updatedAt ?? null,
  }
}

function toBookingStatus(status: string): BookingStatus {
  if (status === 'ACCEPTED' || status === 'DECLINED' || status === 'COUNTER_PROPOSED' || status === 'CANCELLED') return status
  return 'PENDING'
}

function toAvailabilitySlot(value: unknown): AvailabilitySlot | null {
  const slot = value as ApiAvailabilitySlot
  if (!slot || typeof slot.date !== 'string') return null
  const status = slot.status === 'PENDING' ? 'PENDING' : slot.status === 'ACCEPTED' ? 'ACCEPTED' : null
  if (!status) return null

  return {
    date: slot.date,
    startTime: typeof slot.startTime === 'string' && slot.startTime ? slot.startTime : null,
    endTime: typeof slot.endTime === 'string' && slot.endTime ? slot.endTime : null,
    status,
  }
}

function toCounterProposal(value: ApiBooking['counterProposal']): BookingCounterProposal | null {
  if (!value || (typeof value.budget !== 'number' && !value.eventDate)) return null

  return {
    budget: typeof value.budget === 'number' ? value.budget : null,
    eventDate: value.eventDate ?? null,
  }
}
