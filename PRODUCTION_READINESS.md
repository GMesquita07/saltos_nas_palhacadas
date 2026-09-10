# Prontidão para Produção

Este documento resume o estado do projeto **Saltos nas Palhaçadas** para lançamento em produção.

## Segurança e API

| Ponto | Estado | Notas |
| --- | --- | --- |
| API keys privadas | Coberto no código | Segredos ficam em variáveis de ambiente. `.env` e `.env.*` estão ignorados pelo Git. Em produção, usar apenas painel do provider ou secret manager. |
| Separar dev/staging/produção | Coberto | `dev`, `test` e `prod` estão separados. Staging e produção devem usar serviços, bases de dados e segredos diferentes. |
| Backups automáticos | Requer provider | Ativar backups automáticos da base de dados e versioning/lifecycle no storage. Fazer teste de restore antes do lançamento. |
| Forçar HTTPS | Coberto + provider | Backend e frontend emitem HSTS. `prod` falha se `APP_PUBLIC_URL`/CORS não forem HTTPS. Ativar redirect HTTP para HTTPS no provider/CDN. |
| Encriptar dados sensíveis | Parcial | Usar PostgreSQL/storage/backups com encryption at rest. Para encriptação por campo, adicionar chave gerida por KMS/secret manager e converters AES-GCM nos campos que não precisam de pesquisa direta. |
| Server-side auth | Coberto | Spring Security valida JWT e role no backend em todos os endpoints protegidos. |
| Restringir acesso | Coberto | `/api/v1/admin/**` exige `ADMIN`; endpoints privados exigem sessão; media privada valida dono ou admin. |
| Bloquear mass assignment | Coberto | Requests usam DTOs/records e mapeamento manual. JSON com propriedades desconhecidas passa a ser rejeitado. |
| Proteger cookies | Parcial | A autenticação atual usa Bearer token, não cookies de sessão. Se mudares para cookies: usar `HttpOnly`, `Secure`, `SameSite=Lax/Strict`, rotação e CSRF token. |
| Hash de palavras-passe | Coberto | Palavras-passe são guardadas com BCrypt. Tokens de reset são guardados com SHA-256, não em claro. |
| Rate limiting | Coberto | Login, registo, reset, uploads, chatbot, agendamentos, reviews e submissões de partilhas têm rate limit por IP. |
| Proteção contra bots | Parcial | Rate limit existe. Para produção pública, adicionar WAF/CDN e desafio tipo Turnstile/reCAPTCHA em login, registo, chatbot, reviews e uploads. Validar sempre o token no backend. |
| Queries parametrizadas | Coberto | JPA repositories usam métodos Spring Data ou `@Query` com parâmetros nomeados. |
| Validar inputs | Coberto | Jakarta Validation, validação de telefone/URLs, datas, horários, enums e limites de tamanho. |
| Não expor dados sensíveis | Coberto | Responses públicas não incluem password hash, tokens, emails de submissões de clientes nem media privada sem auth. |
| Restringir uploads | Coberto | Allowlist de tipos, validação por assinatura, limites de tamanho, UUID gerado pelo servidor e área privada antes de aprovação. |
| Limitar respostas da API | Coberto | Listas públicas, privadas e admin têm limites configuráveis por ambiente. |
| Security headers | Coberto + provider | Backend envia headers de API. Frontend tem `_headers` para Cloudflare Pages; noutros providers, como Render Static Sites, replica-os no Dashboard ou no blueprint. |
| Dependency scan | Coberto em CI + executar antes do deploy | CI já corre `npm audit`; Dependabot e CodeQL estão ativos. Antes de produção, correr também OWASP Dependency Check no backend com NVD API key. |
| Não confiar apenas no RLS | Coberto | As regras críticas estão no serviço/backend: ownership, roles e estados são validados na aplicação. |

## Produto, SEO e Legal

