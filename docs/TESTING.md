# Testes

Last verified: 2026-09-17.

## Suites

| Área | Comando | Cobertura |
| --- | --- | --- |
| Backend unit/integration | `cd backend && ./mvnw test` | Serviços, controllers, segurança, R2 mockado, Turnstile mockado, maintenance, validação |
| Backend PostgreSQL CI | GitHub Actions com Postgres 17 | Compatibilidade real de JPA/Flyway com PostgreSQL |
| Frontend tests | `cd frontend && npm test` | Testes Node do cliente API/Auth/Turnstile |
| Frontend lint | `cd frontend && npm run lint` | ESLint |
| Frontend build | `cd frontend && npm run build` | TypeScript + Vite |
| Frontend audit | `cd frontend && npm audit --audit-level=moderate` | Vulnerabilidades npm moderadas ou superiores |
| CodeQL | GitHub Actions | Java/Kotlin e JS/TS |
| Dependabot | GitHub | npm, Maven e GitHub Actions |

## Execução desta Auditoria

Executado em 2026-09-15:

| Comando | Resultado |
| --- | --- |
| `cd backend && ./mvnw test` | BUILD SUCCESS; 108 tests; 0 failures; 0 errors; 0 skipped |
| `cd frontend && npm ci` | OK; 154 packages installed |
| `cd frontend && npm test` | 3 tests; 3 pass; 0 fail |
| `cd frontend && npm run lint` | OK |
| `cd frontend && npm run build` | OK; Vite build concluído |
| `cd frontend && npm audit --audit-level=moderate` | 0 vulnerabilities |

Validações de produção reportadas em 2026-09-16:

| Área | Resultado |
| --- | --- |
| Cloud Run backend | Healthy; recentes logs ERROR limpos |
| Turnstile | Admin login no domínio oficial validado; hostname production funciona |
| Email Brevo | Forgot/reset password, booking recebido, booking aceite e booking cancelado validados |
| Scheduler cleanup | Execução manual de `private-media-cleanup` sucedeu |
| Scheduler reminders | Execução manual de `booking-reminders` sucedeu |
| Neon restore | Snapshot `pre-launch-2026-09-16` restaurado em branch isolada; dados/Flyway inspecionados; branch temporária apagada |
| R2 backup | Execução manual e via Scheduler sucederam; `rclone check` com 0 diferenças; restore de PNG validado |
| Domínio | `www` live com HTTPS; apex 301 para `www` preserva query strings |

Validação final da release `main`:

| Área | Resultado |
| --- | --- |
| CI | Passou |
| PostgreSQL CI | Passou |
| CodeQL | Passou |
| Produção manual smoke testing | Passou |
| Cloudflare Pages | Production deployment vem de `main` |
| Backend health | UP |
| Cloud Run logs | Sem ERROR logs recentes durante a verificação final |
| Turnstile | Funciona em produção |
| Schedulers enabled | `saltos-r2-backup-daily`, `saltos-private-media-cleanup`, `saltos-booking-reminders` |
| R2 backups automáticos | Execuções sucederam |

## Testes Relevantes por Tema

| Tema | O que deve ser coberto |
| --- | --- |
| R2 | Contexto Spring com provider `r2`, `S3Client` mockado, copy-before-delete, metadata copy, sem chamadas externas |
| Streaming uploads | Magic bytes sem `readAllBytes`, upload com stream novo desde byte 0 |
| Media privada | Owner/Admin antes de read/getObject, 404 para não autorizado |
| Turnstile | success/action/hostname, resposta realista com campos extra, fail-closed |
| Maintenance | Chave ausente/errada 403, chave certa executa apenas o serviço correto |
| Production verifier | Falha em config fraca/ausente e passa com config forte |
| Upload limits | Imagem 10 MiB, vídeo 30 MiB, MIME e magic bytes |
| Auth | BCrypt, JWT, forgot/reset, account update/delete/export |
| Bookings | Criar, decidir, contraproposta, cancelar, disponibilidade, reminders |
| Frontend | Header Turnstile, validação central de upload, ações por modo auth |

## Matriz de Mudança

| Mudança | Testes mínimos |
| --- | --- |
| Auth/JWT/Turnstile | Backend tests + frontend auth tests + E2E manual login |
| Upload/media/R2 | Backend tests + smoke upload/download/publish |
| Scheduler/maintenance | Backend tests + Cloud Scheduler manual run |
| DB migration | Backend tests H2 + CI Postgres + deploy preview/staging |
| Frontend UI | npm test + lint + build + mobile QA manual |
| Segurança/CSP/headers | lint/build + browser smoke + header check |
| Email | Backend tests + SMTP sandbox/real E2E |

## CI Atual

`.github/workflows/ci.yml`:

- pull requests e pushes para `main` e `dev`
- frontend em Node 22: `npm ci`, `npm run lint`, `npm run build`, `npm audit --audit-level=moderate`
- backend em Java 21: `./mvnw -B test`
- backend com PostgreSQL service container `postgres:17`

`.github/workflows/codeql.yml`:

- pull requests e pushes para `main` e `dev`
- schedule semanal às segundas 05:21 UTC
- Java/Kotlin com build Maven
- JavaScript/TypeScript com autobuild

`.github/dependabot.yml`:

- npm em `/frontend`
- Maven em `/backend`
- GitHub Actions em `/`
- periodicidade semanal
