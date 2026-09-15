# Estado do Projeto

Last verified: 2026-09-15, branch `feat/production-launch`, HEAD `ee8d9c1`.

## Resumo

O projeto está em preparação final de lançamento. A stack principal de produção já foi migrada para Cloudflare Pages, Google Cloud Run, Neon PostgreSQL, Cloudflare R2 e Google Cloud Scheduler. Turnstile e R2 foram validados end-to-end. Os bloqueadores principais são domínio/custom domain, Brevo/SMTP, reminder scheduler, testes finais e merge para `main`.

## Estado por Área

| Área | Estado | Produção? | Validado? | Próximo passo |
| --- | --- | --- | --- | --- |
| Frontend Cloudflare Pages | DONE | Sim, branch temporária | Sim | Após release, mudar production branch para `main` |
| Backend Cloud Run | DONE | Sim | Sim | Monitorização pós-lançamento |
| Neon PostgreSQL | DONE | Sim | Sim | Confirmar política de backups/restore drill |
| Flyway V1-V19 | DONE | Sim | Sim | Manter migrations-only em produção |
| Cloudflare R2 public/private | DONE | Sim | Sim | Limpar objetos de teste e confirmar lifecycle/versioning |
| Uploads 10/30 MiB | DONE | Sim | Sim | Monitorizar erros 413/validação |
| Turnstile | DONE | Sim | Sim | Adicionar hostname final quando domínio estiver ativo |
| CSP Turnstile | DONE | Sim | Sim | Manter `challenges.cloudflare.com` em script/frame |
| Cleanup Scheduler | DONE | Sim | Sim | Monitorizar execuções diárias |
| Booking reminders internos | DONE local/dev | Não em prod | Sim em testes | Manter cron prod desativado com `-` |
| Booking reminder Scheduler | BLOCKED | Não | Não | Ativar só depois de SMTP real validado |
| Brevo DNS preparado | IN PROGRESS | Parcial | Não confirmado publicamente | Aguardar delegação DNS e validar domínio |
| SMTP Brevo | TODO | Não | Não | Criar credenciais, configurar Cloud Run e testar |
| Password reset email real | TODO | Não confirmado | Não | E2E depois de SMTP |
| Domínio registado | DONE | Sim | Sim pelo setup | Confirmar propagação pública |
| Delegação DNS Cloudflare | IN PROGRESS | Não confirmado | Não | Verificar nameservers públicos |
| Custom domain Pages | TODO | Não | Não | Ligar `www` ao Pages depois da zona Active |
| Redirect apex -> www | TODO | Não | Não | Configurar preservando path/query |
| CI GitHub Actions | DONE | Sim | Sim | Continuar PRs via `dev`/`main` |
| CodeQL | DONE | Sim | Sim | Monitorizar alertas |
| Dependabot | DONE | Sim | Sim | Rever PRs semanais |
| Backups | PENDING | Não confirmado | Não | Confirmar Neon/R2 e fazer restore drill |
| RGPD export/delete | DONE técnico | Sim | Sim em código | Revisão jurídica |
| Privacy/Terms/Cookies | DONE técnico | Sim | Sim em código | Revisão jurídica e UX |
| Search Console | TODO | Não | Não | Submeter após domínio final |
| SEO por perfil | POST-LAUNCH | Não | Não | Criar rotas reais por animador |

## Branches e Release

| Branch | SHA conhecido | Papel |
| --- | --- | --- |
| `main` | `675d8fa` | Produção final pretendida após PR final |
| `dev` | `b16c9f4` | Integração antes de `main` |
| `feat/production-launch` | `ee8d9c1` | Release branch atual |

Fluxo previsto:

```text
feat/production-launch
  -> PR para dev
  -> CI/CodeQL/revisão
  -> PR final para main
  -> mudar Cloudflare Pages production branch para main
```

## Produção Conhecida

| Componente | Valor operacional documentável |
| --- | --- |
| Cloudflare Pages project | `saltos-nas-palhacadas-prod` |
| URL temporário | `https://saltos-nas-palhacadas-prod.pages.dev` |
| Cloud Run project | `saltos-prod-gmesquita` |
| Região Cloud Run | `europe-west1` |
| Service | `saltos-backend` |
| Imagem | `backend:ee8d9c1` |
| Revisão | `saltos-backend-00008-f8p` |
| Tráfego | 100% |
| Neon project/branch/database | `saltos-production` / `production` / `neondb` |
| R2 buckets | `saltos-prod-public`, `saltos-prod-private` |
| Scheduler ativo | `saltos-private-media-cleanup`, `30 3 * * *`, `Europe/Lisbon` |

Sem segredos reais nesta documentação.
