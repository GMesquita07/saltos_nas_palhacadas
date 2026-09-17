# Saltos nas Palhaçadas

Aplicação full-stack para apresentar artistas de animação de eventos, gerir portfólios, receber pedidos de agendamento e moderar avaliações.

Este repositório contém:

- `frontend/`: SPA React + TypeScript + Vite, publicada em Cloudflare Pages.
- `backend/`: API Java 21 + Spring Boot + Maven, publicada em Google Cloud Run.
- `docker-compose.yml`: PostgreSQL local para desenvolvimento.
- `render.yaml`: configuração histórica/alternativa, não é a produção atual.
- `docs/`: documentação técnica e operacional atualizada.

## Estado Atual

Produção técnica final validada:

| Área | Provider atual | Estado |
| --- | --- | --- |
| Frontend | Cloudflare Pages | `https://www.saltosnaspalhacadas.pt` live; production branch `main` |
| Backend | Google Cloud Run | Health endpoint UP; sem ERROR logs recentes na verificação final |
| Base de dados | Neon PostgreSQL | Produção/default com Flyway até V19; snapshot/restore validado |
| Media | Cloudflare R2 | Público/privado e backup bucket validados |
| Jobs | Google Cloud Scheduler | R2 backup, cleanup privado e booking reminders validados |
| Anti-bot | Cloudflare Turnstile | Implementado e validado |
| Email | Brevo | SMTP real ativo e validado com DKIM/DMARC |
| Domínio | Cloudflare DNS + Dominios.pt | `www` HTTP 200; apex 301 para `www`; confirmação administrativa .PT pendente |
| Release | GitHub/Cloudflare/GCP | CI, PostgreSQL CI, CodeQL e smoke final passaram na release `main` |

Fluxo de release técnico concluído:

```text
feat/production-launch -> dev -> main -> Cloudflare Pages production branch main
```

Ainda ficam tarefas pós-lançamento como Search Console/sitemap, confirmação administrativa .PT externa e melhorias visuais/performance futuras.

## Stack

- Frontend: React, TypeScript, Vite, CSS Modules.
- Backend: Java 21, Spring Boot, Spring Security, JWT, JPA/Hibernate, Flyway.
- Dados: PostgreSQL em Neon, `ddl-auto=validate` em produção.
- Media: Cloudflare R2 com buckets separados para objetos públicos e privados.
- Segurança: BCrypt, roles `ADMIN`/`CUSTOMER`, CORS allowlist, HSTS, CSP, rate limiting por IP, validação DTO, validação de uploads por MIME e magic bytes, Turnstile.
- CI: GitHub Actions, CodeQL e Dependabot.

## Funcionalidades Principais

- Perfis públicos de artistas, portfólio, materiais e contactos.
- Registo, login, recuperação de password, alteração de password e área de conta.
- Favoritos, reviews moderadas e pedidos de agendamento com disponibilidade.
- Backoffice admin para perfis, portfólio, contactos, materiais, reviews e bookings.
- Uploads privados para avatar/foto de perfil, com media pública para conteúdos admin.
- Exportação de dados da conta e eliminação/anomização de conta.
- Chat de suporte com respostas locais e fallback OpenAI opcional.
- Páginas legais, FAQ, cookie consent, metadata SEO, sitemap, robots e 404.

## Desenvolvimento Local

Pré-requisitos:

- Java 21
- Node.js 22
- npm
- Docker

O ambiente local deve usar valores locais dedicados. Não uses o ficheiro genérico `.env` para desenvolvimento normal, porque ele pode conter configuração remota/staging.

```bash
cp .env.local.example .env.local
```

`.env.local` é ignorado pelo Git e deve conter apenas valores de desenvolvimento local. Nunca copies segredos de produção/staging para este ficheiro, exceto num teste controlado e consciente. Para desenvolvimento normal, não uses a base de dados remota/produção.

O `.env.local.example` inclui um `JWT_SECRET` Base64 apenas para desenvolvimento. Podes substituí-lo localmente com:

```bash
openssl rand -base64 64
```

Stack local esperado:

