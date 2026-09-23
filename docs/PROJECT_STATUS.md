# Estado do Projeto

Last verified: 2026-09-23.

## Resumo

O lançamento técnico de produção está completo. A stack principal está em Cloudflare Pages, Google Cloud Run, Neon PostgreSQL, Cloudflare R2, Google Cloud Scheduler, Google Secret Manager, Cloudflare Turnstile e Brevo SMTP. O domínio `www.saltosnaspalhacadas.pt` responde HTTP 200, o apex redireciona com HTTP 301 para `www`, o backend está UP, a revisão Cloud Run `saltos-backend-00017-88t` serve 100% do tráfego com imagem `backend:55056d5`, Flyway validou 23 migrations e produção está em V23. Feature 4 notifications/email está DONE e o Global UI Redesign foi promovido a `main`, com fixes visuais posteriores.

## Estado por Área

| Área | Estado | Produção? | Validado? | Próximo passo |
| --- | --- | --- | --- | --- |
| Frontend Cloudflare Pages | DONE | Sim, branch `main` | Sim | Monitorização pós-lançamento |
| Backend Cloud Run | DONE | Sim | Sim | Health UP; monitorização pós-lançamento |
| Neon PostgreSQL | DONE | Sim | Sim | Manter snapshot pre-launch e PITR observado |
| Flyway V1-V23 | DONE | Sim | Sim | Produção validada em V23; V23 adiciona profile hero background |
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
| Notificações admin/artista | DONE | Sim | Sim | Usa admins ativos da DB e email privado por perfil |
| Password reset email real | DONE | Sim | Sim | Canonical URL aponta para `www` |
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
| Search Console | TODO | Não | Não | Ainda não configurado; submeter sitemap após Feature 5 |
| SEO por perfil | IN PROGRESS | Não | Em desenvolvimento | Feature 5 prepara metadata client-side, JSON-LD e sitemap; validar no Search Console antes de decidir prerender |

## Branches e Release

| Branch | SHA conhecido | Papel |
| --- | --- | --- |
| `main` | release final validada | Production branch do Cloudflare Pages |
| `dev` | integrado | Branch de integração |
| `feat/production-launch` | integrado | Branch de lançamento já promovida |
| `feat/notifications-and-email` | integrado | Feature 4: notifications/email DONE; PR #27 -> `dev`, PR #29 -> `main` |

Fluxo concluído:

```text
feat/production-launch
  -> PR para dev
  -> CI/CodeQL/revisão
  -> PR final para main
  -> Cloudflare Pages production branch main
```

Distinção importante:

- código/imagem backend atualmente em produção: `backend:55056d5`;
- Cloud Run revision `saltos-backend-00017-88t` serve 100% do tráfego;
- `/actuator/health` está UP;
- Cloudflare Pages production branch é `main`;
- produção estava em Flyway V22 e aplicou V23; Flyway validou 23 migrations;
- V23 adiciona `profile.heroBackgroundImageUrl`;
- Feature 4 notifications/email está DONE e V21 está em produção;
- Global UI Redesign foi promovido por PR #44 -> `dev` e PR #45 -> `main`, seguido de fixes visuais.

## Produção Conhecida

| Componente | Valor operacional documentável |
| --- | --- |
| Cloudflare Pages project | `saltos-nas-palhacadas-prod` |
| URL público | `https://www.saltosnaspalhacadas.pt` |
| Apex | `https://saltosnaspalhacadas.pt` redireciona 301 para `www` |
| URL Pages temporário | `https://saltos-nas-palhacadas-prod.pages.dev` |
| Cloud Run project | `saltos-prod-gmesquita` |
| Região Cloud Run | `europe-west1` |
| Service | `saltos-backend` |
| Cloud Run revision | `saltos-backend-00017-88t` |
| Imagem | `backend:55056d5` |
| Código de produção | `main` em `55056d5`; revisão Cloud Run `saltos-backend-00017-88t` |
| Tráfego | 100% |
| Health | UP |
| Recursos Cloud Run | 1 CPU, 1 GiB RAM, concurrency 80, max 2, scale-to-zero, startup CPU boost |
| Neon branch | `production/default` |
| R2 buckets | `saltos-prod-public`, `saltos-prod-private` |
| R2 backup bucket | `saltos-prod-backup` |
| Schedulers enabled | `saltos-r2-backup-daily`, `saltos-private-media-cleanup`, `saltos-booking-reminders` |

Sem segredos reais nesta documentação.
