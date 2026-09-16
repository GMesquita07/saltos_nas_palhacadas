import { useCallback, useEffect, useMemo, useState, type ChangeEvent, type FormEvent } from 'react'
import { useAuthenticatedMediaUrl } from '../../components/AuthenticatedMedia'
import { uploadClientContentMedia } from '../../services/apiClient'
import { getMyClientContent, getPublishedClientContent, submitClientContent } from '../../services/clientContentService'
import { useAuth } from '../auth/AuthContext'
import type { ClientContentMediaType, ClientContentPost, ClientContentPublicIdentity, ClientContentStatus } from '../../types/clientContent'
import type { Profile } from '../../types/profile'
import styles from './ClientContentPage.module.css'

type ClientContentPageProps = {
  profiles: Profile[]
  onLogin: () => void
}

type ClientContentFormState = {
  profileSlug: string
  title: string
  location: string
  eventDate: string
  caption: string
  mediaId: string
  mediaUrl: string
  mediaType: ClientContentMediaType | null
  thumbnailId: string
  thumbnailUrl: string
  publicIdentity: ClientContentPublicIdentity
  customDisplayName: string
  showLocation: boolean
  showEventDate: boolean
  consentToPublish: boolean
}

type Notice = {
  type: 'success' | 'error'
  text: string
}

type Filter = 'Todos' | 'Foto' | 'Vídeo'

const emptyForm = (): ClientContentFormState => ({
  profileSlug: '',
  title: '',
  location: '',
  eventDate: '',
  caption: '',
  mediaId: '',
  mediaUrl: '',
  mediaType: null,
  thumbnailId: '',
  thumbnailUrl: '',
  publicIdentity: 'ANONYMOUS',
  customDisplayName: '',
  showLocation: false,
  showEventDate: false,
  consentToPublish: false,
})

const statusLabels: Record<ClientContentStatus, string> = {
  PENDING: 'Pendente',
  APPROVED: 'Aprovada',
  REJECTED: 'Recusada',
}

