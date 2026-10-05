# Funcionalidades

Last verified: 2026-10-05.

## Matriz

| Feature | Público/Cliente/Admin | Frontend principal | Backend | Persistência | Dependência externa | Estado |
| --- | --- | --- | --- | --- | --- | --- |
| Perfis de artistas | Público/Admin | `ProfileSelector`, `ProfileCard`, `PortfolioPage`, `AdminArea`, `ArtistProfileImage` | `ProfileController`, `AdminPortfolioController` | `profiles`, incluindo posição/zoom de avatar e hero background | Pages para media seed; R2 para uploads posteriores | DONE |
| Portfólio | Público/Admin | `PortfolioPage`, `PortfolioCard`, `MediaLightbox`, admin content management | `PortfolioController`, `AdminPortfolioController` | `portfolio_items`, incluindo thumbnail URL/posição/zoom | Pages para media seed; R2/media runtime | DONE |
| Reviews | Público/Cliente/Admin | `ReviewsSection`, admin | `ReviewController`, `AdminReviewController` | `reviews` | Nenhuma | DONE |
| Contactos | Público/Admin | `ContactPage`, admin content management | `ContactController`, `AdminContactController` | `contacts` | Nenhuma | DONE |
| Materiais | Público/Admin | `MaterialsPage`, admin content management | `MaterialController`, `AdminMaterialController` | `materials`, incluindo `imagePosition`/`imageZoom` | R2/media pública | DONE |
| Registo/login | Público | `AuthPage`, `AuthContext` | `AuthController`, `JwtService` | `app_users` | Turnstile | DONE |
| Forgot/reset password | Público | `AuthPage` | `AuthController`, `PasswordResetToken` | `password_reset_tokens` | Brevo SMTP | DONE/VALIDATED em produção; pré-validação server-side e UX de link inválido/expirado confirmadas |
| Conta do cliente | Cliente | `AccountPage` com dashboard pessoal e inbox | `AuthController`, `UserNotificationController` | `app_users`, incluindo crop/posição/zoom da foto de conta; `user_notifications` em V27 | R2 para avatar | DONE/VALIDATED em produção; inbox e sino promovidos por PR #95/#96 |
| Alteração de password | Cliente | `AccountPage` | `AuthController` | `app_users` | Nenhuma | DONE |
| Export RGPD | Cliente | `AccountPage` | `AccountLifecycleService` | users, bookings, favorites, reviews, notifications | Nenhuma | DONE técnico; extensão de notifications incluída em produção |
| Eliminação de conta | Cliente | `AccountPage` | `AccountLifecycleService` | Anonimiza/apaga dados relacionados | R2 para apagar media | DONE técnico |
| Favoritos | Cliente | `FavoritesPage`, `AuthContext` | `FavoriteController`, `FavoriteService` | `user_favorites` | Nenhuma | DONE |
| Booking | Cliente | `BookingPage` | `BookingController`, `BookingService` | `booking_requests` | Brevo SMTP | DONE/VALIDATED |
| Disponibilidade | Público | `BookingPage` | `ProfileAvailabilityController` | `booking_requests` | Nenhuma | DONE |
| Decisões/counter-proposals | Cliente/Admin | `BookingPage`, `BookingManagement` | `BookingController`, `AdminBookingController` | `booking_requests`, incluindo proposta/autor/data/horário/orçamento em V28 | Brevo SMTP | DONE/VALIDATED em produção; negociação ADMIN↔CUSTOMER promovida por PR #95/#96 |
| Cancelamentos | Cliente/Admin | `BookingPage`, admin | `BookingController`, `BookingService` | `booking_requests` | Brevo SMTP | DONE/VALIDATED |
| Emails transacionais | Backend | N/A | `EmailService`, `BookingNotificationService`, `SiteNotificationService` | N/A | Brevo SMTP | DONE/VALIDATED; Feature 4 DONE |
| Notificações admin/artista | Admin/Backend | `AdminArea` | `SiteNotificationService`, `BookingNotificationService`, `AdminPortfolioController` | `profiles.notification_email`, `app_users` | Brevo SMTP | DONE/VALIDATED; novo booking envia para o email operacional do artista e admins ativos, com E2E confirmado em produção |
| Reminders de eventos | Maintenance/Admin ops | Sino/`AccountPage` | `BookingReminderService`, `MaintenanceController` | emails em V26; `customer_in_app_reminder_sent_at` em V28 | Scheduler + SMTP | DONE/VALIDATED; emails cliente/artista e canal in-app estão em produção |
| Notificações in-app de booking | Cliente/Admin | `NotificationProvider`, sino global, `AccountPage` | `UserNotificationController`, `UserNotificationService`, integrações de booking/reminder | `user_notifications` (V27), tracking in-app V28 | Nenhuma | DONE/VALIDATED em produção; CUSTOMER e ADMIN usam inbox/sino autenticados |
| Upload/admin content management | Admin | `AdminArea`, `MaterialManagement`, crop editors/previews | `MediaController`, `MediaStorage` | R2/local + posição/zoom nas entidades editáveis | R2 em prod | DONE |
| Media privada | Cliente/Admin | `AuthenticatedMedia` | `PrivateMediaController` | `media_objects` | R2 private bucket | DONE |
| Media pública | Público | Imagens/vídeos no site | `PublicMediaController` | paths públicos | R2 public bucket | DONE |
| Cleanup media privada | Maintenance | N/A | `ManagedMediaService`, `MaintenanceController` | `media_objects` | Scheduler + R2 | DONE |
| Backup R2 | Operação | N/A | Cloud Run Job `saltos-r2-backup` | R2 snapshots | Scheduler + rclone | DONE/VALIDATED |
| Chat suporte | Público | `SupportChat` | `SupportChatController`, `SupportChatService` | Sem persistência própria confirmada | OpenAI opcional | DONE local; IA opcional |
| Privacy/Terms/Cookies | Público | `LegalPage`, `Footer` | N/A | `localStorage` para consentimento | Nenhuma | DONE técnico |
| FAQ | Público | `FAQPage` | N/A | N/A | Nenhuma | DONE |
| SEO metadata | Público | `index.html`, `App.tsx`, React Router | N/A | N/A | Cloudflare Pages | Parcial por ser SPA client-rendered; rotas reais existem |
| Sitemap/robots/404 | Público | `public/` | N/A | N/A | Cloudflare Pages | DONE |
| Turnstile | Público auth | `Turnstile`, `AuthPage` | `TurnstileService` | N/A | Cloudflare Siteverify | DONE/VALIDATED |
| Design system frontend | Público/Cliente/Admin | CSS variables, Tailwind v4, CSS Modules | N/A | N/A | Fontsource self-hosted | DONE |

