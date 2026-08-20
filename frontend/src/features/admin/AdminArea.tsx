import { useEffect, useState, type ChangeEvent } from 'react'
import { apiClient, uploadFile } from '../../services/apiClient'
import { getContacts } from '../../services/contactService'
import { getProfiles } from '../../services/profileService'
import { getPortfolioItems } from '../../services/portfolioService'
import type { Contact, ContactType } from '../../types/contact'
import type { PortfolioItem } from '../../types/portfolio'
import type { Profile } from '../../types/profile'
import styles from './AdminArea.module.css'

type TokenResponse = { accessToken: string; role: 'ADMIN' | 'CUSTOMER' }
type Notice = { type: 'success' | 'error'; text: string }
type AdminPage = 'profile' | 'content' | 'contacts'
const storageKey = 'saltos.admin-token'

export function AdminArea({ onExit }: { onExit: () => void }) {
  const [token, setToken] = useState(() => sessionStorage.getItem(storageKey))
  const [notice, setNotice] = useState<Notice | null>(null)
  const [page, setPage] = useState<AdminPage>('profile')
  const [profiles, setProfiles] = useState<Profile[]>([])
  const [contacts, setContacts] = useState<Contact[]>([])
  const [profileImageUrl, setProfileImageUrl] = useState('')
  const [imagePositionX, setImagePositionX] = useState(50)
  const [imagePositionY, setImagePositionY] = useState(50)
  const [mediaUrl, setMediaUrl] = useState('')
  const [contentProfileSlug, setContentProfileSlug] = useState('')
  const [contentItems, setContentItems] = useState<PortfolioItem[]>([])
  const refreshProfiles = () => getProfiles().then(setProfiles).catch(() => setNotice({ type: 'error', text: 'Não foi possível carregar os perfis.' }))
  const refreshContacts = () => getContacts().then(setContacts).catch(() => setNotice({ type: 'error', text: 'Não foi possível carregar os contactos.' }))
  const refreshContentItems = (slug = contentProfileSlug) => {
    if (!slug) { setContentItems([]); return Promise.resolve() }
    return getPortfolioItems(slug).then(setContentItems).catch(() => setNotice({ type: 'error', text: 'Não foi possível carregar os conteúdos.' }))
  }

  useEffect(() => { if (token) { refreshProfiles(); refreshContacts() } }, [token])

  async function login(form: HTMLFormElement) {
    const values = new FormData(form)
    try {
      const result = await apiClient<TokenResponse>('/auth/login', { method: 'POST', body: JSON.stringify({ email: values.get('email'), password: values.get('password') }) })
      if (result.role !== 'ADMIN') throw new Error('Esta conta não tem permissões de administração.')
      sessionStorage.setItem(storageKey, result.accessToken); setToken(result.accessToken); setNotice(null)
    } catch (error) { setNotice({ type: 'error', text: error instanceof Error ? error.message : 'Erro no login.' }) }
  }

  async function upload(event: ChangeEvent<HTMLInputElement>, setUrl: (url: string) => void) {
    const file = event.target.files?.[0]
    if (!file || !token) return
    try { const result = await uploadFile(file, token); setUrl(result.url); setNotice({ type: 'success', text: 'Ficheiro enviado com sucesso.' }) }
    catch (error) { setNotice({ type: 'error', text: error instanceof Error ? error.message : 'Erro no envio do ficheiro.' }) }
  }

  async function createProfile(form: HTMLFormElement) {
    const values = new FormData(form)
    try {
      await apiClient('/admin/profiles', { method: 'POST', body: JSON.stringify({ slug: values.get('slug'), name: values.get('name'), role: values.get('role'), description: values.get('description'), profileImageUrl: profileImageUrl || null, profileImagePosition: `${imagePositionX}% ${imagePositionY}%` }) }, token!)
      form.reset(); setProfileImageUrl(''); setImagePositionX(50); setImagePositionY(50); await refreshProfiles(); window.dispatchEvent(new Event('profiles:changed')); setNotice({ type: 'success', text: 'Perfil criado com sucesso.' })
    } catch (error) { setNotice({ type: 'error', text: error instanceof Error ? error.message : 'Erro ao criar perfil.' }) }
  }

  async function deleteProfile(slug: string) {
    if (!window.confirm('Apagar este perfil e os respetivos conteúdos?')) return
    try { await apiClient(`/admin/profiles/${slug}`, { method: 'DELETE' }, token!); await refreshProfiles(); window.dispatchEvent(new Event('profiles:changed')); setNotice({ type: 'success', text: 'Perfil apagado com sucesso.' }) }
    catch (error) { setNotice({ type: 'error', text: error instanceof Error ? error.message : 'Erro ao apagar perfil.' }) }
  }

  async function createContent(form: HTMLFormElement) {
    const values = new FormData(form)
    try {
      await apiClient(`/admin/profiles/${values.get('profileSlug')}/portfolio`, { method: 'POST', body: JSON.stringify({ type: values.get('type'), title: values.get('title'), location: values.get('location'), eventDate: values.get('eventDate'), mediaUrl: mediaUrl || values.get('mediaUrl'), thumbnailUrl: values.get('thumbnailUrl') || null, displayOrder: Number(values.get('displayOrder') || 0), published: true }) }, token!)
      form.reset(); setMediaUrl(''); await refreshContentItems(String(values.get('profileSlug'))); setNotice({ type: 'success', text: 'Conteúdo publicado com sucesso.' })
    } catch (error) { setNotice({ type: 'error', text: error instanceof Error ? error.message : 'Erro ao publicar conteúdo.' }) }
  }

  async function createContact(form: HTMLFormElement) {
    const values = new FormData(form)
    try {
      await apiClient('/admin/contacts', { method: 'POST', body: JSON.stringify({ label: values.get('label'), type: values.get('type'), value: values.get('value'), displayOrder: Number(values.get('displayOrder') || 0) }) }, token!)
      form.reset(); await refreshContacts(); window.dispatchEvent(new Event('contacts:changed')); setNotice({ type: 'success', text: 'Contacto adicionado com sucesso.' })
    } catch (error) { setNotice({ type: 'error', text: error instanceof Error ? error.message : 'Erro ao adicionar contacto.' }) }
  }

  async function deleteContact(id: number) {
    if (!window.confirm('Apagar este contacto?')) return
    try { await apiClient(`/admin/contacts/${id}`, { method: 'DELETE' }, token!); await refreshContacts(); window.dispatchEvent(new Event('contacts:changed')); setNotice({ type: 'success', text: 'Contacto apagado com sucesso.' }) }
    catch (error) { setNotice({ type: 'error', text: error instanceof Error ? error.message : 'Erro ao apagar contacto.' }) }
  }

  async function deleteContent(id: string) {
    if (!contentProfileSlug || !window.confirm('Apagar este conteúdo publicado?')) return
    try { await apiClient(`/admin/profiles/${contentProfileSlug}/portfolio/${id}`, { method: 'DELETE' }, token!); await refreshContentItems(); setNotice({ type: 'success', text: 'Conteúdo apagado com sucesso.' }) }
    catch (error) { setNotice({ type: 'error', text: error instanceof Error ? error.message : 'Erro ao apagar conteúdo.' }) }
  }

  if (!token) return <section className={styles.login}><button onClick={onExit} type="button">← Voltar ao site</button><div><p className="eyebrow">Área reservada</p><h1>Administração</h1><p>Inicia sessão para gerir os perfis e conteúdos.</p><form onSubmit={(event) => { event.preventDefault(); login(event.currentTarget) }}><label>Email<input name="email" type="email" required /></label><label>Palavra-passe<input name="password" type="password" required /></label>{notice && <p className={styles[notice.type]}>{notice.text}</p>}<button type="submit">Entrar</button></form></div></section>

  return <section className={styles.dashboard}><div className={styles.top}><div><p className="eyebrow">Área reservada</p><h1>Painel de administração</h1>{notice && <p className={`${styles.notice} ${styles[notice.type]}`} role="status">{notice.text}</p>}</div><button type="button" onClick={() => { sessionStorage.removeItem(storageKey); setToken(null); setNotice(null) }}>Terminar sessão</button></div><nav className={styles.tabs} aria-label="Secções de administração"><button className={page === 'profile' ? styles.activeTab : ''} type="button" onClick={() => setPage('profile')}>Novo perfil</button><button className={page === 'content' ? styles.activeTab : ''} type="button" onClick={() => setPage('content')}>Publicar conteúdo</button><button className={page === 'contacts' ? styles.activeTab : ''} type="button" onClick={() => setPage('contacts')}>Contactos</button></nav>{page === 'profile' && <ProfileManagement profiles={profiles} profileImageUrl={profileImageUrl} setProfileImageUrl={setProfileImageUrl} imagePositionX={imagePositionX} imagePositionY={imagePositionY} setImagePositionX={setImagePositionX} setImagePositionY={setImagePositionY} createProfile={createProfile} deleteProfile={deleteProfile} upload={upload} />}{page === 'content' && <ContentPublishing profiles={profiles} mediaUrl={mediaUrl} setMediaUrl={setMediaUrl} contentProfileSlug={contentProfileSlug} setContentProfileSlug={(slug) => { setContentProfileSlug(slug); refreshContentItems(slug) }} contentItems={contentItems} createContent={createContent} deleteContent={deleteContent} upload={upload} />}{page === 'contacts' && <ContactManagement contacts={contacts} createContact={createContact} deleteContact={deleteContact} />}</section>
}

