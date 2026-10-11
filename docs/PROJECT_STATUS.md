# Estado do Projeto

Last updated: 2026-10-11. Estado operacional de produção em V28; Feature 7 Accessibility em progresso, com implementação local concluída e QA/promoção pendentes.

## Resumo

O lançamento técnico de produção está completo. A stack principal está em Cloudflare Pages, Google Cloud Run, Neon PostgreSQL, Cloudflare R2, Google Cloud Scheduler, Google Secret Manager, Cloudflare Turnstile e Brevo SMTP. O domínio `www.saltosnaspalhacadas.pt` responde HTTP 200, o backend está UP, e a revisão Cloud Run `saltos-backend-00025-vix` serve 100% do tráfego com imagem `backend:3f6a5a6` / digest `sha256:a5d8ffccf7c7009315963e425319b3dd9fa794bd1898b66ed2cef932ac818cf9`. Flyway confirmou V28, 28 migrations validadas e schema já atualizado.

Feature 5 SEO / Google está DONE: Search Console configurado, domain property verificada, sitemap enviado, 11 URLs descobertas e perfis indexados. Feature 6 Performance & Media e Performance 6.2 Image Delivery / LCP estão DONE; a última medição documentada mantém PageSpeed 98 mobile / 100 desktop.

Feature 7 Accessibility está IN PROGRESS. A implementação local e os testes automáticos estão concluídos, incluindo a fase estrutural já integrada e os follow-ups de dialogs, formulários, live regions, foco de rotas SPA, teclado, contraste e reduced motion. QA humano de teclado, dark/light, 375/390/430 px, dialogs e leitor de ecrã básico, além da promoção, continuam pendentes; não é ainda uma release de produção.

Feature 8 Admin Monitoring & Analytics está TODO. Está planeada uma área exclusiva para `ADMIN`, por exemplo `/admin/monitorizacao`, com métricas agregadas de tráfego, contas habilitadas, bookings, conteúdo, operação e performance. A arquitetura será privacy-first, sem tracking individual por defeito, com providers/tokens apenas no backend e conclusão condicionada a revisão Privacy/Cookies/RGPD.

O backend atualmente em produção foi construído do source SHA `3f6a5a6ece13bbcfd40a75cff84d7aa4143bcfae` pelo Cloud Build `4793fb92-e51f-433f-92cb-b98d88feeb21`, que produziu a imagem `backend:3f6a5a6`. A revisão `saltos-backend-00025-vix` encontrou o schema já em V28, validou 28 migrations e não aplicou nova migration. Este source SHA do backend é distinto do HEAD atual de `main`/frontend, `7632482595d2d85b6d653105ced7a4f5ed4caee3`.

A UX de link de reset inválido/expirado foi validada no domínio oficial. Os reminders cliente/artista foram validados com uma execução manual do Scheduler, receção nas duas mailboxes de teste e ausência de duplicados numa segunda execução. O email imediato de novo booking para `profile.notificationEmail` do artista também foi confirmado em produção. A área de conta, sino/inbox, contrapropostas bidirecionais e reminder in-app foram promovidos por PR #95 -> `dev` e PR #96 -> `main`; backend, frontend, PostgreSQL CI, CodeQL, Cloudflare Pages, Flyway V28, health e smoke HTTP ficaram verdes. O trigger automático das 09:00 continua a ser monitorizado separadamente. Próximos trabalhos: QA/promoção da Feature 7; Feature 8 Admin Monitoring & Analytics; observabilidade/manutenção; legal/inbound email/media cleanup.

## Estado por Área

