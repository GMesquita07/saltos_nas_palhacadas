import { useCallback, useEffect, useState, type ChangeEvent, type FormEvent } from 'react'
import { ImageCropEditor } from '../../components/ImageCropEditor'
import { formatImagePosition, parseImageCrop, type ImageCrop } from '../../components/imageCrop'
import { apiClient, uploadFile } from '../../services/apiClient'
import { getContacts, reorderContacts } from '../../services/contactService'
import { getPortfolioItems } from '../../services/portfolioService'
import { getProfiles } from '../../services/profileService'
import { getAdminReviews, moderateReview } from '../../services/reviewService'
import type { Contact, ContactType } from '../../types/contact'
import type { PortfolioItem } from '../../types/portfolio'
import type { Profile } from '../../types/profile'
import type { Review } from '../../types/review'
import { BookingManagement } from './booking/BookingManagement'
import { ClientContentModeration } from './clientContent/ClientContentModeration'
import styles from './AdminArea.module.css'

type Notice = { type: 'success' | 'error'; text: string }
type AdminPage = 'profile' | 'content' | 'contacts' | 'reviews' | 'clientContent' | 'bookings'
type MediaType = 'PHOTO' | 'VIDEO'

type ProfileFormState = {
  name: string
  slug: string
  role: string
  description: string
  profileImageUrl: string
  imageCrop: ImageCrop
  featuredVideoUrl: string
}

type ApiProfileResponse = {
  id: number
  slug: string
  name: string
  role: string
  description: string
  profileImageUrl: string | null
  profileImagePosition: string | null
  profileImageZoom: number | null
  featuredVideoUrl: string | null
}

type ContentFormState = {
  profileSlug: string
  title: string
  location: string
  eventDate: string
  mediaUrl: string
  mediaType: MediaType | null
  thumbnailUrl: string
}

type ContactFormState = {
  label: string
  type: ContactType
  value: string
}

type ReviewModerationState = {
  published: boolean
}

type ContactField = {
  label: string
  placeholder: string
  inputType: 'email' | 'tel' | 'text' | 'url'
  inputMode: 'email' | 'tel' | 'text' | 'url'
}

const slugPattern = /^[a-z0-9]+(?:-[a-z0-9]+)*$/

const emptyProfileForm = (): ProfileFormState => ({
  name: '',
  slug: '',
  role: '',
  description: '',
  profileImageUrl: '',
  imageCrop: { x: 50, y: 50, zoom: 1 },
  featuredVideoUrl: '',
})

const emptyContentForm = (profileSlug = ''): ContentFormState => ({
  profileSlug,
  title: '',
  location: '',
  eventDate: '',
  mediaUrl: '',
  mediaType: null,
  thumbnailUrl: '',
})

const emptyContactForm = (): ContactFormState => ({
  label: '',
  type: 'EMAIL',
  value: '',
})

