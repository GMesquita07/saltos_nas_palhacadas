# Operações

Last verified: 2026-09-15.

## Checks Diários

- Confirmar saúde do backend em `/actuator/health`.
- Rever logs Cloud Run por erros 5xx, falhas R2, falhas Neon e falhas Turnstile.
- Confirmar execução do Scheduler `saltos-private-media-cleanup`.
- Verificar métricas de Cloud Run: cold starts, latência, memória e erros.
- Rever quota/uso de Neon e R2.

## Deploy Frontend

1. Garantir que o código está em branch aprovada.
2. Executar validação local ou confirmar CI.
3. Cloudflare Pages usa root `frontend`, build `npm run build`, output `dist`.
4. Confirmar `VITE_API_URL` e `VITE_TURNSTILE_SITE_KEY`.
5. Validar `_headers` no `dist` depois do build.
6. Confirmar login real com Turnstile no URL publicado.

Rollback: usar deployment anterior no Cloudflare Pages.

## Deploy Backend

1. Build da imagem a partir do commit aprovado.
2. Deploy para Cloud Run `saltos-backend` em `europe-west1`.
3. Confirmar que secrets são injetados por runtime, não embutidos na imagem.
4. Confirmar startup sem falhas do `ProductionSecurityVerifier`.
5. Confirmar `/actuator/health`.
6. Fazer smoke test de auth, perfis, media pública e endpoint protegido.

Rollback: reverter tráfego para revisão anterior em Cloud Run.

## Scheduler

Ativo:

- `saltos-private-media-cleanup`
- `POST /internal/maintenance/private-media-cleanup`
- `30 3 * * *`
- `Europe/Lisbon`

Verificações:

- O request deve ter OIDC e `X-Maintenance-Key`.
- Ausência ou erro da chave deve devolver 403.
- O endpoint deve devolver JSON com `processed`.

Não criar ainda o Scheduler de booking reminders. Ordem obrigatória:

1. Brevo domínio verificado.
2. SMTP credentials configuradas.
3. `BOOKING_EMAIL_ENABLED=true`.
4. Testar recuperação de password real.
5. Testar emails de booking.
6. Só depois criar Scheduler `POST /internal/maintenance/booking-reminders`.

## Turnstile

Verificar:

- Widget carrega no frontend.
- CSP permite `https://challenges.cloudflare.com` em `script-src` e `frame-src`.
- Backend tem `TURNSTILE_ENABLED=true`.
- `TURNSTILE_ALLOWED_HOSTNAMES` inclui o hostname atual e, depois, o domínio final.
- Falhas devolvem mensagem genérica sem token/secret em logs.

## Neon e Migrations

- Produção usa SSL.
- `ddl-auto=validate`.
- Migrations devem ser Flyway.
- Antes de deploy com schema novo, verificar ordem e irreversibilidade.
- Depois do deploy, confirmar que não há falhas Flyway nos logs.

## R2 e Media

Smoke tests úteis:

- Upload privado autenticado.
- Download privado pelo owner.
- Download privado por outro cliente deve falhar sem revelar existência.
- Aprovação admin publica para bucket público.
- `GET /api/v1/media/{key}` devolve media pública sem redirect para endpoint R2.
- Cleanup apaga uploads privados órfãos expirados.

Operação pendente: confirmar versioning/lifecycle e limpar objetos de teste.

## SMTP/Brevo

Antes de ativar:

- Confirmar delegação DNS Cloudflare ativa.
- Confirmar TXT/DKIM/DMARC/CNAMEs Brevo públicos.
- Criar credentials SMTP.
- Configurar Cloud Run com host, porta 587, STARTTLS, username, password e from.
- Fazer E2E de forgot-password e bookings.

Não ativar booking reminders antes destes testes.

## Logs e Incidentes

Não colocar em logs:

- passwords
- JWTs
- Turnstile tokens
- secrets
- connection strings completas
- conteúdo completo de pedidos sensíveis

Runbook detalhado: [Incident Response](../INCIDENT_RESPONSE.md).

## Backups e Recovery

Runbook detalhado: [Disaster Recovery](../DISASTER_RECOVERY.md).

Estado pendente:

- confirmar backups Neon/PITR
- confirmar versioning/lifecycle R2
- executar restore drill trimestral
