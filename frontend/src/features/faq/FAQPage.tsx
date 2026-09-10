import styles from './FAQPage.module.css'

type FAQPageProps = {
  onBack: () => void
}

const faqs = [
  ['Como peço orçamento?', 'Entra em Agendar, escolhe o artista e a data, e envia os detalhes do evento. A equipa analisa o pedido e responde por email ou telefone.'],
  ['Preciso de conta para marcar?', 'Sim. A conta permite enviar pedidos, acompanhar estados, responder a contrapropostas e guardar favoritos.'],
  ['A data fica automaticamente reservada?', 'Não. O pedido fica em análise até ser confirmado pela administração. Quando é aceite, a data aparece como ocupada na disponibilidade.'],
  ['Posso alterar ou cancelar um pedido?', 'Pedidos pendentes ou com contraproposta podem ser cancelados na área de agendamento. Para alterações mais detalhadas, usa os contactos oficiais.'],
  ['As fotos dos clientes aparecem logo no site?', 'Não. Fotografias e vídeos enviados por clientes ficam privados até serem aprovados pela administração.'],
  ['Que dados ficam públicos nas partilhas?', 'O email nunca aparece publicamente. Local e mês do evento só aparecem se o cliente escolher mostrar esses dados.'],
  ['Como recupero a palavra-passe?', 'Na página de login, escolhe recuperar palavra-passe e confirma o link enviado para o teu email.'],
  ['Que materiais estão disponíveis?', 'A página Materiais mostra o equipamento publicado pela administração para apoiar os eventos.'],
] as const

export function FAQPage({ onBack }: FAQPageProps) {
  return (
    <section className={styles.page}>
      <button className={styles.backButton} type="button" onClick={onBack}>Voltar ao site</button>
      <p className="eyebrow">Ajuda</p>
      <h1>Perguntas frequentes</h1>
      <p className={styles.intro}>Respostas rápidas sobre pedidos, contas, disponibilidade e partilhas de clientes.</p>

      <div className={styles.items}>
        {faqs.map(([question, answer]) => (
          <article key={question}>
            <h2>{question}</h2>
            <p>{answer}</p>
          </article>
        ))}
      </div>
    </section>
  )
}
