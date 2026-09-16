# Estado do Projeto

Last verified: 2026-09-16, branch `feat/production-launch`, branch HEAD `25383c0`.

## Resumo

O projeto está em preparação final de lançamento. A stack principal de produção já foi migrada para Cloudflare Pages, Google Cloud Run, Neon PostgreSQL, Cloudflare R2, Google Cloud Scheduler, Google Secret Manager, Cloudflare Turnstile e Brevo SMTP. O domínio `www.saltosnaspalhacadas.pt`, email transacional, schedulers de manutenção, restore Neon e backups R2 foram validados. O lançamento ainda não está concluído: faltam QA final, PRs, validações CI/CodeQL, switch de Pages para `main` e verificação pós-merge.

## Estado por Área

| Área | Estado | Produção? | Validado? | Próximo passo |
| --- | --- | --- | --- | --- |
| Frontend Cloudflare Pages | DONE | Sim, branch temporária | Sim | Após release, mudar production branch para `main` |
| Backend Cloud Run | DONE | Sim | Sim | Monitorização pós-lançamento |
| Neon PostgreSQL | DONE | Sim | Sim | Manter snapshot pre-launch e PITR observado |
| Flyway V1-V19 | DONE | Sim | Sim | Manter migrations-only em produção |
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
| Password reset email real | DONE | Sim | Sim | Canonical URL aponta para `www` |
| Domínio registado | DONE | Sim | Sim pelo setup | DNS/TLS já funcionais; confirmação administrativa .PT separada |
| Domínio `www` HTTPS | DONE | Sim | Sim | QA final antes da release |
| Redirect apex -> www | DONE | Sim | Sim | 301 preserva query strings |
| Confirmação administrativa .PT | PENDING | Externo | Não | Aguardar confirmação do registrante/administrador |
| CI GitHub Actions | DONE | Sim | Sim | Continuar PRs via `dev`/`main` |
| CodeQL | DONE | Sim | Sim | Monitorizar alertas |
| Dependabot | DONE | Sim | Sim | Rever PRs semanais |
| Backups Neon/R2 | DONE | Sim | Sim | Fazer novos drills periódicos |
| RGPD export/delete | DONE técnico | Sim | Sim em código | Revisão jurídica |
| Privacy/Terms/Cookies | DONE técnico | Sim | Sim em código | Revisão jurídica e UX |
| Search Console | POST-LAUNCH | Não | Não | Submeter após launch final |
| SEO por perfil | POST-LAUNCH | Não | Não | Criar rotas reais por animador |

## Branches e Release

| Branch | SHA conhecido | Papel |
| --- | --- | --- |
| `main` | `675d8fa` | Produção final pretendida após PR final |
| `dev` | `b16c9f4` | Integração antes de `main` |
| `feat/production-launch` | `25383c0` | Branch de lançamento atual |

Fluxo previsto:

```text
feat/production-launch
  -> PR para dev
  -> CI/CodeQL/revisão
  -> PR final para main
  -> mudar Cloudflare Pages production branch para main
```

Distinção importante:

- código/imagem da aplicação atualmente em produção: baseado em `ee8d9c1`;
- revisões posteriores do Cloud Run alteraram apenas configuração/secrets;
- branch HEAD atual de `feat/production-launch`: `25383c0`;
- Cloudflare Pages production branch ainda é `feat/production-launch`, não `main`.

## Produção Conhecida

| Componente | Valor operacional documentável |
| --- | --- |
| Cloudflare Pages project | `saltos-nas-palhacadas-prod` |
| URL público | `https://www.saltosnaspalhacadas.pt` |
| URL Pages temporário | `https://saltos-nas-palhacadas-prod.pages.dev` |
| Cloud Run project | `saltos-prod-gmesquita` |
| Região Cloud Run | `europe-west1` |
| Service | `saltos-backend` |
| Imagem | `backend:ee8d9c1` |
| Código de produção | baseado em `ee8d9c1`; revisões posteriores só config/secrets |
| Tráfego | 100% |
| Recursos Cloud Run | 1 CPU, 1 GiB RAM, concurrency 80, max 2, scale-to-zero, startup CPU boost |
| Neon branch | `production/default` |
| R2 buckets | `saltos-prod-public`, `saltos-prod-private` |
| R2 backup bucket | `saltos-prod-backup` |
| Schedulers ativos | cleanup 03:30, booking reminders 09:00, R2 backup 02:30, `Europe/Lisbon` |

Sem segredos reais nesta documentação.