## Regras Importantes

- Outro cliente não recebe informação sobre existência de media privada alheia; os casos não autorizados devolvem `404`.
- O backend usa DTOs/records e validação Jakarta; propriedades JSON desconhecidas são rejeitadas globalmente.
- Passwords usam BCrypt; tokens de reset são guardados como hash SHA-256.
- Em produção, o formulário de reset só aparece após pré-validação server-side do link. Links incompletos/inutilizáveis mostram «Pedir novo link»; falhas de rede têm retry próprio. O POST final repete a validação e continua autoritativo. O caminho de token inválido foi validado no domínio oficial em 2026-10-05.
- Avatares atuais aceitam upload privado com crop/posição/zoom persistente; a proposta futura é avaliar avatares predefinidos.
- `profiles.notification_email` é informação operacional privada: só endpoints admin podem ler/escrever; endpoints públicos de perfis/portfólio não expõem este campo.
- Notificações operacionais usam admins ativos da base de dados (`role=ADMIN`, `active=true`), não `ADMIN_EMAIL`; `ADMIN_EMAIL` continua bootstrap-only.
- A inbox in-app é complementar ao email e regista decisões administrativas ACCEPTED/DECLINED/COUNTER_PROPOSED/CANCELLED e o reminder de 5 dias. Ações iniciadas pelo cliente não criam notificações para o próprio; ao resolver uma proposta administrativa, a notificação pendente deixa de requerer ação. O sino global e a conta reutilizam o mesmo provider, isolado por sessão.
- O conteúdo inicial dos três perfis usa assets estáticos em `frontend/public/content/profiles/`; a metadata permanece editável na BD e uploads posteriores continuam no fluxo R2 existente.
- O admin content management V2 permite gerir portfolio publicado/oculto, editar materiais, ocultar/mostrar contactos e reorganiza o painel num backoffice responsivo sem alterar o comportamento público.
- Editor/admin preview e apresentação final usam a mesma interpretação de crop para as superfícies partilhadas, evitando previews aproximadas.
- A thumbnail personalizada de portfolio é usada no cartão; o lightbox continua a usar a media original.
- Materiais persistem e aplicam `imagePosition`/`imageZoom` na apresentação pública e na gestão admin.
- O portfolio público e admin permanece ordenado cronologicamente por `eventDate DESC`, com `id DESC` como fallback; `display_order` não é usado como ordenação canónica do portfolio.
- O design system frontend usa tokens CSS globais, Tailwind CSS v4 via Vite e fontes Fontsource self-hosted; CSS Modules continuam a existir onde ajudam a encapsular comportamento visual especializado.

## Limitações Funcionais Conhecidas

- O frontend é uma SPA client-rendered; já existem rotas públicas por perfil, mas SEO individual continua limitado sem renderização estática/SSR ou páginas pré-renderizadas.
- Email transacional está ativo em produção, mas a entregabilidade deve continuar monitorizada.
- Reminders de booking têm Scheduler em produção; manter o cron interno do Spring desativado no profile prod. A versão cliente/artista com retries independentes está em produção e teve entrega manual E2E confirmada nas duas mailboxes; o trigger automático das 09:00 continua a ser monitorizado separadamente.
- Analytics não deve ser ativado sem consentimento e revisão da política de cookies.
- A base de dados está operacionalmente confirmada em Flyway V26, com 26 migrations validadas; `saltos-backend-00020-ln5` aplicou V26 e `00021-5mv` confirmou o schema up to date.
- V27 cria `user_notifications` e V28 acrescenta negociação bidirecional e tracking do reminder in-app; ambas foram promovidas por PR #95/#96 e aplicadas em produção pela revisão `saltos-backend-00022-6x6`.
- Reminder in-app de 1 dia, pedido de review pós-evento, atualizações de favoritos e waitlist/disponibilidade continuam apenas ideias futuras.
- Uploads públicos feitos pelo admin ainda não têm ownership persistente em `media_objects`; limpeza automática de media pública órfã/R2 fica como follow-up para evitar apagar URLs reutilizados.
