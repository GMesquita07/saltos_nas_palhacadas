# Plano de Recuperação e Backups

Runbook para recuperar o site **Saltos nas Palhaçadas** após falha de deploy, perda de dados, indisponibilidade de provider ou incidente de segurança.

Last verified: 2026-09-17.

## Componentes Críticos

| Componente | Provider atual | Conteúdo crítico |
| --- | --- | --- |
| Base de dados | Neon PostgreSQL | Contas, perfis, portfólio, bookings, reviews, materiais, contactos, metadata de media |
| Media | Cloudflare R2 | Fotos, vídeos, thumbnails, avatares e uploads privados |
| Backend | Google Cloud Run | Revisões da API Spring Boot |
| Frontend | Cloudflare Pages | Deploys estáticos e assets |
| Secrets | Google Secret Manager | JWT, DB password, R2 keys, maintenance key, Turnstile secret |
| DNS/TLS | Cloudflare | Zona, records, TLS e redirects |

## Backups

| Área | Estado | Ação necessária |
| --- | --- | --- |
| Neon | VALIDATED | PITR/history observado de 6 horas; snapshot durável `pre-launch-2026-09-16`; restore drill em branch isolada validado. |
| R2 runtime | VALIDATED | Buckets runtime sem lifecycle genérico nem bucket lock para permitir delete/move da aplicação. |
| R2 backup | VALIDATED | Bucket `saltos-prod-backup`, snapshots independentes, `rclone check` 0 diferenças, restore de PNG validado. |
| Secret Manager | ONGOING PROCESS | Manter inventário de nomes e rotação; nunca exportar valores para docs. |
| Cloud Run | DONE técnico | Rollback por revisão anterior. |
| Cloudflare Pages | DONE técnico | Rollback por deployment anterior. |
| DNS Cloudflare | ACTIVE | `www` live com HTTPS; apex 301 para `www`; registar configuração sem secrets quando necessário. |

## Restore DB Neon

1. Identificar snapshot ou ponto no tempo válido.
2. Restaurar para branch/projeto isolado primeiro.
3. Configurar ambiente temporário da API com a base restaurada.
4. Confirmar Flyway/schema.
5. Validar login admin, conta cliente, perfis, bookings e reviews.
6. Só promover para produção depois de validação.

## Recovery Media R2

1. Identificar objetos afetados e bucket runtime (`public` ou `private`).
2. Restaurar a partir de `saltos-prod-backup` quando aplicável.
3. Validar que metadata da DB aponta para os objetos esperados.
4. Confirmar que media pública responde em `/api/v1/media/{key}`.
5. Confirmar que media privada exige auth e owner/admin.

O backup R2 usa snapshots em `snapshots/<CLOUD_RUN_EXECUTION>/public` e `private`, com bucket lock de 30 dias e lifecycle de delete aos 35 dias no bucket de backup.

## Rollback Cloud Run

1. Identificar revisão anterior saudável.
2. Mover tráfego para essa revisão.
3. Confirmar `/actuator/health`.
4. Validar smoke tests de auth, perfis, media e bookings.
5. Rever logs para confirmar estabilização.

## Rollback Cloudflare Pages

1. Escolher deployment anterior no dashboard Pages.
2. Promover rollback.
3. Confirmar `_headers`, CSP e Turnstile.
4. Validar login e navegação principal.

## Secrets

Em caso de perda/compromisso:

1. Rodar segredo no provider de origem.
2. Atualizar Google Secret Manager.
3. Redeploy Cloud Run.
4. Invalidar tokens/sessões quando aplicável.
5. Registar data, causa e componentes afetados.

Nunca documentar valores de secrets ou connection strings completas.

## Restore Drill Trimestral

Executar pelo menos trimestralmente:

- restore Neon para ambiente isolado;
- validação de uma conta, perfil, booking e review;
- validação de media pública e privada;
- validação de rollback Cloud Run e Pages;
- revisão do inventário de secrets;
- registo de duração, falhas e ações corretivas.

## Validação Pós-Restore

- Health backend OK.
- Login admin e cliente OK.
- Páginas públicas carregam perfis, materiais e contactos.
- Upload privado e download owner/admin OK.
- Media pública sem auth OK.
- Booking e disponibilidade OK.
- Turnstile OK em login real.
- Logs sem stack traces nem secrets.
