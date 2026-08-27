import { useEffect, useState } from 'react'
import { useAuthenticatedMediaUrl } from '../../../components/AuthenticatedMedia'
import { deleteClientContent, getAdminClientContent, moderateClientContent } from '../../../services/clientContentService'
import type { ClientContentPost, ClientContentStatus } from '../../../types/clientContent'
import styles from './ClientContentModeration.module.css'

type ClientContentModerationProps = {
  token: string
  onNotice: (notice: { type: 'success' | 'error'; text: string }) => void
}

const statusLabels: Record<ClientContentStatus, string> = {
  PENDING: 'Pendente',
  APPROVED: 'Aprovada',
  REJECTED: 'Recusada',
}

export function ClientContentModeration({ token, onNotice }: ClientContentModerationProps) {
  const [posts, setPosts] = useState<ClientContentPost[]>([])
  const [messages, setMessages] = useState<Record<string, string>>({})
  const [isLoading, setIsLoading] = useState(true)
  const [savingId, setSavingId] = useState<string | null>(null)

  useEffect(() => {
    let isCurrent = true

    void getAdminClientContent(token)
      .then((result) => {
        if (!isCurrent) return
        setPosts(result)
        setMessages(toMessageDrafts(result))
      })
      .catch(() => {
        if (isCurrent) onNotice({ type: 'error', text: 'Não foi possível carregar as publicações dos clientes.' })
      })
      .finally(() => {
        if (isCurrent) setIsLoading(false)
      })

    return () => {
      isCurrent = false
    }
  }, [onNotice, token])

  async function moderate(post: ClientContentPost, status: ClientContentStatus) {
    setSavingId(post.id)
    try {
      const updated = await moderateClientContent(post.id, {
        status,
        adminMessage: messages[post.id]?.trim() || undefined,
      }, token)
      setPosts((current) => current.map((item) => item.id === updated.id ? updated : item))
      window.dispatchEvent(new Event('client-content:changed'))
      onNotice({ type: 'success', text: status === 'APPROVED' ? 'Publicação aprovada.' : status === 'REJECTED' ? 'Publicação recusada.' : 'Publicação reposta como pendente.' })
    } catch (error) {
      onNotice({ type: 'error', text: error instanceof Error ? error.message : 'Não foi possível moderar a publicação.' })
    } finally {
      setSavingId(null)
    }
  }

  async function reject(post: ClientContentPost) {
    await remove(post, {
      confirmMessage: 'Recusar esta publicação? Ela será apagada imediatamente.',
      successMessage: 'Publicação recusada e apagada.',
    })
  }

  async function remove(
    post: ClientContentPost,
    options = {
      confirmMessage: 'Apagar esta publicação de cliente?',
      successMessage: 'Publicação apagada.',
    },
  ) {
    if (!window.confirm(options.confirmMessage)) return

    setSavingId(post.id)
    try {
      await deleteClientContent(post.id, token)
      setPosts((current) => current.filter((item) => item.id !== post.id))
      window.dispatchEvent(new Event('client-content:changed'))
      onNotice({ type: 'success', text: options.successMessage })
    } catch (error) {
      onNotice({ type: 'error', text: error instanceof Error ? error.message : 'Não foi possível apagar a publicação.' })
    } finally {
      setSavingId(null)
    }
  }

  return (
    <section className={styles.page}>
      <div className={styles.heading}>
        <p className="eyebrow">Moderação</p>
        <h2>Publicações dos clientes</h2>
        <p>Conteúdos submetidos pelos clientes. Só as publicações aprovadas aparecem no site.</p>
      </div>

      {isLoading ? (
        <p className={styles.feedback}>A carregar publicações...</p>
      ) : posts.length === 0 ? (
        <p className={styles.feedback}>Ainda não existem publicações de clientes para moderar.</p>
      ) : (
        <div className={styles.list}>
          {posts.map((post) => (
            <article className={styles.row} key={post.id}>
              <div className={styles.preview}>
                <ModerationMediaPreview post={post} token={token} />
                <span>{post.type}</span>
              </div>

              <div className={styles.details}>
                <span className={`${styles.statusBadge} ${styles[post.status.toLowerCase()]}`}>{statusLabels[post.status]}</span>
                <h3>{post.title}</h3>
                <p>{post.profileName ?? 'Artista'} · {post.location} · {post.eventDate}</p>
                <p>Submetido por {post.submittedByName}{post.submittedByEmail ? ' · ' + post.submittedByEmail : ''}</p>
                {post.caption && <p className={styles.caption}>{post.caption}</p>}
              </div>

              {post.status === 'PENDING' ? (
                <div className={styles.actions}>
                  <label>
                    Mensagem do admin
                    <textarea
                      maxLength={600}
                      onChange={(event) => setMessages((current) => ({ ...current, [post.id]: event.target.value }))}
                      placeholder="Opcional"
                      value={messages[post.id] ?? ''}
                    />
                  </label>
                  <div>
                    <button disabled={savingId === post.id} type="button" onClick={() => { void moderate(post, 'APPROVED') }}>Aprovar</button>
                    <button disabled={savingId === post.id} type="button" onClick={() => { void reject(post) }}>Recusar</button>
                  </div>
                </div>
              ) : (
                <div className={`${styles.actions} ${styles.singleAction}`}>
                  <button disabled={savingId === post.id} type="button" onClick={() => { void remove(post) }}>Apagar</button>
                </div>
              )}
            </article>
          ))}
        </div>
      )}
    </section>
  )
}

function ModerationMediaPreview({ post, token }: { post: ClientContentPost; token: string }) {
  const mediaUrl = useAuthenticatedMediaUrl(post.mediaUrl, token)
  const thumbnailUrl = useAuthenticatedMediaUrl(post.thumbnailUrl, token)

  if (!mediaUrl) {
    return <p className={styles.previewFallback}>Ficheiro privado</p>
  }

  return post.mediaType === 'VIDEO'
    ? <video controls preload="metadata" poster={thumbnailUrl}><source src={mediaUrl} /></video>
    : <img src={mediaUrl} alt={post.title} />
}

function toMessageDrafts(posts: ClientContentPost[]) {
  return posts.reduce<Record<string, string>>((drafts, post) => {
    drafts[post.id] = post.adminMessage ?? ''
    return drafts
  }, {})
}
