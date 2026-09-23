# Roadmap

Last verified: 2026-09-24.

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
| Google Search Console | DONE | Domain property `saltosnaspalhacadas.pt` verificada |
| Submeter sitemap | DONE | Sitemap enviado com sucesso; 11 URLs descobertas |
| Confirmar indexação | DONE | Páginas públicas/perfis confirmados como indexados no Google |
| Feature 5: SEO / Google | DONE | ProfilePage reconhecido pelo Search Console; favicon pode demorar a atualizar nos resultados |
| URLs reais por perfil | DONE | `/perfis/:slug` |
| Inbound email `ola@` | TODO | Decidir provider ou Cloudflare Email Routing |
| Monitorizar budgets/logs | TODO | GCP, Cloudflare, Neon, R2 |
| Admin content management V2 | DONE | Portfolio admin vê publicados/ocultos, materiais editáveis, contactos ocultáveis e painel redesenhado |
| Feature 4: notificações email admin/artista | DONE | PR #27 -> `dev`; PR #29 -> `main`; V21 em produção |
| Global UI Redesign | DONE | PR #44 -> `dev`; PR #45 -> `main`; houve fixes visuais posteriores |
| Favicon branding final | DONE | PR #53 -> `dev`; PR #54 -> `main` |
| Otimização de media/assets estáticos | POST-LAUNCH | Cache de `/assets/*` concluído na Feature 6; WebP/AVIF e imagens responsivas ficam para avaliação futura |
| Cleanup de media pública órfã | POST-LAUNCH | Requer tracking seguro de ownership/referências antes de apagar objetos R2 |
| Acessibilidade | POST-LAUNCH | Auditoria teclado/leitor de ecrã |
| Feature 6: Performance & Media | DONE | PR #55 -> `dev`; PR #56 -> `main`; produção frontend `a97c3c26deddbc7359a0c1b802eb80a22580be5c`; PageSpeed/Core Web Vitals reais ainda por medir |
| Arquitetura híbrida/static content | PLANNED | Ver [decisões](DECISIONS.md) |
| Avatares predefinidos | OPTIONAL | Reduzir uploads livres |

## Produto e UX Migrado do `todolist.md`

| Item antigo | Estado | Decisão |
| --- | --- | --- |
| Deploy online do site | DONE técnico | Domínio oficial live; Pages production deployment vem de `main` |
| SEO para DJ KidG / João Tomás | DONE | Search Console validou indexação e structured data ProfilePage |
| Tradução da página | POST-LAUNCH | Não é bloqueador |
| Email "O animador" -> "A equipa irá analisar" | DONE | Corrigido na Feature 4; PR #27 -> `dev` e PR #29 -> `main` |
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
