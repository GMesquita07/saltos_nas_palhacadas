# Segurança

Last verified: 2026-09-18.

## Autenticação

- Login e registo em `AuthController`.
- Passwords com BCrypt.
- Sessão do frontend guardada em `sessionStorage` como JWT Bearer.
- `JwtAuthenticationFilter` valida o token no backend.
- Roles suportadas: `ADMIN` e `CUSTOMER`.
- Endpoints admin exigem `ROLE_ADMIN`.

Limitação conhecida: o JWT ainda não usa cookies `HttpOnly`. Se a arquitetura mudar para cookies, será necessário configurar `Secure`, `HttpOnly`, `SameSite`, rotação de sessão e proteção CSRF.

## Recuperação de Palavra-passe

Implementação promovida e validada em produção em 2026-10-05:

- `POST /api/v1/auth/reset-password/validate` é público e usa o mesmo bucket `auth` de rate limiting por IP; recebe `{token}` no body, nunca na query do backend.
- Resposta 204 para token utilizável; 400 genérico para inexistente/expirado/usado/conta inativa/input inválido, sem identidade ou motivo individual.
- Pré-validação read-only não consome o token e serve apenas a UX. O POST final transacional repete a mesma regra antes de alterar a password e marcar uso.
- SHA-256, single-use, invalidação por nova emissão, BCrypt e duração configurável (30 minutos por defeito) preservados. `now == expiresAt` já é inválido.
- Forgot-password mantém resposta neutra; tokens raw não são registados em logs. Falhas de rede não são apresentadas como links inválidos.

## Autorização

- `SecurityConfig` define uma API stateless.
- Endpoints públicos são explicitamente permitidos.
- `/api/v1/admin/**` exige `ADMIN`.
- `/api/v1/auth/me`, favoritos, bookings e media privada exigem sessão.
- `/api/v1/notifications/**` exige sessão; o utilizador é sempre resolvido pelo email autenticado, nunca por `userId` recebido do cliente. Leituras e alterações são filtradas pelo owner e IDs alheios devolvem `404`.
- A contraproposta de cliente em `/api/v1/bookings/{id}/counter-proposal` também deriva o owner exclusivamente da sessão e só é aceite quando a proposta atual pertence ao admin. O backend rejeita autoaceitação e alterações fora do fluxo permitido.
- Media privada valida owner ou `ADMIN` antes de consultar/devolver objetos geridos.
- Falhas de autorização em media privada devolvem `404` para não revelar existência de objetos de outros utilizadores.
- Reminders cliente/artista em V26 mantêm `POST /internal/maintenance/booking-reminders` protegido por `X-Maintenance-Key`. O email operacional usa apenas `profile.notificationEmail`, sem o expor em DTOs públicos; inclui os contactos e detalhes do booking necessários ao artista. Não houve novo endpoint público nem alteração de secrets/Scheduler; a entrega manual E2E foi validada nas duas mailboxes.

## Bot Protection

Turnstile está implementado e validado nos fluxos:

- `POST /api/v1/auth/login`
- `POST /api/v1/auth/register`
- `POST /api/v1/auth/forgot-password`

Não protege `reset-password`; a pré-validação de reset também não exige Turnstile.

O backend valida por Siteverify:

- `success == true`
- `action` igual à esperada
- `hostname` na allowlist

O parser aceita campos adicionais oficiais da Cloudflare sem desativar globalmente `spring.jackson.deserialization.fail-on-unknown-properties=true`.

## Rate Limiting

`IpRateLimiter` é app-level e in-memory. Existe rate limiting para:

- auth pública
- bookings
- reviews
- uploads admin/user
- support chat

Como o rate limiting é em memória por instância, avaliar Cloudflare WAF/rate limiting para defesa distribuída e tráfego público em escala.

## Headers e HTTPS

Backend:

- `X-Content-Type-Options: nosniff`
- `Referrer-Policy: strict-origin-when-cross-origin`
- `X-Frame-Options: DENY`
- `Permissions-Policy`
- `X-Permitted-Cross-Domain-Policies`
- CSP restritiva para API
- `Cache-Control: no-store` em auth, bookings, admin e private media
- HSTS em HTTPS quando ativado

