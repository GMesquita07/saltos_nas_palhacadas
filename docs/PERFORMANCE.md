# Performance & Media

Last verified: 2026-09-24.

## Baseline

Medido com:

```text
cd frontend
npm run build
```

Resultado antes da Feature 6:

| Tipo | Ficheiro principal | Tamanho | Gzip |
| --- | --- | ---: | ---: |
| JS | `index-Cp_cbMdj.js` | 426.15 kB | 125.23 kB |
| CSS | `index-9ucz8qEb.css` | 293.68 kB | 45.65 kB |

O código da aplicação saía essencialmente num chunk JS principal. As fontes emitidas incluíam subsets não necessários para PT-PT:

- `manrope-vietnamese`
- `bricolage-grotesque-vietnamese`
- `manrope-greek`
- `manrope-cyrillic`
- `manrope-latin-ext`
- `bricolage-grotesque-latin-ext`
- `manrope-latin`
- `bricolage-grotesque-latin`

## Depois

Resultado depois da Feature 6:

| Tipo | Ficheiro principal | Tamanho | Gzip |
| --- | --- | ---: | ---: |
| JS inicial | `index-BRp2OeUN.js` | 273.99 kB | 87.33 kB |
| CSS inicial | `index-CJ5mFwrG.css` | 50.31 kB | 10.27 kB |

Chunks principais criados:

- `AdminArea`: 64.26 kB JS / 16.19 kB gzip; 74.47 kB CSS / 9.64 kB gzip
- `BookingPage`: 29.24 kB JS / 8.25 kB gzip; 54.84 kB CSS / 8.78 kB gzip
- `PortfolioPage`: 13.66 kB JS / 4.79 kB gzip; 27.30 kB CSS / 5.21 kB gzip
- `AccountPage`: 12.01 kB JS / 3.73 kB gzip; 21.84 kB CSS / 3.07 kB gzip
- `AuthPage`: 8.63 kB JS / 3.16 kB gzip; 12.37 kB CSS / 2.09 kB gzip
- `SupportChat`: 5.24 kB JS / 1.97 kB gzip; 10.33 kB CSS / 1.98 kB gzip
- páginas menores: Contacts, FAQ, Legal, Materials, Favorites, MediaLightbox e PortfolioCard

Fontes emitidas depois:

- `manrope-latin`
- `manrope-latin-ext`
- `bricolage-grotesque-latin`
- `bricolage-grotesque-latin-ext`

## Estratégia

`App.tsx` mantém o shell inicial eager: Header, Footer, ProfileSelector, CookieConsent, SplashScreen e SeoManager. Rotas e áreas não essenciais ao primeiro render usam `React.lazy` + `Suspense`:

- admin;
- autenticação;
- conta;
- favoritos;
- booking;
- contactos;
- materiais;
- FAQ;
- páginas legais;
- portfolio;
- support chat.

`SupportChat` só é importado depois de `splashPhase === 'done'`.

## Requests

O carregamento de `/profiles` passou a ser route-aware. A app só carrega perfis em:

- `/`
- `/perfis/:slug`
- `/agendar`
- `/agendar/:slug`

O evento `profiles:changed` continua a invalidar a cache e só faz refetch imediato se a rota atual precisar de perfis.

## Imagens

`CroppedImage` aceita agora:

- `loading`
- `decoding`
- `fetchPriority`

Política aplicada:

- primeira imagem de perfil na homepage: `loading="eager"` e `fetchPriority="high"`;
- restantes cards de perfil: `loading="lazy"`;
- avatar principal da página de perfil: eager/high;
- PortfolioCard e MaterialsPage: imagens lazy com `decoding="async"`;
- MediaLightbox: imagem aberta pelo utilizador com `decoding="async"`;
- BrandMark continua eager, sem lazy loading.

## Vídeos

PortfolioCard deixou de usar `preload="metadata"` para vídeos sem thumbnail na grelha e passou a usar `preload="none"`.

MediaLightbox mantém `preload="metadata"` quando o utilizador abre um vídeo. Vídeo em destaque direto no perfil também mantém `preload="metadata"`.

O splash mantém `preload="auto"` porque é parte deliberada da experiência inicial e precisa começar sem atraso visível.

## Fontes

Os imports genéricos de Fontsource foram substituídos por `@font-face` explícito para os ficheiros self-hosted `latin` e `latin-ext`. Não há Google Fonts externo.

Os caracteres portugueses ficam cobertos por `latin`; `latin-ext` fica incluído como margem segura para nomes, marcas e conteúdo editorial.

## Cache

`frontend/public/_headers` adiciona cache longo apenas para assets Vite com hash:

```text
/assets/*
  Cache-Control: public, max-age=31536000, immutable
```

HTML e assets públicos sem hash não recebem cache agressiva.

## Limitações e Futuro

Não foram medidos Lighthouse/Core Web Vitals nesta feature. Para voltar a medir, usar:

```text
cd frontend
npm run build
```

Depois de deploy, validar em PageSpeed Insights e Search Console/Core Web Vitals. Próximas melhorias devem ser avaliadas separadamente:

- responsive images reais;
- pipeline WebP/AVIF;
- prerender/SSG caso Search Console mostre necessidade;
- image transformation service/CDN dedicado;
- service worker/PWA caching.
