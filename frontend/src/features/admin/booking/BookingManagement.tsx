import { useCallback, useEffect, useRef, useState, type FormEvent } from 'react'
import { decideBooking, getAdminBookings } from '../../../services/bookingService'
import type { Booking, BookingDecisionStatus, BookingStatus } from '../../../types/booking'
import { bookingEmptyMessage, bookingFilters, defaultBookingFilter, toBookingStatusFilter, type BookingFilter } from './bookingFilters'
import { bookingSyncIntervalMs, bookingsRefreshEvent } from '../../booking/bookingRefresh'
import styles from './BookingManagement.module.css'

type BookingNotice = {
  type: 'success' | 'error'
  text: string
}

type BookingDecision = BookingDecisionStatus

type AdminBooking = Booking

type DecisionDraft = {
  bookingId: string
  status: BookingDecision
  message: string
  counterEventDate: string
  counterStartTime: string
  counterEndTime: string
  counterBudget: string
  currentEventDate: string
  currentStartTime: string
  currentEndTime: string
  currentBudget: string
}

const statusLabels: Record<BookingStatus, string> = {
  PENDING: 'Pendente',
  ACCEPTED: 'Aceite',
  DECLINED: 'Recusado',
  COUNTER_PROPOSED: 'Alteração proposta',
  CANCELLED: 'Cancelado',
}

const eventTypeLabels: Record<string, string> = {
  WEDDING: 'Casamento',
  BAPTISM: 'Batizado',
  BIRTHDAY: 'Aniversário',
  CORPORATE: 'Evento corporativo',
  FESTIVAL: 'Festival',
  PRIVATE_PARTY: 'Festa privada',
  OTHER: 'Outro evento',
}

