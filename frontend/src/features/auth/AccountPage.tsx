import { useEffect, useState, type ChangeEvent, type FormEvent } from 'react'
import { ImageCropEditor } from '../../components/ImageCropEditor'
import { CroppedImage } from '../../components/CroppedImage'
import { useAuthenticatedMediaUrl } from '../../components/AuthenticatedMedia'
import { formatImagePosition, parseImageCrop, type ImageCrop } from '../../components/imageCrop'
import { uploadUserImage } from '../../services/apiClient'
import { formatNotificationDate } from '../../services/notificationService'
import type { UserNotification } from '../../types/notification'
import { useNotifications } from '../notifications/NotificationContext'
import { useAuth } from './AuthContext'
import { NavIcon } from '../../components/NavIcon/NavIcon'
import headerStyles from '../../components/Header/Header.module.css'
import styles from './AccountPage.module.css'

type AccountPageProps = {
  onBookingsClick: () => void
  onFavoritesClick: () => void
  onExit: () => void
}

type AccountForm = {
  username: string
  firstName: string
  lastName: string
  phone: string
  profileImageUrl: string
  profileImageMediaId: string
  imageCrop: ImageCrop
}

export function AccountPage({ onBookingsClick, onFavoritesClick, onExit }: AccountPageProps) {
  const { changePassword, deleteAccount, exportAccountData, favorites, logout, session, updateAccount } = useAuth()
  const {
    error: notificationLoadError,
    isLoading: isLoadingNotifications,
    markAllRead,
    markRead,
    notifications: accountNotifications,
    refresh: refreshNotifications,
    unreadCount,
  } = useNotifications()
  const [form, setForm] = useState<AccountForm>(() => emptyForm(session))
  const [passwordForm, setPasswordForm] = useState({ currentPassword: '', newPassword: '', confirmation: '' })
  const [deletePassword, setDeletePassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [notice, setNotice] = useState<string | null>(null)
  const [securityError, setSecurityError] = useState<string | null>(null)
  const [securityNotice, setSecurityNotice] = useState<string | null>(null)
  const [dataError, setDataError] = useState<string | null>(null)
  const [isEditing, setIsEditing] = useState(false)
  const [isSaving, setIsSaving] = useState(false)
  const [isChangingPassword, setIsChangingPassword] = useState(false)
  const [isExportingData, setIsExportingData] = useState(false)
  const [isDeletingAccount, setIsDeletingAccount] = useState(false)
  const [isCropDialogOpen, setIsCropDialogOpen] = useState(false)
  const [notificationActionError, setNotificationActionError] = useState<string | null>(null)
  const [notificationActionId, setNotificationActionId] = useState<string | null>(null)
  const [isMarkingAllNotifications, setIsMarkingAllNotifications] = useState(false)

  const visibleForm = session ? (isEditing ? form : emptyForm(session)) : emptyForm(null)
  const resolvedProfileImageUrl = useAuthenticatedMediaUrl(visibleForm.profileImageUrl, session?.token)

  if (!session) return null
  const notificationError = notificationActionError ?? notificationLoadError

  const accountName = displayName(visibleForm.firstName, visibleForm.lastName) || visibleForm.username || session.email
  const avatar = (
    <CroppedImage
      className={styles.avatar}
      fallback={initials(visibleForm.firstName, visibleForm.lastName, session.email)}
      position={formatImagePosition(visibleForm.imageCrop)}
      src={resolvedProfileImageUrl}
      zoom={visibleForm.imageCrop.zoom}
    />
  )

  function startEditing() {
    setForm(emptyForm(session))
    setError(null)
    setNotice(null)
    setIsEditing(true)
  }

  function cancelEditing() {
    setForm(emptyForm(session))
    setError(null)
    setNotice(null)
    setIsCropDialogOpen(false)
    setIsEditing(false)
  }

  async function uploadPhoto(event: ChangeEvent<HTMLInputElement>) {
    const input = event.currentTarget
    const file = input.files?.[0]
    if (!file || !session) return

    if (!file.type.startsWith('image/')) {
      setError('Seleciona uma imagem válida.')
      input.value = ''
      return
    }

    setError(null)
    setNotice(null)
    setIsSaving(true)
    try {
      const result = await uploadUserImage(file, session.token)
      setForm((current) => ({ ...current, profileImageUrl: result.url, profileImageMediaId: result.id, imageCrop: { x: 50, y: 50, zoom: 1 } }))
      setIsCropDialogOpen(true)
      setNotice('Foto carregada. Ajusta o enquadramento e guarda o perfil.')
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'Não foi possível carregar a foto.')
    } finally {
      setIsSaving(false)
      input.value = ''
    }
  }

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const validationError = validateAccount(form)
    if (validationError) {
      setError(validationError)
      return
    }

    setError(null)
    setNotice(null)
    setIsSaving(true)
    try {
      await updateAccount({
        username: form.username.trim().toLowerCase(),
        firstName: form.firstName.trim(),
        lastName: form.lastName.trim(),
        phone: form.phone.trim(),
        profileImageUrl: form.profileImageUrl.trim() || null,
        profileImageMediaId: form.profileImageMediaId || null,
        profileImagePosition: formatImagePosition(form.imageCrop),
        profileImageZoom: form.imageCrop.zoom,
      })
      setNotice('Perfil atualizado.')
      setIsCropDialogOpen(false)
      setIsEditing(false)
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'Não foi possível atualizar o perfil.')
    } finally {
      setIsSaving(false)
    }
  }

  async function submitPasswordChange(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (passwordForm.newPassword.length < 8) {
      setSecurityError('A nova palavra-passe tem de ter pelo menos 8 caracteres.')
      return
    }
    if (passwordForm.newPassword !== passwordForm.confirmation) {
      setSecurityError('As palavras-passe não coincidem.')
      return
    }

    setIsChangingPassword(true)
    setSecurityError(null)
    setSecurityNotice(null)
    try {
      await changePassword({
        currentPassword: passwordForm.currentPassword,
        newPassword: passwordForm.newPassword,
      })
      setPasswordForm({ currentPassword: '', newPassword: '', confirmation: '' })
      setSecurityNotice('Palavra-passe atualizada.')
    } catch (reason) {
      setSecurityError(reason instanceof Error ? reason.message : 'Não foi possível alterar a palavra-passe.')
    } finally {
      setIsChangingPassword(false)
    }
  }

  async function downloadAccountData() {
    setIsExportingData(true)
    setDataError(null)
    try {
      const data = await exportAccountData()
      const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' })
      const url = URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = `saltos-nas-palhacadas-dados-${new Date().toISOString().slice(0, 10)}.json`
      document.body.appendChild(link)
      link.click()
      link.remove()
      URL.revokeObjectURL(url)
    } catch (reason) {
      setDataError(reason instanceof Error ? reason.message : 'Não foi possível descarregar os teus dados.')
    } finally {
      setIsExportingData(false)
    }
  }

  async function submitAccountDeletion(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!session) return

    if (session.role === 'ADMIN') {
      setDataError('A conta de administrador deve ser removida manualmente por outro administrador.')
      return
    }
    if (!deletePassword) {
      setDataError('Confirma a tua palavra-passe para eliminar a conta.')
      return
    }
    if (!window.confirm('Eliminar a conta de forma permanente? Esta ação remove os teus dados pessoais, favoritos e reviews.')) {
      return
    }

    setIsDeletingAccount(true)
    setDataError(null)
    try {
      await deleteAccount(deletePassword)
      onExit()
    } catch (reason) {
      setDataError(reason instanceof Error ? reason.message : 'Não foi possível eliminar a conta.')
    } finally {
      setIsDeletingAccount(false)
    }
  }

  async function markOneNotificationRead(notification: UserNotification) {
    if (notification.read) return
    setNotificationActionId(notification.id)
    setNotificationActionError(null)
    try {
      await markRead(notification.id)
    } catch (reason) {
      setNotificationActionError(reason instanceof Error ? reason.message : 'Não foi possível marcar a notificação como lida.')
    } finally {
      setNotificationActionId(null)
    }
  }

  async function markEveryNotificationRead() {
    setIsMarkingAllNotifications(true)
    setNotificationActionError(null)
    try {
      await markAllRead()
    } catch (reason) {
      setNotificationActionError(reason instanceof Error ? reason.message : 'Não foi possível marcar todas as notificações como lidas.')
    } finally {
      setIsMarkingAllNotifications(false)
    }
  }

  function openNotificationBooking(notification: UserNotification) {
    if (!notification.read) {
      void markRead(notification.id).catch(() => undefined)
    }
    onBookingsClick()
  }

  function retryNotifications() {
    setNotificationActionError(null)
    void refreshNotifications()
  }

  return (
    <section className={styles.page}>
      <button className={styles.back} type="button" onClick={onExit}>
        <NavIcon name="arrow-left" />
        Voltar aos perfis
      </button>
      <div className={styles.content}>
        <p className="eyebrow">A minha conta</p>
        <div className={styles.accountHeader}>
          {avatar}
          <div>
            <h1>{accountName}</h1>
            <p>{session.role === 'ADMIN' ? 'Administração' : 'Utilizador'}</p>
          </div>
        </div>

        {!isEditing ? (
          <div className={styles.summary}>
            <dl className={styles.details}>
              <div><dt>Email</dt><dd>{session.email}</dd></div>
              <div><dt>Nome de utilizador</dt><dd>{visibleForm.username || 'Por preencher'}</dd></div>
              <div><dt>Primeiro nome</dt><dd>{visibleForm.firstName || 'Por preencher'}</dd></div>
              <div><dt>Último nome</dt><dd>{visibleForm.lastName || 'Por preencher'}</dd></div>
              <div><dt>Contacto telefónico</dt><dd>{visibleForm.phone || 'Por preencher'}</dd></div>
              <div><dt>Favoritos</dt><dd>{favorites.length}</dd></div>
            </dl>

            {error && <p className={styles.error} role="alert">{error}</p>}
            {notice && <p className={styles.success} role="status">{notice}</p>}

            <div className={styles.actions}>
              <button type="button" onClick={startEditing}>Editar perfil</button>
              <button type="button" onClick={onFavoritesClick}>Ver favoritos ({favorites.length})</button>
              <button className={styles.logout} type="button" onClick={() => { logout(); onExit() }}>Terminar sessão</button>
            </div>

            <div className={styles.accountSections}>
              <section className={styles.sectionBlock}>
                <h2>Área pessoal</h2>
                <div className={styles.quickLinks}>
                  <button type="button" onClick={onFavoritesClick}>
                    <span>Favoritos</span>
                    <strong>{favorites.length}</strong>
                  </button>
                  <button type="button" onClick={onBookingsClick}>
                    <span>Agendamentos</span>
                    <strong>Ver pedidos</strong>
                  </button>
                </div>
              </section>

              <section className={`${styles.sectionBlock} ${styles.notificationsSection}`} aria-labelledby="account-notifications-title">
                <div className={styles.notificationsHeader}>
                  <div>
                    <h2 id="account-notifications-title">Notificações</h2>
                    <p>Atualizações importantes que requerem a tua atenção.</p>
                  </div>
                  <span className={styles.unreadBadge} aria-label={`${unreadCount} notificações não lidas`}>
                    {unreadCount} não {unreadCount === 1 ? 'lida' : 'lidas'}
                  </span>
                </div>

                {unreadCount > 0 && !isLoadingNotifications && (
                  <button
                    className={styles.markAllButton}
                    disabled={isMarkingAllNotifications}
                    type="button"
                    onClick={() => { void markEveryNotificationRead() }}
                  >
                    {isMarkingAllNotifications ? 'A atualizar...' : 'Marcar todas como lidas'}
                  </button>
                )}

                {isLoadingNotifications && <p className={styles.notificationState} role="status">A carregar notificações...</p>}
                {notificationError && (
                  <div className={styles.notificationError} role="alert">
                    <p>{notificationError}</p>
                    <button type="button" onClick={retryNotifications}>Tentar novamente</button>
                  </div>
                )}
                {!isLoadingNotifications && !notificationError && accountNotifications.length === 0 && (
                  <p className={styles.notificationState}>Não tens notificações.</p>
                )}
                {!isLoadingNotifications && accountNotifications.length > 0 && (
                  <ul className={styles.notificationList}>
                    {accountNotifications.map((notification) => (
                      <li className={notification.read ? styles.notificationRead : styles.notificationUnread} key={notification.id}>
                        <div className={styles.notificationCopy}>
                          <div className={styles.notificationTitleRow}>
                            {!notification.read && <span className={styles.unreadDot} aria-label="Não lida" />}
                            <h3>{notification.title}</h3>
                          </div>
                          <p>{notification.message}</p>
                          <time dateTime={notification.createdAt}>{formatNotificationDate(notification.createdAt)}</time>
                        </div>
                        <div className={styles.notificationActions}>
                          {!notification.read && (
                            <button
                              disabled={notificationActionId === notification.id}
                              type="button"
                              onClick={() => { void markOneNotificationRead(notification) }}
                            >
                              {notificationActionId === notification.id ? 'A atualizar...' : 'Marcar como lida'}
                            </button>
                          )}
                          {notification.bookingId && (
                            <button className={styles.viewBookingButton} type="button" onClick={() => openNotificationBooking(notification)}>
                              Ver agendamentos
                            </button>
                          )}
                        </div>
                      </li>
                    ))}
                  </ul>
                )}
              </section>

              <section className={styles.sectionBlock}>
                <h2>Segurança</h2>
                <form className={styles.securityForm} onSubmit={(event) => { void submitPasswordChange(event) }}>
                  <label>
                    Palavra-passe atual
                    <input
                      autoComplete="current-password"
                      onChange={(event) => setPasswordForm((current) => ({ ...current, currentPassword: event.target.value }))}
                      required
                      type="password"
                      value={passwordForm.currentPassword}
                    />
                  </label>
                  <label>
                    Nova palavra-passe
                    <input
                      autoComplete="new-password"
                      minLength={8}
                      onChange={(event) => setPasswordForm((current) => ({ ...current, newPassword: event.target.value }))}
                      required
                      type="password"
                      value={passwordForm.newPassword}
                    />
                  </label>
                  <label>
                    Confirmar nova palavra-passe
                    <input
                      autoComplete="new-password"
                      minLength={8}
                      onChange={(event) => setPasswordForm((current) => ({ ...current, confirmation: event.target.value }))}
                      required
                      type="password"
                      value={passwordForm.confirmation}
                    />
                  </label>
                  {securityError && <p className={styles.error} role="alert">{securityError}</p>}
                  {securityNotice && <p className={styles.success} role="status">{securityNotice}</p>}
                  <button disabled={isChangingPassword} type="submit">{isChangingPassword ? 'A atualizar...' : 'Alterar palavra-passe'}</button>
                </form>
              </section>

              <section className={styles.sectionBlock}>
                <h2>Dados e privacidade</h2>
                <p>Descarrega uma cópia dos dados associados à tua conta.</p>
                <button className={styles.secondaryButton} disabled={isExportingData} type="button" onClick={() => { void downloadAccountData() }}>{isExportingData ? 'A preparar...' : 'Descarregar os meus dados'}</button>
                <form className={styles.deleteForm} onSubmit={(event) => { void submitAccountDeletion(event) }}>
                  <label>
                    Palavra-passe
                    <input
                      autoComplete="current-password"
                      disabled={session.role === 'ADMIN'}
                      onChange={(event) => setDeletePassword(event.target.value)}
                      type="password"
                      value={deletePassword}
                    />
                  </label>
                  {dataError && <p className={styles.error} role="alert">{dataError}</p>}
                  <button className={styles.dangerButton} disabled={isDeletingAccount || session.role === 'ADMIN'} type="submit">
                    {isDeletingAccount ? 'A eliminar...' : 'Eliminar a minha conta'}
                  </button>
                </form>
              </section>
            </div>
          </div>
        ) : (
          <form className={styles.profileForm} onSubmit={(event) => { void submit(event) }}>
            <div className={styles.avatarEditor}>
              {avatar}
              <div className={styles.avatarUpload}>
                <span className={styles.avatarUploadLabel}>Foto de perfil</span>
                <div className={styles.avatarControls}>
                  <label className={styles.uploadButton}>
                    <span>＋ Adicionar foto</span>
                    <input
                      accept="image/*"
                      className={styles.fileInput}
                      type="file"
                      onChange={(event) => { void uploadPhoto(event) }}
                    />
                  </label>
                  {form.profileImageUrl && resolvedProfileImageUrl && (
                    <button className={styles.adjustPhotoButton} type="button" onClick={() => setIsCropDialogOpen(true)}>
                      Ajustar posição
                    </button>
                  )}
                </div>
                <small>Escolhe uma imagem e ajusta o enquadramento antes de guardar.</small>
              </div>
            </div>

            {isCropDialogOpen && form.profileImageUrl && resolvedProfileImageUrl && (
              <AccountImageCropDialog
                crop={form.imageCrop}
                src={resolvedProfileImageUrl}
                onClose={() => setIsCropDialogOpen(false)}
                onSave={(imageCrop) => setForm((current) => ({ ...current, imageCrop }))}
              />
            )}

            <label>
              Email
              <input readOnly value={session.email} />
            </label>
            <label>
              Nome de utilizador
              <input
                maxLength={30}
                minLength={3}
                onChange={(event) => setForm((current) => ({ ...current, username: event.target.value.toLowerCase().replace(/\s+/g, '') }))}
                pattern="(?!.*\.\.)(?!\.)(?!.*\.$)[a-z0-9._]{3,30}"
                required
                value={form.username}
              />
              <small>Minúsculas, números, ponto ou underscore.</small>
            </label>
            <div className={styles.nameFields}>
              <label>
                Primeiro nome
                <input
                  maxLength={80}
                  minLength={2}
                  onChange={(event) => setForm((current) => ({ ...current, firstName: event.target.value }))}
                  required
                  value={form.firstName}
                />
              </label>
              <label>
                Último nome
                <input
                  maxLength={80}
                  minLength={2}
                  onChange={(event) => setForm((current) => ({ ...current, lastName: event.target.value }))}
                  required
                  value={form.lastName}
                />
              </label>
            </div>
            <label>
              Contacto telefónico
              <input
                inputMode="tel"
                onChange={(event) => setForm((current) => ({ ...current, phone: event.target.value }))}
                placeholder="+351 912 345 678"
                required
                type="tel"
                value={form.phone}
              />
            </label>

            {error && <p className={styles.error} role="alert">{error}</p>}
            {notice && <p className={styles.success} role="status">{notice}</p>}

            <div className={styles.actions}>
              <button disabled={isSaving} type="submit">{isSaving ? 'A guardar...' : 'Guardar perfil'}</button>
              <button className={styles.cancelEditButton} disabled={isSaving} type="button" onClick={cancelEditing}>Cancelar edição</button>
            </div>
          </form>
        )}
      </div>
    </section>
  )
}


