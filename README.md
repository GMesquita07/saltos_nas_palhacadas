# Saltos nas Palhaçadas

Aplicação full-stack para apresentar artistas de animação de eventos, gerir portfólios, receber pedidos de agendamento, moderar avaliações e publicar partilhas de clientes.

Este repositório contém:

- `frontend/`: SPA React + TypeScript + Vite, publicada em Cloudflare Pages.
- `backend/`: API Java 21 + Spring Boot + Maven, publicada em Google Cloud Run.
- `docker-compose.yml`: PostgreSQL local para desenvolvimento.
- `render.yaml`: configuração histórica/alternativa, não é a produção atual.
- `docs/`: documentação técnica e operacional atualizada.

## Estado Atual

Produção atual validada na branch `feat/production-launch`:

| Área | Provider atual | Estado |
| --- | --- | --- |
| Frontend | Cloudflare Pages | Em produção temporária em `saltos-nas-palhacadas-prod.pages.dev` |
| Backend | Google Cloud Run | Em produção com imagem `backend:ee8d9c1` |
| Base de dados | Neon PostgreSQL | Produção separada com Flyway até V19 |
| Media | Cloudflare R2 | Público/privado validado E2E |
| Jobs | Google Cloud Scheduler | Cleanup privado validado; reminders bloqueados por SMTP |
| Anti-bot | Cloudflare Turnstile | Implementado e validado |
| Email | Brevo | Em progresso; SMTP real ainda pendente |
| Domínio | Cloudflare DNS + Dominios.pt | Registo feito; delegação/custom domain pendentes de confirmação |

O fluxo de release previsto é:

```text
feat/production-launch -> PR para dev -> PR final para main
```

Depois do merge final, o Cloudflare Pages deve passar a usar `main` como production branch.

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
- Backoffice admin para perfis, portfólio, contactos, materiais, reviews, bookings e partilhas.
- Uploads privados de clientes, aprovação admin e publicação para media pública.
- Exportação de dados da conta e eliminação/anomização de conta.
- Chat de suporte com respostas locais e fallback OpenAI opcional.
- Páginas legais, FAQ, cookie consent, metadata SEO, sitemap, robots e 404.

## Desenvolvimento Local

Pré-requisitos:

- Java 21
- Node.js 22
- npm
- Docker

Configuração inicial:

```bash
cp .env.example .env
```

Define pelo menos `DB_PASSWORD`, `ADMIN_EMAIL`, `ADMIN_PASSWORD` e um `JWT_SECRET` Base64 forte. Para gerar um segredo local:

```bash
openssl rand -base64 64
```

Arrancar dependências e aplicações:

```bash
docker compose up -d
```

```bash
cd backend
set -a && source ../.env && set +a
./mvnw spring-boot:run
```

```bash
cd frontend
npm ci
npm run dev
```

URLs locais:

- Frontend: `http://localhost:5173`
- API: `http://localhost:8080/api/v1`
- Health: `http://localhost:8080/actuator/health`

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
