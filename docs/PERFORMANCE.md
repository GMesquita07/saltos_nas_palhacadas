# Performance & Media

Last verified: 2026-09-24.

Feature 6 foi promovida por PR #55 -> `dev` e PR #56 -> `main`. Depois, Mobile UX Polish foi promovido por PR #59/#60 e o hotfix do MediaLightbox iOS/iPadOS + refresh completo de branding icons por PR #61/#62. O frontend de produção atual está em `e2bb91cd38b381ec951cae0a6fadf9f4a1055904`; CI, CodeQL e Cloudflare Pages concluíram com sucesso e o último hotfix foi validado num iPhone real.

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

## Validação PageSpeed em Produção

Medição em PageSpeed Insights/Lighthouse em 2026-09-24:

| Métrica | Mobile | Desktop |
| --- | ---: | ---: |
| Performance | 85 | 100 |
| Acessibilidade | 96 | 96 |
| Práticas recomendadas | 100 | 100 |
| SEO | 100 | 100 |
| FCP | 1.4 s | 0.3 s |
| LCP | 4.4 s | 0.8 s |
| TBT | 0 ms | 0 ms |
| CLS | 0.02 | 0.014 |
| Speed Index | 1.4 s | 0.6 s |

O PageSpeed ainda não tinha dados de campo/CrUX suficientes e mostrava "Sem dados" na secção de experiência de utilizadores reais.

Diagnósticos relevantes do Lighthouse:

- image delivery: poupança estimada de ~656 KiB em mobile e ~675 KiB em desktop;
- render-blocking requests: ~300 ms em mobile e ~100 ms em desktop;
- JavaScript não utilizado: ~32 KiB;
- o principal gargalo remanescente é o LCP mobile, não o main-thread blocking.

## Follow-ups Mobile

O polish mobile foi promovido por PR #59 -> `dev` e PR #60 -> `main`. O hotfix seguinte, PR #61 -> `dev` e PR #62 -> `main`, moveu o MediaLightbox para `document.body` via portal, reforçou scroll lock para iOS/iPadOS, respeitou safe areas e substituiu definitivamente os favicons/icons restantes pelo branding Saltos. O comportamento final foi validado num iPhone real.

## Limitações e Futuro

Próximas melhorias devem ser avaliadas separadamente:

- Performance 6.2 focada em image delivery/LCP mobile, começando pelo elemento LCP real e imagens acima da dobra;
- responsive images reais com `srcset`/`sizes` quando houver benefício mensurável;
- WebP/AVIF ou image transformation service apenas se a medição justificar a complexidade;
- monitorizar Search Console/Core Web Vitals quando existirem dados de campo suficientes;
- prerender/SSG apenas se Search Console mostrar necessidade;
- service worker/PWA caching apenas com caso de uso claro.
