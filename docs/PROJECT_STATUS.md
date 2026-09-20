# Estado do Projeto

Last verified: 2026-09-18.

## Resumo

O lançamento técnico de produção está completo. A stack principal está em Cloudflare Pages, Google Cloud Run, Neon PostgreSQL, Cloudflare R2, Google Cloud Scheduler, Google Secret Manager, Cloudflare Turnstile e Brevo SMTP. O domínio `www.saltosnaspalhacadas.pt` responde HTTP 200, o apex redireciona com HTTP 301 para `www`, o backend está UP, os schedulers estão enabled, os backups R2 executaram com sucesso, CI/PostgreSQL CI/CodeQL passaram na release final `main` e o smoke test manual de produção passou.

## Estado por Área

| Área | Estado | Produção? | Validado? | Próximo passo |
| --- | --- | --- | --- | --- |
| Frontend Cloudflare Pages | DONE | Sim, branch `main` | Sim | Monitorização pós-lançamento |
| Backend Cloud Run | DONE | Sim | Sim | Health UP; monitorização pós-lançamento |
| Neon PostgreSQL | DONE | Sim | Sim | Manter snapshot pre-launch e PITR observado |
| Flyway V1-V20 | DONE | Sim | Sim | Produção técnica atual antes da Feature 4 |
| Flyway V21 profile notification email | IN PROGRESS | Não | Em testes na branch | `profiles.notification_email` privado; não production-complete |
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
| Notificações admin/artista | IN PROGRESS | Não | Em testes na branch | Usa admins ativos da DB e email privado por perfil |
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
| Search Console | POST-LAUNCH | Não | Não | Submeter/verificar |
| SEO por perfil | POST-LAUNCH | Parcial | Não | Rotas reais existem; falta Search Console/sitemap e eventual pré-render |

## Branches e Release

| Branch | SHA conhecido | Papel |
| --- | --- | --- |
| `main` | release final validada | Production branch do Cloudflare Pages |
| `dev` | integrado | Branch de integração |
| `feat/production-launch` | integrado | Branch de lançamento já promovida |
| `feat/notifications-and-email` | em desenvolvimento | Feature 4: notificações email admin/artista |

Fluxo concluído:

```text
feat/production-launch
  -> PR para dev
  -> CI/CodeQL/revisão
  -> PR final para main
  -> Cloudflare Pages production branch main
```

Distinção importante:

- código/imagem da aplicação atualmente em produção: baseado em `ee8d9c1`;
- revisões posteriores do Cloud Run alteraram apenas configuração/secrets;
- Cloudflare Pages production branch agora é `main`.
- produção conhecida antes desta feature está em Flyway V20; V21 é alteração desta branch e ainda não foi promovida.

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
| Imagem | `backend:ee8d9c1` |
| Código de produção | baseado em `ee8d9c1`; revisões posteriores só config/secrets |
| Tráfego | 100% |
| Health | UP |
| Recursos Cloud Run | 1 CPU, 1 GiB RAM, concurrency 80, max 2, scale-to-zero, startup CPU boost |
| Neon branch | `production/default` |
| R2 buckets | `saltos-prod-public`, `saltos-prod-private` |
| R2 backup bucket | `saltos-prod-backup` |
| Schedulers enabled | `saltos-r2-backup-daily`, `saltos-private-media-cleanup`, `saltos-booking-reminders` |

Sem segredos reais nesta documentação.
