# Registo de Tratamentos RGPD

Documento de trabalho para manter o inventário dos tratamentos de dados pessoais do projeto **Saltos nas Palhaçadas**. Deve ser validado com apoio jurídico antes de produção pública.

## Responsável

| Campo | Valor |
| --- | --- |
| Responsável pelo tratamento | Saltos nas Palhaçadas |
| Contacto | ola@saltosnaspalhacadas.pt |
| Jurisdição principal | Portugal |
| Autoridade de controlo | CNPD |

## Tratamentos

| Tratamento | Dados | Finalidade | Base legal a validar | Retenção a definir |
| --- | --- | --- | --- | --- |
| Conta de cliente | Email, username, nome, telefone, foto, password hash | Criar e gerir conta | Contrato / diligências pré-contratuais | Enquanto a conta estiver ativa |
| Favoritos | IDs de publicações favoritas | Guardar preferências do utilizador | Contrato / interesse legítimo | Até remoção pelo utilizador ou eliminação da conta |
| Pedidos de agendamento | Nome, email, telefone, local, data, horas, descrição, notas | Analisar pedido e contactar cliente | Diligências pré-contratuais / contrato | Definir por estado do pedido |
| Reviews | Nome público, comentário, rating, user ID, email visível ao admin | Mostrar feedback e moderar abuso | Consentimento / interesse legítimo | Até remoção ou pedido do titular |
| Partilhas de clientes | Fotos, vídeos, local, data, legenda, user ID | Publicar conteúdo aprovado de eventos | Consentimento/licença específica | Até remoção ou retirada de consentimento |
| Materiais e contactos | Dados introduzidos pelo admin | Informação pública do serviço | Interesse legítimo | Enquanto forem atuais |
| Chatbot | Mensagem escrita, IP técnico em logs do provider | Suporte automático ao visitante | Interesse legítimo / consentimento se IA ativa | Curta, conforme provider |
| Emails operacionais | Email, nome, dados do evento | Confirmar pedidos e lembretes | Contrato / interesse legítimo | Conforme histórico necessário |
| Logs de segurança | IP, user agent, eventos técnicos | Segurança, abuso e diagnóstico | Interesse legítimo | Retenção curta |

## Direitos dos Titulares

Processos a preparar:

- Acesso aos dados.
- Retificação.
- Apagamento.
- Limitação.
- Portabilidade.
- Oposição.
- Retirada de consentimento para conteúdos publicados.

## Fornecedores

| Fornecedor | Finalidade | Estado |
| --- | --- | --- |
| Provider da API | Alojamento backend | A definir |
| Provider PostgreSQL | Base de dados | A definir |
| Cloudflare | DNS, CDN, WAF, Pages ou R2 | A definir |
| SMTP | Emails transacionais | A definir |
| OpenAI | Fallback IA do chatbot | Opcional |

## Tarefas Antes de Produção

- Validar política de privacidade final.
- Definir retenção por tipo de dado.
- Definir processo de eliminação/anomização.
- Definir processo de exportação de dados.
- Confirmar DPAs/subprocessors dos fornecedores.
- Confirmar transferências internacionais quando aplicável.
