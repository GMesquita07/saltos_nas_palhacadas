# Roadmap

Last updated: 2026-10-11.

Estados usados: DONE, IN PROGRESS, BLOCKED, TODO, POST-LAUNCH, OPTIONAL.

## P0 - Bloqueadores do Lançamento

| Item | Estado | Nota |
| --- | --- | --- |
| `www` live com HTTPS | DONE | `https://www.saltosnaspalhacadas.pt` validado |
| Redirect apex -> `www` | DONE | HTTP 301 preserva query strings |
| Brevo DNS verification | DONE | Domínio autenticado em Brevo |
| DKIM/DMARC final | DONE | Validados |
| SMTP real | DONE | Brevo SMTP 587 STARTTLS ativo |
| Teste email recuperação password | DONE | Fluxo base + UX de link inválido/expirado em produção; fronteira exata de 30 min coberta por teste determinístico |
| Teste emails bookings | DONE | Recebido, aceite e cancelamento validados |
| Scheduler booking reminders | DONE | Job existente preservado; execução manual pós-release entregou cliente/artista e segunda execução não duplicou; monitorizar próximo trigger automático das 09:00 |
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
| UX: link de recuperação expirado | DONE | Pré-validação server-side, formulário condicionado, «Link inválido ou expirado», «Pedir novo link» e retry implementados; POST final autoritativo. Promovido por PR #89/#90 e validado no domínio oficial em 2026-10-05. |
| Reminders 5 dias: cliente e artista | DONE | V26 em produção, tracking/retries independentes até ao dia do evento; execução manual confirmou ambas as mailboxes e segunda execução sem duplicados. |
| Email pessoal do artista em bookings | DONE | Implementação já existente em `BookingNotificationService`; novo booking envia para `profile.notificationEmail` e admins ativos. Entrega real no email do artista confirmada em produção em 2026-10-05. |
| UX da área de conta + notificações in-app | DONE | Dashboard, provider partilhado, sino global e reminder in-app promovidos por PR #95/#96; V27/V28 aplicadas e produção validada. |
| Contrapropostas bidirecionais de booking | DONE | Fluxo ADMIN↔CUSTOMER, autoria e termos propostos promovidos por PR #95/#96; V28 aplicada e produção validada. |
| Feature 7: Acessibilidade | DONE | Implementação e QA humano concluídos: skip link/navegação, foco de rotas SPA e hashes, dialogs com trap/Escape/retorno de foco, formulários/live regions, teclado, contraste WCAG e reduced motion validados em dark/light e desktop/mobile. Aguarda apenas promoção para produção. |
| Feature 8: Admin Monitoring & Analytics | TODO | Planear `/admin/monitorizacao` com métricas agregadas de tráfego, contas, bookings, conteúdo, operação e performance, numa arquitetura privacy-first sujeita a revisão Privacy/Cookies/RGPD. |
| Observabilidade/manutenção | TODO | Monitorizar budgets/logs de GCP, Cloudflare, Neon e R2; manter drills periódicos e rotinas operacionais. |
| Legal, inbound email e cleanup media | TODO | Rever Privacy/Terms/Cookies juridicamente e em UX; decidir provider ou Cloudflare Email Routing para `ola@`; cleanup de media pública órfã requer tracking seguro de ownership/referências antes de apagar objetos R2. |
| Arquitetura híbrida/static content | PLANNED | Ver [decisões](DECISIONS.md) |
| Avatares predefinidos | OPTIONAL | Reduzir uploads livres |

### Feature 8: Admin Monitoring & Analytics

Objetivo: adicionar uma área exclusiva para `ADMIN`, por exemplo `/admin/monitorizacao`, para acompanhamento agregado e privacy-first do site.

- Tráfego: visitas hoje e em 7/30 dias, page views, páginas públicas mais visitadas, evolução temporal e origens agregadas quando permitido.
- Contas: total, habilitadas/inativas, novos registos em 7/30 dias e distribuição `CUSTOMER`/`ADMIN`; conta ativa significa habilitada no sistema, não tracking da última navegação.
- Bookings: novos pedidos, estados, contrapropostas, próximos eventos e conversão agregada quando fizer sentido.
- Conteúdo: perfis, portfolio publicado, materiais, reviews e favoritos agregados úteis.
- Operação: health do backend, schedulers/jobs, reminders, backups, erros agregados e revisão conhecida.
- Performance: Core Web Vitals e Cloudflare Web Analytics quando disponíveis.
- Privacidade/RGPD: não recolher por defeito histórico individual, páginas ligadas a contas, IP completo, fingerprint, localização precisa ou session replay.

Arquitetura prevista: Admin frontend -> endpoint backend exclusivo para `ADMIN` -> dados agregados da base de dados e provider de analytics. Uma integração futura com Cloudflare Web Analytics/APIs deve manter tokens apenas no backend/Secret Manager, nunca em `VITE_*`. A feature só poderá passar a DONE depois da revisão Privacy/Cookies/RGPD da telemetria efetivamente implementada.

## Produto e UX Migrado do `todolist.md`

| Item antigo | Estado | Decisão |
| --- | --- | --- |
| Deploy online do site | DONE técnico | Domínio oficial live; Pages production deployment vem de `main` |
| SEO para DJ KidG / João Tomás | DONE | Search Console validou indexação e structured data ProfilePage |
| Tradução da página | POST-LAUNCH | Não é bloqueador |
| Email "O animador" -> "A equipa irá analisar" | DONE | Corrigido na Feature 4; PR #27 -> `dev` e PR #29 -> `main` |
| Área pessoal do cliente como botões | DONE | UX da área de conta promovida por PR #95/#96 e validada em produção |
| Zona de notificações in-site | DONE | Inbox e sino global para utilizadores autenticados promovidos por PR #95/#96; V27/V28 em produção |
| Notificações futuras | OPTIONAL | Avaliar reminder in-app de 1 dia, pedido de review pós-evento, favoritos e waitlist/disponibilidade; nada implementado nesta ronda |
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
