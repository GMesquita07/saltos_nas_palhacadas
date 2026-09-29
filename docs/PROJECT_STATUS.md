# Estado do Projeto

Last updated: 2026-09-30.

## Resumo

O lançamento técnico de produção está completo. A stack principal está em Cloudflare Pages, Google Cloud Run, Neon PostgreSQL, Cloudflare R2, Google Cloud Scheduler, Google Secret Manager, Cloudflare Turnstile e Brevo SMTP. O domínio `www.saltosnaspalhacadas.pt` responde HTTP 200, o apex redireciona com HTTP 301 para `www`, o backend está UP, a revisão Cloud Run `saltos-backend-00017-88t` serve 100% do tráfego com imagem `backend:55056d5`, e o último estado operacional confirmado de Flyway em produção é V23.

Feature 5 SEO / Google está DONE: Search Console configurado, domain property verificada, sitemap enviado, 11 URLs descobertas e perfis indexados. Feature 6 Performance & Media e Performance 6.2 Image Delivery / LCP estão DONE; a última medição documentada mantém PageSpeed 98 mobile / 100 desktop.

A alteração de runtime mais recente merged em `main` é a release Artist media/crop UX, promovida por PR #81 (`fix/artist-lightbox-desktop` -> `dev`) e PR #82 (`dev` -> `main`). Esta release inclui melhorias do perfil/artista em desktop, MediaLightbox desktop, hero visual, crop/enquadramento consistente e persistente para avatars/backgrounds/thumbnails/materiais, preview de vídeo, invalidação/cache de portfolio e migrations V24/V25 no código.

Não assumir que V24/V25 já foram aplicadas na base de dados de produção nem que existe nova revisão Cloud Run sem validação operacional posterior. A sequência real de próximos trabalhos é: UX de link de reset expirado; email pessoal/operacional do artista quando existe booking desse artista; UX da área de conta + notificações in-app; Feature 7 Accessibility; observabilidade/manutenção; legal/inbound email/media cleanup.

## Estado por Área

| Área | Estado | Produção? | Validado? | Próximo passo |
| --- | --- | --- | --- | --- |
| Frontend Cloudflare Pages | DONE | Sim, branch `main` | Sim | Monitorização pós-lançamento |
| Backend Cloud Run | DONE | Sim | Sim | Health UP; monitorização pós-lançamento |
| Neon PostgreSQL | DONE | Sim | Sim | Manter snapshot pre-launch e PITR observado |
| Flyway V1-V23 | DONE | Sim | Sim | Último estado operacional confirmado em produção; V23 adiciona profile hero background |
| Flyway V24/V25 | DONE em código/main | Não assumido | Pendente em produção | Presentes em `main`; validar aplicação no próximo deploy backend/Flyway antes de marcar produção |
| Artist media/crop UX | DONE em código/main | Não assumido | Validado em PR; pendente em produção | PR #81/#82 merged; confirmar deploy frontend/backend e Flyway V24/V25 quando aplicável |
| Flyway V21 profile notification email | DONE | Sim | Sim | Feature 4 notifications/email; PR #27 -> `dev`; PR #29 -> `main` |
| Cloudflare R2 public/private | DONE | Sim | Sim | Buckets runtime sem lifecycle genérico nem bucket lock |
| R2 backup Cloud Run Job | DONE | Sim | Sim | Monitorizar Scheduler diário e retenção |
| Uploads 10/30 MiB | DONE | Sim | Sim | Monitorizar erros 413/validação |
| Turnstile | DONE | Sim | Sim | Manter hostname oficial em allowlist |
| CSP Turnstile | DONE | Sim | Sim | Manter `challenges.cloudflare.com` em script/frame |
| Cleanup Scheduler | DONE | Sim | Sim | Monitorizar execuções diárias |
| Booking reminders internos | DONE local/dev | Não em prod | Sim em testes | Manter cron prod desativado com `-` |
| Booking reminder Scheduler | DONE | Sim | Sim | Monitorizar execução diária às 09:00 |
| Brevo DNS/auth | DONE | Sim | Sim | Manter DKIM/DMARC saudáveis |
| SMTP Brevo | DONE | Sim | Sim | Monitorizar entregabilidade |
| Notificações admin/artista | DONE infraestrutura | Sim | Parcial | Infraestrutura em produção; requisito booking -> email pessoal/operacional do artista continua pendente de implementação/validação E2E |
| Password reset email real | DONE (fluxo base) | Sim | Emissão/entrega validadas; utilizador confirmou rejeição de token antigo pelo backend | Melhorar UX: verificar token ao abrir a rota e mostrar link inválido/expirado em vez do formulário; manter teste do limite exato dos 30 min e uso único |
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
| `main` | contém PR #82 | Production branch; a release Artist media/crop UX está merged no código, mas produção operacional não deve ser inferida sem validação posterior |
| `dev` | integrado | Branch de integração; contém PR #81 e receberá este housekeeping |
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