export function BookingManagement({
  token,
  onNotice,
}: {
  token: string
  onNotice: (notice: BookingNotice) => void
}) {
  const [bookings, setBookings] = useState<AdminBooking[]>([])
  const [filter, setFilter] = useState<BookingFilter>(defaultBookingFilter)
  const [isLoading, setIsLoading] = useState(true)
  const [isSending, setIsSending] = useState(false)
  const [draft, setDraft] = useState<DecisionDraft | null>(null)
  const [formError, setFormError] = useState<string | null>(null)
  const bookingRequestRef = useRef<{ controller: AbortController | null; inFlight: boolean }>({
    controller: null,
    inFlight: false,
  })

  const loadBookings = useCallback(async (background = false) => {
    if (background && bookingRequestRef.current.inFlight) return

    if (!background) {
      bookingRequestRef.current.controller?.abort()
      setIsLoading(true)
    }

    const controller = new AbortController()
    bookingRequestRef.current = { controller, inFlight: true }

    try {
      const response = await getAdminBookings(token, toBookingStatusFilter(filter), { signal: controller.signal })
      if (!controller.signal.aborted) setBookings(response)
    } catch (error) {
      if (controller.signal.aborted) return
      if (!background) {
        onNotice({
          type: 'error',
          text: error instanceof Error ? error.message : 'Não foi possível carregar os agendamentos.',
        })
      }
    } finally {
      if (bookingRequestRef.current.controller === controller) {
        bookingRequestRef.current = { controller: null, inFlight: false }
        if (!background) setIsLoading(false)
      }
    }
  }, [filter, onNotice, token])

  useEffect(() => {
    const requestId = window.setTimeout(() => {
      void loadBookings(false)
    }, 0)

    const refreshInBackground = () => {
      if (document.visibilityState === 'visible') void loadBookings(true)
    }

    const intervalId = window.setInterval(refreshInBackground, bookingSyncIntervalMs)
    window.addEventListener(bookingsRefreshEvent, refreshInBackground)
    window.addEventListener('focus', refreshInBackground)
    document.addEventListener('visibilitychange', refreshInBackground)

    return () => {
      window.clearTimeout(requestId)
      window.clearInterval(intervalId)
      bookingRequestRef.current.controller?.abort()
      bookingRequestRef.current = { controller: null, inFlight: false }
      window.removeEventListener(bookingsRefreshEvent, refreshInBackground)
      window.removeEventListener('focus', refreshInBackground)
      document.removeEventListener('visibilitychange', refreshInBackground)
    }
  }, [loadBookings])

  function openDecision(booking: AdminBooking, status: BookingDecision) {
    const hasPendingProposal = booking.status === 'COUNTER_PROPOSED' && booking.counterProposal !== null
    const effectiveEventDate = hasPendingProposal
      ? booking.counterProposal?.eventDate ?? booking.eventDate
      : booking.eventDate
    const effectiveStartTime = hasPendingProposal
      ? booking.counterProposal?.startTime ?? booking.startTime
      : booking.startTime
    const effectiveEndTime = hasPendingProposal
      ? booking.counterProposal?.endTime ?? booking.endTime
      : booking.endTime
    const effectiveBudget = hasPendingProposal
      ? booking.counterProposal?.budget ?? booking.budget
      : booking.budget

    setFormError(null)
    setDraft({
      bookingId: booking.id,
      status,
      message: '',
      counterEventDate: effectiveEventDate,
      counterStartTime: effectiveStartTime?.slice(0, 5) ?? '',
      counterEndTime: effectiveEndTime?.slice(0, 5) ?? '',
      counterBudget: effectiveBudget == null ? '' : String(effectiveBudget),
      currentEventDate: effectiveEventDate,
      currentStartTime: effectiveStartTime?.slice(0, 5) ?? '',
      currentEndTime: effectiveEndTime?.slice(0, 5) ?? '',
      currentBudget: effectiveBudget == null ? '' : String(effectiveBudget),
    })
  }

  function cancelDecision() {
    setDraft(null)
    setFormError(null)
  }

  async function submitDecision(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!draft || isSending) return

    if (draft.status === 'COUNTER_PROPOSED') {
      if (!draft.counterEventDate) {
        setFormError('Escolhe a data proposta para o evento.')
        return
      }

      if ((draft.counterStartTime && !draft.counterEndTime) || (!draft.counterStartTime && draft.counterEndTime)) {
        setFormError('Indica a hora de início e fim, ou deixa ambas em branco.')
        return
      }

      if (draft.counterStartTime && draft.counterEndTime && draft.counterStartTime >= draft.counterEndTime) {
        setFormError('A hora de fim tem de ser posterior à hora de início.')
        return
      }

      const hasBudget = draft.counterBudget.trim() !== ''
      const counterBudget = Number(draft.counterBudget)
      if (hasBudget && (!Number.isFinite(counterBudget) || counterBudget <= 0)) {
        setFormError('O orçamento proposto tem de ser um valor superior a 0 €.')
        return
      }
      if (draft.counterEventDate === draft.currentEventDate
          && draft.counterStartTime === draft.currentStartTime
          && draft.counterEndTime === draft.currentEndTime
          && draft.counterBudget === draft.currentBudget) {
        setFormError('Altera a data, o horário ou o orçamento antes de enviar a proposta.')
        return
      }
    }

    if (draft.status === 'CANCELLED' && !draft.message.trim()) {
      setFormError('Indica a justificação do cancelamento.')
      return
    }

    setIsSending(true)
    setFormError(null)

    try {
      const counterBudget = Number(draft.counterBudget)
      await decideBooking(draft.bookingId, {
        status: draft.status,
        ...(draft.message.trim() ? { message: draft.message.trim() } : {}),
        ...(draft.status === 'COUNTER_PROPOSED' ? {
          counterEventDate: draft.counterEventDate,
          counterStartTime: draft.counterStartTime || null,
          counterEndTime: draft.counterEndTime || null,
          ...(draft.counterBudget.trim() ? { counterBudget } : {}),
        } : {}),
      }, token)

      const labels: Record<BookingDecision, string> = {
        ACCEPTED: 'Pedido confirmado.',
        DECLINED: 'Pedido rejeitado.',
        COUNTER_PROPOSED: 'Alteração enviada.',
        CANCELLED: 'Evento cancelado.',
      }
      onNotice({ type: 'success', text: labels[draft.status] })
      cancelDecision()
      await loadBookings(true)
    } catch (error) {
      setFormError(error instanceof Error ? error.message : 'Não foi possível guardar a decisão.')
    } finally {
      setIsSending(false)
    }
  }

  return (
    <section className={styles.page} aria-label="Gestão de agendamentos">
      <header className={styles.header}>
        <div>
          <p>Consulta pedidos privados, confirma horários e mantém a agenda atualizada.</p>
        </div>
        <div className={styles.controls}>
          <label>
            Estado
            <select
              disabled={isLoading || isSending}
              onChange={(event) => {
                setDraft(null)
                setFormError(null)
                setFilter(event.target.value as BookingFilter)
              }}
              value={filter}
            >
              {bookingFilters.map((option) => (
                <option key={option.value} value={option.value}>{option.label}</option>
              ))}
            </select>
          </label>
          <button disabled={isLoading || isSending} type="button" onClick={() => { void loadBookings(false) }}>
            {isLoading ? 'A atualizar...' : 'Atualizar lista'}
          </button>
        </div>
      </header>

      {isLoading ? (
        <p className={styles.feedback}>A carregar pedidos...</p>
      ) : bookings.length === 0 ? (
        <p className={styles.feedback}>
          {bookingEmptyMessage(filter)}
        </p>
      ) : (
        <div className={styles.list}>
          {bookings.map((booking) => (
            <article className={styles.booking} key={booking.id}>
              <div className={styles.bookingHeader}>
                <div>
                  <p className={styles.eyebrow}>{eventTypeLabels[booking.eventType] ?? booking.eventType}</p>
                  <h3>{booking.profileName}</h3>
                  {booking.eventType === 'OTHER' && booking.customEventType && <p className={styles.subline}>{booking.customEventType}</p>}
                </div>
                <span className={`${styles.status} ${styles[`status${booking.status}`]}`}>
                  {statusLabels[booking.status]}
                </span>
              </div>

              <dl className={styles.details}>
                <div>
                  <dt>Data</dt>
                  <dd>{formatDate(booking.eventDate)}</dd>
                </div>
                <div>
                  <dt>Horário</dt>
                  <dd>{formatTimeRange(booking.startTime, booking.endTime)}</dd>
                </div>
                <div>
                  <dt>Local</dt>
                  <dd>{booking.location || 'Não indicado'}</dd>
                </div>
                <div>
                  <dt>Cliente</dt>
                  <dd>{booking.contactName}</dd>
                </div>
                {booking.eventType === 'WEDDING' && booking.weddingCoupleNames && (
                  <div>
                    <dt>Noivos</dt>
                    <dd>{booking.weddingCoupleNames}</dd>
                  </div>
                )}
                <div>
                  <dt>Email</dt>
                  <dd>{booking.contactEmail ? <a href={`mailto:${booking.contactEmail}`}>{booking.contactEmail}</a> : 'Não indicado'}</dd>
                </div>
                <div>
                  <dt>Telemóvel</dt>
                  <dd><a href={`tel:${booking.contactPhone.replace(/\s/g, '')}`}>{booking.contactPhone}</a></dd>
                </div>
                {booking.budget != null && (
                  <div>
                    <dt>Orçamento acordado</dt>
                    <dd>{formatCurrency(booking.budget)}</dd>
                  </div>
                )}
              </dl>

              <section className={styles.request} aria-label="Descrição do pedido">
                <h4>Descrição</h4>
                <p>{booking.description}</p>
                {booking.notes && (
                  <>
                    <h4>Notas do cliente</h4>
                    <p>{booking.notes}</p>
                  </>
                )}
              </section>

              {booking.counterProposal && (
                <section className={styles.counterSummary} aria-label="Alteração proposta">
                  <strong>{booking.counterProposal.proposedBy === 'CUSTOMER' ? 'Contraproposta do cliente' : 'Proposta enviada ao cliente'}</strong>
                  <span>
                    {booking.counterProposal.budget != null && formatCurrency(booking.counterProposal.budget)}
                    {booking.counterProposal.budget != null && booking.counterProposal.eventDate && ' · '}
                    {booking.counterProposal.eventDate && `Data alternativa: ${formatDate(booking.counterProposal.eventDate)}`}
                    {(booking.counterProposal.budget != null || booking.counterProposal.eventDate) && booking.counterProposal.startTime && ' · '}
                    {booking.counterProposal.startTime && `Horário: ${formatTimeRange(booking.counterProposal.startTime, booking.counterProposal.endTime)}`}
                    {booking.counterProposal.budget == null
                      && booking.counterProposal.eventDate == null
                      && booking.counterProposal.startTime == null
                      && 'Mantém ou restaura os termos originais do pedido.'}
                  </span>
                  {booking.counterProposal.proposedBy === 'ADMIN' && <small>A aguardar resposta do cliente.</small>}
                </section>
              )}

              {booking.message && (
                <section className={styles.message} aria-label="Mensagem da administração">
                  <strong>{booking.counterProposal?.proposedBy === 'CUSTOMER' ? 'Mensagem do cliente' : 'Mensagem da administração'}</strong>
                  <p>{booking.message}</p>
                </section>
              )}

              <p className={styles.createdAt}>Recebido em {formatDateTime(booking.createdAt)}</p>

              {booking.status === 'PENDING' && (
                <div className={styles.actions}>
                  <button disabled={isSending} type="button" onClick={() => openDecision(booking, 'ACCEPTED')}>Aceitar pedido</button>
                  <button disabled={isSending} type="button" onClick={() => openDecision(booking, 'COUNTER_PROPOSED')}>Propor alterações</button>
                  <button className={styles.declineButton} disabled={isSending} type="button" onClick={() => openDecision(booking, 'DECLINED')}>Recusar</button>
                </div>
              )}

              {booking.status === 'ACCEPTED' && (
                <div className={styles.actions}>
                  <button disabled={isSending} type="button" onClick={() => openDecision(booking, 'COUNTER_PROPOSED')}>Propor alteração</button>
                  <button className={styles.declineButton} disabled={isSending} type="button" onClick={() => openDecision(booking, 'CANCELLED')}>Cancelar evento</button>
                </div>
              )}

              {booking.status === 'COUNTER_PROPOSED' && booking.counterProposal?.proposedBy === 'CUSTOMER' && (
                <div className={styles.actions}>
                  <button disabled={isSending} type="button" onClick={() => openDecision(booking, 'ACCEPTED')}>Aceitar proposta</button>
                  <button disabled={isSending} type="button" onClick={() => openDecision(booking, 'COUNTER_PROPOSED')}>Responder com outra proposta</button>
                  <button className={styles.declineButton} disabled={isSending} type="button" onClick={() => openDecision(booking, 'DECLINED')}>Recusar</button>
                  <button className={styles.declineButton} disabled={isSending} type="button" onClick={() => openDecision(booking, 'CANCELLED')}>Cancelar</button>
                </div>
              )}

              {booking.status === 'COUNTER_PROPOSED' && booking.counterProposal?.proposedBy === 'ADMIN' && (
                <div className={styles.actions}>
                  <button disabled={isSending} type="button" onClick={() => openDecision(booking, 'COUNTER_PROPOSED')}>Rever proposta</button>
                  <button className={styles.declineButton} disabled={isSending} type="button" onClick={() => openDecision(booking, 'CANCELLED')}>Cancelar</button>
                </div>
              )}

              {draft?.bookingId === booking.id && (
                <form className={styles.decisionForm} onSubmit={(event) => { void submitDecision(event) }}>
                  <div className={styles.decisionHeading}>
                    <div>
                      <h4>{decisionTitle(draft.status)}</h4>
                      <p>{decisionDescription(draft.status)}</p>
                    </div>
                    <button aria-label="Fechar edição da decisão" className={styles.dismissDecisionButton} disabled={isSending} type="button" onClick={cancelDecision}>Fechar</button>
                  </div>

                  {draft.status === 'COUNTER_PROPOSED' && (
                    <div className={styles.scheduleFields}>
                      <label>
                        Data proposta
                        <input
                          min={todayDateValue()}
                          onChange={(event) => setDraft((current) => current ? { ...current, counterEventDate: event.target.value } : current)}
                          required
                          type="date"
                          value={draft.counterEventDate}
                        />
                      </label>
                      <label>
                        Hora de início <span>(opcional)</span>
                        <input
                          onChange={(event) => setDraft((current) => current ? { ...current, counterStartTime: event.target.value } : current)}
                          type="time"
                          value={draft.counterStartTime}
                        />
                      </label>
                      <label>
                        Hora de fim <span>(opcional)</span>
                        <input
                          onChange={(event) => setDraft((current) => current ? { ...current, counterEndTime: event.target.value } : current)}
                          type="time"
                          value={draft.counterEndTime}
                        />
                      </label>
                      <label>
                        Orçamento proposto
                        <input
                          inputMode="decimal"
                          min="0.01"
                          onChange={(event) => setDraft((current) => current ? { ...current, counterBudget: event.target.value } : current)}
                          placeholder="Ex.: 450"
                          step="0.01"
                          type="number"
                          value={draft.counterBudget}
                        />
                      </label>
                    </div>
                  )}

                  <label>
                    Mensagem para o cliente <span>{draft.status === 'CANCELLED' ? '(obrigatória)' : '(opcional)'}</span>
                    <textarea
                      maxLength={1000}
                      onChange={(event) => setDraft((current) => current ? { ...current, message: event.target.value } : current)}
                      placeholder={draft.status === 'CANCELLED'
                        ? 'Explica o motivo do cancelamento.'
                        : draft.status === 'ACCEPTED'
                          ? 'Confirma ao cliente que o pedido foi aceite como submetido.'
                          : 'Adiciona uma nota, se necessário.'}
                      required={draft.status === 'CANCELLED'}
                      value={draft.message}
                    />
                  </label>

                  {formError && <p className={styles.formError} role="alert">{formError}</p>}

                  <button className={styles.confirmButton} disabled={isSending} type="submit">
                    {isSending ? 'A guardar...' : decisionConfirmLabel(draft.status)}
                  </button>
                </form>
              )}
            </article>
          ))}
        </div>
      )}
    </section>
  )
}

