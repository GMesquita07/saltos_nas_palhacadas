import { useEffect, useRef, useState, type FormEvent } from 'react'
import { askSupportChat } from '../../services/supportChatService'
import type { SupportChatMessage } from '../../types/supportChat'
import styles from './SupportChat.module.css'

const defaultSuggestions = ['Pedir orçamento', 'Ver materiais', 'Contactar a equipa']
const moreSuggestions = ['Ver perfis', 'Publicar fotos do evento', 'Criar conta', 'Ver avaliações']

export function SupportChat() {
  const [isOpen, setIsOpen] = useState(false)
  const [isExpanded, setIsExpanded] = useState(false)
  const [input, setInput] = useState('')
  const [isSending, setIsSending] = useState(false)
  const [showMore, setShowMore] = useState(false)
  const [suggestions, setSuggestions] = useState(defaultSuggestions)
  const [messages, setMessages] = useState<SupportChatMessage[]>(() => [
    createMessage('assistant', 'Olá! Sou o assistente virtual dos Saltos nas Palhaçadas. Posso ajudar com agendamentos, orçamentos, materiais, perfis, contactos e partilhas.'),
    createMessage('assistant', 'Escolhe uma opção rápida ou escreve a tua pergunta.'),
  ])
  const threadRef = useRef<HTMLDivElement | null>(null)

  useEffect(() => {
    if (!isOpen) return
    threadRef.current?.scrollTo({ top: threadRef.current.scrollHeight, behavior: 'smooth' })
  }, [isOpen, messages])

  async function sendMessage(text: string) {
    const message = text.trim()
    if (!message || isSending) return

    setInput('')
    setIsSending(true)
    setShowMore(false)
    setMessages((current) => [...current, createMessage('user', message)])

    try {
      const reply = await askSupportChat(message)
      setMessages((current) => [...current, createMessage('assistant', reply.answer)])
      setSuggestions(reply.suggestions.length > 0 ? reply.suggestions : defaultSuggestions)
    } catch {
      setMessages((current) => [...current, createMessage('assistant', 'Não consegui responder agora. Confirma a ligação à API ou usa a página Contactos para falares diretamente com a equipa.')])
      setSuggestions(['Abrir contactos', 'Pedir orçamento', 'Ver materiais'])
    } finally {
      setIsSending(false)
    }
  }

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    void sendMessage(input)
  }

  return (
    <div className={styles.wrapper}>
      {isOpen ? (
        <section className={`${styles.panel} ${isExpanded ? styles.expanded : ''}`} aria-label="Assistente virtual">
          <header className={styles.header}>
            <div>
              <span className={styles.statusDot} aria-hidden="true" />
              <p>Assistente IA</p>
              <strong>Saltos nas Palhaçadas</strong>
            </div>
            <span className={styles.headerActions}>
              <button type="button" aria-label={isExpanded ? 'Reduzir assistente' : 'Expandir assistente'} onClick={() => setIsExpanded((current) => !current)}>
                <ExpandIcon />
              </button>
              <button type="button" aria-label="Fechar assistente" onClick={() => setIsOpen(false)}>
                <CloseIcon />
              </button>
            </span>
          </header>

          <div className={styles.thread} ref={threadRef}>
            {messages.map((message) => (
              <div className={`${styles.message} ${styles[message.role]}`} key={message.id}>
                {message.text}
              </div>
            ))}
            {isSending && <div className={`${styles.message} ${styles.assistant}`}>A preparar resposta...</div>}
          </div>

          <div className={styles.suggestions}>
            {[...suggestions, ...(showMore ? moreSuggestions : [])].map((suggestion) => (
              <button disabled={isSending} key={suggestion} type="button" onClick={() => { void sendMessage(suggestion) }}>
                {suggestion}
              </button>
            ))}
            <button className={styles.moreButton} type="button" onClick={() => setShowMore((current) => !current)}>
              Mais opções <ChevronIcon isOpen={showMore} />
            </button>
          </div>

          <form className={styles.form} onSubmit={submit}>
            <p className={styles.privacyNotice}>Não envies passwords, dados bancários ou informação sensível.</p>
            <input
              aria-label="Mensagem para o assistente"
              maxLength={700}
              onChange={(event) => setInput(event.target.value)}
              placeholder="Escreve aqui a tua mensagem..."
              value={input}
            />
            <button disabled={isSending || !input.trim()} type="submit" aria-label="Enviar mensagem">
              <SendIcon />
            </button>
          </form>
        </section>
      ) : (
        <button className={styles.launcher} type="button" aria-label="Abrir assistente virtual" onClick={() => setIsOpen(true)}>
          <ChatIcon />
        </button>
      )}
    </div>
  )
}

function createMessage(role: SupportChatMessage['role'], text: string): SupportChatMessage {
  return {
    id: typeof crypto !== 'undefined' && 'randomUUID' in crypto ? crypto.randomUUID() : String(Date.now() + Math.random()),
    role,
    text,
  }
}

function ChatIcon() {
  return (
    <svg aria-hidden="true" fill="none" focusable="false" stroke="currentColor" strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" viewBox="0 0 24 24">
      <path d="M5 18.5V20l2.4-1.3c.5.1 1 .2 1.6.2h6a5.5 5.5 0 0 0 0-11H9a5.5 5.5 0 0 0-4 9.3Z" />
      <path d="M8.5 12h7M8.5 15h4" />
    </svg>
  )
}

function SendIcon() {
  return (
    <svg aria-hidden="true" fill="none" focusable="false" stroke="currentColor" strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" viewBox="0 0 24 24">
      <path d="m4 11.5 15-7-4.5 15-3-6-7.5-2Z" />
      <path d="m11.5 13.5 7.5-9" />
    </svg>
  )
}

function CloseIcon() {
  return (
    <svg aria-hidden="true" fill="none" focusable="false" stroke="currentColor" strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" viewBox="0 0 24 24">
      <path d="M6 6l12 12M18 6 6 18" />
    </svg>
  )
}

function ExpandIcon() {
  return (
    <svg aria-hidden="true" fill="none" focusable="false" stroke="currentColor" strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" viewBox="0 0 24 24">
      <path d="M8 3H3v5M16 3h5v5M3 16v5h5M21 16v5h-5" />
    </svg>
  )
}

function ChevronIcon({ isOpen }: { isOpen: boolean }) {
  return (
    <svg aria-hidden="true" className={isOpen ? styles.chevronOpen : ''} fill="none" focusable="false" stroke="currentColor" strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" viewBox="0 0 24 24">
      <path d="m6 9 6 6 6-6" />
    </svg>
  )
}