export function AdminArea({ onExit, token }: { onExit: () => void; token: string }) {
  const [notice, setNotice] = useState<Notice | null>(null)
  const [page, setPage] = useState<AdminPage>('profile')
  const [profiles, setProfiles] = useState<Profile[]>([])
  const [contacts, setContacts] = useState<Contact[]>([])
  const [reviews, setReviews] = useState<Review[]>([])
  const [contentItems, setContentItems] = useState<PortfolioItem[]>([])
  const [profileForm, setProfileForm] = useState<ProfileFormState>(emptyProfileForm)
  const [contentForm, setContentForm] = useState<ContentFormState>(emptyContentForm)
  const [contactForm, setContactForm] = useState<ContactFormState>(emptyContactForm)
  const [reviewDrafts, setReviewDrafts] = useState<Record<string, ReviewModerationState>>({})
  const [editingProfileSlug, setEditingProfileSlug] = useState<string | null>(null)
  const [editingContentId, setEditingContentId] = useState<string | null>(null)
  const [editingContactId, setEditingContactId] = useState<number | null>(null)
  const [isSaving, setIsSaving] = useState(false)

  const refreshProfiles = useCallback(async () => {
    try {
      const items = await getProfiles()
      setProfiles(items)
      return items
    } catch {
      setNotice({ type: 'error', text: 'Não foi possível carregar os perfis. Confirma se a API está ativa.' })
      return []
    }
  }, [])

  const refreshContacts = useCallback(async () => {
    try {
      const items = await getContacts()
      setContacts(items)
      return items
    } catch {
      setNotice({ type: 'error', text: 'Não foi possível carregar os contactos. Confirma se a API está ativa.' })
      return []
    }
  }, [])

  const refreshReviews = useCallback(async () => {
    if (!token) return []

    try {
      const items = await getAdminReviews(token)
      setReviews(items)
      setReviewDrafts(toReviewDrafts(items))
      return items
    } catch {
      setNotice({ type: 'error', text: 'Não foi possível carregar as avaliações.' })
      return []
    }
  }, [token])

  const refreshContentItems = useCallback(async (slug: string) => {
    if (!slug) {
      setContentItems([])
      return []
    }

    try {
      const items = await getPortfolioItems(slug)
      setContentItems(items)
      return items
    } catch {
      setNotice({ type: 'error', text: 'Não foi possível carregar os conteúdos deste perfil.' })
      return []
    }
  }, [])

  useEffect(() => {
    if (!token) return
    let isCurrent = true

    void Promise.all([getProfiles(), getContacts(), getAdminReviews(token)])
      .then(([profileItems, contactItems, reviewItems]) => {
        if (!isCurrent) return
        setProfiles(profileItems)
        setContacts(contactItems)
        setReviews(reviewItems)
        setReviewDrafts(toReviewDrafts(reviewItems))
      })
      .catch(() => {
        if (isCurrent) {
          setNotice({ type: 'error', text: 'Não foi possível carregar os dados de administração. Confirma se a API está ativa.' })
        }
      })

    return () => {
      isCurrent = false
    }
  }, [token])

  async function upload(
    event: ChangeEvent<HTMLInputElement>,
    onUploaded: (url: string, file: File) => void,
    options?: { imagesOnly?: boolean },
  ) {
    const input = event.currentTarget
    const file = input.files?.[0]

    if (!file || !token) return

    if (options?.imagesOnly && !file.type.startsWith('image/')) {
      setNotice({ type: 'error', text: 'Seleciona uma imagem válida para a miniatura.' })
      input.value = ''
      return
    }

    if (!file.type.startsWith('image/') && !file.type.startsWith('video/')) {
      setNotice({ type: 'error', text: 'Seleciona um ficheiro de imagem ou vídeo válido.' })
      input.value = ''
      return
    }

    try {
      const result = await uploadFile(file, token)
      onUploaded(result.url, file)
      setNotice({ type: 'success', text: 'Ficheiro enviado com sucesso.' })
    } catch (error) {
      setNotice({
        type: 'error',
        text: error instanceof Error ? error.message : 'Não foi possível enviar o ficheiro.',
      })
    } finally {
      input.value = ''
    }
  }

  function cancelProfileEditing() {
    setEditingProfileSlug(null)
    setProfileForm(emptyProfileForm())
  }

  function startProfileEditing(profile: Profile) {
    setEditingProfileSlug(profile.slug)
    setProfileForm({
      name: profile.name,
      slug: profile.slug,
      role: profile.role,
      description: profile.description,
      profileImageUrl: profile.imageUrl ?? '',
      imageCrop: parseImageCrop(profile.imagePosition, profile.imageZoom),
      featuredVideoUrl: profile.featuredVideoUrl ?? '',
    })
    setNotice({ type: 'success', text: 'A editar o perfil ' + profile.name + '. Altera os campos e seleciona Atualizar perfil.' })
    scrollToEditor('profile-editor')
  }

  async function submitProfile(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const validationError = validateProfile(profileForm)

    if (validationError) {
      setNotice({ type: 'error', text: validationError })
      return
    }

    if (!token || isSaving) return
    setIsSaving(true)

    const payload = {
      name: profileForm.name.trim(),
      role: profileForm.role.trim(),
      description: profileForm.description.trim(),
      profileImageUrl: profileForm.profileImageUrl.trim() || null,
      profileImagePosition: formatImagePosition(profileForm.imageCrop),
      profileImageZoom: profileForm.imageCrop.zoom,
      featuredVideoUrl: profileForm.featuredVideoUrl.trim() || null,
    }

    try {
      let savedProfile: Profile | null = null
      if (editingProfileSlug) {
        const response = await apiClient<ApiProfileResponse>('/admin/profiles/' + encodeURIComponent(editingProfileSlug), {
          method: 'PUT',
          body: JSON.stringify(payload),
        }, token)
        savedProfile = toProfile(response)
        setNotice({ type: 'success', text: 'Perfil atualizado com sucesso.' })
      } else {
        const response = await apiClient<ApiProfileResponse>('/admin/profiles', {
          method: 'POST',
          body: JSON.stringify({ ...payload, slug: profileForm.slug.trim() }),
        }, token)
        savedProfile = toProfile(response)
        setNotice({ type: 'success', text: 'Perfil criado com sucesso.' })
      }

      if (savedProfile) {
        setProfiles((current) => upsertProfile(current, savedProfile))
      }
      const refreshedProfiles = await refreshProfiles()
      if (savedProfile) {
        setProfiles((current) => upsertProfile(refreshedProfiles.length ? refreshedProfiles : current, savedProfile))
      }
      window.dispatchEvent(new Event('profiles:changed'))
      cancelProfileEditing()
    } catch (error) {
      setNotice({
        type: 'error',
        text: error instanceof Error ? error.message : 'Não foi possível guardar o perfil.',
      })
    } finally {
      setIsSaving(false)
    }
  }

  function selectContentProfile(slug: string) {
    setEditingContentId(null)
    setContentForm(emptyContentForm(slug))
    void refreshContentItems(slug)
  }

  function cancelContentEditing() {
    setEditingContentId(null)
    setContentForm(emptyContentForm(contentForm.profileSlug))
  }

  function startContentEditing(item: PortfolioItem) {
    setEditingContentId(item.id)
    setContentForm({
      profileSlug: contentForm.profileSlug,
      title: item.title,
      location: item.location,
      eventDate: item.eventDateIso,
      mediaUrl: item.mediaUrl,
      mediaType: item.type === 'Vídeo' ? 'VIDEO' : 'PHOTO',
      thumbnailUrl: item.thumbnailUrl ?? '',
    })
    setNotice({ type: 'success', text: 'A editar o conteúdo ' + item.title + '. Podes substituir o ficheiro ou alterar os restantes campos.' })
    scrollToEditor('content-editor')
  }

  async function submitContent(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const validationError = validateContent(contentForm)

    if (validationError) {
      setNotice({ type: 'error', text: validationError })
      return
    }

    if (!token || isSaving) return
    setIsSaving(true)

    const payload = {
      type: contentForm.mediaType,
      title: contentForm.title.trim(),
      location: contentForm.location.trim(),
      eventDate: contentForm.eventDate,
      mediaUrl: contentForm.mediaUrl,
      thumbnailUrl: contentForm.thumbnailUrl.trim() || null,
      published: true,
    }

    try {
      if (editingContentId) {
        await apiClient('/admin/profiles/' + encodeURIComponent(contentForm.profileSlug) + '/portfolio/' + encodeURIComponent(editingContentId), {
          method: 'PUT',
          body: JSON.stringify(payload),
        }, token)
        setNotice({ type: 'success', text: 'Conteúdo atualizado com sucesso.' })
      } else {
        await apiClient('/admin/profiles/' + encodeURIComponent(contentForm.profileSlug) + '/portfolio', {
          method: 'POST',
          body: JSON.stringify(payload),
        }, token)
        setNotice({ type: 'success', text: 'Conteúdo publicado com sucesso.' })
      }

      await refreshContentItems(contentForm.profileSlug)
      cancelContentEditing()
    } catch (error) {
      setNotice({
        type: 'error',
        text: error instanceof Error ? error.message : 'Não foi possível guardar o conteúdo.',
      })
    } finally {
      setIsSaving(false)
    }
  }

  function cancelContactEditing() {
    setEditingContactId(null)
    setContactForm(emptyContactForm())
  }

  function startContactEditing(contact: Contact) {
    setEditingContactId(contact.id)
    setContactForm({
      label: contact.label,
      type: contact.type,
      value: contact.value,
    })
    setNotice({ type: 'success', text: 'A editar o contacto ' + contact.label + '. Altera os campos e seleciona Atualizar contacto.' })
    scrollToEditor('contact-editor')
  }

  async function submitContact(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const validationError = validateContact(contactForm)

    if (validationError) {
      setNotice({ type: 'error', text: validationError })
      return
    }

    if (!token || isSaving) return
    setIsSaving(true)

    const payload = {
      label: contactForm.label.trim(),
      type: contactForm.type,
      value: contactForm.value.trim(),
    }

    try {
      if (editingContactId !== null) {
        await apiClient('/admin/contacts/' + editingContactId, {
          method: 'PUT',
          body: JSON.stringify(payload),
        }, token)
        setNotice({ type: 'success', text: 'Contacto atualizado com sucesso.' })
      } else {
        await apiClient('/admin/contacts', {
          method: 'POST',
          body: JSON.stringify(payload),
        }, token)
        setNotice({ type: 'success', text: 'Contacto adicionado com sucesso.' })
      }

      await refreshContacts()
      cancelContactEditing()
    } catch (error) {
      setNotice({
        type: 'error',
        text: error instanceof Error ? error.message : 'Não foi possível guardar o contacto.',
      })
    } finally {
      setIsSaving(false)
    }
  }

  async function deleteProfile(profile: Profile) {
    if (!token || !window.confirm('Apagar este perfil e todos os respetivos conteúdos?')) return

    setIsSaving(true)
    try {
      await apiClient('/admin/profiles/' + encodeURIComponent(profile.slug), { method: 'DELETE' }, token)
      await refreshProfiles()
      window.dispatchEvent(new Event('profiles:changed'))

      if (editingProfileSlug === profile.slug) cancelProfileEditing()
      if (contentForm.profileSlug === profile.slug) {
        setContentItems([])
        setEditingContentId(null)
        setContentForm(emptyContentForm())
      }

      setNotice({ type: 'success', text: 'Perfil apagado com sucesso.' })
    } catch (error) {
      setNotice({
        type: 'error',
        text: error instanceof Error ? error.message : 'Não foi possível apagar o perfil.',
      })
    } finally {
      setIsSaving(false)
    }
  }

  async function deleteContent(item: PortfolioItem) {
    if (!token || !contentForm.profileSlug || !window.confirm('Apagar este conteúdo publicado?')) return

    setIsSaving(true)
    try {
      await apiClient('/admin/profiles/' + encodeURIComponent(contentForm.profileSlug) + '/portfolio/' + encodeURIComponent(item.id), {
        method: 'DELETE',
      }, token)
      await refreshContentItems(contentForm.profileSlug)

      if (editingContentId === item.id) cancelContentEditing()
      setNotice({ type: 'success', text: 'Conteúdo apagado com sucesso.' })
    } catch (error) {
      setNotice({
        type: 'error',
        text: error instanceof Error ? error.message : 'Não foi possível apagar o conteúdo.',
      })
    } finally {
      setIsSaving(false)
    }
  }

  async function deleteContact(contact: Contact) {
    if (!token || !window.confirm('Apagar este contacto?')) return

    setIsSaving(true)
    try {
      await apiClient('/admin/contacts/' + contact.id, { method: 'DELETE' }, token)
      await refreshContacts()

      if (editingContactId === contact.id) cancelContactEditing()
      setNotice({ type: 'success', text: 'Contacto apagado com sucesso.' })
    } catch (error) {
      setNotice({
        type: 'error',
        text: error instanceof Error ? error.message : 'Não foi possível apagar o contacto.',
      })
    } finally {
      setIsSaving(false)
    }
  }

  async function reorderContactStack(contactIds: number[]) {
    if (!token || isSaving) return
    setIsSaving(true)

    try {
      const orderedContacts = await reorderContacts(contactIds, token)
      setContacts(orderedContacts)
      window.dispatchEvent(new Event('contacts:changed'))
      setNotice({ type: 'success', text: 'Ordem dos contactos atualizada.' })
    } catch (error) {
      setNotice({
        type: 'error',
        text: error instanceof Error ? error.message : 'Não foi possível guardar a ordem dos contactos.',
      })
    } finally {
      setIsSaving(false)
    }
  }

  async function saveReviewModeration(review: Review) {
    const draft = reviewDrafts[review.id] ?? { published: review.published }

    if (!token || isSaving) return
    setIsSaving(true)

    try {
      await moderateReview(review.id, { published: draft.published }, token)
      await refreshReviews()
      window.dispatchEvent(new Event('reviews:changed'))
      setNotice({ type: 'success', text: draft.published ? 'Avaliação publicada.' : 'Avaliação ocultada.' })
    } catch (error) {
      setNotice({
        type: 'error',
        text: error instanceof Error ? error.message : 'Não foi possível moderar a avaliação.',
      })
    } finally {
      setIsSaving(false)
    }
  }

  async function deleteReview(review: Review) {
    if (!token || !window.confirm('Apagar esta avaliação?')) return

    setIsSaving(true)
    try {
      await apiClient('/admin/reviews/' + review.id, { method: 'DELETE' }, token)
      await refreshReviews()
      window.dispatchEvent(new Event('reviews:changed'))

      setNotice({ type: 'success', text: 'Avaliação apagada com sucesso.' })
    } catch (error) {
      setNotice({
        type: 'error',
        text: error instanceof Error ? error.message : 'Não foi possível apagar a avaliação.',
      })
    } finally {
      setIsSaving(false)
    }
  }

  return (
    <section className={styles.dashboard}>
      <div className={styles.top}>
        <div>
          <p className="eyebrow">Área reservada</p>
          <h1>Painel de administração</h1>
          {notice && (
            <p className={[styles.notice, styles[notice.type]].join(' ')} role="status">
              {notice.text}
            </p>
          )}
        </div>
        <button type="button" onClick={onExit}>Voltar ao site</button>
      </div>

      <nav className={styles.tabs} aria-label="Secções de administração">
        <Tab active={page === 'profile'} onClick={() => setPage('profile')}>Novo perfil</Tab>
        <Tab active={page === 'content'} onClick={() => setPage('content')}>Publicar conteúdo</Tab>
        <Tab active={page === 'contacts'} onClick={() => setPage('contacts')}>Contactos</Tab>
        <Tab active={page === 'reviews'} onClick={() => setPage('reviews')}>Avaliações</Tab>
        <Tab active={page === 'clientContent'} onClick={() => setPage('clientContent')}>Partilhas de clientes</Tab>
        <Tab active={page === 'bookings'} onClick={() => setPage('bookings')}>Agendamentos</Tab>
      </nav>

      {page === 'profile' && (
        <ProfileManagement
          form={profileForm}
          isEditing={editingProfileSlug !== null}
          isSaving={isSaving}
          profiles={profiles}
          onChange={setProfileForm}
          onSubmit={submitProfile}
          onCancel={cancelProfileEditing}
          onEdit={startProfileEditing}
          onDelete={deleteProfile}
          onUpload={(event) => upload(event, (url) => {
            setProfileForm((current) => ({ ...current, profileImageUrl: url, imageCrop: { x: 50, y: 50, zoom: 1 } }))
          })}
          onFeaturedVideoUpload={(event) => upload(event, (url) => {
            setProfileForm((current) => ({ ...current, featuredVideoUrl: url }))
          })}
        />
      )}

      {page === 'content' && (
        <ContentManagement
          form={contentForm}
          isEditing={editingContentId !== null}
          isSaving={isSaving}
          items={contentItems}
          profiles={profiles}
          onChange={setContentForm}
          onSelectProfile={selectContentProfile}
          onSubmit={submitContent}
          onCancel={cancelContentEditing}
          onEdit={startContentEditing}
          onDelete={deleteContent}
          onUpload={(event) => upload(event, (url, file) => {
            setContentForm((current) => ({
              ...current,
              mediaUrl: url,
              mediaType: file.type.startsWith('video/') ? 'VIDEO' : 'PHOTO',
            }))
          })}
          onThumbnailUpload={(event) => upload(event, (url) => {
            setContentForm((current) => ({ ...current, thumbnailUrl: url }))
          }, { imagesOnly: true })}
        />
      )}

      {page === 'contacts' && (
        <ContactManagement
          form={contactForm}
          isEditing={editingContactId !== null}
          isSaving={isSaving}
          contacts={contacts}
          onChange={setContactForm}
          onSubmit={submitContact}
          onCancel={cancelContactEditing}
          onEdit={startContactEditing}
          onDelete={deleteContact}
          onReorder={reorderContactStack}
        />
      )}

      {page === 'reviews' && (
        <ReviewManagement
          drafts={reviewDrafts}
          isSaving={isSaving}
          reviews={reviews}
          onChangeDraft={setReviewDrafts}
          onModerate={saveReviewModeration}
          onDelete={deleteReview}
        />
      )}

      {page === 'bookings' && (
        <BookingManagement token={token} onNotice={setNotice} />
      )}

      {page === 'clientContent' && (
        <ClientContentModeration token={token} onNotice={setNotice} />
      )}
    </section>
  )
}

