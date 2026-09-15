# Produção

Last verified: 2026-09-15.

## Arquitetura Real

| Camada | Provider | Estado |
| --- | --- | --- |
| Frontend | Cloudflare Pages | Ativo em URL temporário |
| Backend | Google Cloud Run | Ativo |
| Base de dados | Neon PostgreSQL | Ativo |
| Media | Cloudflare R2 | Ativo |
| DNS/CDN/TLS | Cloudflare | Domínio em progresso |
| Jobs | Google Cloud Scheduler | Cleanup ativo |
| Secrets | Google Secret Manager | Ativo |
| Email | Brevo | Em progresso |
| Anti-bot | Cloudflare Turnstile | Ativo e validado |

## Frontend

| Campo | Valor |
| --- | --- |
| Provider | Cloudflare Pages |
| Projeto | `saltos-nas-palhacadas-prod` |
| URL temporário | `https://saltos-nas-palhacadas-prod.pages.dev` |
| Production branch atual | `feat/production-launch` |
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
| Imagem | `backend:ee8d9c1` |
| Revisão validada | `saltos-backend-00008-f8p` |
| Tráfego | 100% |
| Scaling | min 0, max 2 |
| CPU/memória | 1 CPU, 1 GiB |
| Billing | request-based |
| Health | `/actuator/health` |

Produção deve correr com `SPRING_PROFILES_ACTIVE=prod`. Nesse profile:

- `spring.jpa.hibernate.ddl-auto=validate`
- `app.security.hsts.enabled=true` por default
- `app.security.require-database-ssl=true` por default
- `app.booking.reminder.cron=-`
- `app.media.client-content.cleanup-cron=-`

## Neon

| Campo | Valor documentável |
| --- | --- |
| Projeto | `saltos-production` |
| Branch | `production` |
| Database | `neondb` |
| Região | AWS Frankfurt / `eu-central-1` |
| SSL | Obrigatório |
| Schema | Flyway até V19 |

Não documentar passwords, connection strings completas ou hosts privados.

## Cloudflare R2

| Bucket | Uso |
| --- | --- |
| `saltos-prod-public` | Media publicada/aprovada |
| `saltos-prod-private` | Uploads pendentes, uploads de clientes e media privada |

Configuração conhecida: jurisdição EU, storage class Standard.

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

## Scheduler

Job ativo:

| Campo | Valor |
| --- | --- |
| Nome | `saltos-private-media-cleanup` |
| Região | `europe-west1` |
| Schedule | `30 3 * * *` |
| Timezone | `Europe/Lisbon` |
| Endpoint | `POST /internal/maintenance/private-media-cleanup` |
| Auth | OIDC com service account dedicado + `X-Maintenance-Key` |

O fluxo Scheduler -> Cloud Run -> endpoint -> 200 foi validado.

O Scheduler de reminders ainda não existe e não deve ser criado antes de SMTP real estar ativo e validado. Depois disso, o endpoint previsto é `POST /internal/maintenance/booking-reminders`, com schedule pretendido `0 9 * * *` em `Europe/Lisbon`.

## Brevo e Email

Estado atual:

| Item | Estado |
| --- | --- |
| Conta/setup Brevo | IN PROGRESS |
| Domínio Brevo | `saltosnaspalhacadas.pt` |
| Branded subdomain | `mail.saltosnaspalhacadas.pt` |
| DNS records preparados | DONE |
| DNS público autoritativo | PENDING DOMAIN DELEGATION |
| Domain verification | PENDING |
| SMTP credentials/config | PENDING |
| `BOOKING_EMAIL_ENABLED` | Ainda false |
| Password reset email real | PENDING E2E |

Registos preparados na Cloudflare: TXT domínio, DKIM 1, DKIM 2, DMARC, `mail`, `r.mail`, `img.mail`. CNAMEs Brevo ficam DNS only.

Quando SMTP for ativado, usar Brevo relay na porta 587 com STARTTLS e remetente pretendido `no-reply@saltosnaspalhacadas.pt`. O contacto público `ola@saltosnaspalhacadas.pt` ainda precisa decisão de inbound email; Brevo SMTP não é mailbox inbound.

## Domínio

| Item | Estado |
| --- | --- |
| Domínio | `saltosnaspalhacadas.pt` |
| Registrar | Dominios.pt |
| Nameservers configurados | `brenna.ns.cloudflare.com`, `pedro.ns.cloudflare.com` |
| Registo | DONE |
| Delegação DNS para Cloudflare | PENDING / VERIFY |
| Custom domain Pages | PENDING |
| Canonical pretendido | `https://www.saltosnaspalhacadas.pt` |

Pretendido:

- `www` aponta para Cloudflare Pages.
- apex redireciona para `www` preservando path/query.
- TLS gerido por Cloudflare.

Não afirmar que `www` está ativo enquanto a delegação pública não for confirmada.

## Secret Manager

Nomes/funções documentáveis:

- `admin-password`
- `db-password`
- `jwt-secret`
- `maintenance-api-key`
- `r2-access-key-id`
- `r2-secret-access-key`
- `turnstile-secret`

Nunca executar ou documentar comandos que imprimam valores de secrets.

## Deploy e Rollback

Frontend:

1. Fazer merge pelo fluxo `feat/production-launch -> dev -> main`.
2. Confirmar CI/CodeQL.
3. Cloudflare Pages deve construir `frontend` com `npm run build`.
4. Após lançamento final, mudar production branch para `main`.
5. Rollback: reverter para deployment anterior em Cloudflare Pages.

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

## Não Confirmado

- Política real de backups Neon/PITR.
- Versioning/lifecycle nos buckets R2.
- Estado público final da delegação DNS.
- Validação final Brevo depois da propagação DNS.
- Política de cleanup no Artifact Registry.