Frontend Pages:

- `_headers` define HSTS, CSP, frame ancestors, object-src none, Permissions-Policy e outros headers.
- CSP permite Turnstile em `script-src` e `frame-src`.

Cloudflare força HTTPS no domínio oficial validado. O apex redireciona para `https://www.saltosnaspalhacadas.pt` com 301.

## CORS

`CorsConfig` aceita apenas origens configuradas em `CORS_ALLOWED_ORIGINS`, sem credentials. Em produção, o `ProductionSecurityVerifier` rejeita wildcard, origens vazias, localhost e URLs sem HTTPS.

## Validação e Mass Assignment

- DTOs/records explícitos nos controllers.
- Jakarta Validation em requests.
- Propriedades JSON desconhecidas rejeitadas globalmente.
- Mapeamento manual entre DTOs e entidades.
- URLs públicos são validados por `PublicUrlValidator`.

## Uploads e Media

- Allowlist de MIME: JPG, PNG, WebP, GIF, MP4, WebM e MOV.
- Validação por magic bytes.
- Nome de storage gerado no servidor com UUID.
- Limites: imagem 10 MiB, vídeo 30 MiB.
- Multipart default: ficheiro 30 MB, request 31 MB.
- Validação lê apenas o cabeçalho necessário; upload para R2 continua streaming.
- R2 usa buckets separados para privado e público.
- `publishPrivate` copia private -> public com `MetadataDirective.COPY` e só depois apaga o privado.
- Runtime buckets não têm bucket lock/lifecycle genérico porque a aplicação precisa de apagar/mover objetos.
- Backups usam bucket separado `saltos-prod-backup`, credenciais separadas e service account dedicada.

## Dados Sensíveis

- Secrets em produção no Google Secret Manager.
- Não guardar valores reais em Markdown, frontend, logs ou imagem Docker.
- Passwords não são encriptadas; são hashes BCrypt.
- Tokens de reset são guardados com hash SHA-256.
- DB SSL obrigatório em produção quando `REQUIRE_DATABASE_SSL=true`.
- `server.error.include-*` está configurado para não expor detalhes internos.
- SMTP password, maintenance key, R2 credentials, Turnstile secret e credenciais de backup ficam no Google Secret Manager.
- Email privado de notificações por artista (`profiles.notification_email`) é dado operacional: só endpoints admin o expõem, nunca endpoints públicos de perfis/portfólio nem exports de conta.
- A inbox V27 guarda apenas títulos/mensagens curtos sobre decisões e reminders de booking. O export RGPD inclui os registos do próprio utilizador e a eliminação da conta remove-os antes de preservar a política existente de anonimização dos bookings. A proteção de utilizador ativo impede que decisões posteriores sobre bookings anonimizados recriem notificações. V27/V28 ainda não estão em produção.
- Notificações para administradores usam utilizadores ativos na DB com role `ADMIN`; a lista nunca é exposta publicamente.

## Startup Verifier

Em profile `prod`, `ProductionSecurityVerifier` falha o startup se faltarem ou forem fracas configurações críticas:

- JWT secret forte
- maintenance API key forte
- Turnstile enabled/secret/hostnames
- admin email/password
- CORS e public URL HTTPS
- HSTS ativo
- DB SSL quando configurado
- `MEDIA_STORAGE_PROVIDER=r2`
- R2 endpoint HTTPS, access key id, secret access key e buckets distintos
- SMTP com SSL/STARTTLS quando email estiver enabled
- OpenAI config quando suporte IA estiver enabled

## CI e Dependências

- GitHub Actions executa frontend lint/build/audit e backend tests.
- Há job backend com PostgreSQL real em service container.
- CodeQL cobre Java/Kotlin e JavaScript/TypeScript.
- Dependabot semanal para npm, Maven e GitHub Actions.

## Limitações e Próximas Defesas

- JWT em `sessionStorage`.
- Rate limiting é app-level/in-memory.
- Neon restore e R2 backup/restore foram validados; continuar drills periódicos.
- Legal/privacy carecem de revisão profissional.
- Avaliar Cloudflare WAF e regras específicas para uploads/auth.
- Avaliar alertas de budget/logs e rotação periódica de secrets.