| Área | Estado | Produção? | Validado? | Próximo passo |
| --- | --- | --- | --- | --- |
| Frontend Cloudflare Pages | DONE | Sim, branch `main` | Sim | Monitorização pós-lançamento |
| Backend Cloud Run | DONE | Sim | Sim | Revisão `saltos-backend-00025-vix` com 100% do tráfego; health, liveness e readiness UP / HTTP 200 |
| Neon PostgreSQL | DONE | Sim | Sim | Manter snapshot pre-launch e PITR observado |
| Flyway V1-V28 | DONE | Sim | Sim | Produção confirmada em V28; 28 migrations validadas; `00025-vix` encontrou o schema up to date e não aplicou nova migration |
| UX da conta + notificações in-app / Flyway V27 | DONE | Sim | Sim | Provider, inbox e sino global para CUSTOMER/ADMIN promovidos por PR #95/#96 e schema V27 aplicado em produção |
| Contrapropostas bidirecionais + reminder in-app / Flyway V28 | DONE | Sim | Sim | Ping-pong ADMIN/CUSTOMER, email + inbox e terceiro canal de reminder promovidos por PR #95/#96; V28 aplicada em produção |
| Artist media/crop UX | DONE | Sim | Sim | Release em produção validada; campos de hero background e thumbnail confirmados; `/materials` respondeu com sucesso mas atualmente não existem materiais publicados |
| Flyway V21 profile notification email | DONE | Sim | Sim | Feature 4 notifications/email; PR #27 -> `dev`; PR #29 -> `main` |
| Cloudflare R2 public/private | DONE | Sim | Sim | Buckets runtime sem lifecycle genérico nem bucket lock |
| R2 backup Cloud Run Job | DONE | Sim | Sim | Monitorizar Scheduler diário e retenção |
| Uploads 10/30 MiB | DONE | Sim | Sim | Monitorizar erros 413/validação |
| Turnstile | DONE | Sim | Sim | Manter hostname oficial em allowlist |
| CSP Turnstile | DONE | Sim | Sim | Manter `challenges.cloudflare.com` em script/frame |
| Cleanup Scheduler | DONE | Sim | Sim | Monitorizar execuções diárias |
| Booking reminders cliente/artista | DONE | Sim | Sim | V26, tracking independente e janela hoje..+5; ambas as mailboxes confirmadas e segunda execução sem duplicados |
| Booking reminder Scheduler | DONE | Sim | Execução manual pós-release + entrega E2E validadas | Manter 09:00 Europe/Lisbon e cron Spring prod desativado com `-`; monitorizar próximo trigger automático |
| Brevo DNS/auth | DONE | Sim | Sim | Manter DKIM/DMARC saudáveis |
| SMTP Brevo | DONE | Sim | Sim | Monitorizar entregabilidade |
| Notificações admin/artista | DONE | Sim | Sim | Novo booking -> `profile.notificationEmail` do artista validado em produção; manter monitorização de entregabilidade |
| Password reset email real | DONE | Sim | Fluxo base, pré-validação e UX de link inválido/expirado validados em produção | POST final continua autoritativo; testes cobrem fronteira de expiração, uso único e nova emissão |
| Domínio registado | DONE | Sim | Sim pelo setup | DNS/TLS já funcionais; confirmação administrativa .PT separada |
| Domínio `www` HTTPS | DONE | Sim | Sim | QA final antes da release |
| Redirect apex -> www | DONE | Sim | Sim | 301 preserva query strings |
| Confirmação administrativa .PT | PENDING | Externo | Não | Aguardar confirmação do registrante/administrador |
| CI GitHub Actions | DONE | Sim | Sim | CI final da release `main` passou |
| PostgreSQL CI | DONE | Sim | Sim | PostgreSQL CI final da release `main` passou |
| CodeQL | DONE | Sim | Sim | CodeQL final da release `main` passou |
| Dependabot | DONE | Sim | Sim | Rever PRs semanais |
| Backups Neon/R2 | DONE | Sim | Sim | Fazer novos drills periódicos |
| RGPD export/delete | DONE técnico | Sim | Sim em código | Revisão jurídica |
| Privacy/Terms/Cookies | DONE técnico | Sim | Sim em código | Revisão jurídica e UX |
| Smoke final de produção | DONE | Sim | Sim | Manter checklist de regressão |
| Search Console | DONE | Sim | Sim | Domain property `saltosnaspalhacadas.pt` verificada; sitemap enviado com 11 URLs descobertas |
| Feature 5 SEO / Google | DONE | Sim | Sim | Páginas públicas/perfis indexados; structured data ProfilePage reconhecido pelo Search Console |
| Performance & Media | DONE | Sim | Sim em build/CI/deploy e PageSpeed | PR #55 -> `dev`; PR #56 -> `main`; baseline pós-Feature 6: mobile 85, desktop 100; TBT 0 ms |
| Performance 6.2 Image Delivery / LCP | DONE | Sim | Sim em PageSpeed pós-deploy | PR #68 -> `dev`; PR #69 -> `main`; mobile 98, LCP 2.3 s, FCP 1.2 s, TBT 0 ms; desktop 100, LCP 0.5 s; image waste estimado caiu de ~656 KiB para ~287 KiB |
| Feature 7 Accessibility | IN PROGRESS — implementação concluída | Não, branch local | Testes automáticos concluídos; QA humano pendente | Validar teclado, dark/light, 375/390/430 px, dialogs e leitor de ecrã básico; depois promover |
| Feature 8 Admin Monitoring & Analytics | TODO | Não | Não | Planear `/admin/monitorizacao`, endpoints ADMIN agregados, provider analytics server-side e revisão Privacy/Cookies/RGPD |
| Mobile UX Polish | DONE | Sim | Sim em dispositivo real | PR #59 -> `dev`; PR #60 -> `main`; scroll-to-top, theme toggle, cards de perfis, portfolio e booking mobile corrigidos |
| Mobile lightbox + branding icons | DONE | Sim | Sim em iPhone | PR #61 -> `dev`; PR #62 -> `main`; portal para `document.body`, scroll lock iOS/iPadOS, safe areas e favicons Saltos atualizados |

