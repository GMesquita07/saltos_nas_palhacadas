# Saltos nas Palhaçadas

Site full-stack para apresentação, gestão e contacto de um animador de eventos. O projeto permite publicar perfis de artistas, portfólios, avaliações, materiais disponíveis, contactos, pedidos de agendamento e conteúdos submetidos por clientes, com moderação no painel de administração.

## Visão Geral

O objetivo do site é centralizar a presença digital da marca **Saltos nas Palhaçadas**: mostrar artistas e serviços, receber pedidos de eventos, recolher feedback de clientes e facilitar o contacto com potenciais interessados.

O projeto tem duas aplicações:

- **Frontend:** React, TypeScript e Vite.
- **Backend:** Java 21, Spring Boot, Maven, Spring Security, JWT, Flyway e PostgreSQL.

Em produção, a arquitetura recomendada é:

- **Frontend estático:** Cloudflare Pages, Netlify, Vercel ou equivalente.
- **API:** Render, Railway, Fly.io ou outro serviço capaz de correr Docker/Java.
- **Base de dados:** PostgreSQL gerido, por exemplo Neon, Supabase, Render Postgres ou equivalente.
- **Uploads:** disco persistente no servidor ou, preferencialmente, storage externo como S3, Cloudinary ou equivalente.

## Funcionalidades

- Homepage com perfis de artistas ordenáveis pelo administrador.
- Perfil individual de artista com descrição, foto recortável, vídeo de destaque, portfólio e avaliações.
- Publicação de fotos e vídeos por artista, ordenada por data e organizada por mês.
- Conta de cliente com dados pessoais, foto de perfil e favoritos.
- Recuperação de palavra-passe por email, alteração de palavra-passe na conta e token temporário de reset.
- Exportação dos dados da conta e eliminação RGPD com confirmação de palavra-passe.
- Avaliações feitas por utilizadores autenticados e aprovadas pelo administrador.
- Pedidos de agendamento e orçamento, com calendário público de disponibilidade.
- Estados de agendamento: pendente, confirmado, alterado, rejeitado e cancelado.
- Emails automáticos para pedido recebido, aceitação, rejeição, contraproposta, cancelamento e recuperação de palavra-passe.
- Email automático de lembrete 5 dias antes de eventos confirmados.
- Lista pública de materiais disponíveis, gerida e ordenada pelo administrador.
- Contactos públicos geridos e ordenados pelo administrador.
- Página de partilhas de clientes com upload de fotos/vídeos e aprovação obrigatória.
- Chatbot de suporte com respostas automáticas locais e fallback opcional por IA.

## Estrutura

```text
saltos_nas_palhacadas/
├── backend/              # API Spring Boot
├── frontend/             # Aplicação React/Vite
├── docker-compose.yml    # PostgreSQL local
├── render.yaml           # Blueprint para deploy da API no Render
├── .env.example          # Exemplo de variáveis locais, sem segredos reais
└── README.md
```

## Pré-requisitos

- Java 21
- Node.js 22 ou superior
- npm
- Docker Desktop ou Docker Engine
- Conta num provider de alojamento para deploy
- Conta SMTP para envio real de emails
- Conta OpenAI, apenas se quiseres ativar IA no chatbot

## Desenvolvimento Local

Na raiz do projeto:

```bash
cd ~/SaltosNasPalhaçadas/saltos_nas_palhacadas
cp .env.example .env
```

Edita o ficheiro `.env` e define, no mínimo:

```bash
DB_PASSWORD=saltos_dev
ADMIN_EMAIL=admin@example.test
ADMIN_PASSWORD=uma-password-local-com-12-caracteres
```

Para gerar um `JWT_SECRET` local:

```bash
openssl rand -base64 64
```

Depois cola o valor em:

```bash
JWT_SECRET=<valor_gerado>
```

Arranca a base de dados:

```bash
docker compose up -d
```

Terminal 1, API:

```bash
cd backend
set -a && source ../.env && set +a
./mvnw spring-boot:run
```

Terminal 2, frontend:

```bash
cd frontend
npm ci
npm run dev
```

Abre:

```text
http://localhost:5173
```

A API fica disponível em:

```text
http://localhost:8080/api/v1
```

Se precisares de correr a API na porta `8081`, usa:

