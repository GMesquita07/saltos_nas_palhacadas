# Roadmap

Last updated: 2026-10-04.

Estados usados: DONE, IN PROGRESS, BLOCKED, TODO, POST-LAUNCH, OPTIONAL.

## P0 - Bloqueadores do Lançamento

| Item | Estado | Nota |
| --- | --- | --- |
| `www` live com HTTPS | DONE | `https://www.saltosnaspalhacadas.pt` validado |
| Redirect apex -> `www` | DONE | HTTP 301 preserva query strings |
| Brevo DNS verification | DONE | Domínio autenticado em Brevo |
| DKIM/DMARC final | DONE | Validados |
| SMTP real | DONE | Brevo SMTP 587 STARTTLS ativo |
| Teste email recuperação password | DONE (fluxo base) | Emissão/entrega e rejeição de token antigo confirmadas; a branch local da UX de reset acrescenta teste determinístico da fronteira exata de 30 min |
| Teste emails bookings | DONE | Recebido, aceite e cancelamento validados |
| Scheduler booking reminders | DONE | Invocação do job diário 09:00 Europe/Lisbon validada; entrega do reminder às mailboxes não comprovada por este teste |
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
| Admin content management V2 | DONE | Portfolio admin vê publicados/ocultos, materiais editáveis, contactos ocultáveis e painel redesenhado |
| Feature 4: notificações email admin/artista | DONE | PR #27 -> `dev`; PR #29 -> `main`; V21 em produção |
| Global UI Redesign | DONE | PR #44 -> `dev`; PR #45 -> `main`; houve fixes visuais posteriores |
| Favicon branding final | DONE | PR #53/#54; refresh completo em PR #61/#62; seleção do favicon desktop estabilizada em PR #70/#71 |
| Otimização de media/assets estáticos | DONE | Cache de `/assets/*` na Feature 6; assets críticos WebP/preload em Performance 6.2; media dinâmica responsiva fica como follow-up opcional |
| Feature 6: Performance & Media | DONE | PR #55 -> `dev`; PR #56 -> `main`; baseline pós-feature 85 mobile / 100 desktop; TBT 0 ms |
| Mobile UX Polish | DONE | PR #59 -> `dev`; PR #60 -> `main`; scroll/routing, theme toggle, cards, portfolio e booking mobile corrigidos |
| Mobile lightbox/iOS + branding icons | DONE | PR #61 -> `dev`; PR #62 -> `main`; validado em iPhone; frontend produção `e2bb91cd38b381ec951cae0a6fadf9f4a1055904` |
| Performance 6.2: image delivery/LCP mobile | DONE | PR #68 -> `dev`; PR #69 -> `main`; mobile 85 -> 98, LCP 4.4 s -> 2.3 s, image waste ~656 -> ~287 KiB; desktop mantém 100 |
| Desktop UX: vista dos conteúdos por artista | DONE | PR #81 -> `dev`; PR #82 -> `main`; produção validada em 2026-09-30. Concluiu apresentação desktop do perfil/media, lightbox desktop, hero visual, enquadramento/crop persistente, thumbnails e preservação intencional do comportamento mobile/touch/iPad. |
| UX: link de recuperação expirado | IN PROGRESS | Pré-validação server-side, formulário condicionado, «Link inválido ou expirado», «Pedir novo link» e retry de rede implementados. POST final autoritativo; testes de fronteira, reuso e nova emissão. Aguardar revisão, deploy e validação E2E. |
| Reminders 5 dias: cliente e artista | IN PROGRESS | Implementação local com V26, tracking e retries independentes até ao dia do evento. Scheduler existente preservado. Aguardar deploy e comprovar entrega cliente/artista em mailboxes reais. |
| Email pessoal do artista em bookings | TODO | Sempre que um cliente fizer um booking para um artista, esse artista deve receber uma notificação no respetivo email pessoal/operacional. Não confundir com a infraestrutura de notificações existente; é um requisito funcional de booking a validar/implementar. |
| UX da área de conta + notificações in-app | TODO | Melhorar a área pessoal do cliente e desenhar zona de notificações in-site. |
| Feature 7: Acessibilidade | TODO | PageSpeed/Lighthouse atual 96; primeiro problema automático confirmado é contraste do botão `Aceitar` no Cookie Consent, seguido de teclado, focus, formulários, dialogs e leitor de ecrã. |
| Observabilidade/manutenção | TODO | Monitorizar budgets/logs de GCP, Cloudflare, Neon e R2; manter drills periódicos e rotinas operacionais. |
| Legal, inbound email e cleanup media | TODO | Rever Privacy/Terms/Cookies juridicamente e em UX; decidir provider ou Cloudflare Email Routing para `ola@`; cleanup de media pública órfã requer tracking seguro de ownership/referências antes de apagar objetos R2. |
| Arquitetura híbrida/static content | PLANNED | Ver [decisões](DECISIONS.md) |
| Avatares predefinidos | OPTIONAL | Reduzir uploads livres |

## Produto e UX Migrado do `todolist.md`

| Item antigo | Estado | Decisão |
| --- | --- | --- |
| Deploy online do site | DONE técnico | Domínio oficial live; Pages production deployment vem de `main` |
| SEO para DJ KidG / João Tomás | DONE | Search Console validou indexação e structured data ProfilePage |
| Tradução da página | POST-LAUNCH | Não é bloqueador |
| Email "O animador" -> "A equipa irá analisar" | DONE | Corrigido na Feature 4; PR #27 -> `dev` e PR #29 -> `main` |
| Área pessoal do cliente como botões | TODO | Produto/UX; agrupado com UX da área de conta |
| Zona de notificações in-site | TODO | Nova funcionalidade; agrupada com UX da área de conta |
| Formatação Privacy/Terms/Cookies | TODO | Melhorar UX e pedir revisão jurídica |

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