- código/imagem backend atualmente confirmados em produção: `backend:55056d5`;
- Cloud Run revision confirmada: `saltos-backend-00017-88t`, a servir 100% do tráfego;
- `/actuator/health` está UP no último estado operacional documentado;
- Cloudflare Pages production branch é `main`;
- último estado operacional confirmado de Flyway em produção: V23;
- `main` já inclui V24/V25, mas a aplicação dessas migrations em produção fica pendente de deploy/backend validation;
- Feature 4 notifications/email está DONE e V21 está em produção;
- Feature 5 SEO / Google está DONE: Search Console configurado, domain property `saltosnaspalhacadas.pt` verificada, sitemap enviado com 11 URLs descobertas, páginas públicas/perfis indexados e ProfilePage reconhecido;
- Feature 6 Performance & Media foi promovida por PR #55 -> `dev` e PR #56 -> `main`; PageSpeed inicial pós-Feature 6 mediu 85 mobile e 100 desktop, com TBT 0 ms e LCP mobile 4.4 s;
- Performance 6.2 Image Delivery / LCP foi promovida por PR #68 -> `dev` e PR #69 -> `main`; nova medição PageSpeed: 98 mobile / 100 desktop, LCP 2.3 s / 0.5 s, FCP 1.2 s / 0.3 s e TBT 0 ms;
- Mobile UX Polish foi promovido por PR #59 -> `dev` e PR #60 -> `main` e validado em telemóvel;
- o hotfix do MediaLightbox iOS/iPadOS e refresh completo dos favicons/icons foi promovido por PR #61 -> `dev` e PR #62 -> `main`; a seleção final do favicon desktop foi corrigida em PR #70 -> `dev` e PR #71 -> `main`;
- Artist media/crop UX foi promovido por PR #81 -> `dev` e PR #82 -> `main`; inclui desktop profile/media/lightbox/hero/crop/thumbnails e preservação mobile/touch;
- PageSpeed ainda não apresenta dados de campo/CrUX suficientes; Performance 6.2 fica fechada;
- o utilizador confirmou que o formulário de reset abre com um link antigo, mas o backend rejeita a submissão. O código verifica `expires_at` e `used_at` no POST; o pendente é UX de pré-validação e mensagem «Link inválido ou expirado» com pedido de novo link. Testes da fronteira dos 30 minutos e uso único continuam recomendados; não há falha de aceitação de token expirado confirmada.

## Produção Conhecida

| Componente | Valor operacional documentável |
| --- | --- |
| Cloudflare Pages project | `saltos-nas-palhacadas-prod` |
| Frontend runtime release SHA | `3e6d64275c2bd10238ae24d5e99f53f18f08ac76` (último runtime validado antes da release Artist media/crop UX; não substituir sem validação posterior) |
| URL público | `https://www.saltosnaspalhacadas.pt` |
| Apex | `https://saltosnaspalhacadas.pt` redireciona 301 para `www` |
| URL Pages temporário | `https://saltos-nas-palhacadas-prod.pages.dev` |
| Cloud Run project | `saltos-prod-gmesquita` |
| Região Cloud Run | `europe-west1` |
| Service | `saltos-backend` |
| Cloud Run revision | `saltos-backend-00017-88t` |
| Imagem | `backend:55056d5` |
| Código de produção backend | `main` em `55056d5`; revisão Cloud Run `saltos-backend-00017-88t` |
| Tráfego | 100% |
| Health | UP |
| Recursos Cloud Run | 1 CPU, 1 GiB RAM, concurrency 80, max 2, scale-to-zero, startup CPU boost |
| Flyway operacional confirmado | V23; `main` inclui V24/V25 ainda a validar no próximo deploy/backend validation |
| Neon branch | `production/default` |
| R2 buckets | `saltos-prod-public`, `saltos-prod-private` |
| R2 backup bucket | `saltos-prod-backup` |
| Schedulers enabled | `saltos-r2-backup-daily`, `saltos-private-media-cleanup`, `saltos-booking-reminders` |

Sem segredos reais nesta documentação.