function ProfileManagement({ profiles, profileImageUrl, setProfileImageUrl, imagePositionX, imagePositionY, setImagePositionX, setImagePositionY, createProfile, deleteProfile, upload }: { profiles: Profile[]; profileImageUrl: string; setProfileImageUrl: (url: string) => void; imagePositionX: number; imagePositionY: number; setImagePositionX: (value: number) => void; setImagePositionY: (value: number) => void; createProfile: (form: HTMLFormElement) => Promise<void>; deleteProfile: (slug: string) => Promise<void>; upload: (event: ChangeEvent<HTMLInputElement>, setUrl: (url: string) => void) => Promise<void> }) { return <div className={styles.page}><form onSubmit={(event) => { event.preventDefault(); createProfile(event.currentTarget) }}><h2>Novo perfil</h2><p className={styles.intro}>Cria o cartão apresentado na página pública de perfis.</p><label>Nome<input name="name" required /></label><label>Slug<input name="slug" placeholder="dj-joao-tomas" pattern="[a-z0-9]+(?:-[a-z0-9]+)*" required /></label><label>Função<input name="role" placeholder="DJ & Animador" required /></label><label>Descrição<textarea name="description" required /></label><label>Enviar imagem<input type="file" accept="image/*" onChange={(event) => upload(event, setProfileImageUrl)} /></label><label>URL da imagem<input name="profileImageUrl" type="url" value={profileImageUrl} onChange={(event) => setProfileImageUrl(event.target.value)} /></label>{profileImageUrl && <div className={styles.cropEditor}><p>Enquadramento da foto</p><div className={styles.cropPreview}><img src={profileImageUrl} alt="Pré-visualização da foto de perfil" style={{ objectPosition: `${imagePositionX}% ${imagePositionY}%` }} /></div><label>Horizontal<input type="range" min="0" max="100" value={imagePositionX} onChange={(event) => setImagePositionX(Number(event.target.value))} /></label><label>Vertical<input type="range" min="0" max="100" value={imagePositionY} onChange={(event) => setImagePositionY(Number(event.target.value))} /></label></div>}<button type="submit">Criar perfil</button></form><section className={styles.manage}><h2>Perfis existentes</h2>{profiles.length === 0 ? <p>Não existem perfis.</p> : profiles.map((profile) => <div className={styles.profileRow} key={profile.id}><span><strong>{profile.name}</strong><small>{profile.slug}</small></span><button type="button" onClick={() => deleteProfile(profile.slug)}>Apagar</button></div>)}</section></div> }

