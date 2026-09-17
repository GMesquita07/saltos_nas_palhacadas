import type { BookingStatus } from '../../../types/booking'

export type BookingFilter = BookingStatus | 'ALL'

export const defaultBookingFilter: BookingFilter = 'ALL'

export const bookingFilters: Array<{ value: BookingFilter; label: string }> = [
  { value: 'ALL', label: 'Todos' },
  { value: 'PENDING', label: 'Pendentes' },
  { value: 'ACCEPTED', label: 'Aceites' },
  { value: 'DECLINED', label: 'Recusados' },
  { value: 'CANCELLED', label: 'Cancelados' },
]

export function toBookingStatusFilter(filter: BookingFilter): BookingStatus | undefined {
  return filter === 'ALL' ? undefined : filter
}

export function bookingEmptyMessage(filter: BookingFilter) {
  if (filter === 'ALL') return 'Ainda não existem agendamentos.'
  if (filter === 'PENDING') return 'Não há pedidos pendentes neste momento.'
  return 'Não existem agendamentos neste estado.'
}
