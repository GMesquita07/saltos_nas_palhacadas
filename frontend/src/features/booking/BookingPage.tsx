import { useEffect, useMemo, useRef, useState } from 'react'
import { useAuth } from '../auth/AuthContext'
import { createBooking, getBookedDates, getMyBookings, respondToCounterProposal } from '../../services/bookingService'
import type { Booking, BookingCounterProposalDecision, BookingProposal, BookingStatus } from '../../types/booking'
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
  const [bookedDates, setBookedDates] = useState<string[]>([])
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
  const bookedDateSet = useMemo(() => new Set(bookedDates), [bookedDates])
  const calendarDays = useMemo(() => getCalendarDays(visibleMonth), [visibleMonth])
  const today = useMemo(() => atStartOfDay(new Date()), [])
  const canMoveToPreviousMonth = firstDayOfMonth(visibleMonth).getTime() > firstDayOfMonth(today).getTime()

  useEffect(() => {
    if (!selectedProfileSlug) {
      return
    }

    let isCurrent = true
    const from = toDateValue(firstDayOfMonth(visibleMonth))
    const to = toDateValue(lastDayOfMonth(visibleMonth))

    void getBookedDates(selectedProfileSlug, from, to)
      .then((dates) => {
        if (isCurrent) setBookedDates(dates)
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
    setBookedDates([])
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
    if (isPastDate(date, today) || bookedDateSet.has(toDateValue(date))) return

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

    const form = new FormData(event.currentTarget)
    const budget = Number(form.get('budget'))
    const contactName = String(form.get('contactName') ?? '').trim()
    const contactPhone = String(form.get('contactPhone') ?? '').trim()
    const description = String(form.get('description') ?? '').trim()

    if (!selectedProfileSlug || !selectedDate) {
      setSubmitError('Escolhe primeiro o artista e uma data disponível.')
      return
    }
    if (!contactName || !contactPhone || !description) {
      setSubmitError('Preenche o teu nome, contacto e descrição do evento.')
      return
    }
    if (!Number.isFinite(budget) || budget <= 0) {
      setSubmitError('Indica uma proposta de orçamento superior a 0 €.')
      return
    }

    const proposal: BookingProposal = {
      profileSlug: selectedProfileSlug,
      eventDate: selectedDate,
      eventType: String(form.get('eventType') ?? ''),
      contactName,
      contactPhone,
      budget,
      description,
      notes: String(form.get('notes') ?? '').trim(),
    }

    setIsSubmitting(true)
    setSubmitError(null)
    setSubmitSuccess(null)
    try {
      const booking = await createBooking(proposal, session.token)
      setMyBookings((current) => [booking, ...current])
      setSubmitSuccess('A tua proposta foi enviada. Vais receber uma resposta nesta página.')
      event.currentTarget.reset()
      setSelectedDate('')
    } catch (reason) {
      setSubmitError(reason instanceof Error ? reason.message : 'Não foi possível enviar a proposta.')
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
        setBookedDates((current) => current.includes(updatedBooking.eventDate)
          ? current
          : [...current, updatedBooking.eventDate])
      }
      setCounterProposalFeedback({
        bookingId,
        type: 'success',
        message: decision === 'ACCEPTED'
          ? 'Aceitaste a contraproposta. O evento ficou confirmado.'
          : 'Recusaste a contraproposta. O pedido foi encerrado.',
      })
    } catch (reason) {
      setCounterProposalFeedback({
        bookingId,
        type: 'error',
        message: reason instanceof Error ? reason.message : 'Não foi possível responder à contraproposta. Tenta novamente.',
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
        <p>Consulta a disponibilidade, envia uma proposta e acompanha a resposta num só lugar.</p>
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
                  const isBooked = bookedDateSet.has(dateValue)
                  const isPast = isPastDate(date, today)
                  const isSelected = selectedDate === dateValue
                  const isDisabled = !isCurrentMonth || isPast || isBooked
                  const dayState = isBooked ? 'Indisponível' : isPast ? 'Data passada' : isSelected ? 'Data selecionada' : 'Disponível'

                  return (
                    <button
                      aria-label={`${dateFormatter.format(date)} · ${dayState}`}
                      aria-pressed={isSelected}
                      className={`${styles.day} ${!isCurrentMonth ? styles.outsideMonth : ''} ${isBooked ? styles.booked : ''} ${isSelected ? styles.selected : ''}`}
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
              <div className={styles.legend} aria-label="Legenda do calendário"><span><i className={styles.available} />Disponível</span><span><i className={styles.unavailable} />Com evento</span></div>
              {isAvailabilityLoading && <p className={styles.calendarFeedback}>A atualizar disponibilidade...</p>}
              {availabilityError && <p className={styles.error} role="status">{availabilityError}</p>}
            </>
          )}
        </div>

        <form className={styles.form} onSubmit={(event) => { void handleSubmit(event) }}>
          <div className={styles.sectionHeading}>
            <p className="eyebrow">2. Proposta</p>
            <h2>Conta-nos sobre o evento</h2>
          </div>
          {selectedProfile && <p className={styles.selection}>Pedido para <strong>{selectedProfile.name}</strong>{selectedDate ? <> em <strong>{dateFormatter.format(toLocalDate(selectedDate))}</strong></> : ''}.</p>}

          <div className={styles.formGrid}>
            <label className={styles.field}>
              <span>Tipo de evento</span>
              <select defaultValue="" name="eventType" required>
                <option disabled value="">Seleciona uma opção</option>
                {eventTypes.map((eventType) => <option key={eventType.value} value={eventType.value}>{eventType.label}</option>)}
              </select>
            </label>
            <label className={styles.field}>
              <span>Proposta de orçamento (€)</span>
              <input min="1" name="budget" placeholder="Ex.: 650" required step="0.01" type="number" />
            </label>
            <label className={styles.field}>
              <span>O teu nome</span>
              <input autoComplete="name" maxLength={100} name="contactName" placeholder="Nome e apelido" required />
            </label>
            <label className={styles.field}>
              <span>Contacto telefónico</span>
              <input autoComplete="tel" inputMode="tel" maxLength={30} name="contactPhone" placeholder="Ex.: 912 345 678" required type="tel" />
            </label>
          </div>
          <label className={styles.field}>
            <span>Descrição do evento</span>
            <textarea maxLength={1500} minLength={10} name="description" placeholder="Indica o local, horário previsto, número de convidados e o ambiente que procuras." required rows={5} />
          </label>
          <label className={styles.field}>
            <span>Notas adicionais <em>Opcional</em></span>
            <textarea maxLength={1000} name="notes" placeholder="Algum detalhe adicional que ajude a preparar a proposta." rows={3} />
          </label>

          {submitError && <p className={styles.error} role="alert">{submitError}</p>}
          {submitSuccess && <p className={styles.success} role="status">{submitSuccess}</p>}
          {session ? (
            <button className={styles.submit} disabled={isSubmitting || !selectedProfile || !selectedDate} type="submit">{isSubmitting ? 'A enviar proposta...' : 'Enviar proposta'}</button>
          ) : (
            <button className={styles.submit} type="button" onClick={onRequireLogin}>Login para enviar proposta</button>
          )}
          {!session && <p className={styles.loginHint}>Podes consultar a agenda sem conta. Para enviar e acompanhar uma proposta, inicia sessão ou cria uma conta.</p>}
        </form>
      </div>

      {session && (
        <section className={styles.requests}>
          <div className={styles.sectionHeading}>
            <p className="eyebrow">Área privada</p>
            <h2>Os meus pedidos</h2>
          </div>
          {isBookingsLoading ? <p className={styles.feedback}>A carregar os teus pedidos...</p> : bookingsError ? <p className={styles.error} role="status">{bookingsError}</p> : myBookings.length === 0 ? <p className={styles.feedback}>Ainda não enviaste nenhuma proposta. Escolhe uma data disponível para começar.</p> : <BookingList bookings={myBookings} counterProposalFeedback={counterProposalFeedback} onCounterProposalDecision={handleCounterProposalDecision} respondingBookingId={respondingBookingId} respondingCounterDecision={respondingCounterDecision} />}
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
            <h3>{eventTypeLabel(booking.eventType)} · {dateFormatter.format(toLocalDate(booking.eventDate))}</h3>
            <dl>
              <div><dt>Orçamento proposto</dt><dd>{formatCurrency(booking.budget)}</dd></div>
              <div><dt>Enviado em</dt><dd>{booking.createdAt ? dateFormatter.format(new Date(booking.createdAt)) : '—'}</dd></div>
            </dl>
            {booking.counterProposal && <div className={styles.counterProposal}><strong>Contra-proposta</strong><span>{[booking.counterProposal.budget === null ? null : formatCurrency(booking.counterProposal.budget), booking.counterProposal.eventDate ? dateFormatter.format(toLocalDate(booking.counterProposal.eventDate)) : null].filter(Boolean).join(' · ')}</span></div>}
            {booking.status === 'COUNTER_PROPOSED' && (
              <div className={styles.counterResponse}>
                <p>Queres aceitar esta contraproposta?</p>
                <div className={styles.counterActions}>
                  <button disabled={respondingBookingId !== null} type="button" onClick={() => onCounterProposalDecision(booking.id, 'ACCEPTED')}>
                    {respondingBookingId === booking.id && respondingCounterDecision === 'ACCEPTED' ? 'A aceitar...' : 'Aceitar contraproposta'}
                  </button>
                  <button className={styles.declineCounter} disabled={respondingBookingId !== null} type="button" onClick={() => onCounterProposalDecision(booking.id, 'DECLINED')}>
                    {respondingBookingId === booking.id && respondingCounterDecision === 'DECLINED' ? 'A recusar...' : 'Recusar contraproposta'}
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
  if (status === 'COUNTER_PROPOSED') return { label: 'Contra-proposta', className: 'countered' }
  return { label: 'Em análise', className: 'pending' }
}

function eventTypeLabel(eventType: string) {
  return eventTypes.find((type) => type.value === eventType)?.label ?? eventType
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

function firstDayOfMonth(date: Date) { return new Date(date.getFullYear(), date.getMonth(), 1) }
function lastDayOfMonth(date: Date) { return new Date(date.getFullYear(), date.getMonth() + 1, 0) }
function addMonths(date: Date, amount: number) { return new Date(date.getFullYear(), date.getMonth() + amount, 1) }
function atStartOfDay(date: Date) { return new Date(date.getFullYear(), date.getMonth(), date.getDate()) }
function isPastDate(date: Date, today: Date) { return atStartOfDay(date).getTime() < today.getTime() }
function toDateValue(date: Date) { return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}` }
function toLocalDate(value: string) { const [year, month, day] = value.split('-').map(Number); return new Date(year, month - 1, day) }
function capitalize(value: string) { return `${value.charAt(0).toUpperCase()}${value.slice(1)}` }
function formatCurrency(value: number) { return new Intl.NumberFormat('pt-PT', { style: 'currency', currency: 'EUR' }).format(value) }
