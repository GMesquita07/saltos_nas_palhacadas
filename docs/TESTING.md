# Testes

Last verified: 2026-09-17.

## Suites

| Área | Comando | Cobertura |
| --- | --- | --- |
| Backend unit/integration | `cd backend && ./mvnw test` | Serviços, controllers, segurança, R2 mockado, Turnstile mockado, maintenance, validação |
| Backend PostgreSQL CI | GitHub Actions com Postgres 17 | Compatibilidade real de JPA/Flyway com PostgreSQL |
| Frontend tests | `cd frontend && npm test` | Testes Node do cliente API/Auth/Turnstile e helpers de routing |
| Frontend lint | `cd frontend && npm run lint` | ESLint |
| Frontend build | `cd frontend && npm run build` | TypeScript + Vite |
| Frontend audit | `cd frontend && npm audit --audit-level=moderate` | Vulnerabilidades npm moderadas ou superiores |
| CodeQL | GitHub Actions | Java/Kotlin e JS/TS |
| Dependabot | GitHub | npm, Maven e GitHub Actions |

## Testes Automatizados Locais

Backend:

```bash
cd backend
./mvnw test
```

Frontend:

```bash
cd frontend
npm test
npm run lint
npx tsc --noEmit
npm run build
```

Estes comandos não requerem serviços de produção.

## Teste Manual de Integração Local

Para testar a aplicação completa localmente, usa o stack documentado no README:

```text
PostgreSQL Docker :5432
Spring Boot backend :8080
Vite frontend :5173
```

Usa `.env.local` criado a partir de `.env.local.example`; não uses o `.env` genérico para desenvolvimento normal. Os defaults locais desativam serviços remotos opcionais como Turnstile, email transacional, suporte OpenAI e storage R2, usando PostgreSQL Docker e storage local.

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
| Frontend | Header Turnstile, validação central de upload, helpers de routing, returnTo seguro, ações por modo auth |

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

## Reset Link UX: Validação Local

Branch `fix/password-reset-expired-link-ux`, 2026-10-04; sem deploy ou teste de email real nesta tarefa.

Resultados locais: `./mvnw test` passou (132 testes, 0 falhas/erros/skips); `npm test` passou (87 testes, 0 falhas/skips); `npm run lint`, `npm run build` e `git diff --check` passaram.

- Backend: pré-validação pública 204/400, sem consumo, token inexistente/expirado/usado/conta inativa, input incompleto/malformado, nova emissão, POST final independente, reuso, race após pré-validação, rate limiting e ausência de Turnstile.
- `PasswordResetTokenTests`: instantes fixos antes/em/depois de `expiresAt`, com validade de 30 minutos; sem sleeps.
- Frontend Node: contrato do endpoint e AbortSignal, HTTP 400 vs 429/503/rede/cancelamento, formulário e submissão só após validação, CTA pela rota existente, invalidação na submissão final e preservação de erros de campo em `ApiError`.
- Regressão mantém contratos de forgot/reset e testes de headers Turnstile e routing legacy.
- QA manual ainda necessário: desktop/mobile, light/dark, email real, retry e navegação durante pedido pendente. Não foi instalado browser/framework de testes adicional.

## Reminders Cliente/Artista: Validação Local

Extensão da mesma branch, 2026-10-04. A implementação/testes de reset acima foram preservados.

- `BookingReminderDeliveryIntegrationTests` usa a query e os serviços reais até `EmailService` mockado: destinatários, assuntos/corpos, timestamps persistidos, ambos com sucesso, falhas independentes, retry a +4 dias, múltiplos bookings e execução sobreposta sem duplicar.
- Casos adicionais: emails null/blank, ambos ausentes, cliente histórico já notificado, estados não aceites, limites hoje/+5, evento passado/+6, reagendamento, reaceitação e contraproposta. Reaplicar aceitação sem alterar data/horário não reinicia reminders.
- `BookingReminderServiceTests` usa `Clock` fixo às 23:30 UTC no verão para comprovar o dia seguinte em `Europe/Lisbon`; sem sleeps. `EmailServiceTests` confirma que modo desativado/sem host não sinaliza sucesso.
- `BookingIntegrationTests` mantém o percurso público/admin; email é mockado explicitamente e a janela do reminder foi isolada de outras fixtures. `MaintenanceControllerTests` preserva proteção da chave e contrato `processed`.
- Suite Maven: **157 testes, 0 falhas, 0 erros, 0 ignorados; BUILD SUCCESS**. Flyway V26 aplicada no H2 de testes e schema validado; PostgreSQL CI ainda não executado nesta tarefa.
- Frontend: **87 testes passaram**; lint e build passaram. A build usa sitemap estático quando não há URL absoluta da API configurada; não consultou produção.
- `git diff --check`: passou. Sem novas dependências ou alterações às migrations históricas.

Não foi enviado email real. O procedimento E2E de deploy/V26, Scheduler, SMTP/Brevo, duas mailboxes e reexecução está em [OPERATIONS.md](OPERATIONS.md#validar-reminders-clienteartista-após-deploy). Sucesso de SMTP é distinto de entrega final. Crash entre envio e commit continua a poder causar reenvio.