## Branches e Release

| Branch / PR | SHA conhecido | Papel |
| --- | --- | --- |
| `main` | `7632482` | HEAD remoto atual do frontend/source; distinto do source SHA da imagem backend em Cloud Run |
| `dev` | `688ce6f` | Base de integração anterior à Feature 7 completa |
| `feat/accessibility-complete` | local | Feature 7 IN PROGRESS; implementação/testes automáticos concluídos, QA e promoção pendentes |
| `fix/artist-lightbox-desktop` | integrado | PR #81 -> `dev`; Artist media/crop UX |
| PR #82 `dev` -> `main` | integrado | Promoveu a release Artist media/crop UX para `main` |
| `feat/production-launch` | integrado | Branch de lançamento já promovida |
| `feat/notifications-and-email` | integrado | Feature 4: notifications/email DONE; PR #27 -> `dev`, PR #29 -> `main` |
| `feat/account-notifications` | integrado | Notificações in-app + contrapropostas bidirecionais; PR #95 -> `dev`, PR #96 -> `main` |

Fluxo histórico concluído:

```text
feat/production-launch
  -> PR para dev
  -> CI/CodeQL/revisão
  -> PR final para main
  -> Cloudflare Pages production branch main
```

Distinção importante:

- HEAD remoto atual de `main`/frontend: `7632482595d2d85b6d653105ced7a4f5ed4caee3`;
- source SHA da imagem backend em Cloud Run: `3f6a5a6ece13bbcfd40a75cff84d7aa4143bcfae`;
- imagem backend atualmente confirmada em produção: `backend:3f6a5a6`;
- image digest imutável: `sha256:a5d8ffccf7c7009315963e425319b3dd9fa794bd1898b66ed2cef932ac818cf9`;
- Cloud Build backend: `4793fb92-e51f-433f-92cb-b98d88feeb21`;
- Cloud Run revision confirmada: `saltos-backend-00025-vix`, a servir 100% do tráfego;
- health, liveness e readiness estão UP / HTTP 200; antes da promoção, a revisão com 0% de tráfego passou 12/12 checks e `/api/v1/profiles` respondeu HTTP 200; depois da promoção, passou 6/6 checks de health e o endpoint PostgreSQL de profiles respondeu HTTP 200;
- Cloudflare Pages production branch é `main`;
- Flyway em produção: 28 migrations validadas, schema `public` na versão 28 e up to date;
- a revisão `00025-vix` confirmou o schema já em V28 e não aplicou nova migration;
- Feature 4 notifications/email está DONE e V21 está em produção;
- Feature 5 SEO / Google está DONE: Search Console configurado, domain property `saltosnaspalhacadas.pt` verificada, sitemap enviado com 11 URLs descobertas, páginas públicas/perfis indexados e ProfilePage reconhecido;
- Feature 6 Performance & Media foi promovida por PR #55 -> `dev` e PR #56 -> `main`; PageSpeed inicial pós-Feature 6 mediu 85 mobile e 100 desktop, com TBT 0 ms e LCP mobile 4.4 s;
- Performance 6.2 Image Delivery / LCP foi promovida por PR #68 -> `dev` e PR #69 -> `main`; nova medição PageSpeed: 98 mobile / 100 desktop, LCP 2.3 s / 0.5 s, FCP 1.2 s / 0.3 s e TBT 0 ms;
- Mobile UX Polish foi promovido por PR #59 -> `dev` e PR #60 -> `main` e validado em telemóvel;
- o hotfix do MediaLightbox iOS/iPadOS e refresh completo dos favicons/icons foi promovido por PR #61 -> `dev` e PR #62 -> `main`; a seleção final do favicon desktop foi corrigida em PR #70 -> `dev` e PR #71 -> `main`;
- Artist media/crop UX foi promovido por PR #81 -> `dev` e PR #82 -> `main` e validado em produção em 2026-09-30; inclui desktop profile/media/lightbox/hero/crop/thumbnails e preservação mobile/touch; campos públicos de hero/portfolio confirmados e endpoint `/materials` validado com resposta vazia por existirem 0 materiais;
- PageSpeed ainda não apresenta dados de campo/CrUX suficientes; Performance 6.2 fica fechada;
- a pré-validação pública do reset token, estados de loading/invalid/error, retry e pedido de novo link estão em produção. A validação final continua obrigatória, sem consumo na pré-validação. O caminho de token inválido foi confirmado no domínio oficial em 2026-10-05; testes cobrem expiração exata, uso único, conta inativa, nova emissão e rejeição após pré-validação.