function ContentPublishing({ profiles, mediaUrl, setMediaUrl, contentProfileSlug, setContentProfileSlug, contentItems, createContent, deleteContent, upload }: { profiles: Profile[]; mediaUrl: string; setMediaUrl: (url: string) => void; contentProfileSlug: string; setContentProfileSlug: (slug: string) => void; contentItems: PortfolioItem[]; createContent: (form: HTMLFormElement) => Promise<void>; deleteContent: (id: string) => Promise<void>; upload: (event: ChangeEvent<HTMLInputElement>, setUrl: (url: string) => void) => Promise<void> }) { return <div className={styles.page}><form onSubmit={(event) => { event.preventDefault(); createContent(event.currentTarget) }}><h2>Publicar conteúdo</h2><p className={styles.intro}>Associa uma fotografia ou vídeo a um perfil já criado.</p><label>Perfil<select name="profileSlug" value={contentProfileSlug} onChange={(event) => setContentProfileSlug(event.target.value)} required><option value="">Seleciona um perfil</option>{profiles.map((profile) => <option key={profile.id} value={profile.slug}>{profile.name}</option>)}</select></label><label>Tipo<select name="type"><option value="PHOTO">Foto</option><option value="VIDEO">Vídeo</option></select></label><label>Título<input name="title" required /></label><label>Local<input name="location" required /></label><label>Data<input name="eventDate" type="date" required /></label><label>Ordem de apresentação<input name="displayOrder" type="number" min="0" defaultValue="0" /></label><label>Enviar foto ou vídeo<input type="file" accept="image/*,video/*" onChange={(event) => upload(event, setMediaUrl)} /></label><label>URL do conteúdo<input name="mediaUrl" type="url" value={mediaUrl} onChange={(event) => setMediaUrl(event.target.value)} required /></label><label>URL da miniatura (opcional)<input name="thumbnailUrl" type="url" /></label><button type="submit">Publicar conteúdo</button></form><section className={styles.manage}><h2>Conteúdos publicados</h2>{!contentProfileSlug ? <p>Seleciona um perfil para gerir os seus conteúdos.</p> : contentItems.length === 0 ? <p>Este perfil ainda não tem conteúdos publicados.</p> : contentItems.map((item) => <div className={styles.profileRow} key={item.id}><span><strong>{item.title}</strong><small>{item.type} · {item.eventDate}</small></span><button type="button" onClick={() => deleteContent(item.id)}>Apagar</button></div>)}</section></div> }

