# Operações

Last verified: 2026-09-17.

## Checks Diários

- Confirmar saúde do backend em `/actuator/health`.
- Rever logs Cloud Run por erros 5xx, falhas R2, falhas Neon e falhas Turnstile.
- Confirmar execução dos Schedulers `saltos-r2-backup-daily`, `saltos-private-media-cleanup` e booking reminders.
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

Ativos:

| Job | Horário | Alvo |
| --- | --- | --- |
| `saltos-r2-backup-daily` | 02:30 Europe/Lisbon | Cloud Run Job `saltos-r2-backup` |
| `saltos-private-media-cleanup` | 03:30 Europe/Lisbon | `POST /internal/maintenance/private-media-cleanup` |
| `saltos-booking-reminders` | 09:00 Europe/Lisbon | `POST /internal/maintenance/booking-reminders` |

Verificações:

- Maintenance endpoints devem ter OIDC e `X-Maintenance-Key`.
- Ausência ou erro da chave deve devolver 403.
- O endpoint deve devolver JSON com `processed`.
- O backup usa OAuth para invocar o Cloud Run Job.
- Execuções manuais dos três jobs foram validadas.

## Turnstile

Verificar:

- Widget carrega no frontend.
- CSP permite `https://challenges.cloudflare.com` em `script-src` e `frame-src`.
- Backend tem `TURNSTILE_ENABLED=true`.
- `TURNSTILE_ALLOWED_HOSTNAMES` inclui o domínio oficial validado.
- Falhas devolvem mensagem genérica sem token/secret em logs.

## Neon e Migrations

- Produção usa SSL.
- `ddl-auto=validate`.
- Migrations devem ser Flyway.
- Antes de deploy com schema novo, verificar ordem e irreversibilidade.
- Depois do deploy, confirmar que não há falhas Flyway nos logs.
- Snapshot manual `pre-launch-2026-09-16` existe e não expira.
- PITR/history observado no plano atual: 6 horas.
- Restore drill já foi executado em branch isolada e apagada no fim.

## R2 e Media

Smoke tests úteis:

- Upload privado autenticado.
- Download privado pelo owner.
- Download privado por outro cliente deve falhar sem revelar existência.
- Aprovação admin publica para bucket público.
- `GET /api/v1/media/{key}` devolve media pública sem redirect para endpoint R2.
- Cleanup apaga uploads privados órfãos expirados.

Runtime buckets: public development URLs desativados, sem lifecycle genérico e sem bucket locks. Não aplicar retenção dos backups aos buckets runtime.

## R2 Backup

Verificar:

- Scheduler `saltos-r2-backup-daily` executou antes do cleanup privado.
- Cloud Run Job `saltos-r2-backup` concluiu.
- Snapshots criados em `snapshots/<CLOUD_RUN_EXECUTION>/public` e `snapshots/<CLOUD_RUN_EXECUTION>/private`.
- `rclone check` reportou 0 diferenças.
- Bucket lock `protect-snapshots-30d` e lifecycle `delete-snapshots-35d` continuam aplicados só ao prefixo `snapshots/` do bucket de backup.
- Runtime service account tem acesso apenas aos quatro secrets de backup necessários.

## SMTP/Brevo

Ativo e validado:

- domínio autenticado em Brevo;
- DKIM e DMARC validados;
- Cloud Run usa Brevo SMTP na porta 587 com STARTTLS;
- sender: `Saltos nas Palhaçadas <no-reply@saltosnaspalhacadas.pt>`;
- forgot/reset password, booking recebido, booking aceite e booking cancelado foram testados end-to-end em produção.

Monitorizar falhas SMTP e entregabilidade. Não escrever SMTP password em logs ou documentação.

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

Estado:

- Neon snapshot/restore validado.
- R2 backup, scheduler, restore drill e retenção validados.
- Manter restore drills periódicos.