function Tab({
  active,
  onClick,
  children,
}: {
  active: boolean
  onClick: () => void
  children: string
}) {
  return (
    <button
      aria-current={active ? 'page' : undefined}
      className={active ? styles.activeTab : ''}
      type="button"
      onClick={onClick}
    >
      {children}
    </button>
  )
}

function ProfileManagement({
  form,
  isEditing,
  isSaving,
  profiles,
  onChange,
  onSubmit,
  onCancel,
  onEdit,
  onDelete,
  onUpload,
  onFeaturedVideoUpload,
}: {
  form: ProfileFormState
  isEditing: boolean
  isSaving: boolean
  profiles: Profile[]
  onChange: (value: ProfileFormState | ((current: ProfileFormState) => ProfileFormState)) => void
  onSubmit: (event: FormEvent<HTMLFormElement>) => Promise<void>
  onCancel: () => void
  onEdit: (profile: Profile) => void
  onDelete: (profile: Profile) => Promise<void>
  onUpload: (event: ChangeEvent<HTMLInputElement>) => Promise<void>
  onFeaturedVideoUpload: (event: ChangeEvent<HTMLInputElement>) => Promise<void>
}) {
  return (
    <div className={styles.page}>
      <form id="profile-editor" onSubmit={(event) => { void onSubmit(event) }}>
        <FormHeading
          title={isEditing ? 'Editar perfil' : 'Novo perfil'}
          description={isEditing
            ? 'Os campos estão preenchidos com os dados atuais. Altera apenas o que for necessário.'
            : 'Cria o cartão apresentado na página pública de perfis.'}
          isEditing={isEditing}
        />

        <label>
          Nome
          <input
            maxLength={120}
            minLength={2}
            onChange={(event) => onChange((current) => ({ ...current, name: event.target.value }))}
            required
            value={form.name}
          />
        </label>

        <label>
          Slug
          <input
            className={isEditing ? styles.readonlyField : undefined}
            maxLength={120}
            onChange={(event) => onChange((current) => ({
              ...current,
              slug: event.target.value.toLowerCase().replace(/\s+/g, '-'),
            }))}
            pattern="[a-z0-9]+(?:-[a-z0-9]+)*"
            placeholder="dj-joao-tomas"
            readOnly={isEditing}
            required
            title="Usa apenas letras minúsculas, números e hífenes entre palavras."
            value={form.slug}
          />
          {isEditing && <small className={styles.fieldHint}>O slug é o endereço do perfil e não pode ser alterado.</small>}
        </label>

        <label>
          Função
          <input
            maxLength={120}
            minLength={2}
            onChange={(event) => onChange((current) => ({ ...current, role: event.target.value }))}
            placeholder="DJ & Animador"
            required
            value={form.role}
          />
        </label>

        <label>
          Descrição
          <textarea
            maxLength={500}
            minLength={10}
            onChange={(event) => onChange((current) => ({ ...current, description: event.target.value }))}
            required
            value={form.description}
          />
        </label>

        <label>
          {isEditing ? 'Substituir imagem de perfil' : 'Enviar imagem de perfil'}
          <input accept="image/*" type="file" onChange={onUpload} />
        </label>

        <label>
          URL da imagem
          <input
            maxLength={2048}
            onChange={(event) => onChange((current) => ({ ...current, profileImageUrl: event.target.value }))}
            placeholder="https://..."
            type="url"
            value={form.profileImageUrl}
          />
        </label>

        {form.profileImageUrl && (
          <ImageCropEditor
            crop={form.imageCrop}
            description="Arrasta a fotografia e ajusta o zoom. Esta pré-visualização usa o mesmo recorte circular que aparece na homepage."
            shape="circle"
            src={form.profileImageUrl}
            title="Ajustar foto de perfil"
            onChange={(imageCrop) => onChange((current) => ({ ...current, imageCrop }))}
          />
        )}

        <label>
          Carregar vídeo de destaque
          <input accept="video/*" type="file" onChange={onFeaturedVideoUpload} />
        </label>

        {form.featuredVideoUrl && (
          <p className={styles.uploadedFile}>Vídeo de destaque pronto para aparecer no perfil.</p>
        )}

        <label>
          URL do vídeo de destaque
          <input
            maxLength={2048}
            onChange={(event) => onChange((current) => ({ ...current, featuredVideoUrl: event.target.value }))}
            placeholder="https://youtu.be/... ou https://..."
            type="url"
            value={form.featuredVideoUrl}
          />
          <small className={styles.fieldHint}>Aceita links do YouTube ou URLs diretas para vídeo. Também podes carregar um ficheiro pelo campo acima.</small>
        </label>

        <FormActions
          isEditing={isEditing}
          isSaving={isSaving}
          createLabel="Criar perfil"
          updateLabel="Atualizar perfil"
          onCancel={onCancel}
        />
      </form>

      <ManagementList
        empty="Não existem perfis."
        items={profiles}
        title="Perfis existentes"
        getDetail={(profile) => profile.slug}
        getId={(profile) => profile.id}
        getTitle={(profile) => profile.name}
        isSaving={isSaving}
        onDelete={onDelete}
        onEdit={onEdit}
      />
    </div>
  )
}

