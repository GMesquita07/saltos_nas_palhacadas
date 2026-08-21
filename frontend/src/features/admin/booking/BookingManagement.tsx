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
  counterBudget: string
  counterEventDate: string
}

const filters: Array<{ value: BookingFilter; label: string }> = [
  { value: 'PENDING', label: 'Pendentes' },
  { value: 'COUNTER_PROPOSED', label: 'Com contraproposta' },
  { value: 'ACCEPTED', label: 'Aceites' },
  { value: 'DECLINED', label: 'Recusados' },
  { value: 'ALL', label: 'Todos' },
]

const statusLabels: Record<BookingStatus, string> = {
  PENDING: 'Pendente',
  ACCEPTED: 'Aceite',
  DECLINED: 'Recusado',
  COUNTER_PROPOSED: 'Contraproposta enviada',
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
      counterBudget: status === 'COUNTER_PROPOSED' && booking.counterProposal?.budget != null
        ? String(booking.counterProposal.budget)
        : '',
      counterEventDate: status === 'COUNTER_PROPOSED' ? booking.counterProposal?.eventDate ?? '' : '',
    })
  }

  function cancelDecision() {
    setDraft(null)
    setFormError(null)
  }

  async function submitDecision(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!draft || isSending) return

    const hasCounterBudget = draft.counterBudget.trim() !== ''
    const counterBudget = Number(draft.counterBudget)
    const hasCounterDate = draft.counterEventDate !== ''

    if (draft.status === 'COUNTER_PROPOSED') {
      if (!hasCounterBudget && !hasCounterDate) {
        setFormError('Indica uma nova proposta de orçamento, uma data alternativa ou ambos.')
        return
      }

      if (hasCounterBudget && (!Number.isFinite(counterBudget) || counterBudget <= 0)) {
        setFormError('O orçamento da contraproposta tem de ser um valor superior a 0 €.')
        return
      }
    }

    setIsSending(true)
    setFormError(null)

    try {
      await decideBooking(draft.bookingId, {
        status: draft.status,
        ...(draft.message.trim() ? { message: draft.message.trim() } : {}),
        ...(draft.status === 'COUNTER_PROPOSED' && hasCounterBudget ? { counterBudget } : {}),
        ...(draft.status === 'COUNTER_PROPOSED' && hasCounterDate ? { counterEventDate: draft.counterEventDate } : {}),
      }, token)

      const labels: Record<BookingDecision, string> = {
        ACCEPTED: 'Proposta aceite.',
        DECLINED: 'Proposta recusada.',
        COUNTER_PROPOSED: 'Contraproposta enviada.',
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
          <p>Consulta pedidos privados, decide cada proposta e mantém a agenda atualizada.</p>
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
        <p className={styles.feedback}>A carregar propostas...</p>
      ) : bookings.length === 0 ? (
        <p className={styles.feedback}>
          {filter === 'PENDING'
            ? 'Não há propostas pendentes neste momento.'
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
                </div>
                <span className={`${styles.status} ${styles[`status${booking.status}`]}`}>
                  {statusLabels[booking.status]}
                </span>
              </div>

              <dl className={styles.details}>
                <div>
                  <dt>Data pedida</dt>
                  <dd>{formatDate(booking.eventDate)}</dd>
                </div>
                <div>
                  <dt>Orçamento proposto</dt>
                  <dd>{formatCurrency(booking.budget)}</dd>
                </div>
                <div>
                  <dt>Cliente</dt>
                  <dd>{booking.contactName}</dd>
                </div>
                <div>
                  <dt>Contacto</dt>
                  <dd><a href={`tel:${booking.contactPhone.replace(/\s/g, '')}`}>{booking.contactPhone}</a></dd>
                </div>
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
                <section className={styles.counterSummary} aria-label="Contraproposta enviada">
                  <strong>Contraproposta enviada</strong>
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
                  <button disabled={isSending} type="button" onClick={() => openDecision(booking, 'ACCEPTED')}>Aceitar</button>
                  <button className={styles.declineButton} disabled={isSending} type="button" onClick={() => openDecision(booking, 'DECLINED')}>Recusar</button>
                  <button className={styles.counterButton} disabled={isSending} type="button" onClick={() => openDecision(booking, 'COUNTER_PROPOSED')}>Enviar contraproposta</button>
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

                  {draft.status === 'COUNTER_PROPOSED' && (
                    <div className={styles.counterFields}>
                      <label>
                        Novo orçamento (€)
                        <input
                          max="99999999.99"
                          inputMode="decimal"
                          min="0.01"
                          onChange={(event) => setDraft((current) => current ? { ...current, counterBudget: event.target.value } : current)}
                          placeholder="Ex.: 450"
                          step="0.01"
                          type="number"
                          value={draft.counterBudget}
                        />
                      </label>
                      <label>
                        Data alternativa
                        <input
                          min={tomorrowDateValue()}
                          onChange={(event) => setDraft((current) => current ? { ...current, counterEventDate: event.target.value } : current)}
                          type="date"
                          value={draft.counterEventDate}
                        />
                      </label>
                    </div>
                  )}

                  <label>
                    Mensagem para o cliente <span>(opcional)</span>
                    <textarea
                      maxLength={1000}
                      onChange={(event) => setDraft((current) => current ? { ...current, message: event.target.value } : current)}
                      placeholder={draft.status === 'COUNTER_PROPOSED'
                        ? 'Explica as condições da contraproposta.'
                        : 'Adiciona uma nota, se necessário.'}
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
  if (status === 'ACCEPTED') return 'Aceitar proposta'
  if (status === 'DECLINED') return 'Recusar proposta'
  return 'Enviar contraproposta'
}

function decisionDescription(status: BookingDecision) {
  if (status === 'ACCEPTED') return 'Confirma que o artista fica reservado para esta data.'
  if (status === 'DECLINED') return 'Informa o cliente de que a proposta não pode avançar.'
  return 'Indica um novo orçamento, uma data alternativa ou ambos.'
}

function decisionConfirmLabel(status: BookingDecision) {
  if (status === 'ACCEPTED') return 'Confirmar aceitação'
  if (status === 'DECLINED') return 'Confirmar recusa'
  return 'Enviar contraproposta'
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

function formatCurrency(value: number) {
  return new Intl.NumberFormat('pt-PT', { style: 'currency', currency: 'EUR' }).format(value)
}

function tomorrowDateValue() {
  const date = new Date()
  date.setHours(0, 0, 0, 0)
  date.setDate(date.getDate() + 1)
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${date.getFullYear()}-${month}-${day}`
}