```bash
cd backend
set -a && source ../.env && set +a
SERVER_PORT=8081 CORS_ALLOWED_ORIGINS=http://localhost:5173,http://127.0.0.1:5173 ./mvnw spring-boot:run
```

E o frontend:

```bash
cd frontend
VITE_API_URL=http://localhost:8081/api/v1 npm run dev -- --host 127.0.0.1
```

## Comandos de Validação

Antes de abrir pull request ou fazer deploy:

```bash
cd frontend
npm run lint
npm run build
```

```bash
cd backend
./mvnw test
```

Para verificar vulnerabilidades conhecidas nas dependências frontend:

```bash
cd frontend
npm audit --audit-level=moderate
```

Para auditoria Java com OWASP Dependency Check, configura primeiro uma NVD API key e corre:

```bash
cd backend
NVD_API_KEY=<nvd_api_key> ./mvnw org.owasp:dependency-check-maven:check -DskipTests
```

## Variáveis de Ambiente

Nunca coloques segredos reais em `.env.example`, no README, em commits ou no frontend.

### Backend

| Variável | Obrigatória em produção | Descrição |
| --- | --- | --- |
| `SPRING_PROFILES_ACTIVE` | Sim | Usar `prod` em produção. |
| `DB_URL` | Sim | JDBC URL da base PostgreSQL. |
| `DB_USERNAME` | Sim | Utilizador da base de dados. |
| `DB_PASSWORD` | Sim | Password da base de dados. |
| `CORS_ALLOWED_ORIGINS` | Sim | Domínio público do frontend, sem `*` e sem `localhost`. |
| `APP_PUBLIC_URL` | Sim | URL público do frontend usado em links enviados por email, como recuperação de password. |
| `JWT_SECRET` | Sim | Segredo Base64 com pelo menos 32 bytes descodificados. |
| `JWT_EXPIRATION_HOURS` | Não | Duração dos tokens JWT. Valor recomendado: `8`. |
| `ADMIN_EMAIL` | Sim | Email do primeiro administrador. |
| `ADMIN_PASSWORD` | Sim | Password forte do primeiro administrador. |
| `AUTH_RATE_LIMIT_PER_MINUTE` | Não | Limite por IP para login, registo e recuperação de password. |
| `PASSWORD_RESET_TOKEN_MINUTES` | Não | Validade, em minutos, dos links de recuperação de password. Valor recomendado: `30`. |
| `MEDIA_LOCAL_DIRECTORY` | Sim se usares uploads locais | Diretório onde a API guarda uploads. |
| `MEDIA_PRIVATE_LOCAL_DIRECTORY` | Não | Diretório privado para media pendente de aprovação. Se vazio, usa uma pasta irmã de `MEDIA_LOCAL_DIRECTORY`. |
| `MEDIA_UPLOAD_RATE_LIMIT_PER_MINUTE` | Não | Limite por IP para uploads. |
| `CLIENT_CONTENT_MAX_PENDING_UPLOADS_PER_USER` | Não | Máximo de uploads pendentes/anexados por cliente antes de aprovação. |
| `CLIENT_CONTENT_MAX_PENDING_UPLOAD_BYTES_PER_USER` | Não | Limite total temporário, em bytes, dos uploads pendentes/anexados por cliente. |
| `CLIENT_CONTENT_PRIVATE_UPLOAD_RETENTION_HOURS` | Não | Horas até apagar automaticamente uploads privados pendentes que nunca foram submetidos. |
| `CLIENT_CONTENT_PRIVATE_UPLOAD_CLEANUP_CRON` | Não | Cron do job que limpa uploads privados órfãos. |
| `MEDIA_MAX_FILE_SIZE` | Não | Tamanho máximo por ficheiro. |
| `MEDIA_MAX_REQUEST_SIZE` | Não | Tamanho máximo por pedido multipart. |
| `BOOKING_EMAIL_ENABLED` | Não | Ativa envio real de emails de agendamento. |
| `BOOKING_EMAIL_SMTP_HOST` | Sim se email ativo | Host SMTP. |
| `BOOKING_EMAIL_SMTP_PORT` | Sim se email ativo | Porta SMTP. |
| `BOOKING_EMAIL_SMTP_SSL` | Não | Usar SSL direto. |
| `BOOKING_EMAIL_SMTP_STARTTLS` | Não | Usar STARTTLS. |
| `BOOKING_EMAIL_USERNAME` | Sim se email ativo | Utilizador SMTP. |
| `BOOKING_EMAIL_PASSWORD` | Sim se email ativo | Password/app password SMTP. |
| `BOOKING_EMAIL_FROM` | Sim se email ativo | Remetente dos emails. |
| `BOOKING_REMINDER_DAYS_BEFORE` | Não | Dias antes do evento para enviar lembrete. Valor atual: `5`. |
| `BOOKING_REMINDER_CRON` | Não | Cron do job de lembretes. |
| `BOOKING_REMINDER_ZONE` | Não | Fuso horário dos lembretes. Valor recomendado: `Europe/Lisbon`. |
| `SUPPORT_AI_ENABLED` | Não | Ativa fallback com IA no chatbot. |
| `OPENAI_API_KEY` | Sim se IA ativa | Chave OpenAI guardada apenas no backend. |
| `OPENAI_API_ENDPOINT` | Não | Endpoint da API OpenAI. |
| `OPENAI_MODEL` | Não | Modelo usado pelo chatbot. Usa um modelo disponível no teu projeto OpenAI. |
| `OPENAI_MAX_OUTPUT_TOKENS` | Não | Limite de tokens por resposta IA. |
| `SUPPORT_CHAT_RATE_LIMIT_PER_MINUTE` | Não | Limite por IP para o chatbot. |