function AccountImageCropDialog({
  crop,
  src,
  onClose,
  onSave,
}: {
  crop: ImageCrop
  src: string
  onClose: () => void
  onSave: (crop: ImageCrop) => void
}) {
  const [draftCrop, setDraftCrop] = useState(crop)

  useEffect(() => {
    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === 'Escape') onClose()
    }

    window.addEventListener('keydown', handleKeyDown)
    return () => window.removeEventListener('keydown', handleKeyDown)
  }, [onClose])

  function saveCrop() {
    onSave(draftCrop)
    onClose()
  }

  return (
    <div className={styles.cropDialogBackdrop} role="presentation" onMouseDown={onClose}>
      <div
        aria-label="Ajustar foto de perfil"
        aria-modal="true"
        className={styles.cropDialog}
        role="dialog"
        onMouseDown={(event) => event.stopPropagation()}
      >
        <ImageCropEditor
          aspectRatio="1 / 1"
          comparisonPreviews={[
            {
              title: 'Conta',
              render: ({ imagePosition, src, zoom }) => (
                <CroppedImage
                  alt="Pré-visualização da foto na conta"
                  className={styles.avatar}
                  position={imagePosition}
                  src={src}
                  zoom={zoom}
                />
              ),
            },
            {
              title: 'Header',
              render: ({ imagePosition, src, zoom }) => (
                <CroppedImage
                  alt="Pré-visualização da foto no header"
                  className={headerStyles.accountAvatar}
                  position={imagePosition}
                  src={src}
                  zoom={zoom}
                />
              ),
            },
          ]}
          crop={draftCrop}
          description="Arrasta a fotografia e ajusta o zoom para escolher como a tua foto aparece na conta."
          shape="circle"
          src={src}
          title="Ajustar foto de perfil"
          onChange={setDraftCrop}
        />
        <div className={styles.cropDialogActions}>
          <button type="button" onClick={saveCrop}>Guardar enquadramento</button>
          <button className={styles.cropDialogCancel} type="button" onClick={onClose}>Cancelar</button>
        </div>
      </div>
    </div>
  )
}