function ContactManagement({ contacts, createContact, deleteContact }: { contacts: Contact[]; createContact: (form: HTMLFormElement) => Promise<void>; deleteContact: (id: number) => Promise<void> }) { const [type, setType] = useState<ContactType>('EMAIL'); const field = contactField(type); return <div className={styles.page}><form onSubmit={(event) => { event.preventDefault(); createContact(event.currentTarget) }}><h2>Novo contacto</h2><p className={styles.intro}>Os contactos ficam automaticamente visíveis no rodapé da página pública.</p><label>Nome a apresentar<input name="label" placeholder={field.label} required /></label><label>Tipo<select name="type" value={type} onChange={(event) => setType(event.target.value as ContactType)}>{(['EMAIL', 'PHONE', 'WHATSAPP', 'INSTAGRAM', 'WEBSITE'] as ContactType[]).map((option) => <option key={option} value={option}>{contactTypeLabel(option)}</option>)}</select></label><label>{field.label}<input name="value" type={field.inputType} placeholder={field.placeholder} required /></label><label>Ordem de apresentação<input name="displayOrder" type="number" min="0" defaultValue="0" /></label><button type="submit">Adicionar contacto</button></form><section className={styles.manage}><h2>Contactos publicados</h2>{contacts.length === 0 ? <p>Não existem contactos publicados.</p> : contacts.map((contact) => <div className={styles.profileRow} key={contact.id}><span><strong>{contact.label}</strong><small>{contact.value}</small></span><button type="button" onClick={() => deleteContact(contact.id)}>Apagar</button></div>)}</section></div> }

function contactTypeLabel(type: ContactType) { return { EMAIL: 'Email', PHONE: 'Telefone', WHATSAPP: 'WhatsApp', INSTAGRAM: 'Instagram', WEBSITE: 'Website' }[type] }
function contactField(type: ContactType) { return { EMAIL: { label: 'Endereço de email', placeholder: 'ola@exemplo.pt', inputType: 'email' }, PHONE: { label: 'Número de telefone', placeholder: '+351 912 345 678', inputType: 'tel' }, WHATSAPP: { label: 'Número de WhatsApp', placeholder: '+351 912 345 678', inputType: 'tel' }, INSTAGRAM: { label: 'Perfil de Instagram', placeholder: '@saltosnaspalhacadas', inputType: 'text' }, WEBSITE: { label: 'URL do website', placeholder: 'https://exemplo.pt', inputType: 'url' } }[type] }
