# SEO

Last verified: 2026-09-23.

## Arquitetura atual

O frontend continua a ser uma SPA React/Vite publicada em Cloudflare Pages. A base SEO da aplicação vive em `frontend/src/seo`:

- `seoConfig.ts` contém a lógica pura de metadata por rota, canonical URLs, social images e JSON-LD;
- `SeoManager.tsx` aplica essa metadata ao `<head>` durante a navegação da SPA;
- `scripts/generate-sitemap.mjs` gera `public/sitemap.xml` antes do build.

Não há SSR, SSG, prerender, Next.js ou React Helmet nesta fase.

## Domínio canónico

O domínio canónico é:

```text
https://www.saltosnaspalhacadas.pt
```

Todas as canonical URLs, `og:url`, URLs do sitemap e imagens sociais absolutas usam este domínio.

## Metadata por rota

As rotas públicas com metadata própria são:

- `/`
- `/agendar`
- `/contactos`
- `/materiais`
- `/faq`
- `/privacidade`
- `/termos`
- `/cookies`

Para cada rota, a app atualiza `document.title`, description, robots, canonical, Open Graph, Twitter Card e JSON-LD.

## Metadata por artista

As páginas `/perfis/:slug` usam os dados reais carregados pela API pública de perfis.

Regras principais:

- title no formato `Nome — Role | Saltos nas Palhaçadas`;
- description usa `profile.description` quando existe, com whitespace normalizado e limite aproximado de 150-160 caracteres;
- se não houver description, usa fallback com nome e role;
- canonical aponta para `/perfis/<slug>`;
- imagem social usa `heroBackgroundImageUrl`, depois `imageUrl`, depois a imagem raster fallback do site;
- perfis inexistentes ficam `noindex,nofollow` só depois de terminar o loading inicial.

## JSON-LD

A aplicação mantém um único script `application/ld+json` no `<head>` e substitui o conteúdo ao navegar.

O grafo inclui:

- `Organization`;
- `WebSite`;
- `WebPage` nas páginas públicas;
- `ProfilePage` e `Person` nas páginas de perfil.

Em `Person`, só são usados dados existentes no perfil: `name`, `jobTitle`, `description`, `image` e `sameAs` com URLs sociais absolutas `http`/`https` válidas. Não são inventados telefone, morada, preço, rating ou localização.

## Indexação

Indexáveis:

- `/`
- `/agendar`
- `/contactos`
- `/materiais`
- `/faq`
- `/privacidade`
- `/termos`
- `/cookies`
- `/perfis/:slug` quando o perfil existe

Noindex:

- `/admin`
- `/admin/*`
- `/conta`
- `/favoritos`
- `/login`
- `/registo`
- `/recuperar-password`
- `/reset-password`
- rotas desconhecidas/404

As rotas `/agendar/:slug` usam `noindex,follow` e canonical para `/agendar`, para evitar duplicação por artista.

## Robots.txt

`frontend/public/robots.txt` permite crawling geral e referencia o sitemap:

```text
User-agent: *
Allow: /

Sitemap: https://www.saltosnaspalhacadas.pt/sitemap.xml
```

As áreas privadas não são bloqueadas por `Disallow`, para permitir que crawlers vejam `noindex`.

## Sitemap

O sitemap é gerado por:

```text
cd frontend
npm run generate:sitemap
```

O build executa este passo automaticamente antes de TypeScript/Vite.

O script inclui as rotas públicas estáticas e tenta obter perfis publicados em:

```text
GET <API_BASE>/profiles
```

`API_BASE` vem de `SEO_API_URL` quando definida; caso contrário, de `VITE_API_URL`.

Se a API URL não for absoluta `http/https`, se a API estiver indisponível, ou se a resposta não for um array válido, o script emite warning e gera apenas as URLs públicas estáticas. O build não falha por indisponibilidade da API.

## Search Console

Google Search Console ainda não está configurado.

Próximos passos:

1. Criar/verificar a propriedade para `https://www.saltosnaspalhacadas.pt`.
2. Submeter `https://www.saltosnaspalhacadas.pt/sitemap.xml`.
3. Usar URL Inspection para `/`, `/perfis/dj-kidg` e outros perfis publicados.
4. Confirmar se o Google renderiza a metadata client-side esperada.
5. Só avaliar prerender/static rendering depois de existirem dados reais do Search Console.

## Limitação SPA

A metadata específica por rota é aplicada client-side depois do React arrancar. O Google normalmente consegue renderizar JavaScript, mas isso ainda tem de ser validado no Search Console.

Bots sociais ou crawlers que não executem JavaScript podem receber apenas a metadata fallback de `index.html`.
