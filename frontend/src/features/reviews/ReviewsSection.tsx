import { useEffect, useMemo, useState, type FormEvent } from 'react'
import { useAuth } from '../auth/AuthContext'
import { getProfileReviews, submitProfileReview } from '../../services/reviewService'
import type { Profile } from '../../types/profile'
import type { Review } from '../../types/review'
import styles from './ReviewsSection.module.css'

type ReviewFormState = {
  reviewerName: string
  title: string
  comment: string
  rating: number
}

type ReviewsSectionProps = {
  profile: Profile
  onLoginClick: () => void
}

export function ReviewsSection({ profile, onLoginClick }: ReviewsSectionProps) {
  const { session } = useAuth()
  const [reviews, setReviews] = useState<Review[]>([])
  const [form, setForm] = useState<ReviewFormState>(() => emptyReviewForm(session?.email))
  const [error, setError] = useState<string | null>(null)
  const [notice, setNotice] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)

  useEffect(() => {
    let isCurrent = true

    const loadReviews = () => {
      void getProfileReviews(profile.slug)
        .then((items) => {
          if (isCurrent) setReviews(items)
        })
        .catch(() => {
          if (isCurrent) setReviews([])
        })
    }

    loadReviews()
    window.addEventListener('reviews:changed', loadReviews)

    return () => {
      isCurrent = false
      window.removeEventListener('reviews:changed', loadReviews)
    }
  }, [profile.slug])

  const average = useMemo(() => {
    if (reviews.length === 0) return 0
    return reviews.reduce((total, review) => total + review.rating, 0) / reviews.length
  }, [reviews])

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    if (!session) {
      onLoginClick()
      return
    }

    const validationError = validateReview(form)
    if (validationError) {
      setError(validationError)
      return
    }

    setIsSubmitting(true)
    setError(null)
    setNotice(null)

    try {
      await submitProfileReview(profile.slug, {
        reviewerName: form.reviewerName.trim(),
        title: form.title.trim(),
        comment: form.comment.trim(),
        rating: form.rating,
      }, session.token)
      setForm(emptyReviewForm(session.email))
      setNotice('Avaliação enviada. Vai aparecer aqui depois de ser aprovada.')
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'Não foi possível enviar a avaliação.')
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <section className={styles.section} aria-labelledby="reviews-title">
      <div className={styles.header}>
        <div>
          <p className="eyebrow">Avaliações</p>
          <h2 id="reviews-title">Opiniões sobre {profile.name}</h2>
        </div>
        {reviews.length > 0 && (
          <div className={styles.summary} aria-label={'Avaliação média ' + average.toFixed(1) + ' em 5'}>
            <span aria-hidden="true">★</span>
            <strong>{average.toFixed(1)}</strong>
            <small>{ratingLabel(average)} · {reviews.length} {reviews.length === 1 ? 'Opinião' : 'Opiniões'}</small>
          </div>
        )}
      </div>

      {reviews.length > 0 ? (
        <div className={styles.grid}>
          {reviews.map((review) => (
            <article className={styles.card} key={review.id}>
              <div className={styles.author}>
                <span aria-hidden="true">{initials(review.reviewerName)}</span>
                <div>
                  <strong>{review.reviewerName}</strong>
                  <small>Enviado a {review.reviewDate}</small>
                </div>
              </div>
              <div className={styles.rating} aria-label={review.rating + ' em 5 estrelas'}>
                <span aria-hidden="true">{stars(review.rating)}</span>
                <strong>{review.rating.toFixed(1)}</strong>
              </div>
              <h3>{review.title}</h3>
              <p>{review.comment}</p>
            </article>
          ))}
        </div>
      ) : (
        <p className={styles.empty}>Ainda não existem opiniões publicadas sobre este artista.</p>
      )}

      <div className={styles.reviewPrompt}>
        <h3>Deixar uma opinião</h3>
        {session ? (
          <form className={styles.form} onSubmit={(event) => { void submit(event) }}>
            <label>
              Nome a apresentar
              <input
                maxLength={120}
                minLength={2}
                onChange={(event) => setForm((current) => ({ ...current, reviewerName: event.target.value }))}
                required
                value={form.reviewerName}
              />
            </label>
            <label>
              Avaliação
              <div className={styles.starPicker} aria-label="Escolher avaliação de 1 a 5 estrelas" role="radiogroup">
                {[1, 2, 3, 4, 5].map((rating) => (
                  <button
                    aria-checked={form.rating === rating}
                    aria-label={rating + ' ' + (rating === 1 ? 'estrela' : 'estrelas')}
                    className={rating <= form.rating ? styles.selectedStar : ''}
                    key={rating}
                    onClick={() => setForm((current) => ({ ...current, rating }))}
                    role="radio"
                    type="button"
                  >
                    ★
                  </button>
                ))}
              </div>
              <small className={styles.ratingPreview}>{form.rating.toFixed(1)} em 5</small>
            </label>
            <label>
              Título
              <input
                maxLength={180}
                minLength={2}
                onChange={(event) => setForm((current) => ({ ...current, title: event.target.value }))}
                placeholder="Excelente animação"
                required
                value={form.title}
              />
            </label>
            <label>
              Comentário
              <textarea
                maxLength={1200}
                minLength={10}
                onChange={(event) => setForm((current) => ({ ...current, comment: event.target.value }))}
                required
                value={form.comment}
              />
            </label>
            {error && <p className={styles.error} role="alert">{error}</p>}
            {notice && <p className={styles.success} role="status">{notice}</p>}
            <button disabled={isSubmitting} type="submit">{isSubmitting ? 'A enviar...' : 'Enviar avaliação'}</button>
          </form>
        ) : (
          <div className={styles.loginPrompt}>
            <p>Inicia sessão para deixar uma opinião sobre este artista.</p>
            <button type="button" onClick={onLoginClick}>Entrar para avaliar</button>
          </div>
        )}
      </div>
    </section>
  )
}

function emptyReviewForm(email?: string): ReviewFormState {
  return {
    reviewerName: email ? userNameFromEmail(email) : '',
    title: '',
    comment: '',
    rating: 5,
  }
}

function validateReview(form: ReviewFormState) {
  if (form.reviewerName.trim().length < 2) return 'Indica o nome que queres apresentar.'
  if (form.rating < 1 || form.rating > 5) return 'Seleciona uma avaliação entre 1 e 5 estrelas.'
  if (form.title.trim().length < 2) return 'Escreve um título curto para a avaliação.'
  if (form.comment.trim().length < 10) return 'O comentário deve ter pelo menos 10 caracteres.'
  return null
}

function stars(rating: number) {
  return Array.from({ length: 5 }, (_, index) => index < rating ? '★' : '☆').join('')
}

function initials(name: string) {
  return name
    .split(/\s+/)
    .filter(Boolean)
    .map((part) => part[0])
    .join('')
    .slice(0, 2)
    .toUpperCase()
}

function userNameFromEmail(email: string) {
  return email.split('@')[0]?.replace(/[._-]+/g, ' ').trim() || email
}

function ratingLabel(average: number) {
  if (average >= 4.8) return 'Fantástico'
  if (average >= 4.2) return 'Excelente'
  if (average >= 3.2) return 'Muito bom'
  return 'Bom'
}
