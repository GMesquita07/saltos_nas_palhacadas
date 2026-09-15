# Prontidão para Produção

Documento-resumo. A documentação detalhada está em [docs/PRODUCTION.md](docs/PRODUCTION.md), [docs/SECURITY.md](docs/SECURITY.md), [docs/OPERATIONS.md](docs/OPERATIONS.md) e [docs/ROADMAP.md](docs/ROADMAP.md).

Last verified: 2026-09-15, branch `feat/production-launch`, HEAD `ee8d9c1`.

## Estado Geral

| Área | Estado |
| --- | --- |
| Frontend Cloudflare Pages | DONE em URL temporário |
| Backend Google Cloud Run | DONE |
| Neon PostgreSQL | DONE |
| Cloudflare R2 | DONE |
| Turnstile | DONE/VALIDATED |
| Cleanup Scheduler | DONE/VALIDATED |
| Domínio final | IN PROGRESS |
| Brevo/SMTP | IN PROGRESS/TODO |
| Booking reminders em produção | BLOCKED BY SMTP |
| Backups/restore drill | PENDING |
| Final merge para `main` | TODO |

## Checklist Técnico

| Ponto | Estado | Notas |
| --- | --- | --- |
| API keys privadas | Coberto | Secrets em variáveis/provider/Secret Manager; não em frontend ou Git. |
| Separar dev/prod | Coberto | Profiles `dev`, `test`, `prod`; produção exige R2 e DB SSL. |
| Backups automáticos | PENDING | Confirmar Neon/PITR e R2 versioning/lifecycle; executar restore drill. |
| Forçar HTTPS | Coberto + provider | HSTS no backend/frontend; Cloudflare deve forçar HTTPS no domínio final. |
| Encriptação de dados sensíveis | Parcial | Passwords com hash; TLS/SSL/at-rest dependem dos providers; encriptação por campo não implementada. |
| Server-side auth | Coberto | Spring Security valida JWT e roles. |
| Restringir acesso | Coberto | Admin, owner checks e endpoints privados. |
| Bloquear mass assignment | Coberto | DTOs/records, mapeamento manual e unknown JSON rejeitado. |
| Cookies | N/A parcial | Auth atual usa JWT em `sessionStorage`, não cookies HttpOnly. |
| Hash passwords | Coberto | BCrypt; reset tokens com SHA-256. |
| Rate limiting | Coberto | App-level/in-memory por IP. Avaliar WAF Cloudflare. |
| Proteção bots | Coberto | Turnstile em login/registo/forgot-password. |
| Queries parametrizadas | Coberto | Spring Data/JPA com parâmetros. |
| Validar inputs | Coberto | Jakarta Validation, URL validation e validação de media. |
| Não expor dados sensíveis | Coberto | Responses públicas limitadas; errors sem stacktrace. |
| Restringir uploads | Coberto | MIME allowlist, magic bytes, UUID, tamanho e R2 privado antes de aprovação. |
| Limitar respostas da API | Coberto | Limites configuráveis por público/privado/admin. |
| Security headers | Coberto | Backend filter e `_headers` do Pages. |
| Dependency scan | Coberto | CI com npm audit, CodeQL e Dependabot; backend Dependency Check é opcional/manual. |
| Não confiar apenas em RLS | Coberto | Autorização na aplicação; RLS não é a fronteira principal. |

## Checklist Produto/SEO/Legal

| Ponto | Estado | Notas |
| --- | --- | --- |
| Privacy policy | DONE técnico | Rever juridicamente. |
| Terms page | DONE técnico | Rever juridicamente. |
| FAQ | DONE | SPA interna. |
| Clear CTA | DONE | Booking visível no header/perfis. |
| robots.txt | DONE | Bloqueia rotas privadas convencionais e aponta sitemap. |
| sitemap.xml | Parcial | Só homepage; rotas por perfil ficam para roadmap. |
| Custom 404 | DONE | `frontend/public/404.html`. |
| Alt text | Parcial | Existe nas principais imagens; validar manualmente. |
| Analytics | TODO | Só com consentimento e política atualizada. |
| Meta titles/descriptions | DONE técnico | Atualização client-side por view. |
| Social share | DONE técnico | OG/Twitter configurados; validar imagem final. |
| Favicon/canonical | DONE | Canonical aponta para domínio pretendido. |
| Cookie consent | DONE técnico | Preferência local para opcionais. |
| Mobile/accessibility/performance | TODO QA | Fazer QA antes do release final. |
| Broken links/forms | TODO QA | Teste final E2E. |

## Bloqueadores Antes do Release Final

1. Confirmar delegação DNS Cloudflare e zona Active.
2. Ligar `www.saltosnaspalhacadas.pt` ao Pages e configurar redirect apex -> `www`.
3. Concluir Brevo DNS/domain verification.
4. Configurar SMTP real e testar forgot-password/bookings.
5. Criar Scheduler de booking reminders só depois do SMTP validado.
6. Confirmar backups e executar restore drill.
7. Fazer QA mobile, links, legal e E2E.
8. Abrir PR `feat/production-launch` -> `dev`, depois PR `dev` -> `main`.
9. Mudar Cloudflare Pages production branch para `main`.

## Render

`render.yaml` permanece no repositório como configuração histórica/alternativa. A produção atual documentada é Cloudflare Pages + Google Cloud Run + Neon + R2.
