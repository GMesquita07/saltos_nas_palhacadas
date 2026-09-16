# Roadmap

Last verified: 2026-09-16.

Estados usados: DONE, IN PROGRESS, BLOCKED, TODO, POST-LAUNCH, OPTIONAL.

## P0 - Bloqueadores do Lançamento

| Item | Estado | Nota |
| --- | --- | --- |
| `www` live com HTTPS | DONE | `https://www.saltosnaspalhacadas.pt` validado |
| Redirect apex -> `www` | DONE | HTTP 301 preserva query strings |
| Brevo DNS verification | DONE | Domínio autenticado em Brevo |
| DKIM/DMARC final | DONE | Validados |
| SMTP real | DONE | Brevo SMTP 587 STARTTLS ativo |
| Teste email recuperação password | DONE | Forgot/reset password validado em produção |
| Teste emails bookings | DONE | Recebido, aceite e cancelamento validados |
| Scheduler booking reminders | DONE | Job diário 09:00 Europe/Lisbon executado com sucesso |
| Neon restore drill | DONE | Snapshot `pre-launch-2026-09-16` restaurado em branch isolada |
| R2 backup e restore drill | DONE | Backup Scheduler, Cloud Run Job, rclone check e restore PNG validados |
| Artifact Registry cleanup policy | DONE | Ativa: delete >30 dias, keep pelo menos 5 versões |
| E2E/smoke final amplo | TODO | Validar fluxos principais antes do PR final |
| Mobile QA | TODO | Validar em dispositivos reais |
| Links QA | TODO | Navegação, footer, links externos |
| Legal/privacy/cookie/contact-content QA | TODO | Revisão de conteúdo e obrigações aplicáveis |
| Rever consistência final da documentação | TODO | Antes do PR final |
| PR `feat/production-launch` -> `dev` | TODO | Depois dos QAs finais |
| CI/CodeQL final | TODO | No PR |
| PR `dev` -> `main` | TODO | Release final |
| Mudar Pages production branch para `main` | TODO | Depois do merge final; atualmente ainda é `feat/production-launch` |
| Verificação final pós-merge | TODO | Confirmar produção depois do switch para `main` |
| Confirmação administrativa .PT | PENDING | Externo; separado de DNS/TLS já funcionais |

## P1 - Depois do Lançamento

| Item | Estado | Nota |
| --- | --- | --- |
| Google Search Console | POST-LAUNCH | Após release final |
| Submeter sitemap | POST-LAUNCH | Após release final; atualmente só homepage |
| Confirmar indexação | POST-LAUNCH | Especialmente marca e artistas |
| SEO por animador | POST-LAUNCH | Exige rotas reais por perfil |
| URLs reais por perfil | POST-LAUNCH | Ex.: `/animadores/kidg` |
| Inbound email `ola@` | TODO | Decidir provider ou Cloudflare Email Routing |
| Monitorizar budgets/logs | TODO | GCP, Cloudflare, Neon, R2 |
| Otimização de imagens/assets | POST-LAUNCH | WebP/AVIF, tamanhos responsivos |
| Acessibilidade | POST-LAUNCH | Auditoria teclado/leitor de ecrã |
| Performance | POST-LAUNCH | Lighthouse/WebPageTest |
| Arquitetura híbrida/static content | PLANNED | Ver [decisões](DECISIONS.md) |
| Avatares predefinidos | OPTIONAL | Reduzir uploads livres |

## Produto e UX Migrado do `todolist.md`

| Item antigo | Estado | Decisão |
| --- | --- | --- |
| Deploy online do site | PARTIAL/DONE técnico | Domínio oficial já está live; release final ainda depende de QA, PRs e Pages em `main` |
| SEO para DJ KidG / João Tomás | STILL TODO | Limitado pela SPA sem rotas públicas individuais |
| Tradução da página | POST-LAUNCH | Não é bloqueador |
| Email "O animador" -> "A equipa irá analisar" | STILL TODO | Rever wording/conteúdo apesar do SMTP real já estar ativo |
| Corrigir página de partilhas de clientes | STILL TODO | UX/layout a melhorar |
| Partilhas: fotos primeiro e upload melhor posicionado | STILL TODO | Produto/UX |
| Área pessoal do cliente como botões | STILL TODO | Produto/UX |
| Zona de notificações in-site | STILL TODO | Nova funcionalidade |
| Formatação Privacy/Terms/Cookies | STILL TODO | Melhorar UX e pedir revisão jurídica |

## Arquitetura Híbrida Planeada

Objetivo futuro: reduzir uso desnecessário de Neon, Cloud Run e R2.

Candidatos a estático/hardcoded:

- perfis públicos dos animadores
- fotografias principais dos animadores
- textos institucionais
- FAQ
- informação raramente alterada
- materiais/contactos se a edição admin deixar de ser necessária

Manter dinâmico:

- contas
- auth
- bookings
- favoritos
- reviews
- partilhas de clientes
- moderação
- estados operacionais

Nota: `frontend/src/data/profiles.ts` existe com dados hardcoded temporários, mas `App.tsx` atualmente carrega perfis pela API.
