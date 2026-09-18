# Produção

Last verified: 2026-09-18.

## Arquitetura Real

| Camada | Provider | Estado |
| --- | --- | --- |
| Frontend | Cloudflare Pages | Ativo em `https://www.saltosnaspalhacadas.pt` |
| Backend | Google Cloud Run | Ativo |
| Base de dados | Neon PostgreSQL | Ativo |
| Media | Cloudflare R2 | Ativo |
| DNS/CDN/TLS | Cloudflare | `www` live com HTTPS; apex redireciona para `www` |
| Jobs | Google Cloud Scheduler | Cleanup, booking reminders e R2 backup ativos |
| Secrets | Google Secret Manager | Ativo |
| Email | Brevo | Ativo e validado |
| Anti-bot | Cloudflare Turnstile | Ativo e validado |

## Frontend

| Campo | Valor |
| --- | --- |
| Provider | Cloudflare Pages |
| Projeto | `saltos-nas-palhacadas-prod` |
| URL público | `https://www.saltosnaspalhacadas.pt` |
| URL Pages temporário | `https://saltos-nas-palhacadas-prod.pages.dev` |
| Production branch atual | `main` |
| Root | `frontend` |
| Build | `npm run build` |
| Output | `dist` |

Variáveis:

- `VITE_API_URL`
- `VITE_TURNSTILE_SITE_KEY`

`VITE_TURNSTILE_SITE_KEY` é pública por definição. Nenhum secret deve usar prefixo `VITE_`.

O ficheiro `frontend/public/_headers` define headers de segurança para Pages. A CSP permite `https://challenges.cloudflare.com` em `script-src` e `frame-src` para Turnstile; `connect-src` continua como `'self' https:`.

## Backend

| Campo | Valor |
| --- | --- |
| GCP project | `saltos-prod-gmesquita` |
| Região | `europe-west1` |
| Cloud Run service | `saltos-backend` |
| Imagem/código da aplicação | baseado em `backend:ee8d9c1` |
| Revisões recentes | alterações apenas de ambiente/secrets |
| Tráfego | 100% |
| Scaling | scale-to-zero enabled, max 2 |
| CPU/memória | 1 CPU, 1 GiB |
| Concurrency | 80 |
| Startup CPU boost | Ativo |
| Billing | request-based |
| Health | `/actuator/health` |

O health endpoint de produção está UP. Recentes logs ERROR do Cloud Run foram verificados e estavam limpos durante a verificação final. Produção deve correr com `SPRING_PROFILES_ACTIVE=prod`. Nesse profile:

- `spring.jpa.hibernate.ddl-auto=validate`
- `app.security.hsts.enabled=true` por default
- `app.security.require-database-ssl=true` por default
- `app.booking.reminder.cron=-`
- `app.media.private-upload-cleanup-cron=-`

## Neon

| Campo | Valor documentável |
| --- | --- |
| Projeto | `saltos-production` |
| Branch | `production/default` |
| Database | `neondb` |
| Região | AWS Frankfurt / `eu-central-1` |
| SSL | Obrigatório |
| Schema | Flyway até V20 antes da Feature 4 |
| PITR/history observado no plano atual | 6 horas |
| Snapshot manual durável | `pre-launch-2026-09-16` |

Não documentar passwords, connection strings completas ou hosts privados.

Restore validado:

- foi criada uma branch isolada a partir do snapshot `pre-launch-2026-09-16`;
- a branch `production/default` não foi modificada;
- dados restaurados e migrations Flyway foram inspecionados com sucesso;
- a branch temporária de restore foi apagada no fim.

## Cloudflare R2

| Bucket | Uso |
| --- | --- |
| `saltos-prod-public` | Media publicada/aprovada |
| `saltos-prod-private` | Uploads pendentes, uploads de clientes e media privada |

Configuração conhecida: jurisdição EU, storage class Standard, public development URLs desativados, sem lifecycle genérico de expiração e sem bucket locks nos buckets runtime. Os buckets runtime não devem herdar regras de retenção do bucket de backup porque a aplicação precisa de apagar/mover objetos.

Validado E2E:

- upload privado
- download privado autenticado
- publish/copy para bucket público
- download público sem auth

Limites efetivos:

- imagens: 10 MiB
- vídeos: 30 MiB
- multipart file: 30 MB
- multipart request: 31 MB

Em produção, `MEDIA_STORAGE_PROVIDER` tem de estar explicitamente definido como `r2`; `local` falha no startup porque Cloud Run não fornece storage persistente.

## R2 Backup e Disaster Recovery

Bucket de backup:

| Campo | Valor |
| --- | --- |
| Bucket | `saltos-prod-backup` |
| Jurisdição | EU |
| Acesso app backend | Nenhum |
| Prefixo de snapshots | `snapshots/<CLOUD_RUN_EXECUTION>/public` e `snapshots/<CLOUD_RUN_EXECUTION>/private` |

Credenciais separadas:

- source token read-only, apenas para `saltos-prod-public` e `saltos-prod-private`;
- destination token read/write, apenas para `saltos-prod-backup`;
- quatro credenciais de backup guardadas como secrets separados no Google Secret Manager;
- runtime service account do backup tem `secretAccessor` apenas nesses quatro secrets.

Implementação:

- `ops/r2-backup/Dockerfile`
- `ops/r2-backup/backup.sh`
- rclone `1.75.1`
- Artifact Registry image: `europe-west1-docker.pkg.dev/saltos-prod-gmesquita/saltos-backend/r2-backup:1.75.1`
- Cloud Run Job: `saltos-r2-backup`
- runtime service account: `saltos-r2-backup-runtime@saltos-prod-gmesquita.iam.gserviceaccount.com`

