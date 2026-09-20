# Decisões Técnicas

Last verified: 2026-09-17.

## Cloudflare Pages para Frontend

Status: Accepted.

Contexto: o frontend é uma SPA React/Vite estática.

Razão: deploy simples, CDN global, TLS e `_headers` com CSP/HSTS.

Consequências: a production branch do Cloudflare Pages é agora `main`; deep links SPA usam `_redirects` explícito para rotas conhecidas. SEO por perfil ainda pode exigir sitemap e eventual pré-render, apesar de já existirem URLs reais.

## Google Cloud Run para Backend

Status: Accepted.

Contexto: a API é Spring Boot stateless e usa PostgreSQL/R2 externos.

Razão: runtime gerido, escala para zero e rollback por revisão.

Consequências: tarefas agendadas não podem depender exclusivamente de `@Scheduled` em produção.

## Neon PostgreSQL

Status: Accepted.

Contexto: a aplicação usa JPA/Flyway e PostgreSQL.

Razão: base gerida separada para produção, SSL e migrations controladas.

Consequências: produção usa snapshot manual durável `pre-launch-2026-09-16`, PITR/history observado de 6 horas no plano atual e restore drill validado em branch isolada.

## Cloudflare R2 com Buckets Separados

Status: Accepted.

Contexto: Cloud Run não fornece filesystem persistente.

Razão: separar media pública de uploads privados/pendentes reduz exposição acidental e mantém URLs servidos pela API.

Consequências: em prod `MEDIA_STORAGE_PROVIDER` deve ser `r2`; bucket público e privado não podem ser iguais.

## Cloud Scheduler para Maintenance

Status: Accepted.

Contexto: Cloud Run pode escalar para zero, logo cron interno não é garantia operacional.

Razão: Scheduler externo aciona endpoints internos com OIDC e `X-Maintenance-Key`.

Consequências: `@Scheduled` fica para dev/test; prod usa cron `-` em `application-prod.properties`. Scheduler externo aciona cleanup privado, booking reminders e backup R2.

## R2 Backup Separado

Status: Accepted / validated.

Contexto: os buckets runtime precisam de apagar e mover objetos, por isso não podem ter bucket lock/lifecycle agressivo.

Razão: usar um bucket separado `saltos-prod-backup`, credenciais separadas e Cloud Run Job com `rclone copy --immutable` permite snapshots independentes sem dar acesso de backup à aplicação principal.

Consequências: backups ficam em `snapshots/<CLOUD_RUN_EXECUTION>/public` e `private`, com bucket lock de 30 dias e lifecycle delete aos 35 dias. O Scheduler do backup corre às 02:30 Europe/Lisbon, antes do cleanup privado às 03:30.

## Brevo SMTP em Produção

Status: Accepted / validated.

Contexto: forgot/reset password e notificações de booking precisam de email transacional real.

Razão: Brevo foi autenticado para `saltosnaspalhacadas.pt` com DKIM/DMARC e SMTP 587 STARTTLS.

Consequências: booking reminders já podem correr em produção; continuar a monitorizar entregabilidade e manter SMTP password no Secret Manager.

## Turnstile em Auth Pública

Status: Accepted.

Contexto: login, registo e forgot-password são superfícies públicas de abuso.

Razão: Cloudflare Turnstile reduz bots mantendo validação server-side.

Consequências: CSP do frontend tem de permitir `https://challenges.cloudflare.com`; backend tem de validar success, action e hostname.

## JWT Atual

Status: Accepted with known limitation.

Contexto: frontend guarda JWT em `sessionStorage`.

Razão: implementação simples e stateless.

Consequências: não há cookies HttpOnly/CSRF neste modelo. Futuro hardening pode migrar para cookies seguros.

## Flyway para Schema

Status: Accepted.

Contexto: produção usa `ddl-auto=validate`.

Razão: alterações de schema auditáveis e previsíveis.

Consequências: toda alteração persistente deve vir numa migration versionada.

## Render como Histórico/Alternativo

Status: Historical.

Contexto: `render.yaml` existe no repo.

Razão: foi útil em fase anterior/staging/alternativa.

Consequências: não deve ser apresentado como arquitetura principal de produção.

## Estratégia Híbrida de Conteúdo

Status: Planned / not implemented.

Contexto: algumas áreas públicas mudam pouco e podem consumir backend/storage sem necessidade.

Razão: reduzir custo e dependência de Neon, Cloud Run e R2 para conteúdo quase estático.

Consequências propostas:

- mover perfis públicos, fotos principais, textos institucionais e FAQ para assets/dados estáticos quando fizer sentido;
- manter dinâmico auth, bookings, favoritos, reviews, moderação e dados operacionais;
- avaliar avatares predefinidos em vez de upload livre, guardando apenas IDs como `avatar-03`.

Nada desta decisão está implementado nesta tarefa documental.