function decisionTitle(status: BookingDecision) {
  if (status === 'ACCEPTED') return 'Aceitar pedido'
  if (status === 'DECLINED') return 'Rejeitar pedido'
  if (status === 'CANCELLED') return 'Cancelar evento'
  return 'Enviar alteração'
}

function decisionDescription(status: BookingDecision) {
  if (status === 'ACCEPTED') return 'Aceita exatamente os termos atuais do pedido ou da contraproposta do cliente.'
  if (status === 'DECLINED') return 'Informa o cliente de que o pedido não pode avançar.'
  if (status === 'CANCELLED') return 'Cancela o evento e regista a justificação para libertar o calendário.'
  return 'Regista a alteração proposta ao cliente.'
}

function decisionConfirmLabel(status: BookingDecision) {
  if (status === 'ACCEPTED') return 'Confirmar agendamento'
  if (status === 'DECLINED') return 'Confirmar rejeição'
  if (status === 'CANCELLED') return 'Confirmar cancelamento'
  return 'Enviar alteração'
}

function formatDate(value: string) {
  const date = new Date(`${value}T12:00:00`)
  if (Number.isNaN(date.getTime())) return value
  return new Intl.DateTimeFormat('pt-PT', { dateStyle: 'long' }).format(date)
}

function formatDateTime(value: string) {
  const date = new Date(value)
  if (!value || Number.isNaN(date.getTime())) return 'data indisponível'
  return new Intl.DateTimeFormat('pt-PT', { dateStyle: 'medium', timeStyle: 'short' }).format(date)
}

function formatTimeRange(startTime: string | null, endTime: string | null) {
  return startTime && endTime ? `${startTime.slice(0, 5)} - ${endTime.slice(0, 5)}` : 'Horário a combinar'
}

function formatCurrency(value: number) {
  return new Intl.NumberFormat('pt-PT', { style: 'currency', currency: 'EUR' }).format(value)
}

function todayDateValue() {
  const date = new Date()
  date.setHours(0, 0, 0, 0)
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${date.getFullYear()}-${month}-${day}`
}
