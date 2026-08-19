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

1. Definir entidades `Media`, `Servico` e `Contacto` com migrations Flyway.
2. Criar área de administração protegida para o animador gerir o conteúdo.
3. Integrar o formulário de contacto com um serviço de e-mail; não expor chaves no frontend.
4. Adicionar testes de API, acessibilidade e uma política de privacidade/cookies antes do lançamento.
