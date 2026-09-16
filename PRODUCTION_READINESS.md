# Prontidão para Produção

Documento-resumo. A documentação detalhada está em [docs/PRODUCTION.md](docs/PRODUCTION.md), [docs/SECURITY.md](docs/SECURITY.md), [docs/OPERATIONS.md](docs/OPERATIONS.md) e [docs/ROADMAP.md](docs/ROADMAP.md).

Last verified: 2026-09-16, branch `feat/production-launch`, branch HEAD `25383c0`.

## Estado Geral

| Área | Estado |
| --- | --- |
| Frontend Cloudflare Pages | DONE em domínio oficial; production branch ainda `feat/production-launch` |
| Backend Google Cloud Run | DONE |
| Neon PostgreSQL | DONE |
| Cloudflare R2 runtime + backup | DONE |
| Turnstile | DONE/VALIDATED |
| Cleanup Scheduler | DONE/VALIDATED |
| Booking reminder Scheduler | DONE/VALIDATED |
| Domínio final | DONE técnico; confirmação administrativa .PT pendente |
| Brevo/SMTP | DONE/VALIDATED |
| Backups/restore drill | DONE/VALIDATED |
| Final merge para `main` | TODO |

## Checklist Técnico

| Ponto | Estado | Notas |
| --- | --- | --- |
| API keys privadas | Coberto | Secrets em variáveis/provider/Secret Manager; não em frontend ou Git. |
| Separar dev/prod | Coberto | Profiles `dev`, `test`, `prod`; produção exige R2 e DB SSL. |
| Backups automáticos | Coberto | Neon snapshot/restore validado; R2 backup em bucket separado com lock/lifecycle; manter drills periódicos. |
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

1. Fazer QA mobile.
2. Fazer QA de links/navegação.
3. Rever legal/privacy/cookies/contact-content conforme aplicável.
4. Fazer E2E/smoke final mais amplo em produção.
5. Rever consistência final da documentação.
6. Abrir PR `feat/production-launch` -> `dev` e validar CI/CodeQL.
7. Abrir PR `dev` -> `main`.
8. Mudar Cloudflare Pages production branch para `main`.
9. Fazer verificação final pós-merge.
10. Acompanhar confirmação administrativa .PT externa.

## Render

`render.yaml` permanece no repositório como configuração histórica/alternativa. A produção atual documentada é Cloudflare Pages + Google Cloud Run + Neon + R2.
