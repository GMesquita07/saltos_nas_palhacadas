# Saltos nas Palhaçadas

Aplicação full-stack para apresentar artistas de animação de eventos, gerir portfólios, receber pedidos de agendamento, moderar avaliações e publicar partilhas de clientes.

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