### Frontend

| Variável | Descrição |
| --- | --- |
| `VITE_API_URL` | URL pública da API, por exemplo `https://api.exemplo.pt/api/v1`. |

Só variáveis com prefixo `VITE_` entram no bundle do frontend. Não uses esse prefixo para segredos.

## Segurança Implementada

O backend inclui várias proteções importantes para deploy:

- Autenticação com JWT.
- Separação de permissões entre `CUSTOMER` e `ADMIN`.
- Endpoints administrativos protegidos por role `ADMIN`.
- Rate limit por IP em login, registo, uploads e chatbot.
- Validação de URLs públicas guardadas em conteúdos, materiais e perfis.
- Uploads com allowlist de MIME, validação por assinatura do ficheiro, limites de tamanho e nome gerado pelo servidor.
- Partilhas de clientes carregadas para zona privada e promovidas para media pública apenas após aprovação.
- Ficheiros privados de partilhas ligados ao utilizador dono; outro cliente recebe `404` mesmo que tente adivinhar o URL.
- Quotas temporárias por cliente e limpeza automática de uploads privados órfãos.
- Partilhas públicas escondem email, local e data completa por defeito; o cliente escolhe nome público e consente antes de submeter.
- Migrations Flyway com `ddl-auto=validate` em produção.
- Arranque em `prod` bloqueado quando faltam segredos ou quando o CORS está inseguro.
- Open Session in View desativado.
- `.env` ignorado pelo Git.

## Deploy Seguro

### 1. Base de Dados

Cria uma base PostgreSQL gerida. Para Neon, copia uma connection string compatível com JDBC:

```text
jdbc:postgresql://<host>/<database>?sslmode=require
```

Em produção, prefere uma ligação direta para migrations Flyway. Se usares pooler/PgBouncer, confirma primeiro que o modo de pooling é compatível com as migrations.

### 2. API

No Render ou provider equivalente:

1. Cria um Web Service a partir do repositório.
2. Usa o `render.yaml` existente ou configura manualmente o Dockerfile em `backend/Dockerfile`.
3. Define `SPRING_PROFILES_ACTIVE=prod`.
4. Define todas as variáveis obrigatórias no painel do provider.
5. Não coloques passwords, API keys ou connection strings diretamente no `render.yaml`.
6. Confirma que o health check aponta para `/actuator/health`.

O blueprint inclui região europeia, plano não-free e disco persistente para reduzir o risco de perda de uploads. Confirma custos e limites no provider antes de publicar. Para produção mais robusta, troca o armazenamento local por object storage.

Se usares Render com Docker, evita usar segredos em build args ou no Dockerfile. Os segredos devem existir apenas como variáveis de runtime no painel do provider.

### 3. Frontend

Para Cloudflare Pages:

| Campo | Valor |
| --- | --- |
| Root directory | `frontend` |
| Build command | `npm run build` |
| Build output directory | `dist` |
| Environment variable | `VITE_API_URL=https://<api-publica>/api/v1` |

