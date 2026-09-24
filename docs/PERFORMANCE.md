# Performance & Media

Last verified: 2026-09-24.

Feature 6 foi promovida por PR #55 -> `dev` e PR #56 -> `main`. Performance 6.2 Image Delivery / LCP foi promovida por PR #68 -> `dev` e PR #69 -> `main`. Depois da correção final de seleção do favicon desktop em PR #70/#71, a última release frontend com alteração de runtime é `3e6d64275c2bd10238ae24d5e99f53f18f08ac76`. CI, CodeQL e Cloudflare Pages concluíram com sucesso.

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

- primeira imagem de perfil na homepage: `loading="eager"` e `fetchPriority="auto"`, para não competir com o dock logo que é o LCP real;
- restantes cards de perfil: `loading="lazy"` e `fetchPriority="low"`;
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

## Performance 6.2 Image Delivery / LCP

Estado: DONE. Implementado, promovido para produção e validado com nova medição PageSpeed.

Diagnóstico de produção usado como base:

- PageSpeed mobile: Performance 85, FCP 1.4 s, LCP 4.4 s, TBT 0 ms, CLS 0.02, Speed Index 1.4 s;
- PageSpeed desktop: Performance 100, LCP 0.8 s, TBT 0 ms;
- principal oportunidade: `Melhore a entrega de imagens`, com poupança estimada de ~656 KiB em mobile;
- elemento LCP mobile identificado: `div.splash > img.dockLogo`.

Alterações implementadas neste passe:

| Asset | Antes | Depois | Uso |
| --- | ---: | ---: | --- |
| `saltos_logo_redondo.png` | 783x783 PNG, 364966 bytes / 356.4 KiB | `saltos-logo-dock.webp`, 384x384 WebP, 19846 bytes / 19.4 KiB | dock logo do splash / LCP |
| `saltos_logo_redondo.png` | 783x783 PNG, 364966 bytes / 356.4 KiB | `saltos-logo-round-192.webp`, 192x192 WebP, 8250 bytes / 8.1 KiB | BrandMark round e 404 visual |
| `saltos_logo.jpeg` | 797x783 JPEG, 48929 bytes / 47.8 KiB | `saltos-logo-square-192.webp`, 192x192 WebP, 6746 bytes / 6.6 KiB | BrandMark square |

O `index.html` faz preload do LCP asset com o mesmo URL usado pelo `<img>`:

```html
<link rel="preload" as="image" href="/saltos-logo-dock.webp" type="image/webp" fetchpriority="high" />
```

O splash mantém o timing e a experiência visual existentes. O dock logo passa a usar `fetchPriority="high"`, `decoding="async"`, `width="384"` e `height="384"`. A duração do vídeo e o momento em que o dock logo aparece não foram alterados; esse timing estrutural do splash pode continuar a limitar o LCP mesmo com a transferência e descoberta do asset otimizadas.

A política dos profile cards foi ajustada para haver apenas um recurso de imagem com prioridade alta no arranque: o dock logo. O primeiro profile card continua eager, mas com `fetchPriority="auto"`; os restantes ficam lazy/low.

Preconnect ao backend não foi implementado neste passe. `VITE_API_URL` contém path (`/api/v1`) e pode não existir em builds locais; usar `%VITE_API_URL%` no HTML arriscaria gerar um valor literal/inválido ou frágil. Não foi criado `VITE_API_ORIGIN` novo.

As imagens dinâmicas de perfil continuam como follow-up. O caso KidG observado pelo Lighthouse usa uma imagem fonte muito maior do que o display real, mas sem variantes servidas pelo backend/R2 não há `srcset` seguro a gerar no frontend. O caminho correto fica para derivatives no upload, resize no backend ou transformação/CDN com URLs suportados oficialmente.

### Resultado pós-deploy

Nova medição PageSpeed Insights/Lighthouse em 2026-09-24:

| Métrica | Antes mobile | Depois mobile | Desktop depois |
| --- | ---: | ---: | ---: |
| Performance | 85 | 98 | 100 |
| Acessibilidade | 96 | 96 | 96 |
| Práticas recomendadas | 100 | 100 | 100 |
| SEO | 100 | 100 | 100 |
| FCP | 1.4 s | 1.2 s | 0.3 s |
| LCP | 4.4 s | 2.3 s | 0.5 s |
| TBT | 0 ms | 0 ms | 0 ms |
| CLS | 0.02 | 0.022 | 0.019 |

A oportunidade `Melhore a entrega de imagens` caiu de ~656 KiB para ~287 KiB. O asset LCP estático do splash deixou de ser o principal desperdício; a maior parte do remanescente vem de imagens dinâmicas de perfis sem variantes responsivas no backend/CDN, incluindo o caso KidG observado pelo Lighthouse.

O PageSpeed continuava sem dados de campo/CrUX suficientes ("Sem dados"). Com Performance 98 mobile, LCP 2.3 s, TBT 0 ms e desktop 100, Performance 6.2 fica fechada. Render-blocking (~320 ms), JavaScript não utilizado (~32 KiB) e media dinâmica responsiva permanecem como otimizações futuras de retorno marginal ou que exigem suporte arquitetural adicional.

## Follow-ups Mobile

O polish mobile foi promovido por PR #59 -> `dev` e PR #60 -> `main`. O hotfix seguinte, PR #61 -> `dev` e PR #62 -> `main`, moveu o MediaLightbox para `document.body` via portal, reforçou scroll lock para iOS/iPadOS, respeitou safe areas e substituiu definitivamente os favicons/icons restantes pelo branding Saltos. O comportamento final foi validado num iPhone real.

## Limitações e Futuro

Próximas melhorias devem ser avaliadas separadamente:

- responsive images reais com `srcset`/`sizes` quando o backend/CDN disponibilizar variantes de media dinâmicos e houver justificação pelo uso real;
- WebP/AVIF ou image transformation service apenas se a medição justificar a complexidade;
- monitorizar Search Console/Core Web Vitals quando existirem dados de campo suficientes;
- prerender/SSG apenas se Search Console mostrar necessidade;
- service worker/PWA caching apenas com caso de uso claro.
