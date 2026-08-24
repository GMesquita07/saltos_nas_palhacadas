import { useEffect, useMemo, useRef, useState } from 'react'
import { useAuth } from '../auth/AuthContext'
import { createBooking, getAvailability, getMyBookings, respondToCounterProposal } from '../../services/bookingService'
import type { AvailabilitySlot, Booking, BookingCounterProposalDecision, BookingProposal, BookingStatus } from '../../types/booking'
import type { Profile } from '../../types/profile'
import styles from './BookingPage.module.css'

type BookingPageProps = {
  profiles: Profile[]
  initialProfile?: Profile | null
  onBack: () => void
  onRequireLogin: () => void
}

type CalendarDay = {
  date: Date
  isCurrentMonth: boolean
}

const eventTypes = [
  { value: 'WEDDING', label: 'Casamento' },
  { value: 'BIRTHDAY', label: 'Aniversário' },
  { value: 'BAPTISM', label: 'Batizado' },
  { value: 'CORPORATE', label: 'Evento empresarial' },
  { value: 'PRIVATE_PARTY', label: 'Festa privada' },
  { value: 'FESTIVAL', label: 'Festival' },
  { value: 'OTHER', label: 'Outro' },
]
const weekdayNames = ['Seg', 'Ter', 'Qua', 'Qui', 'Sex', 'Sáb', 'Dom']
const dateFormatter = new Intl.DateTimeFormat('pt-PT', { day: 'numeric', month: 'long', year: 'numeric' })
const monthFormatter = new Intl.DateTimeFormat('pt-PT', { month: 'long', year: 'numeric' })

