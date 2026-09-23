import { useCallback, useEffect, useRef, useState, type ChangeEvent, type FormEvent, type ReactNode } from 'react'
import { ImageCropEditor } from '../../components/ImageCropEditor'
import { SocialIcon } from '../../components/SocialIcon/SocialIcon'
import { NavIcon } from '../../components/NavIcon/NavIcon'
import { formatImagePosition, parseImageCrop, type ImageCrop } from '../../components/imageCrop'
import { MediaLightbox } from '../portfolio/MediaLightbox'
import type { AdminPage } from '../../navigation/routes'
import { apiClient, uploadFile } from '../../services/apiClient'
import {
  createAdminPortfolioItem,
  deleteAdminPortfolioItem,
  getAdminPortfolioItems,
  updateAdminPortfolioItem,
} from '../../services/adminContentService'
import {
  createAdminProfile,
  deleteAdminProfile,
  getAdminProfiles,
  reorderAdminProfiles,
  updateAdminProfile,
  type AdminManagedProfile,
} from '../../services/adminProfileService'
import { getAdminBookings } from '../../services/bookingService'
import {
  createContact,
  deleteContact as deleteAdminContact,
  getAdminContacts,
  invalidateContactsCache,
  reorderContacts,
  updateContact,
} from '../../services/contactService'
import { invalidateProfilesCache } from '../../services/profileService'
import { getAdminReviews, moderateReview } from '../../services/reviewService'
import type { Booking } from '../../types/booking'
import type { Contact, ContactType } from '../../types/contact'
import type { AdminPortfolioItem, MediaType, PortfolioItem } from '../../types/portfolio'
import type { Profile, ProfileSocialLink } from '../../types/profile'
import type { Review } from '../../types/review'
import { BookingManagement } from './booking/BookingManagement'
import { contactToInput, contactVisibilityLabel, nextContactVisible } from './contacts/contactHelpers'
import { portfolioItemToSaveInput, portfolioPublicationLabel, togglePortfolioPublication } from './content/contentHelpers'
import { MaterialManagement } from './materials/MaterialManagement'
import {
  nextReviewPublished,
  replaceReview,
  reviewVisibilityLabel,
  reviewVisibilityNotice,
  updateReviewPublished,
} from './reviewModeration'
import { socialPlatformIcon, socialPlatformLabel, socialPlatformOptions, validateSocialLink } from '../profiles/socialLinks'
import styles from './AdminArea.module.css'

type Notice = { type: 'success' | 'error'; text: string }

type ProfileFormState = {
  name: string
  slug: string
  role: string
  description: string
  notificationEmail: string
  profileImageUrl: string
  imageCrop: ImageCrop
  featuredVideoUrl: string
  heroBackgroundImageUrl: string
  socialLinks: ProfileSocialLinkFormState[]
}

type ProfileSocialLinkFormState = {
  platform: string
  label: string
  url: string
}

type ContentFormState = {
  profileSlug: string
  title: string
  location: string
  eventDate: string
  mediaUrl: string
  mediaType: MediaType | null
  thumbnailUrl: string
  published: boolean
}

type ContactFormState = {
  label: string
  type: ContactType
  value: string
  visible: boolean
}

type ContactField = {
  label: string
  placeholder: string
  inputType: 'email' | 'tel' | 'text'
  inputMode: 'email' | 'tel' | 'text' | 'url'
}

const slugPattern = /^[a-z0-9]+(?:-[a-z0-9]+)*$/

const emptyProfileForm = (): ProfileFormState => ({
  name: '',
  slug: '',
  role: '',
  description: '',
  notificationEmail: '',
  profileImageUrl: '',
  imageCrop: { x: 50, y: 50, zoom: 1 },
  featuredVideoUrl: '',
  heroBackgroundImageUrl: '',
  socialLinks: [],
})

const emptyContentForm = (profileSlug = ''): ContentFormState => ({
  profileSlug,
  title: '',
  location: '',
  eventDate: '',
  mediaUrl: '',
  mediaType: null,
  thumbnailUrl: '',
  published: true,
})

const emptyContactForm = (): ContactFormState => ({
  label: '',
  type: 'EMAIL',
  value: '',
  visible: true,
})