| Ponto | Estado | Notas |
| --- | --- | --- |
| Privacy policy | Existe | Rever com apoio jurídico antes do lançamento. |
| Terms page | Existe | Rever com apoio jurídico antes do lançamento. |
| Clear CTA | Existe | CTA principal de agendamento está visível no header e nos perfis. |
| FAQ | Adicionado | Página FAQ acessível pelo footer. |
| robots.txt | Existe | Inclui sitemap e bloqueia rotas privadas convencionais. |
| sitemap.xml | Existe | Como a app é SPA sem URLs públicas por perfil, lista a homepage. Se forem criadas rotas reais, adicionar essas URLs. |
| Custom 404 | Adicionado | `frontend/public/404.html`. |
| Alt text | Melhorado | Imagens de perfis e media principal têm texto alternativo. |
| Analytics | Requer decisão | Só ativar depois de escolher provider, atualizar política de cookies e respeitar consentimento. |
| Meta titles | Existe | Atualizados dinamicamente por vista. |
| Meta description | Existe | Atualizada dinamicamente por vista. |
| Social share | Melhorado | Metadata Open Graph/Twitter aponta para imagem pública de partilha. Idealmente substituir por PNG/JPG 1200x630 final. |
| Favicon | Existe | `frontend/public/favicon.svg`. |
| Canonical URLs | Existe | Como a app usa uma única URL pública, canonical aponta para a homepage. |
| Cookie consents | Adicionado | Banner para aceitar/rejeitar opcionais. Não ativa analytics por si só. |
| Mobile version | A validar visualmente | CSS tem media queries e layout responsivo; validar em telemóvel real antes do deploy final. |
| Accessibility | Parcial | Labels/aria e alt text existem; correr teste manual com teclado e leitor de ecrã leve. |
| Test forms | Coberto por testes backend + validar staging | Testar login, registo, reset, agendamento, reviews, uploads, contactos e admin no staging. |
| Broken links | A validar antes do deploy | Fazer clique manual no staging e confirmar contactos externos. |
| Performance | Parcial | Build Vite estático. Antes de produção, comprimir assets no provider/CDN, usar cache e otimizar imagens reais carregadas pelo admin. |

## Como Ativar Backups Automáticos

1. Base de dados: ativa backups diários ou point-in-time recovery no provider PostgreSQL.
2. Retenção: define pelo menos 7 a 30 dias, conforme custo e risco.
3. Storage/media: usa object storage com versioning/lifecycle ou disco persistente com snapshot automático.
4. Segredos: guarda cópia em gestor de passwords, nunca em Git.
5. Restore drill: antes de produção, restaura um backup para ambiente isolado e valida login, perfis, uploads e agendamentos.

## Como Ativar Proteção Contra Bots

1. Coloca o frontend atrás de CDN/WAF com regras de rate limit por IP, país e path.
2. Adiciona desafio Turnstile/reCAPTCHA aos formulários públicos e de maior risco.
3. Envia o token do desafio para o backend.
4. No backend, valida o token junto do provider antes de aceitar o pedido.
5. Mantém os rate limits atuais mesmo com WAF ativo.

## Como Fazer Encriptação por Campo

Usa isto apenas para dados que não precisem de pesquisa direta, como notas internas, descrições sensíveis ou telefone secundário.

1. Criar uma chave `DATA_ENCRYPTION_KEY` fora do Git, guardada em KMS/secret manager.
2. Criar um `AttributeConverter` JPA com AES-GCM.
3. Guardar nonce + ciphertext + tag no campo.
4. Versionar a chave para permitir rotação.
5. Nunca encriptar password: passwords continuam com hash BCrypt.

## Comandos Antes do Deploy

```bash
cd frontend
npm ci
npm run lint
npm run build
npm audit --audit-level=moderate
```

```bash
cd backend
./mvnw test
NVD_API_KEY=<nvd_api_key> ./mvnw org.owasp:dependency-check-maven:check -DskipTests
```

## Variáveis Obrigatórias em Produção

- `SPRING_PROFILES_ACTIVE=prod`
- `DB_URL` com SSL, por exemplo `jdbc:postgresql://...?...sslmode=require`
- `DB_USERNAME`
- `DB_PASSWORD`
- `CORS_ALLOWED_ORIGINS=https://<dominio-frontend>`
- `APP_PUBLIC_URL=https://<dominio-frontend>`
- `JWT_SECRET` gerado com `openssl rand -base64 64`
- `ADMIN_EMAIL`
- `ADMIN_PASSWORD`
- `SECURITY_HSTS_ENABLED=true`
- `REQUIRE_DATABASE_SSL=true`
- `VITE_API_URL=https://<api-publica>/api/v1`