export function BookingPage({ profiles, initialProfile, onBack, onRequireLogin }: BookingPageProps) {
  const { session } = useAuth()
  const [selectedProfileSlug, setSelectedProfileSlug] = useState(initialProfile?.slug ?? '')
  const [selectedDate, setSelectedDate] = useState('')
  const [visibleMonth, setVisibleMonth] = useState(() => firstDayOfMonth(new Date()))
  const [availabilitySlots, setAvailabilitySlots] = useState<AvailabilitySlot[]>([])
  const [selectedEventType, setSelectedEventType] = useState('')
  const [isAvailabilityLoading, setIsAvailabilityLoading] = useState(Boolean(initialProfile))
  const [availabilityError, setAvailabilityError] = useState<string | null>(null)
  const [myBookings, setMyBookings] = useState<Booking[]>([])
  const [isBookingsLoading, setIsBookingsLoading] = useState(Boolean(session))
  const [bookingsError, setBookingsError] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [submitError, setSubmitError] = useState<string | null>(null)
  const [submitSuccess, setSubmitSuccess] = useState<string | null>(null)
  const [respondingBookingId, setRespondingBookingId] = useState<string | null>(null)
  const [respondingCounterDecision, setRespondingCounterDecision] = useState<BookingCounterProposalDecision | null>(null)
  const [counterProposalFeedback, setCounterProposalFeedback] = useState<{ bookingId: string; type: 'error' | 'success'; message: string } | null>(null)
  const respondingBookingRef = useRef<string | null>(null)

  const selectedProfile = profiles.find((profile) => profile.slug === selectedProfileSlug) ?? null
  const availabilityByDate = useMemo(() => groupAvailabilityByDate(availabilitySlots), [availabilitySlots])
  const fullyBookedDateSet = useMemo(() => new Set(availabilitySlots
    .filter((slot) => slot.status === 'ACCEPTED' && (!slot.startTime || !slot.endTime))
    .map((slot) => slot.date)), [availabilitySlots])
  const selectedDateSlots = selectedDate ? availabilityByDate.get(selectedDate) ?? [] : []
  const calendarDays = useMemo(() => getCalendarDays(visibleMonth), [visibleMonth])
  const today = useMemo(() => atStartOfDay(new Date()), [])
  const canMoveToPreviousMonth = firstDayOfMonth(visibleMonth).getTime() > firstDayOfMonth(today).getTime()
  const defaultContactName = useMemo(() => {
    const fullName = [session?.firstName, session?.lastName].filter(Boolean).join(' ').trim()
    return fullName || session?.username || ''
  }, [session])

  useEffect(() => {
    if (!selectedProfileSlug) {
      return
    }

    let isCurrent = true
    const from = toDateValue(firstDayOfMonth(visibleMonth))
    const to = toDateValue(lastDayOfMonth(visibleMonth))

    void getAvailability(selectedProfileSlug, from, to)
      .then((slots) => {
        if (isCurrent) setAvailabilitySlots(slots)
      })
      .catch(() => {
        if (isCurrent) setAvailabilityError('Não foi possível confirmar as datas ocupadas deste perfil. Tenta novamente.')
      })
      .finally(() => {
        if (isCurrent) setIsAvailabilityLoading(false)
      })

    return () => { isCurrent = false }
  }, [selectedProfileSlug, visibleMonth])

  useEffect(() => {
    if (!session) {
      return
    }

    let isCurrent = true
    void getMyBookings(session.token)
      .then((bookings) => {
        if (isCurrent) setMyBookings(bookings)
      })
      .catch((reason) => {
        if (isCurrent) setBookingsError(reason instanceof Error ? reason.message : 'Não foi possível carregar os teus pedidos.')
      })
      .finally(() => {
        if (isCurrent) setIsBookingsLoading(false)
      })

    return () => { isCurrent = false }
  }, [session])

  function handleProfileChange(profileSlug: string) {
    setSelectedProfileSlug(profileSlug)
    setSelectedDate('')
    setIsAvailabilityLoading(Boolean(profileSlug))
    setAvailabilitySlots([])
    setAvailabilityError(null)
    setSubmitError(null)
    setSubmitSuccess(null)
  }

  function changeVisibleMonth(amount: number) {
    setIsAvailabilityLoading(true)
    setAvailabilityError(null)
    setVisibleMonth((month) => addMonths(month, amount))
  }

  function handleDateSelect(date: Date) {
    if (isPastDate(date, today) || fullyBookedDateSet.has(toDateValue(date))) return

    setSelectedDate(toDateValue(date))
    setSubmitError(null)
    setSubmitSuccess(null)
  }

  async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()

    if (!session) {
      onRequireLogin()
      return
    }

    const formElement = event.currentTarget
    const form = new FormData(formElement)
    const eventType = String(form.get('eventType') ?? '').trim()
    const customEventType = String(form.get('customEventType') ?? '').trim()
    const weddingCoupleNames = String(form.get('weddingCoupleNames') ?? '').trim()
    const location = String(form.get('location') ?? '').trim()
    const contactName = String(form.get('contactName') ?? '').trim()
    const contactEmail = String(form.get('contactEmail') ?? '').trim()
    const contactPhone = String(form.get('contactPhone') ?? '').trim()
    const startTime = String(form.get('startTime') ?? '').trim()
    const endTime = String(form.get('endTime') ?? '').trim()
    const description = String(form.get('description') ?? '').trim()

    if (!selectedProfileSlug || !selectedDate) {
      setSubmitError('Escolhe primeiro o artista e uma data disponível.')
      return
    }
    if (!eventType) {
      setSubmitError('Escolhe o tipo de evento.')
      return
    }
    if (eventType === 'WEDDING' && !weddingCoupleNames) {
      setSubmitError('Indica os nomes dos noivos.')
      return
    }
    if (eventType === 'OTHER' && !customEventType) {
      setSubmitError('Indica o tipo de evento.')
      return
    }
    if (!location || !contactName || !contactEmail || !contactPhone || !description) {
      setSubmitError('Preenche local, nome, email, telemóvel e descrição do evento.')
      return
    }
    if ((startTime && !endTime) || (!startTime && endTime)) {
      setSubmitError('Indica a hora de início e de fim, ou deixa ambas em branco.')
      return
    }
    if (startTime && endTime && startTime >= endTime) {
      setSubmitError('A hora de fim tem de ser posterior à hora de início.')
      return
    }
    if (hasAcceptedOverlap(selectedDateSlots, startTime || null, endTime || null)) {
      setSubmitError('Já existe um evento confirmado nesse horário. Escolhe outro intervalo.')
      return
    }

    const proposal: BookingProposal = {
      profileSlug: selectedProfileSlug,
      eventDate: selectedDate,
      startTime: startTime || null,
      endTime: endTime || null,
      eventType,
      customEventType: eventType === 'OTHER' ? customEventType : null,
      weddingCoupleNames: eventType === 'WEDDING' ? weddingCoupleNames : null,
      location,
      contactName,
      contactEmail,
      contactPhone,
      description,
      notes: String(form.get('notes') ?? '').trim(),
    }

    setIsSubmitting(true)
    setSubmitError(null)
    setSubmitSuccess(null)
    try {
      const booking = await createBooking(proposal, session.token)
      setMyBookings((current) => [booking, ...current])
      setAvailabilitySlots((current) => booking.profileSlug === selectedProfileSlug
        ? [{ date: booking.eventDate, startTime: booking.startTime, endTime: booking.endTime, status: 'PENDING' }, ...current]
        : current)
      setSubmitSuccess('O teu pedido foi enviado. Vais receber um email de confirmação e o animador vai analisar o pedido.')
      formElement.reset()
      setSelectedEventType('')
      setSelectedDate('')
    } catch (reason) {
      setSubmitError(reason instanceof Error ? reason.message : 'Não foi possível enviar o pedido.')
    } finally {
      setIsSubmitting(false)
    }
  }

  async function handleCounterProposalDecision(bookingId: string, decision: BookingCounterProposalDecision) {
    if (!session || respondingBookingRef.current) return

    respondingBookingRef.current = bookingId
    setRespondingBookingId(bookingId)
    setRespondingCounterDecision(decision)
    setCounterProposalFeedback(null)

    try {
      const updatedBooking = await respondToCounterProposal(bookingId, decision, session.token)
      setMyBookings((current) => current.map((booking) => booking.id === bookingId ? updatedBooking : booking))
      if (decision === 'ACCEPTED' && updatedBooking.profileSlug === selectedProfileSlug) {
        setAvailabilitySlots((current) => [
          { date: updatedBooking.eventDate, startTime: updatedBooking.startTime, endTime: updatedBooking.endTime, status: 'ACCEPTED' },
          ...current,
        ])
      }
      setCounterProposalFeedback({
        bookingId,
        type: 'success',
        message: decision === 'ACCEPTED'
          ? 'Aceitaste a alteração. O evento ficou confirmado.'
          : 'Recusaste a alteração. O pedido foi encerrado.',
      })
    } catch (reason) {
      setCounterProposalFeedback({
        bookingId,
        type: 'error',
        message: reason instanceof Error ? reason.message : 'Não foi possível responder à alteração. Tenta novamente.',
      })
    } finally {
      respondingBookingRef.current = null
      setRespondingBookingId(null)
      setRespondingCounterDecision(null)
    }
  }

  return (
    <section className={styles.page}>
      <button className={styles.back} type="button" onClick={onBack}>← Voltar aos perfis</button>
      <header className={styles.header}>
        <p className="eyebrow">Agendamento</p>
        <h1>Planeia o teu evento</h1>
        <p>Consulta a disponibilidade, envia um pedido de orçamento e acompanha a resposta num só lugar.</p>
      </header>

      <div className={styles.layout}>
        <div className={styles.calendarPanel}>
          <div className={styles.sectionHeading}>
            <p className="eyebrow">1. Disponibilidade</p>
            <h2>Escolhe o artista e a data</h2>
          </div>

          <label className={styles.field} htmlFor="booking-profile">
            <span>Artista</span>
            <select id="booking-profile" value={selectedProfileSlug} onChange={(event) => handleProfileChange(event.target.value)}>
              <option value="">Seleciona um perfil</option>
              {profiles.map((profile) => <option key={profile.id} value={profile.slug}>{profile.name} · {profile.role}</option>)}
            </select>
          </label>

          {!selectedProfile ? (
            <div className={styles.calendarEmpty}>Seleciona um artista para ver as datas disponíveis.</div>
          ) : (
            <>
              <div className={styles.calendarHeader}>
                <button aria-label="Mês anterior" className={styles.monthButton} disabled={!canMoveToPreviousMonth} type="button" onClick={() => changeVisibleMonth(-1)}>←</button>
                <h3>{capitalize(monthFormatter.format(visibleMonth))}</h3>
                <button aria-label="Mês seguinte" className={styles.monthButton} type="button" onClick={() => changeVisibleMonth(1)}>→</button>
              </div>
              <div className={styles.calendar} aria-busy={isAvailabilityLoading}>
                {weekdayNames.map((day) => <span className={styles.weekday} key={day}>{day}</span>)}
                {calendarDays.map(({ date, isCurrentMonth }) => {
                  const dateValue = toDateValue(date)
                  const daySlots = availabilityByDate.get(dateValue) ?? []
                  const hasAccepted = daySlots.some((slot) => slot.status === 'ACCEPTED')
                  const hasPending = daySlots.some((slot) => slot.status === 'PENDING')
                  const isFullyBooked = fullyBookedDateSet.has(dateValue)
                  const isPast = isPastDate(date, today)
                  const isSelected = selectedDate === dateValue
                  const isDisabled = !isCurrentMonth || isPast || isFullyBooked
                  const dayState = isFullyBooked
                    ? 'Indisponível'
                    : hasAccepted
                      ? 'Com horários ocupados'
                      : hasPending
                        ? 'Em stand by'
                        : isPast
                          ? 'Data passada'
                          : isSelected
                            ? 'Data selecionada'
                            : 'Disponível'

                  return (
                    <button
                      aria-label={`${dateFormatter.format(date)} · ${dayState}`}
                      aria-pressed={isSelected}
                      className={`${styles.day} ${!isCurrentMonth ? styles.outsideMonth : ''} ${hasAccepted ? styles.booked : ''} ${hasPending ? styles.standby : ''} ${isSelected ? styles.selected : ''}`}
                      disabled={isDisabled}
                      key={dateValue}
                      type="button"
                      onClick={() => handleDateSelect(date)}
                    >
                      <span>{date.getDate()}</span>
                    </button>
                  )
                })}
              </div>
              <div className={styles.legend} aria-label="Legenda do calendário">
                <span><i className={styles.available} />Disponível</span>
                <span><i className={styles.pendingLegend} />Em stand by</span>
                <span><i className={styles.unavailable} />Confirmado</span>
              </div>
              {selectedDate && selectedDateSlots.length > 0 && (
                <div className={styles.daySchedule}>
                  <strong>{dateFormatter.format(toLocalDate(selectedDate))}</strong>
                  {selectedDateSlots.map((slot, index) => (
                    <span key={`${slot.status}-${slot.startTime ?? 'day'}-${index}`}>
                      {slot.status === 'PENDING' ? 'Em stand by' : 'Confirmado'} · {formatTimeRange(slot.startTime, slot.endTime)}
                    </span>
                  ))}
                </div>
              )}
              {isAvailabilityLoading && <p className={styles.calendarFeedback}>A atualizar disponibilidade...</p>}
              {availabilityError && <p className={styles.error} role="status">{availabilityError}</p>}
            </>
          )}
        </div>

        <form className={styles.form} onSubmit={(event) => { void handleSubmit(event) }}>
          <div className={styles.sectionHeading}>
            <p className="eyebrow">2. Pedido</p>
            <h2>Conta-nos sobre o evento</h2>
          </div>
          {selectedProfile && <p className={styles.selection}>Pedido para <strong>{selectedProfile.name}</strong>{selectedDate ? <> em <strong>{dateFormatter.format(toLocalDate(selectedDate))}</strong></> : ''}.</p>}

          <div className={styles.formGrid}>
            <label className={styles.field}>
              <span>Tipo de evento</span>
              <select name="eventType" required value={selectedEventType} onChange={(event) => setSelectedEventType(event.target.value)}>
                <option disabled value="">Seleciona uma opção</option>
                {eventTypes.map((eventType) => <option key={eventType.value} value={eventType.value}>{eventType.label}</option>)}
              </select>
            </label>
            {selectedEventType === 'WEDDING' && (
              <label className={styles.field}>
                <span>Nomes dos noivos</span>
                <input maxLength={180} name="weddingCoupleNames" placeholder="Ex.: Ana e Miguel" required />
              </label>
            )}
            {selectedEventType === 'OTHER' && (
              <label className={styles.field}>
                <span>Qual é o tipo de evento?</span>
                <input maxLength={120} name="customEventType" placeholder="Ex.: Baile de finalistas" required />
              </label>
            )}
            <label className={styles.field}>
              <span>Local do evento</span>
              <input autoComplete="street-address" maxLength={180} name="location" placeholder="Localidade, quinta, salão ou morada" required />
            </label>
            <label className={styles.field}>
              <span>Hora de início <em>Opcional</em></span>
              <input name="startTime" type="time" />
            </label>
            <label className={styles.field}>
              <span>Hora de fim <em>Opcional</em></span>
              <input name="endTime" type="time" />
            </label>
            <label className={styles.field}>
              <span>O teu nome</span>
              <input autoComplete="name" defaultValue={defaultContactName} maxLength={100} name="contactName" placeholder="Nome e apelido" required />
            </label>
            <label className={styles.field}>
              <span>Email de contacto</span>
              <input autoComplete="email" defaultValue={session?.email ?? ''} maxLength={254} name="contactEmail" placeholder="email@exemplo.pt" required type="email" />
            </label>
            <label className={styles.field}>
              <span>Telemóvel</span>
              <input autoComplete="tel" defaultValue={session?.phone ?? ''} inputMode="tel" maxLength={30} name="contactPhone" placeholder="Ex.: 912 345 678" required type="tel" />
            </label>
          </div>
          <label className={styles.field}>
            <span>Descrição do evento / serviços pretendidos</span>
            <textarea maxLength={2000} minLength={10} name="description" placeholder="Indica o número de convidados, ambiente pretendido, materiais/serviços necessários e outros detalhes importantes." required rows={5} />
          </label>
          <label className={styles.field}>
            <span>Notas adicionais <em>Opcional</em></span>
            <textarea maxLength={1000} name="notes" placeholder="Algum detalhe adicional que ajude a preparar o pedido." rows={3} />
          </label>

          {submitError && <p className={styles.error} role="alert">{submitError}</p>}
          {submitSuccess && <p className={styles.success} role="status">{submitSuccess}</p>}
          {session ? (
            <button className={styles.submit} disabled={isSubmitting || !selectedProfile || !selectedDate} type="submit">{isSubmitting ? 'A enviar pedido...' : 'Enviar pedido'}</button>
          ) : (
            <button className={styles.submit} type="button" onClick={onRequireLogin}>Login para enviar pedido</button>
          )}
          {!session && <p className={styles.loginHint}>Podes consultar a agenda sem conta. Para enviar e acompanhar um pedido, inicia sessão ou cria uma conta.</p>}
        </form>
      </div>

      {session && (
        <section className={styles.requests}>
          <div className={styles.sectionHeading}>
            <p className="eyebrow">Área privada</p>
            <h2>Os meus pedidos</h2>
          </div>
          {isBookingsLoading ? <p className={styles.feedback}>A carregar os teus pedidos...</p> : bookingsError ? <p className={styles.error} role="status">{bookingsError}</p> : myBookings.length === 0 ? <p className={styles.feedback}>Ainda não enviaste nenhum pedido. Escolhe uma data disponível para começar.</p> : <BookingList bookings={myBookings} counterProposalFeedback={counterProposalFeedback} onCounterProposalDecision={handleCounterProposalDecision} respondingBookingId={respondingBookingId} respondingCounterDecision={respondingCounterDecision} />}
        </section>
      )}
    </section>
  )
}