function emptyForm(session: { email: string; username?: string; firstName?: string; lastName?: string; phone?: string; profileImageUrl?: string; profileImagePosition?: string; profileImageZoom?: number } | null): AccountForm {
  if (!session) {
    return { username: '', firstName: '', lastName: '', phone: '', profileImageUrl: '', profileImageMediaId: '', imageCrop: { x: 50, y: 50, zoom: 1 } }
  }

  return {
    username: session.username ?? userNameFromEmail(session.email),
    firstName: session.firstName ?? '',
    lastName: session.lastName ?? '',
    phone: session.phone ?? '',
    profileImageUrl: session.profileImageUrl ?? '',
    profileImageMediaId: '',
    imageCrop: parseImageCrop(session.profileImagePosition, session.profileImageZoom),
  }
}

function validateAccount(form: AccountForm) {
  if (!isValidUsername(form.username)) return 'Escolhe um nome de utilizador com 3 a 30 caracteres, em minúsculas, usando letras, números, ponto ou underscore.'
  if (form.firstName.trim().length < 2) return 'Indica o teu primeiro nome.'
  if (form.lastName.trim().length < 2) return 'Indica o teu último nome.'
  if (!isValidPhone(form.phone)) return 'Indica um contacto telefónico válido.'
  if (form.profileImageUrl.length > 2048) return 'O URL da foto é demasiado longo.'
  return null
}

function isValidUsername(value: string) {
  return /^(?!.*\.\.)(?!\.)(?!.*\.$)[a-z0-9._]{3,30}$/.test(value.trim())
}

function isValidPhone(value: string) {
  const trimmed = value.trim()
  const digitCount = trimmed.replace(/\D/g, '').length
  return /^\+?[0-9][0-9().\s-]{7,24}$/.test(trimmed) && digitCount >= 9 && digitCount <= 15
}

function displayName(firstName?: string, lastName?: string) {
  return [firstName, lastName].filter(Boolean).join(' ').trim()
}

function initials(firstName: string, lastName: string, email: string) {
  const source = displayName(firstName, lastName) || email
  return source
    .split(/[\s@._-]+/)
    .filter(Boolean)
    .map((part) => part[0])
    .join('')
    .slice(0, 2)
    .toUpperCase()
}

function userNameFromEmail(email: string) {
  const username = email
    .split('@')[0]
    ?.toLowerCase()
    .replace(/[^a-z0-9._]/g, '')
    .replace(/\.+/g, '.')
    .replace(/^\.|\.$/g, '')
    .slice(0, 30)

  return username && username.length >= 3 ? username : 'utilizador'
}