export function ClientContentPage({ profiles, onLogin }: ClientContentPageProps) {
  const { session } = useAuth()
  const [form, setForm] = useState<ClientContentFormState>(emptyForm)
  const [filter, setFilter] = useState<Filter>('Todos')
  const [publishedPosts, setPublishedPosts] = useState<ClientContentPost[]>([])
  const [myPosts, setMyPosts] = useState<ClientContentPost[]>([])
  const [notice, setNotice] = useState<Notice | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [isMineLoading, setIsMineLoading] = useState(false)
  const [isSaving, setIsSaving] = useState(false)
  const [uploading, setUploading] = useState<'media' | 'thumbnail' | null>(null)

  const loadPublishedPosts = useCallback(async () => {
    try {
      setPublishedPosts(await getPublishedClientContent())
    } catch {
      setNotice({ type: 'error', text: 'Não foi possível carregar as partilhas dos clientes.' })
    } finally {
      setIsLoading(false)
    }
  }, [])

  const loadMyPosts = useCallback(async () => {
    if (!session) {
      await Promise.resolve()
      setMyPosts([])
      return
    }

    try {
      setMyPosts(await getMyClientContent(session.token))
    } catch {
      setNotice({ type: 'error', text: 'Não foi possível carregar as tuas publicações.' })
    } finally {
      setIsMineLoading(false)
    }
  }, [session])

  useEffect(() => {
    let isCurrent = true

    void getPublishedClientContent()
      .then((posts) => { if (isCurrent) setPublishedPosts(posts) })
      .catch(() => { if (isCurrent) setNotice({ type: 'error', text: 'Não foi possível carregar as partilhas dos clientes.' }) })
      .finally(() => { if (isCurrent) setIsLoading(false) })

    window.addEventListener('client-content:changed', loadPublishedPosts)
    return () => {
      isCurrent = false
      window.removeEventListener('client-content:changed', loadPublishedPosts)
    }
  }, [loadPublishedPosts])

  useEffect(() => {
    if (!session) return
    let isCurrent = true

    void getMyClientContent(session.token)
      .then((posts) => { if (isCurrent) setMyPosts(posts) })
      .catch(() => { if (isCurrent) setNotice({ type: 'error', text: 'Não foi possível carregar as tuas publicações.' }) })
      .finally(() => { if (isCurrent) setIsMineLoading(false) })

    return () => {
      isCurrent = false
    }
  }, [session])

  async function handleMediaUpload(event: ChangeEvent<HTMLInputElement>) {
    const input = event.currentTarget
    const file = input.files?.[0]
    if (!file || !session) return

    if (!file.type.startsWith('image/') && !file.type.startsWith('video/')) {
      setNotice({ type: 'error', text: 'Seleciona uma fotografia ou vídeo válido.' })
      input.value = ''
      return
    }

    setUploading('media')
    try {
      const result = await uploadClientContentMedia(file, session.token)
      setForm((current) => ({
        ...current,
        mediaId: result.id,
        mediaUrl: result.url,
        mediaType: result.contentType.startsWith('video/') ? 'VIDEO' : 'PHOTO',
      }))
      setNotice({ type: 'success', text: 'Ficheiro principal carregado.' })
    } catch (error) {
      setNotice({ type: 'error', text: error instanceof Error ? error.message : 'Não foi possível carregar o ficheiro.' })
    } finally {
      setUploading(null)
      input.value = ''
    }
  }

  async function handleThumbnailUpload(event: ChangeEvent<HTMLInputElement>) {
    const input = event.currentTarget
    const file = input.files?.[0]
    if (!file || !session) return

    if (!file.type.startsWith('image/')) {
      setNotice({ type: 'error', text: 'A miniatura tem de ser uma imagem.' })
      input.value = ''
      return
    }

    setUploading('thumbnail')
    try {
      const result = await uploadClientContentMedia(file, session.token)
      setForm((current) => ({ ...current, thumbnailId: result.id, thumbnailUrl: result.url }))
      setNotice({ type: 'success', text: 'Miniatura carregada.' })
    } catch (error) {
      setNotice({ type: 'error', text: error instanceof Error ? error.message : 'Não foi possível carregar a miniatura.' })
    } finally {
      setUploading(null)
      input.value = ''
    }
  }

  async function submitPost(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const formElement = event.currentTarget

    if (!session || isSaving || uploading) return

    const validationError = validateForm(form, profiles)
    if (validationError) {
      setNotice({ type: 'error', text: validationError })
      return
    }

    setIsSaving(true)
    try {
      await submitClientContent({
        profileSlug: form.profileSlug,
        type: form.mediaType as ClientContentMediaType,
        mediaId: form.mediaId,
        thumbnailId: form.thumbnailId || null,
        title: form.title.trim(),
        location: form.location.trim(),
        eventDate: form.eventDate,
        caption: form.caption.trim(),
        publicIdentity: form.publicIdentity,
        customDisplayName: form.publicIdentity === 'CUSTOM' ? form.customDisplayName.trim() : null,
        showLocation: form.showLocation,
        showEventDate: form.showEventDate,
        consentToPublish: form.consentToPublish,
      }, session.token)
      setForm(emptyForm())
      formElement.reset()
      setNotice({ type: 'success', text: 'Publicação enviada. Fica pendente até ser aprovada pelo admin.' })
      await Promise.all([loadPublishedPosts(), loadMyPosts()])
    } catch (error) {
      setNotice({ type: 'error', text: error instanceof Error ? error.message : 'Não foi possível enviar a publicação.' })
    } finally {
      setIsSaving(false)
    }
  }

  const filteredPosts = useMemo(() => (
    publishedPosts.filter((post) => filter === 'Todos' || post.type === filter)
  ), [filter, publishedPosts])
  const groupedPosts = useMemo(() => groupClientContent(filteredPosts), [filteredPosts])

  return (
    <section className={styles.page}>
      <header className={styles.header}>
        <p className="eyebrow">Clientes</p>
        <h1>Partilhas dos clientes</h1>
        <p>Fotografias e vídeos dos eventos, publicados depois de aprovação.</p>
      </header>

      {notice && <p className={`${styles.notice} ${styles[notice.type]}`} role="status">{notice.text}</p>}

      <div className={styles.submissionArea}>
        <section className={styles.submissionPanel}>
          <div className={styles.sectionHeading}>
            <p className="eyebrow">Nova publicação</p>
            <h2>Partilha o teu momento</h2>
          </div>

          {session ? (
            <form className={styles.form} onSubmit={(event) => { void submitPost(event) }}>
              <label>
                Artista do evento
                <select
                  onChange={(event) => setForm((current) => ({ ...current, profileSlug: event.target.value }))}
                  required
                  value={form.profileSlug}
                >
                  <option value="">Seleciona um artista</option>
                  {profiles.map((profile) => (
                    <option key={profile.id} value={profile.slug}>{profile.name}</option>
                  ))}
                </select>
              </label>

              <div className={styles.formGrid}>
                <label>
                  Título
                  <input
                    maxLength={180}
                    minLength={2}
                    onChange={(event) => setForm((current) => ({ ...current, title: event.target.value }))}
                    placeholder="Festa de aniversário"
                    required
                    value={form.title}
                  />
                </label>

                <label>
                  Local
                  <input
                    maxLength={180}
                    minLength={2}
                    onChange={(event) => setForm((current) => ({ ...current, location: event.target.value }))}
                    placeholder="Viseu"
                    required
                    value={form.location}
                  />
                </label>
              </div>

              <label>
                Data do evento
                <input
                  max={todayIso()}
                  onChange={(event) => setForm((current) => ({ ...current, eventDate: event.target.value }))}
                  required
                  type="date"
                  value={form.eventDate}
                />
              </label>

              <label>
                Fotografia ou vídeo
                <input accept="image/*,video/*" disabled={uploading !== null} type="file" onChange={(event) => { void handleMediaUpload(event) }} />
              </label>

              {form.mediaUrl && (
                <p className={styles.uploadedFile}>
                  Ficheiro pronto: {form.mediaType === 'VIDEO' ? 'vídeo' : 'fotografia'}.
                </p>
              )}

              <label>
                Miniatura opcional
                <input accept="image/*" disabled={uploading !== null} type="file" onChange={(event) => { void handleThumbnailUpload(event) }} />
              </label>

              {form.thumbnailUrl && (
                <UploadedThumbnailPreview
                  onRemove={() => setForm((current) => ({ ...current, thumbnailId: '', thumbnailUrl: '' }))}
                  token={session.token}
                  url={form.thumbnailUrl}
                />
              )}

              <label>
                Legenda
                <textarea
                  maxLength={800}
                  minLength={2}
                  onChange={(event) => setForm((current) => ({ ...current, caption: event.target.value }))}
                  placeholder="Conta um detalhe especial deste evento"
                  required
                  value={form.caption}
                />
              </label>

              <fieldset className={styles.privacyChoices}>
                <legend>Como queres aparecer?</legend>
                <label>
                  <input
                    checked={form.publicIdentity === 'ANONYMOUS'}
                    name="publicIdentity"
                    onChange={() => setForm((current) => ({ ...current, publicIdentity: 'ANONYMOUS' }))}
                    type="radio"
                  />
                  Anónimo
                </label>
                <label>
                  <input
                    checked={form.publicIdentity === 'USERNAME'}
                    name="publicIdentity"
                    onChange={() => setForm((current) => ({ ...current, publicIdentity: 'USERNAME' }))}
                    type="radio"
                  />
                  @{session.username ?? 'username'}
                </label>
                <label>
                  <input
                    checked={form.publicIdentity === 'CUSTOM'}
                    name="publicIdentity"
                    onChange={() => setForm((current) => ({ ...current, publicIdentity: 'CUSTOM' }))}
                    type="radio"
                  />
                  Nome personalizado
                </label>
              </fieldset>

              {form.publicIdentity === 'CUSTOM' && (
                <label>
                  Nome público
                  <input
                    maxLength={80}
                    minLength={2}
                    onChange={(event) => setForm((current) => ({ ...current, customDisplayName: event.target.value }))}
                    placeholder="Ex.: Cliente feliz"
                    required
                    value={form.customDisplayName}
                  />
                </label>
              )}

              <div className={styles.checkboxGroup}>
                <label>
                  <input
                    checked={form.showLocation}
                    onChange={(event) => setForm((current) => ({ ...current, showLocation: event.target.checked }))}
                    type="checkbox"
                  />
                  Mostrar local na publicação pública
                </label>
                <label>
                  <input
                    checked={form.showEventDate}
                    onChange={(event) => setForm((current) => ({ ...current, showEventDate: event.target.checked }))}
                    type="checkbox"
                  />
                  Mostrar mês e ano do evento
                </label>
                <label>
                  <input
                    checked={form.consentToPublish}
                    onChange={(event) => setForm((current) => ({ ...current, consentToPublish: event.target.checked }))}
                    required
                    type="checkbox"
                  />
                  Confirmo que tenho direito a partilhar este conteúdo e autorizo a publicação caso seja aprovado.
                </label>
              </div>

              <button disabled={isSaving || uploading !== null} type="submit">
                {isSaving ? 'A enviar...' : uploading ? 'A carregar ficheiro...' : 'Enviar para aprovação'}
              </button>
            </form>
          ) : (
            <div className={styles.loginGate}>
              <p>Inicia sessão para submeter fotografias ou vídeos dos eventos em que participaste.</p>
              <button type="button" onClick={onLogin}>Login / Criar conta</button>
            </div>
          )}
        </section>

        {session && (
          <aside className={styles.myPosts}>
            <div className={styles.sectionHeading}>
              <p className="eyebrow">Conta</p>
              <h2>As tuas publicações</h2>
            </div>
            {isMineLoading ? (
              <p className={styles.feedback}>A carregar...</p>
            ) : myPosts.length === 0 ? (
              <p className={styles.feedback}>Ainda não enviaste publicações.</p>
            ) : (
              <div className={styles.myPostList}>
                {myPosts.map((post) => (
                  <article className={styles.myPostRow} key={post.id}>
                    <span className={`${styles.statusBadge} ${styles[post.status.toLowerCase()]}`}>{statusLabels[post.status]}</span>
                    <strong>{post.title}</strong>
                    <small>{post.profileName ?? 'Artista'} · {post.eventDate}</small>
                    {post.adminMessage && <p>{post.adminMessage}</p>}
                  </article>
                ))}
              </div>
            )}
          </aside>
        )}
      </div>

      <section className={styles.publicSection}>
        <div className={styles.publicHeading}>
          <div>
            <p className="eyebrow">Galeria</p>
            <h2>Publicações aprovadas</h2>
          </div>
          <div className={styles.filters} aria-label="Filtrar publicações">
            {(['Todos', 'Vídeo', 'Foto'] as Filter[]).map((option) => (
              <button className={filter === option ? styles.active : ''} key={option} type="button" onClick={() => setFilter(option)}>
                {option}
              </button>
            ))}
          </div>
        </div>

        {isLoading ? (
          <p className={styles.feedback}>A carregar partilhas...</p>
        ) : filteredPosts.length === 0 ? (
          <p className={styles.feedback}>Ainda não existem publicações aprovadas dos clientes.</p>
        ) : (
          <div className={styles.monthGroups}>
            {groupedPosts.map((group) => (
              <section className={styles.monthGroup} key={group.key}>
                <h3>{group.label}</h3>
                <div className={styles.grid}>
                  {group.items.map((post) => <ClientContentCard key={post.id} post={post} />)}
                </div>
              </section>
            ))}
          </div>
        )}
      </section>
    </section>
  )
}

