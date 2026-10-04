# Estado do Projeto

Last updated: 2026-10-05. Estado operacional de produção, V26, UX de reset e reminders cliente/artista confirmados após a release.

## Resumo

O lançamento técnico de produção está completo. A stack principal está em Cloudflare Pages, Google Cloud Run, Neon PostgreSQL, Cloudflare R2, Google Cloud Scheduler, Google Secret Manager, Cloudflare Turnstile e Brevo SMTP. O domínio `www.saltosnaspalhacadas.pt` responde HTTP 200, o backend está UP, a revisão Cloud Run `saltos-backend-00021-5mv` serve 100% do tráfego com imagem `backend:dd872aa` / digest `sha256:d16e16a8c72440e973e5e1ad5b4facb8495ac511b1ba431cc097b40ab0252763`, e Flyway confirmou produção em V26 com 26 migrations validadas.

Feature 5 SEO / Google está DONE: Search Console configurado, domain property verificada, sitemap enviado, 11 URLs descobertas e perfis indexados. Feature 6 Performance & Media e Performance 6.2 Image Delivery / LCP estão DONE; a última medição documentada mantém PageSpeed 98 mobile / 100 desktop.

A alteração de runtime mais recente em produção é a release `dd872aa5c9418650cab3ed8c52a8ff41fcb3faa5`, que acrescenta pré-validação/UX de password reset e reminders independentes de cliente/artista. O Cloud Build `874171bf-c557-4b22-aa92-6cb270e04d74` produziu a imagem `backend:dd872aa`; a revisão `saltos-backend-00020-ln5` aplicou V26 e a revisão atual `00021-5mv` validou o schema já em V26.

A UX de link de reset inválido/expirado foi validada no domínio oficial. Os reminders cliente/artista foram validados com uma execução manual do Scheduler, receção nas duas mailboxes de teste e ausência de duplicados numa segunda execução. O email imediato de novo booking para `profile.notificationEmail` do artista também foi confirmado em produção. O trigger automático das 09:00 continua a ser monitorizado separadamente. A sequência real de próximos trabalhos é: UX da área de conta + notificações in-app; Feature 7 Accessibility; observabilidade/manutenção; legal/inbound email/media cleanup.

## Estado por Área

| Área | Estado | Produção? | Validado? | Próximo passo |
| --- | --- | --- | --- | --- |
| Frontend Cloudflare Pages | DONE | Sim, branch `main` | Sim | Monitorização pós-lançamento |
| Backend Cloud Run | DONE | Sim | Sim | Revisão `saltos-backend-00021-5mv` com 100% do tráfego e health UP |
| Neon PostgreSQL | DONE | Sim | Sim | Manter snapshot pre-launch e PITR observado |
| Flyway V1-V26 | DONE | Sim | Sim | Produção confirmada em V26; 26 migrations validadas; `00020-ln5` aplicou V26 e `00021-5mv` confirmou up to date |
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
| Mobile UX Polish | DONE | Sim | Sim em dispositivo real | PR #59 -> `dev`; PR #60 -> `main`; scroll-to-top, theme toggle, cards de perfis, portfolio e booking mobile corrigidos |
| Mobile lightbox + branding icons | DONE | Sim | Sim em iPhone | PR #61 -> `dev`; PR #62 -> `main`; portal para `document.body`, scroll lock iOS/iPadOS, safe areas e favicons Saltos atualizados |

## Branches e Release

| Branch / PR | SHA conhecido | Papel |
| --- | --- | --- |
| `main` | `ea3eddf` | Source release em produção; Artist media/crop UX operacionalmente validada |
| `dev` | integrado | Branch de integração |
| `fix/artist-lightbox-desktop` | integrado | PR #81 -> `dev`; Artist media/crop UX |
| PR #82 `dev` -> `main` | integrado | Promoveu a release Artist media/crop UX para `main` |
| `feat/production-launch` | integrado | Branch de lançamento já promovida |
| `feat/notifications-and-email` | integrado | Feature 4: notifications/email DONE; PR #27 -> `dev`, PR #29 -> `main` |

Fluxo histórico concluído:

```text
feat/production-launch
  -> PR para dev
  -> CI/CodeQL/revisão
  -> PR final para main
  -> Cloudflare Pages production branch main
```

Distinção importante:

- source release atualmente confirmada em produção: `ea3eddf`;
- imagem backend atualmente confirmada em produção: `backend:ea3eddf`;
- image digest imutável: `sha256:d16e16a8c72440e973e5e1ad5b4facb8495ac511b1ba431cc097b40ab0252763`;
- Cloud Run revision confirmada: `saltos-backend-00021-5mv`, a servir 100% do tráfego;
- `/actuator/health` está UP; houve um HTTP 503 no primeiro pedido manual durante startup/rollout antes da prontidão, seguido de health UP;
- Cloudflare Pages production branch é `main`;
- Flyway em produção: 26 migrations validadas, schema `public` na versão 26 e up to date;
- a revisão `00020-ln5` encontrou V25 e aplicou V26; a revisão `00021-5mv` encontrou V26 e confirmou o schema up to date;
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

Release de 2026-10-05: reminders de cliente e artista têm timestamps separados e retry independente. `V26__artist_booking_reminders.sql` foi aplicada por `saltos-backend-00020-ln5`. A entrega real para as duas mailboxes de teste foi confirmada e uma segunda execução manual não gerou duplicados.

| Componente | Valor operacional documentável |
| --- | --- |
| Cloudflare Pages project | `saltos-nas-palhacadas-prod` |
| Source release | `ea3eddf` |
| URL público | `https://www.saltosnaspalhacadas.pt` |
| Apex | `https://saltosnaspalhacadas.pt` redireciona 301 para `www` |
| URL Pages temporário | `https://saltos-nas-palhacadas-prod.pages.dev` |
| Cloud Run project | `saltos-prod-gmesquita` |
| Região Cloud Run | `europe-west1` |
| Service | `saltos-backend` |
| Cloud Run revision | `saltos-backend-00021-5mv` |
| Imagem | `backend:dd872aa` / `sha256:d16e16a8c72440e973e5e1ad5b4facb8495ac511b1ba431cc097b40ab0252763` |
| Código de produção backend | `main` em `dd872aa5c9418650cab3ed8c52a8ff41fcb3faa5`; revisão Cloud Run `saltos-backend-00021-5mv` |
| Tráfego | 100% |
| Health | UP; primeiro pedido manual durante rollout devolveu 503 transitório antes da prontidão |
| Recursos Cloud Run | 1 CPU, 1 GiB RAM, concurrency 80, max 2, scale-to-zero, startup CPU boost |
| Flyway operacional confirmado | V26; 26 migrations validadas, schema `public` up to date |
| Cloud Build backend | `874171bf-c557-4b22-aa92-6cb270e04d74` |
| Image digest | `sha256:537486d2e2c12f59039383052944d0326cf0c21531a84c4a912063b4a55f14fb` |
| Neon branch | `production/default` |
| R2 buckets | `saltos-prod-public`, `saltos-prod-private` |
| R2 backup bucket | `saltos-prod-backup` |
| Schedulers enabled | `saltos-r2-backup-daily`, `saltos-private-media-cleanup`, `saltos-booking-reminders` |

Sem segredos reais nesta documentação.