function ContentManagement({
  form,
  isEditing,
  isSaving,
  items,
  profiles,
  onChange,
  onSelectProfile,
  onSubmit,
  onCancel,
  onEdit,
  onDelete,
  onUpload,
  onThumbnailUpload,
}: {
  form: ContentFormState
  isEditing: boolean
  isSaving: boolean
  items: PortfolioItem[]
  profiles: Profile[]
  onChange: (value: ContentFormState | ((current: ContentFormState) => ContentFormState)) => void
  onSelectProfile: (slug: string) => void
  onSubmit: (event: FormEvent<HTMLFormElement>) => Promise<void>
  onCancel: () => void
  onEdit: (item: PortfolioItem) => void
  onDelete: (item: PortfolioItem) => Promise<void>
  onUpload: (event: ChangeEvent<HTMLInputElement>) => Promise<void>
  onThumbnailUpload: (event: ChangeEvent<HTMLInputElement>) => Promise<void>
}) {
  return (
    <div className={styles.page}>
      <form id="content-editor" onSubmit={(event) => { void onSubmit(event) }}>
        <FormHeading
          title={isEditing ? 'Editar conteúdo' : 'Publicar conteúdo'}
          description={isEditing
            ? 'Os campos estão preenchidos com o conteúdo atual. Envia um novo ficheiro apenas se o quiseres substituir.'
            : 'O tipo é reconhecido automaticamente a partir do ficheiro enviado.'}
          isEditing={isEditing}
        />

        <label>
          Perfil
          <select
            disabled={isEditing}
            onChange={(event) => onSelectProfile(event.target.value)}
            required
            value={form.profileSlug}
          >
            <option value="">Seleciona um perfil</option>
            {profiles.map((profile) => (
              <option key={profile.id} value={profile.slug}>{profile.name}</option>
            ))}
          </select>
          {isEditing && <small className={styles.fieldHint}>Para manter a associação correta, o conteúdo não pode mudar de perfil.</small>}
        </label>

        <label>
          Título
          <input
            maxLength={180}
            minLength={2}
            onChange={(event) => onChange((current) => ({ ...current, title: event.target.value }))}
            required
            value={form.title}
          />
        </label>

        <label>
          Local
          <input
            maxLength={180}
            minLength={2}
            onChange={(event) => onChange((current) => ({ ...current, location: event.target.value }))}
            required
            value={form.location}
          />
        </label>

        <label>
          Data do evento
          <input
            onChange={(event) => onChange((current) => ({ ...current, eventDate: event.target.value }))}
            required
            type="date"
            value={form.eventDate}
          />
        </label>

        <label>
          {isEditing ? 'Substituir foto ou vídeo' : 'Enviar foto ou vídeo'}
          <input accept="image/*,video/*" type="file" onChange={onUpload} />
        </label>

        {form.mediaUrl && (
          <p className={styles.uploadedFile}>
            {isEditing ? 'Ficheiro atual: ' : 'Ficheiro pronto: '}
            {form.mediaType === 'VIDEO' ? 'vídeo' : 'fotografia'}.
          </p>
        )}

        <label>
          URL da miniatura (opcional)
          <input
            maxLength={2048}
            onChange={(event) => onChange((current) => ({ ...current, thumbnailUrl: event.target.value }))}
            placeholder="https://..."
            type="url"
            value={form.thumbnailUrl}
          />
        </label>

        <label>
          Carregar miniatura do dispositivo
          <input accept="image/*" type="file" onChange={onThumbnailUpload} />
          <small className={styles.fieldHint}>Usada como capa dos vídeos e como imagem de pré-visualização do conteúdo.</small>
        </label>

        {form.thumbnailUrl && (
          <div className={styles.thumbnailPreview}>
            <img src={form.thumbnailUrl} alt="Pré-visualização da miniatura" />
            <button
              type="button"
              onClick={() => onChange((current) => ({ ...current, thumbnailUrl: '' }))}
            >
              Remover miniatura
            </button>
          </div>
        )}

        <FormActions
          isEditing={isEditing}
          isSaving={isSaving}
          createLabel="Publicar conteúdo"
          updateLabel="Atualizar conteúdo"
          onCancel={onCancel}
        />
      </form>

      <ManagementList
        empty={form.profileSlug
          ? 'Este perfil ainda não tem conteúdos publicados.'
          : 'Seleciona um perfil para gerir os seus conteúdos.'}
        items={items}
        title="Conteúdos publicados"
        getDetail={(item) => item.type + ' · ' + item.eventDate}
        getId={(item) => item.id}
        getTitle={(item) => item.title}
        isSaving={isSaving}
        onDelete={onDelete}
        onEdit={onEdit}
      />
    </div>
  )
}