function UploadedThumbnailPreview({ onRemove, token, url }: { onRemove: () => void; token: string; url: string }) {
  const resolvedUrl = useAuthenticatedMediaUrl(url, token)

  return (
    <div className={styles.thumbnailPreview}>
      {resolvedUrl ? <img src={resolvedUrl} alt="Miniatura carregada" /> : <p>Miniatura carregada.</p>}
      <button type="button" onClick={onRemove}>Remover miniatura</button>
    </div>
  )
}

function ClientContentCard({ post }: { post: ClientContentPost }) {
  const meta = [post.profileName ?? 'Evento', post.location, post.eventDate].filter(Boolean)

  return (
    <article className={styles.postCard}>
      <div className={styles.mediaFrame}>
        {post.mediaType === 'VIDEO'
          ? <video controls preload="metadata" poster={post.thumbnailUrl}><source src={post.mediaUrl} /></video>
          : <img src={post.mediaUrl} alt={post.title} />}
        <small>{post.type}</small>
      </div>
      <div className={styles.postDetails}>
        {meta.length > 0 && <p>{meta.join(' · ')}</p>}
        <h3>{post.title}</h3>
        {post.caption && <p className={styles.caption}>{post.caption}</p>}
        <span>Partilhado por {post.submittedByName}</span>
      </div>
    </article>
  )
}