## Produção Conhecida

Estado operacional confirmado em 2026-10-11: a revisão backend `saltos-backend-00025-vix`, construída do source SHA `3f6a5a6ece13bbcfd40a75cff84d7aa4143bcfae`, está saudável e serve 100% do tráfego. Esta revisão confirmou o schema já em V28; não executou novas migrations.

| Componente | Valor operacional documentável |
| --- | --- |
| Cloudflare Pages project | `saltos-nas-palhacadas-prod` |
| HEAD atual de `main`/frontend | `7632482595d2d85b6d653105ced7a4f5ed4caee3` |
| URL público | `https://www.saltosnaspalhacadas.pt` |
| Apex | `https://saltosnaspalhacadas.pt` redireciona 301 para `www` |
| URL Pages temporário | `https://saltos-nas-palhacadas-prod.pages.dev` |
| Cloud Run project | `saltos-prod-gmesquita` |
| Região Cloud Run | `europe-west1` |
| Service | `saltos-backend` |
| Cloud Run revision | `saltos-backend-00025-vix` |
| Imagem | `backend:3f6a5a6` / `sha256:a5d8ffccf7c7009315963e425319b3dd9fa794bd1898b66ed2cef932ac818cf9` |
| Source SHA da imagem backend | `3f6a5a6ece13bbcfd40a75cff84d7aa4143bcfae`; distinto do HEAD atual de `main`/frontend |
| Tráfego | 100% |
| Health | UP / HTTP 200; 6/6 checks após promoção |
| Liveness | UP / HTTP 200 |
| Readiness | UP / HTTP 200 |
| Validação antes da promoção | Revisão com 0% de tráfego: 12/12 health/readiness checks OK e `/api/v1/profiles` HTTP 200 |
| Validação depois da promoção | 6/6 `/actuator/health` HTTP 200 e PostgreSQL profiles HTTP 200 |
| Recursos Cloud Run | 1 CPU, 1 GiB RAM, concurrency 80, max 2, scale-to-zero, startup CPU boost |
| Flyway operacional confirmado | V28; 28 migrations validadas, schema `public` up to date; `00025-vix` não aplicou nova migration |
| Cloud Build backend | `4793fb92-e51f-433f-92cb-b98d88feeb21` |
| Image digest | `sha256:a5d8ffccf7c7009315963e425319b3dd9fa794bd1898b66ed2cef932ac818cf9` |
| Neon branch | `production/default` |
| R2 buckets | `saltos-prod-public`, `saltos-prod-private` |
| R2 backup bucket | `saltos-prod-backup` |
| Schedulers enabled | `saltos-r2-backup-daily`, `saltos-private-media-cleanup`, `saltos-booking-reminders` |

Sem segredos reais nesta documentação.