type BookingListProps = {
  bookings: Booking[]
  counterProposalFeedback: { bookingId: string; type: 'error' | 'success'; message: string } | null
  onCounterProposalDecision: (bookingId: string, decision: BookingCounterProposalDecision) => void
  respondingBookingId: string | null
  respondingCounterDecision: BookingCounterProposalDecision | null
}

function BookingList({ bookings, counterProposalFeedback, onCounterProposalDecision, respondingBookingId, respondingCounterDecision }: BookingListProps) {
  return (
    <div className={styles.bookingList}>
      {bookings.map((booking) => {
        const status = statusMeta(booking.status)
        return (
          <article className={styles.bookingCard} key={booking.id}>
            <div className={styles.bookingTopline}>
              <p>{booking.profileName}</p>
              <span className={`${styles.status} ${styles[status.className]}`}>{status.label}</span>
            </div>
            <h3>{eventTypeLabel(booking)} · {dateFormatter.format(toLocalDate(booking.eventDate))}</h3>
            <dl>
              <div><dt>Horário</dt><dd>{formatTimeRange(booking.startTime, booking.endTime)}</dd></div>
              {booking.location && <div><dt>Local</dt><dd>{booking.location}</dd></div>}
              <div><dt>Enviado em</dt><dd>{booking.createdAt ? dateFormatter.format(new Date(booking.createdAt)) : '—'}</dd></div>
            </dl>
            {booking.counterProposal && <div className={styles.counterProposal}><strong>Alteração proposta</strong><span>{[booking.counterProposal.budget === null ? null : formatCurrency(booking.counterProposal.budget), booking.counterProposal.eventDate ? dateFormatter.format(toLocalDate(booking.counterProposal.eventDate)) : null].filter(Boolean).join(' · ')}</span></div>}
            {booking.status === 'COUNTER_PROPOSED' && (
              <div className={styles.counterResponse}>
                <p>Queres aceitar esta alteração?</p>
                <div className={styles.counterActions}>
                  <button disabled={respondingBookingId !== null} type="button" onClick={() => onCounterProposalDecision(booking.id, 'ACCEPTED')}>
                    {respondingBookingId === booking.id && respondingCounterDecision === 'ACCEPTED' ? 'A aceitar...' : 'Aceitar alteração'}
                  </button>
                  <button className={styles.declineCounter} disabled={respondingBookingId !== null} type="button" onClick={() => onCounterProposalDecision(booking.id, 'DECLINED')}>
                    {respondingBookingId === booking.id && respondingCounterDecision === 'DECLINED' ? 'A recusar...' : 'Recusar alteração'}
                  </button>
                </div>
              </div>
            )}
            {counterProposalFeedback?.bookingId === booking.id && (
              <p className={counterProposalFeedback.type === 'success' ? styles.counterSuccess : styles.counterError} role={counterProposalFeedback.type === 'error' ? 'alert' : 'status'}>
                {counterProposalFeedback.message}
              </p>
            )}
            {booking.message && <p className={styles.adminMessage}><strong>Mensagem da equipa:</strong> {booking.message}</p>}
          </article>
        )
      })}
    </div>
  )
}