function ContactManagement({
  form,
  isEditing,
  isSaving,
  contacts,
  onChange,
  onSubmit,
  onCancel,
  onEdit,
  onDelete,
  onReorder,
}: {
  form: ContactFormState
  isEditing: boolean
  isSaving: boolean
  contacts: Contact[]
  onChange: (value: ContactFormState | ((current: ContactFormState) => ContactFormState)) => void
  onSubmit: (event: FormEvent<HTMLFormElement>) => Promise<void>
  onCancel: () => void
  onEdit: (contact: Contact) => void
  onDelete: (contact: Contact) => Promise<void>
  onReorder: (contactIds: number[]) => Promise<void>
}) {
  const field = contactField(form.type)

  return (
    <div className={styles.page}>
      <form id="contact-editor" onSubmit={(event) => { void onSubmit(event) }}>
        <FormHeading
          title={isEditing ? 'Editar contacto' : 'Novo contacto'}
          description={isEditing
            ? 'Os campos estão preenchidos com os dados atuais. Atualiza o que for necessário.'
            : 'Os contactos ficam automaticamente visíveis na página pública de contacto e suporte.'}
          isEditing={isEditing}
        />

        <label>
          Nome a apresentar
          <input
            maxLength={80}
            minLength={2}
            onChange={(event) => onChange((current) => ({ ...current, label: event.target.value }))}
            placeholder="Reservas e eventos"
            required
            value={form.label}
          />
        </label>

        <label>
          Tipo
          <select
            onChange={(event) => onChange((current) => ({
              ...current,
              type: event.target.value as ContactType,
              value: '',
            }))}
            value={form.type}
          >
            {(['EMAIL', 'PHONE', 'WHATSAPP', 'INSTAGRAM', 'WEBSITE'] as ContactType[]).map((option) => (
              <option key={option} value={option}>{contactTypeLabel(option)}</option>
            ))}
          </select>
        </label>

        <label>
          {field.label}
          <input
            inputMode={field.inputMode}
            maxLength={500}
            onChange={(event) => onChange((current) => ({ ...current, value: event.target.value }))}
            placeholder={field.placeholder}
            required
            type={field.inputType}
            value={form.value}
          />
        </label>

        <FormActions
          isEditing={isEditing}
          isSaving={isSaving}
          createLabel="Adicionar contacto"
          updateLabel="Atualizar contacto"
          onCancel={onCancel}
        />
      </form>

      <ContactOrderList
        contacts={contacts}
        isSaving={isSaving}
        onDelete={onDelete}
        onEdit={onEdit}
        onReorder={onReorder}
      />
    </div>
  )
}

