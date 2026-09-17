import { useState } from 'react'
import styles from './LegalPage.module.css'

type LegalPageProps = {
  type: 'privacy' | 'terms' | 'cookies'
  onBack: () => void
}

const pages = {
  privacy: {
    eyebrow: 'Privacidade',
    title: 'Política de Privacidade',
    intro: 'Esta política explica como são tratados os dados pessoais usados nas contas, pedidos de agendamento, avaliações, contactos e chatbot.',
    sections: [
      ['Responsável pelo tratamento', 'Saltos nas Palhaçadas é responsável pelo tratamento dos dados recolhidos neste site. Para questões de privacidade, usa o contacto ola@saltosnaspalhacadas.pt.'],
      ['Dados tratados', 'Podem ser tratados nome, email, telefone, username, foto de perfil, favoritos, avaliações, mensagens de agendamento, local e data do evento e mensagens enviadas ao chatbot.'],
      ['Finalidades', 'Os dados são usados para gerir contas, responder a pedidos de eventos, administrar reservas, apresentar avaliações, prestar suporte e proteger a segurança da aplicação.'],
      ['Avaliações', 'Avaliações enviadas por clientes só aparecem publicamente depois de aprovação. O email do utilizador não é apresentado nas páginas públicas.'],
      ['Chatbot com IA', 'Quando a IA estiver ativa, algumas mensagens podem ser processadas por fornecedor externo para gerar resposta. Não devem ser enviados dados sensíveis, passwords ou dados bancários.'],
      ['Direitos', 'O utilizador pode pedir acesso, retificação, apagamento, limitação, portabilidade ou oposição através do contacto indicado.'],
      ['Retenção', 'Os dados são mantidos apenas pelo período necessário para cada finalidade, obrigações legais, segurança e resolução de litígios.'],
    ],
  },
  terms: {
    eyebrow: 'Condições',
    title: 'Termos de Utilização',
    intro: 'Estes termos definem as regras gerais de utilização do site, criação de conta, pedidos de agendamento e avaliações.',
    sections: [
      ['Utilização do site', 'O utilizador compromete-se a fornecer informação correta e a não usar o site para fins abusivos, ilegais ou que prejudiquem terceiros.'],
      ['Contas', 'Cada utilizador é responsável por manter a confidencialidade dos seus dados de acesso e por comunicar uso indevido da conta.'],
      ['Pedidos de agendamento', 'O pedido enviado pelo site não constitui contrato automático. O animador analisa o pedido e entra em contacto por email ou telefone.'],
      ['Avaliações', 'Ao submeter uma avaliação, o utilizador confirma que a informação é verdadeira e adequada para publicação caso seja aprovada.'],
      ['Moderação', 'O administrador pode aprovar, recusar ou remover avaliações ou informação que não respeite estas regras.'],
      ['Disponibilidade', 'A equipa procura manter o site operacional, mas podem existir interrupções técnicas, manutenção ou indisponibilidade de serviços externos.'],
    ],
  },
  cookies: {
    eyebrow: 'Cookies',
    title: 'Política de Cookies',
    intro: 'Esta página descreve o uso de cookies e tecnologias semelhantes neste site.',
    sections: [
      ['Cookies necessários', 'O site pode usar armazenamento técnico necessário para autenticação, segurança e funcionamento normal da aplicação.'],
      ['Cookies não essenciais', 'Neste momento não devem ser ativados cookies de marketing ou analytics sem consentimento prévio do utilizador.'],
      ['Gestão de consentimento', 'Se forem adicionadas ferramentas de analytics, publicidade ou tracking, deve existir uma interface para aceitar, rejeitar ou gerir preferências.'],
      ['Alterações futuras', 'Esta política deve ser atualizada sempre que forem adicionados novos fornecedores ou tecnologias de tracking.'],
    ],
  },
} as const

export function LegalPage({ onBack, type }: LegalPageProps) {
  const page = pages[type]
  const [cookieConsent, setCookieConsent] = useState(readCookieConsent)

  function saveCookieConsent(nextChoice: CookieConsentChoice) {
    try {
      localStorage.setItem(cookieConsentStorageKey, nextChoice)
      window.dispatchEvent(new CustomEvent('saltos:cookie-consent', { detail: { analytics: nextChoice === 'accepted' } }))
    } catch {
      // Preference still applies for the current page even if browser storage is blocked.
    }
    setCookieConsent(nextChoice)
  }

  return (
    <section className={styles.page}>
      <button className={styles.backButton} type="button" onClick={onBack}>Voltar ao site</button>
      <p className="eyebrow">{page.eyebrow}</p>
      <h1>{page.title}</h1>
      <p className={styles.intro}>{page.intro}</p>
      <p className={styles.updated}>Última atualização: 27 de agosto de 2026</p>

      {type === 'cookies' && (
        <div className={styles.cookiePanel}>
          <p>
            Preferência atual:{' '}
            <strong>{cookieConsent === 'accepted' ? 'opcionais aceites' : cookieConsent === 'rejected' ? 'opcionais rejeitados' : 'sem escolha guardada'}</strong>
          </p>
          <div className={styles.cookieActions}>
            <button type="button" onClick={() => saveCookieConsent('rejected')}>Rejeitar opcionais</button>
            <button type="button" onClick={() => saveCookieConsent('accepted')}>Aceitar opcionais</button>
          </div>
        </div>
      )}

      <div className={styles.sections}>
        {page.sections.map(([title, content]) => (
          <article key={title}>
            <h2>{title}</h2>
            <p>{content}</p>
          </article>
        ))}
      </div>
    </section>
  )
}

type CookieConsentChoice = 'accepted' | 'rejected'

const cookieConsentStorageKey = 'saltos.cookie-consent'

function readCookieConsent(): CookieConsentChoice | null {
  try {
    const value = localStorage.getItem(cookieConsentStorageKey)
    return value === 'accepted' || value === 'rejected' ? value : null
  } catch {
    return null
  }
}
