import { useEffect, useState } from 'react'
import { getContacts } from '../../services/contactService'
import type { Contact } from '../../types/contact'
import styles from './ContactPage.module.css'

export function ContactPage() {
  const [contacts, setContacts] = useState<Contact[]>([])
  const [hasError, setHasError] = useState(false)

  useEffect(() => {
    let isCurrent = true

    const loadContacts = () => {
      void getContacts()
        .then((items) => {
          if (!isCurrent) return
          setContacts(items)
          setHasError(false)
        })
        .catch(() => {
          if (isCurrent) setHasError(true)
        })
    }

    loadContacts()
    window.addEventListener('contacts:changed', loadContacts)
    return () => {
      isCurrent = false
      window.removeEventListener('contacts:changed', loadContacts)
    }
  }, [])

  return <section className={styles.page}><p className="eyebrow">Estamos disponíveis</p><h1>Contacto e suporte</h1><p className={styles.intro}>Encontra aqui os canais oficiais para pedidos, informações e suporte.</p>{hasError ? <p className={styles.feedback}>Não foi possível carregar os contactos.</p> : contacts.length === 0 ? <p className={styles.feedback}>Os contactos serão disponibilizados em breve.</p> : <div className={styles.list}>{contacts.map((contact) => <a className={styles.contact} key={contact.id} href={contactHref(contact)} target={isExternal(contact) ? '_blank' : undefined} rel={isExternal(contact) ? 'noreferrer' : undefined}><small>{contactTypeLabel(contact.type)}</small><strong>{contact.label}</strong><span>{contact.value}</span><b>→</b></a>)}</div>}</section>
}

function contactHref(contact: Contact) {
  if (contact.type === 'EMAIL') return `mailto:${contact.value}`
  if (contact.type === 'PHONE') return `tel:${contact.value.replace(/\s/g, '')}`
  if (contact.type === 'WHATSAPP') return `https://wa.me/${contact.value.replace(/\D/g, '')}`
  if (contact.type === 'INSTAGRAM') {
    const handle = contact.value.replace(/^(?:https?:\/\/)?(?:www\.)?instagram\.com\//, '').replace(/^@/, '').replace(/\/$/, '')
    return `https://instagram.com/${handle}`
  }
  return contact.value.startsWith('http') ? contact.value : `https://${contact.value}`
}

function contactTypeLabel(type: Contact['type']) { return { EMAIL: 'Email', PHONE: 'Telefone', WHATSAPP: 'WhatsApp', INSTAGRAM: 'Instagram', WEBSITE: 'Website' }[type] }
function isExternal(contact: Contact) { return !['EMAIL', 'PHONE'].includes(contact.type) }