function ContactOrderList({
  contacts,
  isSaving,
  onDelete,
  onEdit,
  onReorder,
}: {
  contacts: Contact[]
  isSaving: boolean
  onDelete: (contact: Contact) => Promise<void>
  onEdit: (contact: Contact) => void
  onReorder: (contactIds: number[]) => Promise<void>
}) {
  const [draggingId, setDraggingId] = useState<number | null>(null)

  function dropOn(contactId: number) {
    if (draggingId === null || draggingId === contactId) return

    const draggingIndex = contacts.findIndex((contact) => contact.id === draggingId)
    const targetIndex = contacts.findIndex((contact) => contact.id === contactId)
    if (draggingIndex < 0 || targetIndex < 0) return

    const nextContacts = [...contacts]
    const [draggingContact] = nextContacts.splice(draggingIndex, 1)
    nextContacts.splice(targetIndex, 0, draggingContact)
    void onReorder(nextContacts.map((contact) => contact.id))
  }

  return (
    <section className={styles.manage}>
      <h2>Contactos publicados</h2>
      {contacts.length === 0 ? (
        <p>Não existem contactos publicados.</p>
      ) : (
        <div className={styles.contactStack}>
          {contacts.map((contact, index) => (
            <div
              className={[styles.contactRow, draggingId === contact.id ? styles.draggingRow : ''].join(' ')}
              draggable={!isSaving}
              key={contact.id}
              onDragEnd={() => setDraggingId(null)}
              onDragOver={(event) => event.preventDefault()}
              onDragStart={() => setDraggingId(contact.id)}
              onDrop={() => dropOn(contact.id)}
            >
              <span className={styles.dragHandle} aria-hidden="true">☰</span>
              <span>
                <strong>{index + 1}. {contact.label}</strong>
                <small>{contactTypeLabel(contact.type)} · {contact.value}</small>
              </span>
              <span className={styles.rowActions}>
                <button disabled={isSaving} type="button" onClick={() => onEdit(contact)}>Editar</button>
                <button disabled={isSaving} type="button" onClick={() => { void onDelete(contact) }}>Apagar</button>
              </span>
            </div>
          ))}
        </div>
      )}
    </section>
  )
}

