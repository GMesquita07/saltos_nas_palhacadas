import { useCallback, useEffect, useState, type FormEvent } from 'react'
import { decideBooking, getAdminBookings } from '../../../services/bookingService'
import type { Booking, BookingDecisionStatus, BookingStatus } from '../../../types/booking'
import styles from './BookingManagement.module.css'

type BookingNotice = {
  type: 'success' | 'error'
  text: string
}

type BookingFilter = BookingStatus | 'ALL'
type BookingDecision = BookingDecisionStatus

type AdminBooking = Booking

type DecisionDraft = {
  bookingId: string
  status: BookingDecision
  message: string
  eventDate: string
  startTime: string
  endTime: string
  agreedBudget: string
}

const filters: Array<{ value: BookingFilter; label: string }> = [
  { value: 'PENDING', label: 'Pendentes' },
  { value: 'ACCEPTED', label: 'Aceites' },
  { value: 'DECLINED', label: 'Recusados' },
  { value: 'CANCELLED', label: 'Cancelados' },
  { value: 'ALL', label: 'Todos' },
]

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
  const [filter, setFilter] = useState<BookingFilter>('PENDING')
  const [isLoading, setIsLoading] = useState(true)
  const [isSending, setIsSending] = useState(false)
  const [draft, setDraft] = useState<DecisionDraft | null>(null)
  const [formError, setFormError] = useState<string | null>(null)

  const loadBookings = useCallback(async () => {
    setIsLoading(true)

    try {
      const response = await getAdminBookings(token, filter === 'ALL' ? undefined : filter)
      setBookings(response)
    } catch (error) {
      onNotice({
        type: 'error',
        text: error instanceof Error ? error.message : 'Não foi possível carregar os agendamentos.',
      })
    } finally {
      setIsLoading(false)
    }
  }, [filter, onNotice, token])

  useEffect(() => {
    const requestId = window.setTimeout(() => {
      void loadBookings()
    }, 0)

    return () => window.clearTimeout(requestId)
  }, [loadBookings])

  function openDecision(booking: AdminBooking, status: BookingDecision) {
    setFormError(null)
    setDraft({
      bookingId: booking.id,
      status,
      message: '',
      eventDate: booking.eventDate,
      startTime: booking.startTime?.slice(0, 5) ?? '',
      endTime: booking.endTime?.slice(0, 5) ?? '',
      agreedBudget: booking.budget == null ? '' : String(booking.budget),
    })
  }

  function cancelDecision() {
    setDraft(null)
    setFormError(null)
  }

  async function submitDecision(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!draft || isSending) return

    if (draft.status === 'ACCEPTED') {
      if (!draft.eventDate) {
        setFormError('Escolhe a data confirmada para o evento.')
        return
      }

      if ((draft.startTime && !draft.endTime) || (!draft.startTime && draft.endTime)) {
        setFormError('Indica a hora de início e fim, ou deixa ambas em branco.')
        return
      }

      if (draft.startTime && draft.endTime && draft.startTime >= draft.endTime) {
        setFormError('A hora de fim tem de ser posterior à hora de início.')
        return
      }

      const hasAgreedBudget = draft.agreedBudget.trim() !== ''
      const agreedBudget = Number(draft.agreedBudget)
      if (hasAgreedBudget && (!Number.isFinite(agreedBudget) || agreedBudget <= 0)) {
        setFormError('O orçamento acordado tem de ser um valor superior a 0 €.')
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
      const agreedBudget = Number(draft.agreedBudget)
      await decideBooking(draft.bookingId, {
        status: draft.status,
        ...(draft.message.trim() ? { message: draft.message.trim() } : {}),
        ...(draft.status === 'ACCEPTED' ? {
          eventDate: draft.eventDate,
          startTime: draft.startTime || null,
          endTime: draft.endTime || null,
          ...(draft.agreedBudget.trim() ? { agreedBudget } : {}),
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
      await loadBookings()
    } catch (error) {
      setFormError(error instanceof Error ? error.message : 'Não foi possível guardar a decisão.')
    } finally {
      setIsSending(false)
    }
  }

  return (
    <section className={styles.page} aria-labelledby="bookings-heading">
      <header className={styles.header}>
        <div>
          <h2 id="bookings-heading">Agendamentos</h2>
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
              {filters.map((option) => (
                <option key={option.value} value={option.value}>{option.label}</option>
              ))}
            </select>
          </label>
          <button disabled={isLoading || isSending} type="button" onClick={() => { void loadBookings() }}>
            {isLoading ? 'A atualizar...' : 'Atualizar lista'}
          </button>
        </div>
      </header>

      {isLoading ? (
        <p className={styles.feedback}>A carregar pedidos...</p>
      ) : bookings.length === 0 ? (
        <p className={styles.feedback}>
          {filter === 'PENDING'
            ? 'Não há pedidos pendentes neste momento.'
            : 'Não existem agendamentos neste estado.'}
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
                  <strong>Alteração proposta</strong>
                  <span>
                    {booking.counterProposal.budget != null && formatCurrency(booking.counterProposal.budget)}
                    {booking.counterProposal.budget != null && booking.counterProposal.eventDate && ' · '}
                    {booking.counterProposal.eventDate && `Data alternativa: ${formatDate(booking.counterProposal.eventDate)}`}
                  </span>
                </section>
              )}

              {booking.message && (
                <section className={styles.message} aria-label="Mensagem da administração">
                  <strong>Mensagem da administração</strong>
                  <p>{booking.message}</p>
                </section>
              )}

              <p className={styles.createdAt}>Recebido em {formatDateTime(booking.createdAt)}</p>

              {booking.status === 'PENDING' && (
                <div className={styles.actions}>
                  <button disabled={isSending} type="button" onClick={() => openDecision(booking, 'ACCEPTED')}>Confirmar / alterar</button>
                  <button className={styles.declineButton} disabled={isSending} type="button" onClick={() => openDecision(booking, 'DECLINED')}>Recusar</button>
                </div>
              )}

              {booking.status === 'ACCEPTED' && (
                <div className={styles.actions}>
                  <button className={styles.declineButton} disabled={isSending} type="button" onClick={() => openDecision(booking, 'CANCELLED')}>Cancelar evento</button>
                </div>
              )}

              {draft?.bookingId === booking.id && (
                <form className={styles.decisionForm} onSubmit={(event) => { void submitDecision(event) }}>
                  <div className={styles.decisionHeading}>
                    <div>
                      <h4>{decisionTitle(draft.status)}</h4>
                      <p>{decisionDescription(draft.status)}</p>
                    </div>
                    <button disabled={isSending} type="button" onClick={cancelDecision}>Cancelar</button>
                  </div>

                  {draft.status === 'ACCEPTED' && (
                    <div className={styles.scheduleFields}>
                      <label>
                        Data confirmada
                        <input
                          min={todayDateValue()}
                          onChange={(event) => setDraft((current) => current ? { ...current, eventDate: event.target.value } : current)}
                          required
                          type="date"
                          value={draft.eventDate}
                        />
                      </label>
                      <label>
                        Hora de início <span>(opcional)</span>
                        <input
                          onChange={(event) => setDraft((current) => current ? { ...current, startTime: event.target.value } : current)}
                          type="time"
                          value={draft.startTime}
                        />
                      </label>
                      <label>
                        Hora de fim <span>(opcional)</span>
                        <input
                          onChange={(event) => setDraft((current) => current ? { ...current, endTime: event.target.value } : current)}
                          type="time"
                          value={draft.endTime}
                        />
                      </label>
                      <label>
                        Orçamento acordado <span>(só admin)</span>
                        <input
                          inputMode="decimal"
                          min="0.01"
                          onChange={(event) => setDraft((current) => current ? { ...current, agreedBudget: event.target.value } : current)}
                          placeholder="Ex.: 450"
                          step="0.01"
                          type="number"
                          value={draft.agreedBudget}
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
                          ? 'Indica qualquer ajuste combinado por telefone/email.'
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
  if (status === 'ACCEPTED') return 'Confirmar ou alterar pedido'
  if (status === 'DECLINED') return 'Rejeitar pedido'
  if (status === 'CANCELLED') return 'Cancelar evento'
  return 'Enviar alteração'
}

function decisionDescription(status: BookingDecision) {
  if (status === 'ACCEPTED') return 'Confirma a data pedida ou ajusta a data/hora combinada fora do site.'
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