```text
Browser
  -> Vite frontend :5173
  -> Vite /api proxy
  -> Spring Boot backend :8080
  -> PostgreSQL Docker :5432
```

### Terminal 1 - PostgreSQL

```bash
docker compose --env-file .env.local up -d
docker compose --env-file .env.local ps
```

PostgreSQL fica disponível em `localhost:5432` com:

- DB: `saltos_nas_palhacadas`
- user: `saltos`
- password: `saltos_dev`

Logs, se precisares:

```bash
docker compose logs postgres
```

### Terminal 2 - Backend

```bash
cd backend
set -a
source ../.env.local
set +a
./mvnw spring-boot:run
```

Spring usa o profile `dev` por defeito. Não uses o profile de produção localmente.

URLs esperados:

- Backend: `http://localhost:8080`
- API: `http://localhost:8080/api/v1`
- Health: `http://localhost:8080/actuator/health`

Validar health:

```bash
curl http://localhost:8080/actuator/health
```

A resposta deve indicar `UP`.

### Terminal 3 - Frontend

```bash
cd frontend
npm ci
npm run dev
```

`npm ci` é normalmente necessário na primeira configuração ou depois de alterações no lockfile, não em todos os arranques.

Frontend: `http://localhost:5173`

Não configures o frontend local para apontar para Cloud Run. Localmente, o frontend usa `/api/v1` e o Vite faz proxy de `/api` para `http://localhost:8080`.

### Admin Local

Credenciais do `.env.local.example` para desenvolvimento local apenas:

- email: `admin@example.test`
- password: `change-me-now`

Estas credenciais são `LOCAL DEVELOPMENT ONLY` e não têm relação com produção. O bootstrap admin é criado na base de dados local quando aplicável.

### Parar ou Reiniciar o Ambiente Local

Parar o PostgreSQL local:

```bash
docker compose --env-file .env.local down
```

Apagar completamente os dados locais do PostgreSQL:

```bash
docker compose --env-file .env.local down -v
```

`-v` remove permanentemente o volume/dados locais do PostgreSQL. Isto não afeta produção.

### Troubleshooting Local

#### Backend falha com JDBC URL / PostgreSQL

Confirma:

```bash
echo "$DB_URL"
```

Para desenvolvimento local normal deve ser:

```text
jdbc:postgresql://localhost:5432/saltos_nas_palhacadas
```

Se a shell contém variáveis antigas de staging/produção, abre uma shell nova ou volta a executar:

```bash
set -a
source ../.env.local
set +a
```

#### Porta 5432 ocupada

```bash
docker compose ps
ss -ltnp | grep 5432
```

#### Backend health

```bash
curl http://localhost:8080/actuator/health
```

#### Logs PostgreSQL

```bash
docker compose logs postgres
```

## Validação Local

Backend:

```bash
cd backend
./mvnw test
```

Frontend:

```bash
cd frontend
npm ci
npm test
npm run lint
npm run build
npm audit --audit-level=moderate
```

## Documentação

- [Estado do projeto](docs/PROJECT_STATUS.md)
- [Arquitetura](docs/ARCHITECTURE.md)
- [Funcionalidades](docs/FEATURES.md)
- [Produção](docs/PRODUCTION.md)
- [Segurança](docs/SECURITY.md)
- [Operações](docs/OPERATIONS.md)
- [Testes](docs/TESTING.md)
- [Roadmap](docs/ROADMAP.md)
- [Decisões técnicas](docs/DECISIONS.md)
- [Prontidão para produção](PRODUCTION_READINESS.md)
- [Recuperação e backups](DISASTER_RECOVERY.md)
- [Resposta a incidentes](INCIDENT_RESPONSE.md)
- [Registo RGPD](RGPD_REGISTER.md)

## Regra de Segredos

Nunca colocar valores reais de passwords, tokens, access keys, connection strings privadas ou secrets em Git, Markdown, frontend ou logs. Em produção, os segredos conhecidos ficam no Google Secret Manager e são injetados no runtime do Cloud Run ou configurados como variáveis seguras no provider apropriado.
