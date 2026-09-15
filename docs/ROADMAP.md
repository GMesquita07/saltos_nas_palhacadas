# Roadmap

Last verified: 2026-09-15.

Estados usados: DONE, IN PROGRESS, BLOCKED, TODO, POST-LAUNCH, OPTIONAL.

## P0 - Bloqueadores do Lançamento

| Item | Estado | Nota |
| --- | --- | --- |
| Confirmar propagação dos nameservers | TODO | Último estado: Cloudflare mostrava invalid nameservers |
| Cloudflare zone Active | TODO | Dependente da delegação DNS |
| Ligar `www` ao Cloudflare Pages | TODO | Depois da zona Active |
| TLS custom domain | TODO | Gerido por Cloudflare |
| Redirect apex -> `www` | TODO | Preservar path/query |
| Brevo DNS verification | BLOCKED | Bloqueado pela delegação DNS |
| DKIM/SPF/DMARC final | BLOCKED | Confirmar publicamente depois da delegação |
| SMTP real | TODO | Configurar Brevo relay e secrets |
| Teste email recuperação password | BLOCKED | Requer SMTP |
| Teste emails bookings | BLOCKED | Requer SMTP |
| Scheduler booking reminders | BLOCKED | Só depois de SMTP validado |
| Teste final E2E | TODO | Auth, bookings, uploads, admin, media, email |
| Backup/restore drill | TODO | Neon/R2 |
| Mobile QA | TODO | Validar em dispositivos reais |
| Links QA | TODO | Navegação, footer, links externos |
| Legal QA | TODO | Revisão jurídica |
| PR `feat/production-launch` -> `dev` | TODO | Depois dos bloqueadores |
| CI/CodeQL final | TODO | No PR |
| PR `dev` -> `main` | TODO | Release final |
| Mudar Pages production branch para `main` | TODO | Depois do merge final |

## P1 - Depois do Lançamento

| Item | Estado | Nota |
| --- | --- | --- |
| Google Search Console | POST-LAUNCH | Usar domínio final |
| Submeter sitemap | POST-LAUNCH | Atualmente só homepage |
| Confirmar indexação | POST-LAUNCH | Especialmente marca e artistas |
| SEO por animador | POST-LAUNCH | Exige rotas reais por perfil |
| URLs reais por perfil | POST-LAUNCH | Ex.: `/animadores/kidg` |
| Inbound email `ola@` | TODO | Decidir provider ou Cloudflare Email Routing |
| Artifact Registry cleanup policy | TODO | Evitar custo/lixo de imagens |
| Limpar objetos de teste R2 | TODO | Confirmar buckets |
| Monitorizar budgets/logs | TODO | GCP, Cloudflare, Neon, R2 |
| Otimização de imagens/assets | POST-LAUNCH | WebP/AVIF, tamanhos responsivos |
| Acessibilidade | POST-LAUNCH | Auditoria teclado/leitor de ecrã |
| Performance | POST-LAUNCH | Lighthouse/WebPageTest |
| Arquitetura híbrida/static content | PLANNED | Ver [decisões](DECISIONS.md) |
| Avatares predefinidos | OPTIONAL | Reduzir uploads livres |

## Produto e UX Migrado do `todolist.md`

| Item antigo | Estado | Decisão |
| --- | --- | --- |
| Deploy online do site | STILL TODO | O ambiente temporário existe; lançamento final ainda depende de domínio, SMTP, PRs e Pages em `main` |
| SEO para DJ KidG / João Tomás | STILL TODO | Limitado pela SPA sem rotas públicas individuais |
| Tradução da página | POST-LAUNCH | Não é bloqueador |
| Email "O animador" -> "A equipa irá analisar" | STILL TODO | Rever wording dos emails antes de SMTP real |
| Corrigir página de partilhas de clientes | STILL TODO | UX/layout a melhorar |
| Partilhas: fotos primeiro e upload melhor posicionado | STILL TODO | Produto/UX |
| Área pessoal do cliente como botões | STILL TODO | Produto/UX |
| Zona de notificações in-site | STILL TODO | Nova funcionalidade |
| Formatação Privacy/Terms/Cookies | STILL TODO | Melhorar UX e pedir revisão jurídica |

## Arquitetura Híbrida Planeada

Objetivo futuro: reduzir uso desnecessário de Neon, Cloud Run e R2.

Candidatos a estático/hardcoded:

- perfis públicos dos animadores
- fotografias principais dos animadores
- textos institucionais
- FAQ
- informação raramente alterada
- materiais/contactos se a edição admin deixar de ser necessária

Manter dinâmico:

- contas
- auth
- bookings
- favoritos
- reviews
- partilhas de clientes
- moderação
- estados operacionais

Nota: `frontend/src/data/profiles.ts` existe com dados hardcoded temporários, mas `App.tsx` atualmente carrega perfis pela API.