function statusMeta(status: BookingStatus) {
  if (status === 'ACCEPTED') return { label: 'Aceite', className: 'accepted' }
  if (status === 'DECLINED') return { label: 'Não aceite', className: 'declined' }
  if (status === 'COUNTER_PROPOSED') return { label: 'Alteração proposta', className: 'countered' }
  if (status === 'CANCELLED') return { label: 'Cancelado', className: 'cancelled' }
  return { label: 'Em análise', className: 'pending' }
}

function eventTypeLabel(booking: Booking) {
  if (booking.eventType === 'OTHER' && booking.customEventType) return booking.customEventType
  return eventTypes.find((type) => type.value === booking.eventType)?.label ?? booking.eventType
}

function getCalendarDays(month: Date): CalendarDay[] {
  const firstDay = firstDayOfMonth(month)
  const weekdayOffset = (firstDay.getDay() + 6) % 7
  const gridStart = new Date(firstDay.getFullYear(), firstDay.getMonth(), 1 - weekdayOffset)

  return Array.from({ length: 42 }, (_, index) => {
    const date = new Date(gridStart.getFullYear(), gridStart.getMonth(), gridStart.getDate() + index)
    return { date, isCurrentMonth: date.getMonth() === firstDay.getMonth() }
  })
}

function groupAvailabilityByDate(slots: AvailabilitySlot[]) {
  return slots.reduce((map, slot) => {
    const currentSlots = map.get(slot.date) ?? []
    currentSlots.push(slot)
    map.set(slot.date, currentSlots)
    return map
  }, new Map<string, AvailabilitySlot[]>())
}

