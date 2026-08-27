import styles from './LegalPage.module.css'

type LegalPageProps = {
  type: 'privacy' | 'terms' | 'cookies'
  onBack: () => void
}

const pages = {
  privacy: {
    eyebrow: 'Privacidade',
    title: 'Política de Privacidade',
    intro: 'Esta política explica como são tratados os dados pessoais usados nas contas, pedidos de agendamento, avaliações, partilhas de clientes, contactos e chatbot.',
    sections: [
      ['Responsável pelo tratamento', 'Saltos nas Palhaçadas é responsável pelo tratamento dos dados recolhidos neste site. Para questões de privacidade, usa o contacto ola@saltosnaspalhacadas.pt.'],
      ['Dados tratados', 'Podem ser tratados nome, email, telefone, username, foto de perfil, favoritos, avaliações, mensagens de agendamento, local e data do evento, fotografias, vídeos e mensagens enviadas ao chatbot.'],
      ['Finalidades', 'Os dados são usados para gerir contas, responder a pedidos de eventos, administrar reservas, publicar conteúdos aprovados, apresentar avaliações, prestar suporte e proteger a segurança da aplicação.'],
      ['Partilhas e avaliações', 'Conteúdos enviados por clientes só aparecem publicamente depois de aprovação. O email do utilizador não é apresentado nas páginas públicas.'],
      ['Chatbot com IA', 'Quando a IA estiver ativa, algumas mensagens podem ser processadas por fornecedor externo para gerar resposta. Não devem ser enviados dados sensíveis, passwords ou dados bancários.'],
      ['Direitos', 'O utilizador pode pedir acesso, retificação, apagamento, limitação, portabilidade ou oposição através do contacto indicado.'],
      ['Retenção', 'Os dados são mantidos apenas pelo período necessário para cada finalidade, obrigações legais, segurança e resolução de litígios.'],
    ],
  },
  terms: {
    eyebrow: 'Condições',
    title: 'Termos de Utilização',
    intro: 'Estes termos definem as regras gerais de utilização do site, criação de conta, pedidos de agendamento e submissão de conteúdos.',
    sections: [
      ['Utilização do site', 'O utilizador compromete-se a fornecer informação correta e a não usar o site para fins abusivos, ilegais ou que prejudiquem terceiros.'],
      ['Contas', 'Cada utilizador é responsável por manter a confidencialidade dos seus dados de acesso e por comunicar uso indevido da conta.'],
      ['Pedidos de agendamento', 'O pedido enviado pelo site não constitui contrato automático. O animador analisa o pedido e entra em contacto por email ou telefone.'],
      ['Conteúdos de clientes', 'Ao submeter fotografias ou vídeos, o utilizador confirma que tem direito a partilhar esse conteúdo e autoriza a sua publicação caso seja aprovado.'],
      ['Moderação', 'O administrador pode aprovar, recusar ou remover conteúdos, avaliações ou informação que não respeite estas regras.'],
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

  return (
    <section className={styles.page}>
      <button className={styles.backButton} type="button" onClick={onBack}>Voltar ao site</button>
      <p className="eyebrow">{page.eyebrow}</p>
      <h1>{page.title}</h1>
      <p className={styles.intro}>{page.intro}</p>
      <p className={styles.updated}>Última atualização: 27 de agosto de 2026</p>

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