Depois do primeiro deploy, copia o domínio público do frontend e atualiza `CORS_ALLOWED_ORIGINS` na API.
Define também `APP_PUBLIC_URL` com esse mesmo domínio público para os links enviados por email:

```bash
APP_PUBLIC_URL=https://<dominio-do-frontend>
```

O ficheiro `frontend/public/_headers` inclui uma Content Security Policy funcional para deploy estático. Antes da publicação final, troca o `connect-src 'self' https:` por uma lista explícita com o domínio real da API, por exemplo:

```text
connect-src 'self' https://api.exemplo.pt;
```

Mantém `frame-src` apenas para os domínios usados nos vídeos incorporados, como YouTube ou YouTube NoCookie.

### 4. CORS e Domínios

Em produção:

```bash
CORS_ALLOWED_ORIGINS=https://<dominio-do-frontend>
```

Não uses:

```bash
CORS_ALLOWED_ORIGINS=*
CORS_ALLOWED_ORIGINS=http://localhost:5173
```

Se tiveres domínio principal e domínio `www`, lista ambos explicitamente:

```bash
CORS_ALLOWED_ORIGINS=https://exemplo.pt,https://www.exemplo.pt
```

### 5. Uploads e Media

O projeto aceita uploads de imagens e vídeos. Antes de produção, decide onde esses ficheiros vão viver:

- Para testes: `MEDIA_LOCAL_DIRECTORY=uploads`.
- Para produção com disco persistente: usa um mount estável, por exemplo `/var/data/uploads` ou `/app/uploads`, conforme o provider.
- Para produção mais robusta: usa storage externo como S3 ou Cloudinary.

Atenção: muitos providers têm filesystem efémero por defeito. Se não houver disco persistente, os uploads podem desaparecer após redeploy, restart ou mudança de instância.

### 6. Emails

Para emails reais:

```bash
BOOKING_EMAIL_ENABLED=true
BOOKING_EMAIL_SMTP_HOST=<smtp_host>
BOOKING_EMAIL_SMTP_PORT=587
BOOKING_EMAIL_SMTP_STARTTLS=true
BOOKING_EMAIL_USERNAME=<smtp_user>
BOOKING_EMAIL_PASSWORD=<smtp_password_ou_app_password>
BOOKING_EMAIL_FROM=no-reply@<dominio>
```

Testa estes casos antes do lançamento:

- Pedido de agendamento criado por cliente autenticado.
- Email de confirmação recebido pelo cliente.
- Evento confirmado pelo admin.
- Pedido rejeitado, contraproposta e cancelamento.
- Recuperação de palavra-passe com link recebido por email.
- Disponibilidade atualizada no calendário.
- Lembrete enviado 5 dias antes da data do evento.
- Evento cancelado sem novo lembrete posterior.

### 7. Chatbot com IA

O chatbot funciona sem IA através de respostas locais. Para ativar IA:

```bash
SUPPORT_AI_ENABLED=true
OPENAI_API_KEY=<openai_api_key>
OPENAI_MODEL=<modelo_disponivel_no_teu_projeto>
OPENAI_MAX_OUTPUT_TOKENS=320
```

Regras importantes:

- A `OPENAI_API_KEY` fica apenas no backend.
- O frontend chama apenas `/api/v1/support-chat`.
- Define limites de custo e alertas na conta OpenAI.
- Mantém `SUPPORT_CHAT_RATE_LIMIT_PER_MINUTE` ativo para evitar abuso.
- Não envies dados sensíveis desnecessários para o modelo.

## Checklist Antes de Produção

Obrigatório antes de abrir o site ao público:

- `SPRING_PROFILES_ACTIVE=prod`.
- `JWT_SECRET` gerado com `openssl rand -base64 64`.
- `ADMIN_EMAIL` real e `ADMIN_PASSWORD` forte.
- `CORS_ALLOWED_ORIGINS` com domínio público real.
- `APP_PUBLIC_URL` com o domínio HTTPS público do frontend.
- `DB_URL`, `DB_USERNAME` e `DB_PASSWORD` definidos no provider.
- Base de dados com SSL ativo.
- `VITE_API_URL` aponta para a API pública.
- Emails testados com SMTP real, se `BOOKING_EMAIL_ENABLED=true`.
- Uploads apontam para storage persistente.
- `npm run lint`, `npm run build` e `./mvnw test` passam.
- `npm audit --audit-level=moderate` sem vulnerabilidades críticas/relevantes.
- Auditoria Java executada com OWASP Dependency Check ou ferramenta equivalente.
- OpenAI com chave só no backend, rate limit ativo e limites de custo configurados.
- Política de privacidade e cookies preparada, especialmente porque existem contas, contactos, uploads e mensagens.