Comportamento validado:

- usa `rclone copy`, não `sync`;
- usa `--immutable`;
- executa `rclone check` depois de copiar public e private;
- execução manual do Cloud Run Job sucedeu;
- execução via Scheduler sucedeu end-to-end;
- restore drill restaurou um objeto do bucket de backup e verificou PNG válido;
- `rclone check` reportou 0 diferenças.

Retenção do prefixo `snapshots/` em `saltos-prod-backup`:

- Bucket Lock `protect-snapshots-30d`, retenção 30 dias;
- lifecycle `delete-snapshots-35d`, apaga objetos após 35 dias;
- regra default de abortar multipart uploads incompletos continua ativa.

## Scheduler

Jobs ativos:

| Job | Região | Schedule | Timezone | Alvo | Auth |
| --- | --- | --- | --- | --- | --- |
| `saltos-r2-backup-daily` | `europe-west1` | `30 2 * * *` | `Europe/Lisbon` | Cloud Run Job `saltos-r2-backup` | OAuth + Scheduler service account |
| `saltos-private-media-cleanup` | `europe-west1` | `30 3 * * *` | `Europe/Lisbon` | `POST /internal/maintenance/private-media-cleanup` | OIDC + Scheduler service account + `X-Maintenance-Key` |
| `saltos-booking-reminders` | `europe-west1` | `0 9 * * *` | `Europe/Lisbon` | `POST /internal/maintenance/booking-reminders` | OIDC + Scheduler service account + `X-Maintenance-Key` |

Os três jobs foram executados manualmente com sucesso. O backup também foi acionado via Scheduler e criou/concluiu uma execução do Cloud Run Job. O cron interno da aplicação continua desativado em produção.

`MAINTENANCE_API_KEY` fica no Secret Manager. A documentação não deve registar o valor.

## Brevo e Email

Estado atual:

| Item | Estado |
| --- | --- |
| Conta/setup Brevo | DONE |
| Domínio Brevo | `saltosnaspalhacadas.pt` |
| Domain authentication | DONE |
| DKIM | Validado |
| DMARC | Validado |
| SMTP | Brevo SMTP porta 587 com STARTTLS |
| Sender | `Saltos nas Palhaçadas <no-reply@saltosnaspalhacadas.pt>` |
| SMTP password | Secret Manager |
| Password reset canonical URL | `https://www.saltosnaspalhacadas.pt` |

Validações E2E em produção:

- forgot/reset password;
- booking received email;
- booking accepted email;
- booking cancellation email.

O contacto público `ola@saltosnaspalhacadas.pt` ainda precisa decisão de inbound email; Brevo SMTP não é automaticamente mailbox inbound.

Feature 4 em desenvolvimento na branch `feat/notifications-and-email` mantém Brevo SMTP e adiciona:

- `V21__profile_notification_email.sql` com `profiles.notification_email VARCHAR(254)` nullable;
- email privado por artista usado apenas para notificações operacionais;
- notificações para todos os admins ativos da DB;
- wording PT-PT atualizado em emails de booking.

Esta feature ainda não está production-complete nem promovida a produção.

## Domínio

| Item | Estado |
| --- | --- |
| Domínio | `saltosnaspalhacadas.pt` |
| Registrar | Dominios.pt |
| Nameservers configurados | `brenna.ns.cloudflare.com`, `pedro.ns.cloudflare.com` |
| Registo | DONE |
| `www` HTTPS | DONE |
| Apex redirect | 301 para `https://www.saltosnaspalhacadas.pt`, preservando query strings |
| Pages production branch | `main` |
| Confirmação administrativa .PT | PENDING externo |
| Canonical | `https://www.saltosnaspalhacadas.pt` |

O domínio está funcional para DNS/TLS/site. A confirmação administrativa do registrante .PT é um pendente externo separado da disponibilidade técnica do domínio.

## Secret Manager

Nomes/funções documentáveis:

- `admin-password`
- `db-password`
- `jwt-secret`
- `maintenance-api-key`
- `r2-access-key-id`
- `r2-secret-access-key`
- `turnstile-secret`
- quatro secrets separados para credenciais do R2 backup
- SMTP password Brevo

Nunca executar ou documentar comandos que imprimam valores de secrets.

## Deploy e Rollback

Frontend:

1. Estado atual: Cloudflare Pages production branch é `main`.
2. A release final `main` passou CI/PostgreSQL CI/CodeQL.
3. Cloudflare Pages constrói `frontend` com `npm run build`.
4. Rollback: reverter para deployment anterior em Cloudflare Pages.

Backend:

1. Construir imagem a partir do commit aprovado.
2. Deploy no Cloud Run mantendo secrets fora da imagem.
3. Confirmar `/actuator/health`.
4. Confirmar logs sem erros de startup.
5. Rollback: voltar para revisão anterior do Cloud Run.

Migrations:

- Nunca usar `ddl-auto` para alterar schema em produção.
- Alterações de schema devem ser migrations Flyway versionadas.
- Depois do deploy, confirmar que o schema está no nível esperado.

## Release Final

Concluído:

- CI, PostgreSQL CI e CodeQL passaram na release final `main`;
- Cloudflare Pages production deployment vem de `main`;
- produção manual smoke testing passou;
- Turnstile funciona em produção;
- R2 automated backup executions sucederam.

Ainda pós-lançamento:

- confirmação administrativa .PT externa;
- Google Search Console e sitemap;
- melhorias visuais/frontend futuras;
- otimização futura de media/assets estáticos.

## Artifact Registry

Cleanup policy ativa, não dry-run:

- apagar imagens com mais de 30 dias;
- manter pelo menos as 5 versões mais recentes.