function validateForm(form: ClientContentFormState, profiles: Profile[]) {
  if (!form.profileSlug || !profiles.some((profile) => profile.slug === form.profileSlug)) return 'Escolhe o artista do evento.'
  if (!form.title.trim()) return 'Indica um título para a publicação.'
  if (!form.location.trim()) return 'Indica o local do evento.'
  if (!form.eventDate) return 'Indica a data do evento.'
  if (form.eventDate > todayIso()) return 'A data do evento não pode ser no futuro.'
  if (!form.caption.trim()) return 'Escreve uma legenda para a publicação.'
  if (!form.mediaId || !form.mediaUrl || !form.mediaType) return 'Carrega uma fotografia ou vídeo antes de enviar.'
  if (form.publicIdentity === 'CUSTOM' && form.customDisplayName.trim().length < 2) return 'Indica o nome público a apresentar.'
  if (!form.consentToPublish) return 'Confirma que tens autorização para publicar este conteúdo.'
  return null
}

function groupClientContent(posts: ClientContentPost[]) {
  const monthFormatter = new Intl.DateTimeFormat('pt-PT', { month: 'long', year: 'numeric' })
  const groups: { key: string; label: string; items: ClientContentPost[] }[] = []

  posts.forEach((post) => {
    const date = new Date(post.eventDateIso + 'T00:00:00')
    const key = post.eventMonth ?? (Number.isNaN(date.getTime()) ? post.eventDateIso : date.getFullYear() + '-' + String(date.getMonth() + 1).padStart(2, '0'))
    const existing = groups.find((group) => group.key === key)

    if (existing) {
      existing.items.push(post)
      return
    }

    const rawLabel = post.eventMonth
      ? monthFormatter.format(new Date(post.eventMonth + '-01T00:00:00'))
      : Number.isNaN(date.getTime()) ? 'Publicações recentes' : monthFormatter.format(date)
    groups.push({ key, label: capitalize(rawLabel), items: [post] })
  })

  return groups
}

function capitalize(value: string) {
  return value ? value.charAt(0).toUpperCase() + value.slice(1) : value
}

function todayIso() {
  const today = new Date()
  return `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`
}