export function AdminArea({
  onExit,
  onPageChange,
  page,
  token,
}: {
  onExit: () => void
  onPageChange: (page: AdminPage) => void
  page: AdminPage
  token: string
}) {
  const [notice, setNotice] = useState<Notice | null>(null)
  const [profiles, setProfiles] = useState<AdminManagedProfile[]>([])
  const [contacts, setContacts] = useState<Contact[]>([])
  const [reviews, setReviews] = useState<Review[]>([])
  const [adminBookings, setAdminBookings] = useState<Booking[]>([])
  const [contentItems, setContentItems] = useState<AdminPortfolioItem[]>([])
  const [profileForm, setProfileForm] = useState<ProfileFormState>(emptyProfileForm)
  const [contentForm, setContentForm] = useState<ContentFormState>(emptyContentForm)
  const [contactForm, setContactForm] = useState<ContactFormState>(emptyContactForm)
  const [editingProfileSlug, setEditingProfileSlug] = useState<string | null>(null)
  const [editingContentId, setEditingContentId] = useState<string | null>(null)
  const [editingContactId, setEditingContactId] = useState<number | null>(null)
  const [isSaving, setIsSaving] = useState(false)
  const [isContentLoading, setIsContentLoading] = useState(false)
  const [isDashboardLoading, setIsDashboardLoading] = useState(false)
  const [selectedPreviewItem, setSelectedPreviewItem] = useState<PortfolioItem | null>(null)
  const [savingReviewIds, setSavingReviewIds] = useState<Set<string>>(() => new Set())
  const savingReviewIdsRef = useRef<Set<string>>(new Set())

  const refreshProfiles = useCallback(async () => {
    if (!token) return []

    try {
      const items = await getAdminProfiles(token)
      setProfiles(items)
      return items
    } catch {
      setNotice({ type: 'error', text: 'Não foi possível carregar os perfis. Confirma se a API está ativa.' })
      return []
    }
  }, [token])

  const refreshContacts = useCallback(async () => {
    try {
      const items = await getAdminContacts(token)
      setContacts(items)
      return items
    } catch {
      setNotice({ type: 'error', text: 'Não foi possível carregar os contactos. Confirma se a API está ativa.' })
      return []
    }
  }, [token])

  const refreshReviews = useCallback(async () => {
    if (!token) return []

    try {
      const items = await getAdminReviews(token)
      setReviews(items)
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

    setIsContentLoading(true)
    try {
      const items = await getAdminPortfolioItems(slug, token)
      setContentItems(items)
      return items
    } catch {
      setNotice({ type: 'error', text: 'Não foi possível carregar os conteúdos deste perfil.' })
      return []
    } finally {
      setIsContentLoading(false)
    }
  }, [token])

  const refreshDashboard = useCallback(async () => {
    if (!token) return

    setIsDashboardLoading(true)
    try {
      const [bookingItems, reviewItems] = await Promise.all([
        getAdminBookings(token),
        getAdminReviews(token),
      ])
      setAdminBookings(bookingItems)
      setReviews(reviewItems)
    } catch {
      setNotice({ type: 'error', text: 'Não foi possível atualizar o resumo do painel.' })
    } finally {
      setIsDashboardLoading(false)
    }
  }, [token])

  useEffect(() => {
    if (!token) return
    let isCurrent = true

    void Promise.all([getAdminProfiles(token), getAdminContacts(token), getAdminReviews(token), getAdminBookings(token)])
      .then(([profileItems, contactItems, reviewItems, bookingItems]) => {
        if (!isCurrent) return
        setProfiles(profileItems)
        setContacts(contactItems)
        setReviews(reviewItems)
        setAdminBookings(bookingItems)
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

  function startProfileEditing(profile: AdminManagedProfile) {
    setEditingProfileSlug(profile.slug)
    setProfileForm({
      name: profile.name,
      slug: profile.slug,
      role: profile.role,
      description: profile.description,
      notificationEmail: profile.notificationEmail ?? '',
      profileImageUrl: profile.imageUrl ?? '',
      imageCrop: parseImageCrop(profile.imagePosition, profile.imageZoom),
      featuredVideoUrl: profile.featuredVideoUrl ?? '',
      heroBackgroundImageUrl: profile.heroBackgroundImageUrl ?? '',
      socialLinks: profile.socialLinks.map(toProfileSocialLinkForm),
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
      notificationEmail: profileForm.notificationEmail.trim() || null,
      profileImageUrl: profileForm.profileImageUrl.trim() || null,
      profileImagePosition: formatImagePosition(profileForm.imageCrop),
      profileImageZoom: profileForm.imageCrop.zoom,
      featuredVideoUrl: profileForm.featuredVideoUrl.trim() || null,
      heroBackgroundImageUrl: profileForm.heroBackgroundImageUrl.trim() || null,
      socialLinks: profileForm.socialLinks.map((link) => ({
        platform: link.platform.trim(),
        label: link.label.trim() || null,
        url: link.url.trim(),
      })),
    }

    try {
      let savedProfile: Profile | null = null
      if (editingProfileSlug) {
        savedProfile = await updateAdminProfile(editingProfileSlug, payload, token)
        setNotice({ type: 'success', text: 'Perfil atualizado com sucesso.' })
      } else {
        savedProfile = await createAdminProfile({ ...payload, slug: profileForm.slug.trim() }, token)
        setNotice({ type: 'success', text: 'Perfil criado com sucesso.' })
      }

      if (savedProfile) {
        setProfiles((current) => upsertProfile(current, savedProfile))
      }
      invalidateProfilesCache()
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

  function startContentEditing(item: AdminPortfolioItem) {
    setEditingContentId(item.id)
    setContentForm({
      profileSlug: contentForm.profileSlug,
      title: item.title,
      location: item.location,
      eventDate: item.eventDateIso,
      mediaUrl: item.mediaUrl,
      mediaType: item.type === 'Vídeo' ? 'VIDEO' : 'PHOTO',
      thumbnailUrl: item.thumbnailUrl ?? '',
      published: item.published,
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
      type: contentForm.mediaType as MediaType,
      title: contentForm.title.trim(),
      location: contentForm.location.trim(),
      eventDate: contentForm.eventDate,
      mediaUrl: contentForm.mediaUrl,
      thumbnailUrl: contentForm.thumbnailUrl.trim() || null,
      published: contentForm.published,
    }

    try {
      if (editingContentId) {
        await updateAdminPortfolioItem(contentForm.profileSlug, editingContentId, payload, token)
        setNotice({ type: 'success', text: 'Conteúdo atualizado com sucesso.' })
      } else {
        await createAdminPortfolioItem(contentForm.profileSlug, payload, token)
        setNotice({ type: 'success', text: contentForm.published ? 'Conteúdo publicado com sucesso.' : 'Conteúdo guardado como oculto.' })
      }

      await refreshContentItems(contentForm.profileSlug)
      window.dispatchEvent(new Event('portfolio:changed'))
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
      visible: contact.visible ?? true,
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
      visible: contactForm.visible,
    }

    try {
      if (editingContactId !== null) {
        await updateContact(editingContactId, payload, token)
        setNotice({ type: 'success', text: 'Contacto atualizado com sucesso.' })
      } else {
        await createContact(payload, token)
        setNotice({ type: 'success', text: 'Contacto adicionado com sucesso.' })
      }

      invalidateContactsCache()
      await refreshContacts()
      window.dispatchEvent(new Event('contacts:changed'))
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
      await deleteAdminProfile(profile.slug, token)
      invalidateProfilesCache()
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
      await deleteAdminPortfolioItem(contentForm.profileSlug, item.id, token)
      await refreshContentItems(contentForm.profileSlug)
      window.dispatchEvent(new Event('portfolio:changed'))

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

  async function toggleContentVisibility(item: AdminPortfolioItem) {
    if (!token || !contentForm.profileSlug || isSaving) return

    setIsSaving(true)
    try {
      await updateAdminPortfolioItem(contentForm.profileSlug, item.id, {
        ...portfolioItemToSaveInput(item),
        published: togglePortfolioPublication(item),
      }, token)
      await refreshContentItems(contentForm.profileSlug)
      window.dispatchEvent(new Event('portfolio:changed'))
      setNotice({ type: 'success', text: !item.published ? 'Conteúdo publicado.' : 'Conteúdo ocultado.' })
    } catch (error) {
      setNotice({
        type: 'error',
        text: error instanceof Error ? error.message : 'Não foi possível alterar a visibilidade do conteúdo.',
      })
    } finally {
      setIsSaving(false)
    }
  }

  async function deleteContact(contact: Contact) {
    if (!token || !window.confirm('Apagar este contacto?')) return

    setIsSaving(true)
    try {
      await deleteAdminContact(contact.id, token)
      invalidateContactsCache()
      await refreshContacts()
      window.dispatchEvent(new Event('contacts:changed'))

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

  async function toggleContactVisibility(contact: Contact) {
    if (!token || isSaving) return

    setIsSaving(true)
    try {
      const updated = await updateContact(contact.id, {
        ...contactToInput(contact),
        visible: nextContactVisible(contact),
      }, token)
      setContacts((current) => current.map((item) => item.id === contact.id ? updated : item))
      invalidateContactsCache()
      window.dispatchEvent(new Event('contacts:changed'))
      setNotice({ type: 'success', text: updated.visible ? 'Contacto visível no site público.' : 'Contacto ocultado do site público.' })
    } catch (error) {
      setNotice({
        type: 'error',
        text: error instanceof Error ? error.message : 'Não foi possível alterar a visibilidade do contacto.',
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

  async function reorderProfileStack(profileSlugs: string[]) {
    if (!token || isSaving) return
    setIsSaving(true)

    try {
      const orderedProfiles = await reorderAdminProfiles(profileSlugs, token)
      setProfiles(orderedProfiles)
      invalidateProfilesCache()
      window.dispatchEvent(new Event('profiles:changed'))
      setNotice({ type: 'success', text: 'Ordem dos perfis atualizada.' })
    } catch (error) {
      setNotice({
        type: 'error',
        text: error instanceof Error ? error.message : 'Não foi possível guardar a ordem dos perfis.',
      })
    } finally {
      setIsSaving(false)
    }
  }

  async function toggleReviewVisibility(review: Review) {
    if (!token || savingReviewIdsRef.current.has(review.id)) return

    const previousPublished = review.published
    const nextPublished = nextReviewPublished(review)

    savingReviewIdsRef.current.add(review.id)
    setSavingReviewIds(new Set(savingReviewIdsRef.current))
    setReviews((current) => updateReviewPublished(current, review.id, nextPublished))

    try {
      const updatedReview = await moderateReview(review.id, { published: nextPublished }, token)
      setReviews((current) => replaceReview(current, updatedReview))
      window.dispatchEvent(new Event('reviews:changed'))
      setNotice({ type: 'success', text: reviewVisibilityNotice(nextPublished) })
    } catch (error) {
      setReviews((current) => updateReviewPublished(current, review.id, previousPublished))
      setNotice({
        type: 'error',
        text: error instanceof Error ? error.message : 'Não foi possível moderar a avaliação.',
      })
    } finally {
      savingReviewIdsRef.current.delete(review.id)
      setSavingReviewIds(new Set(savingReviewIdsRef.current))
    }
  }

  async function deleteReview(review: Review) {
    if (!token || savingReviewIdsRef.current.has(review.id) || !window.confirm('Apagar esta avaliação?')) return

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
      <aside className={styles.sidebar}>
        <div className={styles.sidebarBrand}>
          <p className="eyebrow">Área reservada</p>
          <h1>Administração</h1>
        </div>
        <nav className={styles.tabs} aria-label="Secções de administração">
          <Tab active={page === 'dashboard'} onClick={() => onPageChange('dashboard')}>Resumo</Tab>
          <Tab active={page === 'profile'} onClick={() => onPageChange('profile')}>Perfis</Tab>
          <Tab active={page === 'content'} onClick={() => onPageChange('content')}>Conteúdo</Tab>
          <Tab active={page === 'contacts'} onClick={() => onPageChange('contacts')}>Contactos</Tab>
          <Tab active={page === 'materials'} onClick={() => onPageChange('materials')}>Materiais</Tab>
          <Tab active={page === 'reviews'} badge={reviews.filter((review) => !review.published).length} onClick={() => onPageChange('reviews')}>Avaliações</Tab>
          <Tab active={page === 'bookings'} badge={adminBookings.filter((booking) => booking.status === 'PENDING').length} onClick={() => onPageChange('bookings')}>Agendamentos</Tab>
        </nav>
        <button className={styles.exitButton} type="button" onClick={onExit}>
          <NavIcon name="arrow-left" />
          Voltar ao site
        </button>
      </aside>

      <div className={styles.adminContent}>
        <div className={styles.top}>
          <div>
            <p className="eyebrow">{adminPageEyebrow(page)}</p>
            <h2>{adminPageTitle(page)}</h2>
          </div>
          {notice && (
            <p className={[styles.notice, styles[notice.type]].join(' ')} role="status">
              {notice.text}
            </p>
          )}
        </div>

        {page === 'dashboard' && (
          <AdminDashboard
            bookings={adminBookings}
            isLoading={isDashboardLoading}
            profiles={profiles}
            reviews={reviews}
            onNavigate={onPageChange}
            onRefresh={refreshDashboard}
          />
        )}

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
            onReorder={reorderProfileStack}
            onUpload={(event) => upload(event, (url) => {
              setProfileForm((current) => ({ ...current, profileImageUrl: url, imageCrop: { x: 50, y: 50, zoom: 1 } }))
            })}
            onHeroBackgroundUpload={(event) => upload(event, (url) => {
              setProfileForm((current) => ({ ...current, heroBackgroundImageUrl: url }))
            }, { imagesOnly: true })}
            onFeaturedVideoUpload={(event) => upload(event, (url) => {
              setProfileForm((current) => ({ ...current, featuredVideoUrl: url }))
            })}
          />
        )}

        {page === 'content' && (
          <ContentManagement
            form={contentForm}
            isEditing={editingContentId !== null}
            isLoading={isContentLoading}
            isSaving={isSaving}
            items={contentItems}
            profiles={profiles}
            onChange={setContentForm}
            onSelectProfile={selectContentProfile}
            onSubmit={submitContent}
            onCancel={cancelContentEditing}
            onEdit={startContentEditing}
            onDelete={deleteContent}
            onPreview={setSelectedPreviewItem}
            onToggleVisibility={toggleContentVisibility}
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
            onToggleVisibility={toggleContactVisibility}
          />
        )}

        {page === 'reviews' && (
          <ReviewManagement
            isSaving={isSaving}
            savingReviewIds={savingReviewIds}
            reviews={reviews}
            onToggleVisibility={toggleReviewVisibility}
            onDelete={deleteReview}
          />
        )}

        {page === 'materials' && (
          <MaterialManagement token={token} onNotice={setNotice} />
        )}

        {page === 'bookings' && (
          <BookingManagement token={token} onNotice={setNotice} />
        )}
      </div>

      {selectedPreviewItem && <MediaLightbox item={selectedPreviewItem} onClose={() => setSelectedPreviewItem(null)} />}
    </section>
  )
}

function AdminDashboard({
  bookings,
  isLoading,
  profiles,
  reviews,
  onNavigate,
  onRefresh,
}: {
  bookings: Booking[]
  isLoading: boolean
  profiles: Profile[]
  reviews: Review[]
  onNavigate: (page: AdminPage) => void
  onRefresh: () => Promise<void>
}) {
  const today = new Date()
  const todayValue = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`
  const pendingBookings = bookings.filter((booking) => booking.status === 'PENDING')
  const pendingReviews = reviews.filter((review) => !review.published)
  const upcomingEvents = bookings
    .filter((booking) => booking.status === 'ACCEPTED' && booking.eventDate >= todayValue)
    .sort((first, second) => first.eventDate.localeCompare(second.eventDate) || (first.startTime ?? '').localeCompare(second.startTime ?? ''))
    .slice(0, 5)

  return (
    <section className={styles.overview} aria-labelledby="admin-overview-heading">
      <div className={styles.formHeading}>
        <div>
          <h2 id="admin-overview-heading">Visão geral</h2>
          <p className={styles.intro}>Prioridades do backoffice, próximos eventos e avaliações por moderar.</p>
        </div>
        <button className={styles.refreshButton} disabled={isLoading} type="button" onClick={() => { void onRefresh() }}>
          {isLoading ? 'A atualizar...' : 'Atualizar'}
        </button>
      </div>

      <div className={styles.metricGrid}>
        <button className={styles.metric} type="button" onClick={() => onNavigate('bookings')}>
          <span>Pedidos pendentes</span>
          <strong>{pendingBookings.length}</strong>
        </button>
        <button className={styles.metric} type="button" onClick={() => onNavigate('reviews')}>
          <span>Avaliações por aprovar</span>
          <strong>{pendingReviews.length}</strong>
        </button>
        <button className={styles.metric} type="button" onClick={() => onNavigate('profile')}>
          <span>Perfis ativos</span>
          <strong>{profiles.length}</strong>
        </button>
      </div>

      <div className={styles.overviewGrid}>
        <OverviewPanel title="Pedidos pendentes" empty="Sem pedidos pendentes." onOpen={() => onNavigate('bookings')}>
          {pendingBookings.slice(0, 4).map((booking) => (
            <li key={booking.id}>
              <strong>{booking.profileName}</strong>
              <span>{eventTypeSummary(booking)} · {formatAdminDate(booking.eventDate)} · {formatAdminTimeRange(booking.startTime, booking.endTime)}</span>
            </li>
          ))}
        </OverviewPanel>

        <OverviewPanel title="Avaliações por aprovar" empty="Sem avaliações por aprovar." onOpen={() => onNavigate('reviews')}>
          {pendingReviews.slice(0, 4).map((review) => (
            <li key={review.id}>
              <strong>{review.reviewerName}</strong>
              <span>{review.profileName ?? 'Artista'} · {review.rating}/5 · {review.reviewDate}</span>
            </li>
          ))}
        </OverviewPanel>

        <OverviewPanel title="Próximos eventos" empty="Sem eventos confirmados futuros." onOpen={() => onNavigate('bookings')}>
          {upcomingEvents.map((booking) => (
            <li key={booking.id}>
              <strong>{booking.profileName}</strong>
              <span>{formatAdminDate(booking.eventDate)} · {formatAdminTimeRange(booking.startTime, booking.endTime)}</span>
            </li>
          ))}
        </OverviewPanel>
      </div>
    </section>
  )
}

function OverviewPanel({
  children,
  empty,
  onOpen,
  title,
}: {
  children: ReactNode
  empty: string
  onOpen: () => void
  title: string
}) {
  const items = Array.isArray(children) ? children.filter(Boolean) : children ? [children] : []

  return (
    <section className={styles.overviewPanel}>
      <div>
        <h3>{title}</h3>
        <button type="button" onClick={onOpen}>Abrir</button>
      </div>
      {items.length === 0 ? (
        <p>{empty}</p>
      ) : (
        <ul>{items}</ul>
      )}
    </section>
  )
}

function Tab({
  active,
  badge,
  onClick,
  children,
}: {
  active: boolean
  badge?: number
  onClick: () => void
  children: ReactNode
}) {
  return (
    <button
      aria-current={active ? 'page' : undefined}
      className={active ? styles.activeTab : ''}
      type="button"
      onClick={onClick}
    >
      {children}
      {badge !== undefined && badge > 0 && <span className={styles.tabBadge}>{badge}</span>}
    </button>
  )
}

function StatusBadge({
  children,
  tone,
}: {
  children: ReactNode
  tone: 'success' | 'warning' | 'neutral'
}) {
  return (
    <span className={[styles.statusBadge, styles['statusBadge-' + tone]].join(' ')}>
      {children}
    </span>
  )
}

function adminPageTitle(page: AdminPage) {
  return {
    dashboard: 'Resumo',
    profile: 'Perfis',
    content: 'Conteúdo',
    contacts: 'Contactos',
    materials: 'Materiais',
    reviews: 'Avaliações',
    bookings: 'Agendamentos',
  }[page]
}

function adminPageEyebrow(page: AdminPage) {
  return {
    dashboard: 'Prioridades',
    profile: 'Homepage e artistas',
    content: 'Portfolio',
    contacts: 'Canais públicos',
    materials: 'Equipamento',
    reviews: 'Moderação',
    bookings: 'Pedidos e agenda',
  }[page]
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
  onReorder,
  onUpload,
  onHeroBackgroundUpload,
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
  onReorder: (profileSlugs: string[]) => Promise<void>
  onUpload: (event: ChangeEvent<HTMLInputElement>) => Promise<void>
  onHeroBackgroundUpload: (event: ChangeEvent<HTMLInputElement>) => Promise<void>
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

        <fieldset className={styles.formSection}>
          <legend>Identidade</legend>
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
        </fieldset>

        <fieldset className={styles.formSection}>
          <legend>Notificações</legend>
        <label>
          Email de notificações
          <input
            maxLength={254}
            onChange={(event) => onChange((current) => ({ ...current, notificationEmail: event.target.value }))}
            placeholder="artista@example.com"
            type="email"
            value={form.notificationEmail}
          />
          <small className={styles.fieldHint}>Privado. Usado apenas para notificações relacionadas com este artista e nunca apresentado no site público.</small>
        </label>
        </fieldset>

        <fieldset className={styles.formSection}>
          <legend>Imagem</legend>
        <label className={styles.fileUploadField}>
          <span>{isEditing ? 'Substituir imagem de perfil' : 'Imagem de perfil'}</span>
          <span className={styles.fileUploadButton}>
            <span aria-hidden="true">＋</span>
            {isEditing ? 'Substituir fotografia' : 'Adicionar fotografia'}
            <input
              accept="image/*"
              aria-label={isEditing ? 'Substituir imagem de perfil' : 'Adicionar imagem de perfil'}
              type="file"
              onChange={onUpload}
            />
          </span>
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
        </fieldset>

        <fieldset className={styles.formSection}>
          <legend>Background do perfil</legend>
          <label className={styles.fileUploadField}>
            <span>Imagem de fundo do topo do perfil</span>
            <span className={styles.fileUploadButton}>
              <span aria-hidden="true">＋</span>
              {form.heroBackgroundImageUrl ? 'Substituir background' : 'Adicionar background'}
              <input
                accept="image/*"
                aria-label={form.heroBackgroundImageUrl ? 'Substituir background do perfil' : 'Adicionar background do perfil'}
                type="file"
                onChange={onHeroBackgroundUpload}
              />
            </span>
          </label>

          <label>
            URL do background
            <input
              maxLength={2048}
              onChange={(event) => onChange((current) => ({ ...current, heroBackgroundImageUrl: event.target.value }))}
              placeholder="https://..."
              type="url"
              value={form.heroBackgroundImageUrl}
            />
          </label>

          {form.heroBackgroundImageUrl && (
            <div className={styles.thumbnailPreview}>
              <img src={form.heroBackgroundImageUrl} alt="Pré-visualização do background do perfil" />
              <button type="button" onClick={() => onChange((current) => ({ ...current, heroBackgroundImageUrl: '' }))}>
                Remover background
              </button>
            </div>
          )}

          <small className={styles.fieldHint}>
            Aparece apenas no topo do perfil. Sem imagem definida, o site usa automaticamente uma fotografia do portfólio como fallback.
          </small>
        </fieldset>

        <fieldset className={styles.formSection}>
          <legend>Vídeo em destaque</legend>
        <label className={styles.fileUploadField}>
          <span>Vídeo em destaque</span>
          <span className={styles.fileUploadButton}>
            <span aria-hidden="true">＋</span>
            Adicionar vídeo
            <input
              accept="video/*"
              aria-label="Adicionar vídeo de destaque"
              type="file"
              onChange={onFeaturedVideoUpload}
            />
          </span>
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
        </fieldset>

        <SocialLinksEditor
          links={form.socialLinks}
          onChange={(socialLinks) => onChange((current) => ({ ...current, socialLinks }))}
        />

        <FormActions
          isEditing={isEditing}
          isSaving={isSaving}
          createLabel="Criar perfil"
          updateLabel="Atualizar perfil"
          onCancel={onCancel}
        />
      </form>

      <ProfileOrderList
        profiles={profiles}
        isSaving={isSaving}
        onDelete={onDelete}
        onEdit={onEdit}
        onReorder={onReorder}
      />
    </div>
  )
}

function SocialLinksEditor({
  links,
  onChange,
}: {
  links: ProfileSocialLinkFormState[]
  onChange: (links: ProfileSocialLinkFormState[]) => void
}) {
  function updateLink(index: number, patch: Partial<ProfileSocialLinkFormState>) {
    onChange(links.map((link, currentIndex) => currentIndex === index ? { ...link, ...patch } : link))
  }

  function removeLink(index: number) {
    onChange(links.filter((_, currentIndex) => currentIndex !== index))
  }

  function moveLink(index: number, direction: -1 | 1) {
    const targetIndex = index + direction
    if (targetIndex < 0 || targetIndex >= links.length) return

    const nextLinks = [...links]
    const [link] = nextLinks.splice(index, 1)
    nextLinks.splice(targetIndex, 0, link)
    onChange(nextLinks)
  }

  return (
    <fieldset className={styles.socialLinksEditor}>
      <legend>Links sociais do perfil</legend>
      <p className={styles.fieldHint}>Adiciona redes sociais, website ou email que queres mostrar neste perfil.</p>

      {links.length === 0 ? (
        <p className={styles.emptyInline}>Sem links sociais.</p>
      ) : (
        <div className={styles.socialLinkRows}>
          {links.map((link, index) => {
            const isKnownPlatform = socialPlatformOptions.some((platform) => platform === link.platform)
            const isEmailPlatform = link.platform.trim().toUpperCase() === 'EMAIL'
            return (
              <div className={styles.socialLinkCard} key={index}>
                <div className={styles.socialLinkCardHeader}>
                  <span className={styles.socialLinkIcon} aria-hidden="true">
                    <SocialIcon name={socialPlatformIcon(link.platform)} />
                  </span>
                  <label>
                    Plataforma
                    <select
                      value={link.platform}
                      onChange={(event) => updateLink(index, { platform: event.target.value })}
                    >
                      {!isKnownPlatform && (
                        <option value={link.platform}>{socialPlatformLabel(link.platform)}</option>
                      )}
                      {socialPlatformOptions.map((platform) => (
                        <option key={platform} value={platform}>{socialPlatformLabel(platform)}</option>
                      ))}
                    </select>
                  </label>
                  <div className={styles.socialLinkActions}>
                    <button aria-label={`Subir ${socialPlatformLabel(link.platform)}`} disabled={index === 0} type="button" onClick={() => moveLink(index, -1)}>↑</button>
                    <button aria-label={`Descer ${socialPlatformLabel(link.platform)}`} disabled={index === links.length - 1} type="button" onClick={() => moveLink(index, 1)}>↓</button>
                    <button aria-label={`Remover ${socialPlatformLabel(link.platform)}`} type="button" onClick={() => removeLink(index)}>×</button>
                  </div>
                </div>
                <label>
                  {isEmailPlatform ? 'Email' : 'Link'}
                  <input
                    inputMode={isEmailPlatform ? 'email' : 'url'}
                    maxLength={2048}
                    onChange={(event) => updateLink(index, { url: event.target.value })}
                    placeholder={isEmailPlatform ? 'artista@example.com' : 'https://...'}
                    type={isEmailPlatform ? 'email' : 'url'}
                    value={link.url}
                  />
                </label>
                <label>
                  Nome a mostrar (opcional)
                  <input
                    maxLength={80}
                    onChange={(event) => updateLink(index, { label: event.target.value })}
                    placeholder={socialPlatformLabel(link.platform)}
                    value={link.label}
                  />
                </label>
              </div>
            )
          })}
        </div>
      )}

      <button
        className={styles.addInlineButton}
        disabled={links.length >= 12}
        type="button"
        onClick={() => onChange([...links, { platform: 'INSTAGRAM', label: '', url: '' }])}
      >
        Adicionar link social
      </button>
    </fieldset>
  )
}

function ProfileOrderList({
  profiles,
  isSaving,
  onDelete,
  onEdit,
  onReorder,
}: {
  profiles: AdminManagedProfile[]
  isSaving: boolean
  onDelete: (profile: Profile) => Promise<void>
  onEdit: (profile: AdminManagedProfile) => void
  onReorder: (profileSlugs: string[]) => Promise<void>
}) {
  const [draggingSlug, setDraggingSlug] = useState<string | null>(null)

  function dropOn(profileSlug: string) {
    if (draggingSlug === null || draggingSlug === profileSlug) return

    const draggingIndex = profiles.findIndex((profile) => profile.slug === draggingSlug)
    const targetIndex = profiles.findIndex((profile) => profile.slug === profileSlug)
    if (draggingIndex < 0 || targetIndex < 0) return

    const nextProfiles = [...profiles]
    const [draggingProfile] = nextProfiles.splice(draggingIndex, 1)
    nextProfiles.splice(targetIndex, 0, draggingProfile)
    void onReorder(nextProfiles.map((profile) => profile.slug))
  }

  function moveProfile(index: number, direction: -1 | 1) {
    const targetIndex = index + direction
    if (targetIndex < 0 || targetIndex >= profiles.length) return

    const nextProfiles = [...profiles]
    const [profile] = nextProfiles.splice(index, 1)
    nextProfiles.splice(targetIndex, 0, profile)
    void onReorder(nextProfiles.map((item) => item.slug))
  }

  return (
    <section className={styles.manage}>
      <h2>Perfis na homepage</h2>
      {profiles.length === 0 ? (
        <p>Não existem perfis.</p>
      ) : (
        <div className={styles.contactStack}>
          {profiles.map((profile, index) => (
            <div
              className={[styles.contactRow, draggingSlug === profile.slug ? styles.draggingRow : ''].join(' ')}
              draggable={!isSaving}
              key={profile.slug}
              onDragEnd={() => setDraggingSlug(null)}
              onDragOver={(event) => event.preventDefault()}
              onDragStart={() => setDraggingSlug(profile.slug)}
              onDrop={() => dropOn(profile.slug)}
            >
              <span className={styles.dragHandle} aria-hidden="true">☰</span>
              <span>
                <strong>{index + 1}. {profile.name}</strong>
                <small>{profile.slug}</small>
              </span>
              <span className={styles.rowActions}>
                <button aria-label={`Subir ${profile.name}`} disabled={isSaving || index === 0} type="button" onClick={() => moveProfile(index, -1)}>↑</button>
                <button aria-label={`Descer ${profile.name}`} disabled={isSaving || index === profiles.length - 1} type="button" onClick={() => moveProfile(index, 1)}>↓</button>
                <button disabled={isSaving} type="button" onClick={() => onEdit(profile)}>Editar</button>
                <button disabled={isSaving} type="button" onClick={() => { void onDelete(profile) }}>Apagar</button>
              </span>
            </div>
          ))}
        </div>
      )}
    </section>
  )
}

function ContentManagement({
  form,
  isEditing,
  isLoading,
  isSaving,
  items,
  profiles,
  onChange,
  onSelectProfile,
  onSubmit,
  onCancel,
  onEdit,
  onDelete,
  onPreview,
  onToggleVisibility,
  onUpload,
  onThumbnailUpload,
}: {
  form: ContentFormState
  isEditing: boolean
  isLoading: boolean
  isSaving: boolean
  items: AdminPortfolioItem[]
  profiles: Profile[]
  onChange: (value: ContentFormState | ((current: ContentFormState) => ContentFormState)) => void
  onSelectProfile: (slug: string) => void
  onSubmit: (event: FormEvent<HTMLFormElement>) => Promise<void>
  onCancel: () => void
  onEdit: (item: AdminPortfolioItem) => void
  onDelete: (item: AdminPortfolioItem) => Promise<void>
  onPreview: (item: PortfolioItem) => void
  onToggleVisibility: (item: AdminPortfolioItem) => Promise<void>
  onUpload: (event: ChangeEvent<HTMLInputElement>) => Promise<void>
  onThumbnailUpload: (event: ChangeEvent<HTMLInputElement>) => Promise<void>
}) {
  return (
    <div className={styles.page}>
      <form id="content-editor" className={styles.editorCard} onSubmit={(event) => { void onSubmit(event) }}>
        <FormHeading
          title={isEditing ? 'Editar conteúdo' : 'Novo conteúdo'}
          description={isEditing
            ? 'Atualiza o conteúdo, o estado público ou os ficheiros associados.'
            : 'Seleciona o perfil, carrega uma fotografia ou vídeo e decide se fica logo público.'}
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

        <div className={styles.formGrid}>
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
        </div>

        <label>
          Data do evento
          <input
            onChange={(event) => onChange((current) => ({ ...current, eventDate: event.target.value }))}
            required
            type="date"
            value={form.eventDate}
          />
          <small className={styles.fieldHint}>A listagem pública e admin usa sempre a data, do evento mais recente para o mais antigo.</small>
        </label>

        <label className={styles.checkboxLabel}>
          <input
            checked={form.published}
            onChange={(event) => onChange((current) => ({ ...current, published: event.target.checked }))}
            type="checkbox"
          />
          <span>{form.published ? 'Publicado no perfil público' : 'Guardar como oculto'}</span>
        </label>

        <label className={styles.fileUploadField}>
          <span>{isEditing ? 'Substituir conteúdo' : 'Ficheiro do conteúdo'}</span>
          <span className={styles.fileUploadButton}>
            <span aria-hidden="true">＋</span>
            {isEditing ? 'Substituir foto ou vídeo' : 'Adicionar foto ou vídeo'}
            <input
              accept="image/*,video/*"
              aria-label={isEditing ? 'Substituir foto ou vídeo' : 'Adicionar foto ou vídeo'}
              type="file"
              onChange={onUpload}
            />
          </span>
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

        <label className={styles.fileUploadField}>
          <span>Miniatura do conteúdo</span>
          <span className={styles.fileUploadButton}>
            <span aria-hidden="true">＋</span>
            Adicionar miniatura
            <input
              accept="image/*"
              aria-label="Adicionar miniatura do conteúdo"
              type="file"
              onChange={onThumbnailUpload}
            />
          </span>
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
          createLabel={form.published ? 'Publicar conteúdo' : 'Guardar oculto'}
          updateLabel="Atualizar conteúdo"
          onCancel={onCancel}
        />
      </form>

      <section className={styles.manage}>
        <div className={styles.sectionTitleRow}>
          <div>
            <h2>Conteúdo do perfil</h2>
            <p>{form.profileSlug ? 'Ordenado automaticamente por data do evento.' : 'Seleciona um perfil para ver e gerir o portfolio.'}</p>
          </div>
          {form.profileSlug && <StatusBadge tone="neutral">{items.length} itens</StatusBadge>}
        </div>

        {isLoading ? (
          <p className={styles.emptyState}>A carregar conteúdos...</p>
        ) : !form.profileSlug ? (
          <p className={styles.emptyState}>Seleciona um perfil para gerir o seu portfolio.</p>
        ) : items.length === 0 ? (
          <p className={styles.emptyState}>Este perfil ainda não tem conteúdos.</p>
        ) : (
          <div className={styles.contentList}>
            {items.map((item) => (
              <article className={styles.contentCard} key={item.id}>
                <button
                  className={styles.contentPreview}
                  type="button"
                  onClick={() => onPreview(item)}
                >
                  {item.thumbnailUrl || item.type === 'Foto' ? (
                    <img src={item.thumbnailUrl ?? item.mediaUrl} alt="" />
                  ) : (
                    <span>Vídeo</span>
                  )}
                  {item.type === 'Vídeo' && <span className={styles.videoMarker}>▶</span>}
                </button>
                <div className={styles.contentMeta}>
                  <StatusBadge tone={item.published ? 'success' : 'warning'}>
                    {portfolioPublicationLabel(item.published)}
                  </StatusBadge>
                  <h3>{item.title}</h3>
                  <p>{item.location} · {item.eventDate} · {item.type}</p>
                </div>
                <div className={styles.contentActions}>
                  <button disabled={isSaving} type="button" onClick={() => onEdit(item)}>Editar</button>
                  <button disabled={isSaving} type="button" onClick={() => { void onToggleVisibility(item) }}>
                    {item.published ? 'Ocultar' : 'Publicar'}
                  </button>
                  <button className={styles.dangerButton} disabled={isSaving} type="button" onClick={() => { void onDelete(item) }}>Eliminar</button>
                </div>
              </article>
            ))}
          </div>
        )}
      </section>
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
  onToggleVisibility,
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
  onToggleVisibility: (contact: Contact) => Promise<void>
}) {
  const field = contactField(form.type)

  return (
    <div className={styles.page}>
      <form id="contact-editor" className={styles.editorCard} onSubmit={(event) => { void onSubmit(event) }}>
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

        <label className={styles.checkboxLabel}>
          <input
            checked={form.visible}
            onChange={(event) => onChange((current) => ({ ...current, visible: event.target.checked }))}
            type="checkbox"
          />
          <span>{form.visible ? 'Visível na página pública' : 'Oculto da página pública'}</span>
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
        onToggleVisibility={onToggleVisibility}
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
  onToggleVisibility,
}: {
  contacts: Contact[]
  isSaving: boolean
  onDelete: (contact: Contact) => Promise<void>
  onEdit: (contact: Contact) => void
  onReorder: (contactIds: number[]) => Promise<void>
  onToggleVisibility: (contact: Contact) => Promise<void>
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

  function moveContact(index: number, direction: -1 | 1) {
    const targetIndex = index + direction
    if (targetIndex < 0 || targetIndex >= contacts.length) return

    const nextContacts = [...contacts]
    const [contact] = nextContacts.splice(index, 1)
    nextContacts.splice(targetIndex, 0, contact)
    void onReorder(nextContacts.map((item) => item.id))
  }

  return (
    <section className={styles.manage}>
      <div className={styles.sectionTitleRow}>
        <div>
          <h2>Contactos</h2>
          <p>Arrasta para ordenar. Contactos ocultos ficam fora da página pública.</p>
        </div>
      </div>
      {contacts.length === 0 ? (
        <p>Não existem contactos.</p>
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
              <StatusBadge tone={contact.visible ?? true ? 'success' : 'warning'}>
                {contactVisibilityLabel(contact.visible)}
              </StatusBadge>
              <span className={styles.rowActions}>
                <button aria-label={`Subir ${contact.label}`} disabled={isSaving || index === 0} type="button" onClick={() => moveContact(index, -1)}>↑</button>
                <button aria-label={`Descer ${contact.label}`} disabled={isSaving || index === contacts.length - 1} type="button" onClick={() => moveContact(index, 1)}>↓</button>
                <button disabled={isSaving} type="button" onClick={() => onEdit(contact)}>Editar</button>
                <button disabled={isSaving} type="button" onClick={() => { void onToggleVisibility(contact) }}>
                  {contact.visible ?? true ? 'Ocultar' : 'Mostrar'}
                </button>
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
  isSaving,
  savingReviewIds,
  reviews,
  onToggleVisibility,
  onDelete,
}: {
  isSaving: boolean
  savingReviewIds: Set<string>
  reviews: Review[]
  onToggleVisibility: (review: Review) => Promise<void>
  onDelete: (review: Review) => Promise<void>
}) {
  return (
    <section className={styles.singlePage}>
      <div className={styles.formHeading}>
        <div>
          <p className={styles.intro}>As avaliações são submetidas pelos utilizadores nos perfis dos artistas. Aqui escolhes quais aparecem publicamente; a ordem é sempre da mais recente para a mais antiga.</p>
        </div>
      </div>

      {reviews.length === 0 ? (
        <p className={styles.intro}>Ainda não existem avaliações submetidas.</p>
      ) : (
        <div className={styles.reviewList}>
          {reviews.map((review) => {
            const isVisibilitySaving = savingReviewIds.has(review.id)
            return (
              <article className={styles.reviewRow} key={review.id}>
                <div>
                  <p className={styles.reviewMeta}>{review.profileName ?? 'Artista'} · {review.reviewDate} · {review.submittedByEmail ?? 'utilizador'}</p>
                  <h3>{review.reviewerName} - {review.title}</h3>
                  <p className={styles.reviewStars}>{stars(review.rating)} {review.rating.toFixed(1)}</p>
                  <p className={styles.reviewComment}>{review.comment}</p>
                </div>
                <div className={styles.reviewModeration}>
                  <button
                    aria-pressed={review.published}
                    className={[
                      styles.visibilityToggle,
                      review.published ? styles.visibilityPublished : styles.visibilityHidden,
                    ].join(' ')}
                    disabled={isSaving || isVisibilitySaving}
                    type="button"
                    onClick={() => { void onToggleVisibility(review) }}
                  >
                    {reviewVisibilityLabel(review.published)}{isVisibilitySaving ? ' · a guardar...' : ''}
                  </button>
                  <button disabled={isSaving || isVisibilitySaving} type="button" onClick={() => { void onDelete(review) }}>Apagar</button>
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

function toProfileSocialLinkForm(link: ProfileSocialLink): ProfileSocialLinkFormState {
  return {
    platform: link.platform,
    label: link.label ?? '',
    url: link.url,
  }
}

function upsertProfile(profiles: AdminManagedProfile[], profile: AdminManagedProfile) {
  const exists = profiles.some((item) => item.slug === profile.slug)
  const nextProfiles = exists
    ? profiles.map((item) => item.slug === profile.slug ? profile : item)
    : [...profiles, profile]
  return [...nextProfiles].sort(sortProfiles)
}

function sortProfiles(first: Profile, second: Profile) {
  return (first.displayOrder ?? 0) - (second.displayOrder ?? 0) || first.name.localeCompare(second.name, 'pt-PT')
}

function validateProfile(form: ProfileFormState) {
  const notificationEmail = form.notificationEmail.trim()
  if (form.name.trim().length < 2) return 'O nome do perfil tem de ter pelo menos 2 caracteres.'
  if (!slugPattern.test(form.slug.trim())) return 'O slug deve usar apenas letras minúsculas, números e hífenes, por exemplo: dj-joao-tomas.'
  if (form.role.trim().length < 2) return 'Indica uma função com pelo menos 2 caracteres.'
  if (form.description.trim().length < 10) return 'A descrição tem de ter pelo menos 10 caracteres.'
  if (notificationEmail.length > 254) return 'O email de notificações é demasiado longo.'
  if (notificationEmail && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(notificationEmail)) return 'Indica um email de notificações válido.'
  if (form.profileImageUrl.length > 2048) return 'A URL da imagem é demasiado longa.'
  if (form.featuredVideoUrl.length > 2048) return 'A URL do vídeo de destaque é demasiado longa.'
  if (form.socialLinks.length > 12) return 'Um perfil pode ter no máximo 12 links sociais.'
  for (const [index, link] of form.socialLinks.entries()) {
    if (link.label.length > 80) return `O rótulo do link social ${index + 1} é demasiado longo.`
    if (link.url.length > 2048) return `O URL do link social ${index + 1} é demasiado longo.`
    const socialLinkError = validateSocialLink(link.platform, link.url)
    if (socialLinkError) return `Link social ${index + 1}: ${socialLinkError}`
  }
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

  if (form.type === 'WEBSITE' && !isWebsiteValue(value)) {
    return 'Indica um website válido, com ou sem https://.'
  }

  return null
}

function eventTypeSummary(booking: Booking) {
  const labels: Record<string, string> = {
    WEDDING: 'Casamento',
    BIRTHDAY: 'Aniversário',
    BAPTISM: 'Batizado',
    CORPORATE: 'Evento empresarial',
    PRIVATE_PARTY: 'Festa privada',
    FESTIVAL: 'Festival',
  }

  if (booking.eventType === 'OTHER' && booking.customEventType) return booking.customEventType
  return labels[booking.eventType] ?? booking.eventType
}

function formatAdminDate(value: string) {
  return new Intl.DateTimeFormat('pt-PT', { day: '2-digit', month: 'short', year: 'numeric' })
    .format(new Date(`${value}T00:00:00`))
}

function formatAdminTimeRange(startTime: string | null, endTime: string | null) {
  return startTime && endTime ? `${startTime.slice(0, 5)} - ${endTime.slice(0, 5)}` : 'Horário a combinar'
}

function stars(rating: number) {
  return Array.from({ length: 5 }, (_, index) => index < rating ? '★' : '☆').join('')
}

function isWebsiteValue(value: string) {
  const candidate = value.toLowerCase().startsWith('http://') || value.toLowerCase().startsWith('https://')
    ? value
    : 'https://' + value

  try {
    const url = new URL(candidate)
    return (url.protocol === 'http:' || url.protocol === 'https:') && Boolean(url.hostname)
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
      placeholder: 'saltosnaspalhacadas.pt',
      inputType: 'text',
      inputMode: 'url',
    },
  } as const)[type]
}

function scrollToEditor(id: string) {
  window.requestAnimationFrame(() => {
    document.getElementById(id)?.scrollIntoView({ behavior: 'smooth', block: 'start' })
  })
}