function ReviewManagement({
  drafts,
  isSaving,
  reviews,
  onChangeDraft,
  onModerate,
  onDelete,
}: {
  drafts: Record<string, ReviewModerationState>
  isSaving: boolean
  reviews: Review[]
  onChangeDraft: (value: Record<string, ReviewModerationState> | ((current: Record<string, ReviewModerationState>) => Record<string, ReviewModerationState>)) => void
  onModerate: (review: Review) => Promise<void>
  onDelete: (review: Review) => Promise<void>
}) {
  return (
    <section className={styles.singlePage}>
      <div className={styles.formHeading}>
        <div>
          <h2>Avaliações</h2>
          <p className={styles.intro}>As avaliações são submetidas pelos utilizadores nos perfis dos artistas. Aqui escolhes quais aparecem publicamente; a ordem é sempre da mais recente para a mais antiga.</p>
        </div>
      </div>

      {reviews.length === 0 ? (
        <p className={styles.intro}>Ainda não existem avaliações submetidas.</p>
      ) : (
        <div className={styles.reviewList}>
          {reviews.map((review) => {
            const draft = drafts[review.id] ?? { published: review.published }
            return (
              <article className={styles.reviewRow} key={review.id}>
                <div>
                  <p className={styles.reviewMeta}>{review.profileName ?? 'Artista'} · {review.reviewDate} · {review.submittedByEmail ?? 'utilizador'}</p>
                  <h3>{review.reviewerName} - {review.title}</h3>
                  <p className={styles.reviewStars}>{stars(review.rating)} {review.rating.toFixed(1)}</p>
                  <p className={styles.reviewComment}>{review.comment}</p>
                </div>
                <div className={styles.reviewModeration}>
                  <label>
                    Estado
                    <select
                      onChange={(event) => onChangeDraft((current) => ({
                        ...current,
                        [review.id]: { ...draft, published: event.target.value === 'published' },
                      }))}
                      value={draft.published ? 'published' : 'hidden'}
                    >
                      <option value="published">Publicada</option>
                      <option value="hidden">Oculta</option>
                    </select>
                  </label>
                  <span className={draft.published ? styles.publishedBadge : styles.hiddenBadge}>{draft.published ? 'Publicada' : 'Oculta'}</span>
                  <button disabled={isSaving} type="button" onClick={() => { void onModerate(review) }}>Guardar</button>
                  <button disabled={isSaving} type="button" onClick={() => { void onDelete(review) }}>Apagar</button>
                </div>
              </article>
            )
          })}
        </div>
      )}
    </section>
  )
}

function FormHeading({
  title,
  description,
  isEditing,
}: {
  title: string
  description: string
  isEditing: boolean
}) {
  return (
    <div className={styles.formHeading}>
      <div>
        <h2>{title}</h2>
        <p className={styles.intro}>{description}</p>
      </div>
      {isEditing && <span className={styles.editingBadge}>A editar</span>}
    </div>
  )
}

function FormActions({
  isEditing,
  isSaving,
  createLabel,
  updateLabel,
  onCancel,
}: {
  isEditing: boolean
  isSaving: boolean
  createLabel: string
  updateLabel: string
  onCancel: () => void
}) {
  return (
    <div className={styles.formActions}>
      <button disabled={isSaving} type="submit">
        {isSaving ? 'A guardar...' : isEditing ? updateLabel : createLabel}
      </button>
      {isEditing && (
        <button className={styles.cancelButton} disabled={isSaving} type="button" onClick={onCancel}>
          Cancelar edição
        </button>
      )}
    </div>
  )
}

function ManagementList<T>({
  title,
  empty,
  items,
  getId,
  getTitle,
  getDetail,
  isSaving,
  onEdit,
  onDelete,
}: {
  title: string
  empty: string
  items: T[]
  getId: (item: T) => string | number
  getTitle: (item: T) => string
  getDetail: (item: T) => string
  isSaving: boolean
  onEdit: (item: T) => void
  onDelete: (item: T) => Promise<void>
}) {
  return (
    <section className={styles.manage}>
      <h2>{title}</h2>
      {items.length === 0 ? (
        <p>{empty}</p>
      ) : (
        items.map((item) => (
          <div className={styles.profileRow} key={getId(item)}>
            <span>
              <strong>{getTitle(item)}</strong>
              <small>{getDetail(item)}</small>
            </span>
            <span className={styles.rowActions}>
              <button disabled={isSaving} type="button" onClick={() => onEdit(item)}>Editar</button>
              <button disabled={isSaving} type="button" onClick={() => { void onDelete(item) }}>Apagar</button>
            </span>
          </div>
        ))
      )}
    </section>
  )
}

