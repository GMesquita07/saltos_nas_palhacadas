# Registo de Tratamentos RGPD

Documento de trabalho para inventariar tratamentos de dados pessoais. Deve ser revisto por profissional jurídico antes de produção pública com domínio final.

Last verified: 2026-09-15.

## Responsável

| Campo | Valor |
| --- | --- |
| Responsável pelo tratamento | Saltos nas Palhaçadas |
| Contacto público | `ola@saltosnaspalhacadas.pt` |
| Jurisdição principal | Portugal |
| Autoridade de controlo | CNPD |

## Tratamentos

| Tratamento | Dados | Finalidade | Base legal a validar | Retenção a validar |
| --- | --- | --- | --- | --- |
| Conta de cliente | Email, username, nome, telefone, avatar, password hash | Criar e gerir conta | Contrato / diligências pré-contratuais | Enquanto conta ativa e períodos legais aplicáveis |
| Favoritos | IDs de itens favoritos | Guardar preferências | Contrato / interesse legítimo | Até remoção ou eliminação de conta |
| Pedidos de booking | Nome, email, telefone, local, data, horas, evento, descrição, notas | Analisar pedido e gerir evento | Diligências pré-contratuais / contrato | Definir por estado/obrigações |
| Reviews | Nome público, comentário, rating, user ID, moderação | Publicar feedback e moderar abuso | Consentimento / interesse legítimo | Até remoção/pedido aplicável |
| Partilhas de clientes | Fotos, vídeos, local, data, legenda, user ID, consentimento | Publicar conteúdo aprovado | Consentimento/licença específica | Até remoção ou retirada de consentimento |
| Avatar/media privada | Ficheiros e metadata | Personalização de conta e fluxo privado | Contrato / consentimento | Até substituição, cleanup ou eliminação |
| Contactos e materiais | Conteúdo gerido por admin | Informação pública do serviço | Interesse legítimo | Enquanto atual |
| Chat suporte | Mensagens e IP técnico em logs | Suporte e segurança | Interesse legítimo / consentimento se IA ativa | Curta, conforme provider |
| Emails operacionais | Email, nome e dados mínimos do evento | Notificações, reset password, bookings | Contrato / interesse legítimo | Conforme necessidade operacional/legal |
| Logs de segurança | IP, user agent, eventos técnicos | Segurança e diagnóstico | Interesse legítimo | Curta e proporcional |

## Implementação Técnica Existente

- Exportação de dados da conta em `AccountLifecycleService.exportFor`.
- Eliminação/anomização de conta em `AccountLifecycleService.deleteAccount`.
- Eliminação de favoritos, reviews e partilhas associadas na eliminação de conta.
- Anonimização de bookings para preservar histórico operacional sem manter identidade direta do cliente.
- Media gerida do utilizador apagada via `ClientContentMediaService`/`MediaStorage`.
- Passwords com BCrypt; reset tokens com hash SHA-256.
- Reviews e partilhas passam por moderação.

## Fornecedores

| Fornecedor | Finalidade | Estado |
| --- | --- | --- |
| Google Cloud Run | Alojamento backend | Ativo |
| Google Secret Manager | Gestão de secrets | Ativo |
| Google Cloud Scheduler | Jobs agendados | Ativo para cleanup |
| Neon | PostgreSQL gerido | Ativo |
| Cloudflare Pages | Frontend estático | Ativo |
| Cloudflare R2 | Storage media | Ativo |
| Cloudflare DNS/CDN/TLS | Domínio, TLS, CDN | Domínio em progresso |
| Cloudflare Turnstile | Anti-bot | Ativo |
| Brevo | Email transacional | Em progresso |
| OpenAI | Fallback IA do chat | Opcional, só se ativado |

## Direitos dos Titulares

Processos suportados tecnicamente ou a formalizar:

- Acesso/exportação: implementado para conta autenticada.
- Retificação: edição de dados de conta implementada.
- Apagamento: eliminação/anomização implementada para cliente; admin requer processo manual.
- Portabilidade: export JSON técnico disponível.
- Retirada de consentimento de conteúdos publicados: processo operacional a definir.
- Oposição/limitação: processo operacional a definir.

## Tarefas Antes do Lançamento Final

- Revisão jurídica de privacy, terms e cookies.
- Definir retenção por categoria de dados.
- Formalizar processo para pedidos RGPD por email.
- Confirmar DPAs/subprocessors dos providers.
- Confirmar transferências internacionais quando OpenAI/Brevo estiverem ativos.
- Confirmar política de logs e retenção.
- Confirmar backups e restore drill.
