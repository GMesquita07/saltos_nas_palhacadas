# Saltos nas Palhaçadas

Portfolio digital para o animador **Saltos nas Palhaçadas**: galeria de fotos, vídeos, serviços e contactos.

## Stack e decisões

- **Frontend:** React, TypeScript e Vite. É rápido de desenvolver e gera ficheiros estáticos, ideais para alojamento gratuito.
- **Backend:** Java 21, Spring Boot e Maven. Expõe uma API REST versionada em `/api/v1`.
- **Base de dados:** PostgreSQL. Localmente corre em Docker; em produção a recomendação é Neon.
- **Deploy:** Cloudflare Pages (frontend) + Render (API) + Neon (PostgreSQL).

Os vídeos e fotos não devem ficar guardados no repositório nem na base de dados. Publique vídeos no YouTube/Vimeo e imagens num serviço de armazenamento/CDN; a API guarda apenas os metadados e URLs. Isto mantém custos e deploys simples.

## Pré-requisitos

- Node.js 22+ e npm
- Java 21
- Docker Desktop ou Docker Engine (para PostgreSQL local)

## Desenvolvimento local

Na raiz do projeto:

```bash
cp .env.example .env
docker compose up -d
```

Terminal 1 — API:

```bash
cd backend
set -a && source ../.env && set +a
./mvnw spring-boot:run
```

Terminal 2 — site:

```bash
cd frontend
npm ci
npm run dev
```

Abra `http://localhost:5173`. A rota de verificação da API está em `http://localhost:8080/api/v1/health`.

Para validar antes de cada commit:

```bash
cd frontend && npm run lint && npm run build
cd ../backend && ./mvnw test
```

## Configuração

Copie os ficheiros de exemplo; nunca publique os ficheiros `.env`.

- `.env`: configuração local do Docker/API.
- `frontend/.env`: use `VITE_API_URL=/api/v1` localmente (o Vite encaminha para a API) ou a URL pública da API em produção.
- `CORS_ALLOWED_ORIGINS`: origem do frontend permitida pela API. Em produção deve ser, por exemplo, `https://saltos-nas-palhacadas.pages.dev`.

O perfil `dev` tem valores locais seguros. O perfil `prod` exige `DB_URL`, `DB_USERNAME` e `DB_PASSWORD` no provedor de alojamento.

## API pública

A API usa migrations Flyway em `backend/src/main/resources/db/migration`. Nunca altere uma migration que já tenha sido aplicada; para cada mudança de schema, crie uma nova migration versionada.

| Método | Endpoint | Descrição |
| --- | --- | --- |
| `GET` | `/api/v1/health` | Estado da API |
| `GET` | `/api/v1/profiles` | Lista perfis ativos |
| `GET` | `/api/v1/profiles/{slug}` | Detalhe de um perfil ativo |
| `GET` | `/api/v1/profiles/{slug}/portfolio?type=PHOTO` | Conteúdos publicados; `type` pode ser `PHOTO` ou `VIDEO` |
| `GET` | `/api/v1/contacts` | Lista os contactos visíveis no site |
| `POST` | `/api/v1/auth/register` | Cria uma conta normal (`CUSTOMER`) |
| `POST` | `/api/v1/auth/login` | Inicia sessão e devolve um JWT |
| `GET` | `/api/v1/auth/me` | Devolve a conta da sessão atual |
| `GET` | `/api/v1/favorites` | Lista os favoritos da conta autenticada |
| `POST` / `DELETE` | `/api/v1/favorites/{portfolioItemId}` | Adiciona ou remove uma publicação dos favoritos |

Nesta fase não existem dados de demonstração: os perfis, conteúdos e contactos são criados no painel de administração. Os endpoints públicos só devolvem dados visíveis ao visitante.

## Administração

Em desenvolvimento, a API cria o primeiro administrador com `ADMIN_EMAIL` e `ADMIN_PASSWORD`. Defina ambos no `.env` antes do primeiro arranque. O login é único para todo o site: contas normais criadas em **Criar conta** recebem o papel `CUSTOMER`; apenas contas com papel `ADMIN` veem e podem abrir o botão **Admin**.

- `POST /api/v1/auth/login` — devolve um token de sessão JWT.
- `POST /api/v1/auth/register` — cria exclusivamente contas `CUSTOMER`.
- `GET /api/v1/auth/me` — valida a sessão atual.
- `GET`, `POST` e `DELETE /api/v1/favorites/**` — requerem uma conta autenticada.
- `POST /api/v1/admin/profiles` — requer token `ADMIN`.
- `POST /api/v1/admin/profiles/{slug}/portfolio` — requer token `ADMIN`.
- `DELETE /api/v1/admin/profiles/{slug}/portfolio/{itemId}` — remove um conteúdo; requer token `ADMIN`.
- `POST /api/v1/admin/contacts` — adiciona um contacto público; requer token `ADMIN`.
- `DELETE /api/v1/admin/contacts/{id}` — remove um contacto; requer token `ADMIN`.

Em produção, use variáveis de ambiente do provedor de alojamento para `JWT_SECRET`, `ADMIN_EMAIL` e `ADMIN_PASSWORD`; não coloque estes valores no repositório.

## Deploy gratuito

1. Crie uma base PostgreSQL no [Neon](https://neon.com/pricing) e guarde as credenciais apenas nas variáveis de ambiente do Render.
2. No Render, crie um **Web Service** a partir deste repositório usando `render.yaml`. Defina `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` e, depois de criar o site, `CORS_ALLOWED_ORIGINS`.
3. No Cloudflare Pages, importe o repositório GitHub com:
   - **Root directory:** `frontend`
   - **Build command:** `npm run build`
   - **Build output directory:** `dist`
   - **Environment variable:** `VITE_API_URL=https://<nome-da-api>.onrender.com/api/v1`
4. Copie o URL `*.pages.dev` para `CORS_ALLOWED_ORIGINS` no Render e faça novo deploy da API.

O Cloudflare Pages faz deploy automático a cada push para `main` e cria previews para pull requests. Render tem um nível gratuito útil para projeto pessoal, mas não é um SLA de produção; confirme sempre os limites atuais antes de apresentar o site ao cliente.

## Fluxo GitHub

O repositório já tem o remoto GitHub configurado. Para o primeiro commit deste setup:

```bash
git status
git add .
git commit -m "chore: configurar base full-stack"
git push -u origin main
```

Antes de `git add .`, confirme que `.env` não aparece na lista. Para alterações futuras, crie uma branch (`feat/galeria`, por exemplo), abra um pull request e só depois faça merge em `main`.

## Próximas funcionalidades

1. Integrar o formulário de contacto com um serviço de e-mail; não expor chaves no frontend.
2. Adicionar recuperação de palavra-passe e verificação de email antes do lançamento público.
3. Adicionar CI no GitHub Actions, testes de acessibilidade e uma política de privacidade/cookies antes do lançamento.