function toReviewDrafts(reviews: Review[]): Record<string, ReviewModerationState> {
  return reviews.reduce<Record<string, ReviewModerationState>>((drafts, review) => {
    drafts[review.id] = {
      published: review.published,
    }
    return drafts
  }, {})
}

function toProfile(profile: ApiProfileResponse): Profile {
  return {
    id: profile.slug,
    slug: profile.slug,
    name: profile.name,
    role: profile.role,
    description: profile.description,
    imageUrl: profile.profileImageUrl ?? undefined,
    imagePosition: profile.profileImagePosition ?? '50% 50%',
    imageZoom: profile.profileImageZoom ?? 1,
    featuredVideoUrl: profile.featuredVideoUrl ?? undefined,
  }
}

function upsertProfile(profiles: Profile[], profile: Profile) {
  const exists = profiles.some((item) => item.slug === profile.slug)
  const nextProfiles = exists
    ? profiles.map((item) => item.slug === profile.slug ? profile : item)
    : [...profiles, profile]
  return [...nextProfiles].sort((first, second) => first.name.localeCompare(second.name, 'pt-PT'))
}

function validateProfile(form: ProfileFormState) {
  if (form.name.trim().length < 2) return 'O nome do perfil tem de ter pelo menos 2 caracteres.'
  if (!slugPattern.test(form.slug.trim())) return 'O slug deve usar apenas letras minúsculas, números e hífenes, por exemplo: dj-joao-tomas.'
  if (form.role.trim().length < 2) return 'Indica uma função com pelo menos 2 caracteres.'
  if (form.description.trim().length < 10) return 'A descrição tem de ter pelo menos 10 caracteres.'
  if (form.profileImageUrl.length > 2048) return 'A URL da imagem é demasiado longa.'
  if (form.featuredVideoUrl.length > 2048) return 'A URL do vídeo de destaque é demasiado longa.'
  return null
}

function validateContent(form: ContentFormState) {
  if (!form.profileSlug) return 'Seleciona o perfil onde queres publicar o conteúdo.'
  if (form.title.trim().length < 2) return 'O título tem de ter pelo menos 2 caracteres.'
  if (form.location.trim().length < 2) return 'Indica o local do evento.'
  if (!/^\d{4}-\d{2}-\d{2}$/.test(form.eventDate)) return 'Indica uma data de evento válida no formato AAAA-MM-DD.'
  if (!form.mediaUrl || !form.mediaType) return 'Envia uma fotografia ou vídeo antes de publicar.'
  if (form.thumbnailUrl.length > 2048) return 'A URL da miniatura é demasiado longa.'
  return null
}

function validateContact(form: ContactFormState) {
  const value = form.value.trim()
  if (form.label.trim().length < 2) return 'O nome a apresentar tem de ter pelo menos 2 caracteres.'
  if (!value) return 'Preenche o campo ' + contactField(form.type).label.toLowerCase() + '.'
  if (value.length > 500) return 'O contacto é demasiado longo.'

  if (form.type === 'EMAIL' && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value)) {
    return 'Indica um endereço de email válido.'
  }

  if ((form.type === 'PHONE' || form.type === 'WHATSAPP') && !/^\+?[0-9\s()-]{6,25}$/.test(value)) {
    return 'Indica um número de telefone válido.'
  }

  if (form.type === 'INSTAGRAM' && !/^(?:@?[a-zA-Z0-9._]{1,30}|(?:https?:\/\/)?(?:www\.)?instagram\.com\/[a-zA-Z0-9._]+\/?)$/.test(value)) {
    return 'Indica um utilizador de Instagram válido, por exemplo @saltosnaspalhacadas.'
  }

  if (form.type === 'WEBSITE' && !isHttpUrl(value)) {
    return 'Indica um URL válido que comece por http:// ou https://.'
  }

  return null
}

function stars(rating: number) {
  return Array.from({ length: 5 }, (_, index) => index < rating ? '★' : '☆').join('')
}

function isHttpUrl(value: string) {
  try {
    const url = new URL(value)
    return url.protocol === 'http:' || url.protocol === 'https:'
  } catch {
    return false
  }
}

function contactTypeLabel(type: ContactType) {
  return {
    EMAIL: 'Email',
    PHONE: 'Telefone',
    WHATSAPP: 'WhatsApp',
    INSTAGRAM: 'Instagram',
    WEBSITE: 'Website',
  }[type]
}

function contactField(type: ContactType): ContactField {
  return ({
    EMAIL: {
      label: 'Endereço de email',
      placeholder: 'ola@exemplo.pt',
      inputType: 'email',
      inputMode: 'email',
    },
    PHONE: {
      label: 'Número de telefone',
      placeholder: '+351 912 345 678',
      inputType: 'tel',
      inputMode: 'tel',
    },
    WHATSAPP: {
      label: 'Número de WhatsApp',
      placeholder: '+351 912 345 678',
      inputType: 'tel',
      inputMode: 'tel',
    },
    INSTAGRAM: {
      label: 'Perfil de Instagram',
      placeholder: '@saltosnaspalhacadas',
      inputType: 'text',
      inputMode: 'text',
    },
    WEBSITE: {
      label: 'URL do website',
      placeholder: 'https://exemplo.pt',
      inputType: 'url',
      inputMode: 'url',
    },
  } as const)[type]
}

function scrollToEditor(id: string) {
  window.requestAnimationFrame(() => {
    document.getElementById(id)?.scrollIntoView({ behavior: 'smooth', block: 'start' })
  })
}