## Operação Depois do Deploy

Depois de publicar:

- Cria uma conta admin e guarda as credenciais num gestor de passwords.
- Testa login, criação de conta, perfil, favoritos, avaliações e upload de media.
- Testa o painel admin: perfis, ordem dos perfis, materiais, contactos, avaliações, partilhas e agendamentos.
- Testa agendamento completo: pedido do cliente, email, confirmação admin, disponibilidade e lembrete.
- Verifica logs da API depois dos primeiros testes reais.
- Ativa alertas do provider para erros, consumo de storage, consumo de base de dados e uso da OpenAI.
- Mantém backups automáticos da base de dados.
- Planeia rotação periódica de segredos, principalmente `JWT_SECRET`, SMTP e OpenAI.

## Rotas Principais

### Públicas

- `GET /api/v1/health`
- `GET /api/v1/profiles`
- `GET /api/v1/profiles/{slug}`
- `GET /api/v1/profiles/{slug}/portfolio`
- `GET /api/v1/profiles/{slug}/availability`
- `GET /api/v1/contacts`
- `GET /api/v1/materials`
- `GET /api/v1/client-posts`
- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`
- `POST /api/v1/auth/forgot-password`
- `POST /api/v1/auth/reset-password`
- `POST /api/v1/support-chat`

### Autenticadas

- `GET /api/v1/auth/me`
- `PUT /api/v1/auth/me`
- `PUT /api/v1/auth/me/password`
- `GET /api/v1/auth/me/export`
- `DELETE /api/v1/auth/me`
- `POST /api/v1/media`
- `GET /api/v1/favorites`
- `POST /api/v1/favorites/{portfolioItemId}`
- `DELETE /api/v1/favorites/{portfolioItemId}`
- `POST /api/v1/profiles/{slug}/reviews`
- `POST /api/v1/bookings`
- `GET /api/v1/bookings/mine`
- `PUT /api/v1/bookings/{bookingId}/cancel`
- `PUT /api/v1/bookings/{bookingId}/counter-proposal/decision`
- `POST /api/v1/client-posts`

### Administração

- `/api/v1/admin/**`

Todos os endpoints administrativos requerem token JWT com role `ADMIN`.

## Boas Práticas de Desenvolvimento

- Não alterar migrations Flyway já aplicadas. Criar sempre uma nova migration.
- Não guardar ficheiros de upload no Git.
- Não guardar `.env` ou credenciais reais no repositório.
- Validar frontend e backend antes de cada deploy.
- Fazer deploy primeiro para ambiente de staging ou preview quando possível.
- Rever logs após cada migration de base de dados.

## Documentos Operacionais

- [INCIDENT_RESPONSE.md](INCIDENT_RESPONSE.md) define o processo para responder a incidentes de segurança ou privacidade.
- [DISASTER_RECOVERY.md](DISASTER_RECOVERY.md) descreve backups, restore e recuperação após falha.
- [RGPD_REGISTER.md](RGPD_REGISTER.md) mantém o registo de tratamentos e tarefas RGPD a validar.

## Referências de Segurança

- [OWASP File Upload Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/File_Upload_Cheat_Sheet.html)
- [OWASP REST Security Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/REST_Security_Cheat_Sheet.html)
- [OWASP CSRF Prevention Cheat Sheet, CORS e origens controladas](https://cheatsheetseries.owasp.org/cheatsheets/Cross-Site_Request_Forgery_Prevention_Cheat_Sheet.html)
- [OpenAI API Reference, autenticação e proteção de API keys](https://developers.openai.com/api/reference/overview)
- [Render Docs, environment variables e secrets](https://render.com/docs/configure-environment-variables)
- [Render Docs, persistent disks](https://render.com/docs/disks)
- [Cloudflare Pages, build configuration](https://developers.cloudflare.com/pages/configuration/build-configuration/)
- [Neon Docs, connection pooling](https://neon.com/docs/connect/connection-pooling)