function hasAcceptedOverlap(slots: AvailabilitySlot[], startTime: string | null, endTime: string | null) {
  return slots
    .filter((slot) => slot.status === 'ACCEPTED')
    .some((slot) => timeRangesOverlap(slot.startTime, slot.endTime, startTime, endTime))
}

function timeRangesOverlap(existingStart: string | null, existingEnd: string | null, startTime: string | null, endTime: string | null) {
  if (!existingStart || !existingEnd || !startTime || !endTime) return true
  return startTime < existingEnd && existingStart < endTime
}

function firstDayOfMonth(date: Date) { return new Date(date.getFullYear(), date.getMonth(), 1) }
function lastDayOfMonth(date: Date) { return new Date(date.getFullYear(), date.getMonth() + 1, 0) }
function addMonths(date: Date, amount: number) { return new Date(date.getFullYear(), date.getMonth() + amount, 1) }
function atStartOfDay(date: Date) { return new Date(date.getFullYear(), date.getMonth(), date.getDate()) }
function isPastDate(date: Date, today: Date) { return atStartOfDay(date).getTime() < today.getTime() }
function toDateValue(date: Date) { return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}` }
function toLocalDate(value: string) { const [year, month, day] = value.split('-').map(Number); return new Date(year, month - 1, day) }
function capitalize(value: string) { return `${value.charAt(0).toUpperCase()}${value.slice(1)}` }
function formatTimeRange(startTime: string | null, endTime: string | null) { return startTime && endTime ? `${startTime.slice(0, 5)} - ${endTime.slice(0, 5)}` : 'Horário a combinar' }
function formatCurrency(value: number) { return new Intl.NumberFormat('pt-PT', { style: 'currency', currency: 'EUR' }).format(value) }
