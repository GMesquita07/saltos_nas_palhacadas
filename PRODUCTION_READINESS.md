# Prontidão para Produção

Documento-resumo. A documentação detalhada está em [docs/PRODUCTION.md](docs/PRODUCTION.md), [docs/SECURITY.md](docs/SECURITY.md), [docs/OPERATIONS.md](docs/OPERATIONS.md) e [docs/ROADMAP.md](docs/ROADMAP.md).

Last verified: 2026-10-05.

## Estado Geral

| Área | Estado |
| --- | --- |
| Frontend Cloudflare Pages | DONE em domínio oficial; production branch `main` |
| Backend Google Cloud Run | DONE; health UP; rollout validado |
| Neon PostgreSQL | DONE; produção em Flyway V28 com 28 migrations validadas; V27/V28 aplicadas por `saltos-backend-00022-6x6` |
| Cloudflare R2 runtime + backup | DONE |
| Turnstile | DONE/VALIDATED |
| Cleanup Scheduler | DONE/VALIDATED |
| Booking reminder Scheduler | DONE/VALIDATED; execução manual pós-release e E2E cliente/artista validados; próximo trigger automático 09:00 fica para monitorização operacional |
| Reminders cliente/artista/in-app | DONE/VALIDATED; email cliente/artista validado E2E e canal in-app promovido em V28 |
| Domínio final | DONE técnico; confirmação administrativa .PT pendente |
| Brevo/SMTP | DONE/VALIDATED |
| Backups/restore drill | DONE/VALIDATED |
| CI/PostgreSQL CI/CodeQL final | DONE |
| Smoke final de produção | DONE |
| Final merge para `main` | DONE; notificações in-app + contrapropostas promovidas por PR #95/#96 e validadas em produção em 2026-10-05 |

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
| sitemap.xml | DONE | Sitemap enviado no Search Console com 11 URLs descobertas. |
| Custom 404 | DONE | `frontend/public/404.html`. |
| Alt text | Parcial | Existe nas principais imagens; validar manualmente. |
| Analytics | TODO | Só com consentimento e política atualizada. |
| Meta titles/descriptions | DONE técnico | Atualização client-side por view. |
| Social share | DONE técnico | OG/Twitter configurados; validar imagem final. |
| Favicon/canonical | DONE | Canonical aponta para domínio pretendido. |
| Cookie consent | DONE técnico | Preferência local para opcionais. |
| Mobile/accessibility/performance | POST-LAUNCH | Performance 6.2 validada com 98 mobile / 100 desktop; acessibilidade continua como Feature 7 futura. |
| Broken links/forms | DONE técnico | Smoke final de produção passou; continuar regressão em mudanças futuras. |

## Pós-Lançamento Ainda Pendente

1. Acompanhar confirmação administrativa .PT externa.
2. Monitorizar o próximo trigger automático das 09:00 Europe/Lisbon do Scheduler de booking reminders; a execução manual pós-release e a entrega cliente/artista já foram validadas.
3. Monitorizar entregabilidade do email operacional do artista nos bookings; criação de booking -> `profile.notificationEmail` validada em produção em 2026-10-05.
4. Executar Feature 7 Accessibility.
5. Manter observabilidade/manutenção e fechar legal/inbound email/media cleanup.

Nota operacional: V28 está confirmada em produção. A revisão `saltos-backend-00022-6x6` encontrou o schema em V26, aplicou V27/V28, validou 28 migrations e ficou saudável a servir 100% do tráfego.

## Render

`render.yaml` permanece no repositório como configuração histórica/alternativa. A produção atual documentada é Cloudflare Pages + Google Cloud Run + Neon + R2.
