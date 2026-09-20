# Roadmap

Last verified: 2026-09-20.

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
| E2E/smoke final amplo | DONE | Smoke manual final de produção passou |
| CI/PostgreSQL CI/CodeQL final | DONE | Passou na release final `main` |
| PR `feat/production-launch` -> `dev` | DONE | Integrado |
| PR `dev` -> `main` | DONE | Integrado |
| Mudar Pages production branch para `main` | DONE | Production deployment vem de `main` |
| Verificação final pós-merge | DONE | Domínio, backend health, Turnstile, schedulers e R2 backups validados |
| Confirmação administrativa .PT | PENDING | Externo; separado de DNS/TLS já funcionais |

## P1 - Depois do Lançamento

| Item | Estado | Nota |
| --- | --- | --- |
| Google Search Console | POST-LAUNCH | Submeter/verificar |
| Submeter sitemap | POST-LAUNCH | Rever após rotas públicas por perfil |
| Confirmar indexação | POST-LAUNCH | Especialmente marca e artistas |
| SEO por animador | POST-LAUNCH | Rotas reais existem; falta avaliar sitemap, conteúdo e eventual pré-render |
| URLs reais por perfil | DONE | `/perfis/:slug` |
| Inbound email `ola@` | TODO | Decidir provider ou Cloudflare Email Routing |
| Monitorizar budgets/logs | TODO | GCP, Cloudflare, Neon, R2 |
| Admin content management V2 | DONE | Portfolio admin vê publicados/ocultos, materiais editáveis, contactos ocultáveis e painel redesenhado |
| Feature 4: notificações email admin/artista | IN PROGRESS | Branch `feat/notifications-and-email`; ainda não production-complete |
| Otimização de media/assets estáticos | POST-LAUNCH | WebP/AVIF, tamanhos responsivos e cache |
| Cleanup de media pública órfã | POST-LAUNCH | Requer tracking seguro de ownership/referências antes de apagar objetos R2 |
| Acessibilidade | POST-LAUNCH | Auditoria teclado/leitor de ecrã |
| Performance | POST-LAUNCH | Lighthouse/WebPageTest |
| Arquitetura híbrida/static content | PLANNED | Ver [decisões](DECISIONS.md) |
| Avatares predefinidos | OPTIONAL | Reduzir uploads livres |

## Produto e UX Migrado do `todolist.md`

| Item antigo | Estado | Decisão |
| --- | --- | --- |
| Deploy online do site | DONE técnico | Domínio oficial live; Pages production deployment vem de `main` |
| SEO para DJ KidG / João Tomás | POST-LAUNCH | Rotas individuais existem; falta trabalho de sitemap/indexação e possível pré-render |
| Tradução da página | POST-LAUNCH | Não é bloqueador |
| Email "O animador" -> "A equipa irá analisar" | IN PROGRESS | Atualizado na branch `feat/notifications-and-email`; validar antes de promover |
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
- moderação
- estados operacionais

Nota: `frontend/src/data/profiles.ts` existe com dados hardcoded temporários, mas `App.tsx` atualmente carrega perfis pela API.
