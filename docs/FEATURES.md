# Funcionalidades

Last verified: 2026-09-20.

## Matriz

| Feature | Público/Cliente/Admin | Frontend principal | Backend | Persistência | Dependência externa | Estado |
| --- | --- | --- | --- | --- | --- | --- |
| Perfis de artistas | Público/Admin | `ProfileSelector`, `PortfolioPage`, `AdminArea` | `ProfileController`, `AdminPortfolioController` | `profiles` | Pages para media seed; R2 para uploads posteriores | DONE |
| Portfólio | Público/Admin | `PortfolioPage`, `PortfolioCard`, admin content management | `PortfolioController`, `AdminPortfolioController` | `portfolio_items` | Pages para media seed; R2/media runtime | DONE |
| Reviews | Público/Cliente/Admin | `ReviewsSection`, admin | `ReviewController`, `AdminReviewController` | `reviews` | Nenhuma | DONE |
| Contactos | Público/Admin | `ContactPage`, admin content management | `ContactController`, `AdminContactController` | `contacts` | Nenhuma | DONE |
| Materiais | Público/Admin | `MaterialsPage`, admin content management | `MaterialController`, `AdminMaterialController` | `materials` | R2/media pública | DONE |
| Registo/login | Público | `AuthPage`, `AuthContext` | `AuthController`, `JwtService` | `app_users` | Turnstile | DONE |
| Forgot/reset password | Público | `AuthPage` | `AuthController`, `PasswordResetToken` | `password_reset_tokens` | Brevo SMTP | DONE/VALIDATED |
| Conta do cliente | Cliente | `AccountPage` | `AuthController` | `app_users` | R2 para avatar | DONE |
| Alteração de password | Cliente | `AccountPage` | `AuthController` | `app_users` | Nenhuma | DONE |
| Export RGPD | Cliente | `AccountPage` | `AccountLifecycleService` | users, bookings, favorites, reviews | Nenhuma | DONE técnico |
| Eliminação de conta | Cliente | `AccountPage` | `AccountLifecycleService` | Anonimiza/apaga dados relacionados | R2 para apagar media | DONE técnico |
| Favoritos | Cliente | `FavoritesPage`, `AuthContext` | `FavoriteController`, `FavoriteService` | `user_favorites` | Nenhuma | DONE |
| Booking | Cliente | `BookingPage` | `BookingController`, `BookingService` | `booking_requests` | Brevo SMTP | DONE/VALIDATED |
| Disponibilidade | Público | `BookingPage` | `ProfileAvailabilityController` | `booking_requests` | Nenhuma | DONE |
| Decisões/counter-proposals | Cliente/Admin | `BookingPage`, admin | `BookingController`, `AdminBookingController` | `booking_requests` | Brevo SMTP | DONE |
| Cancelamentos | Cliente/Admin | `BookingPage`, admin | `BookingController`, `BookingService` | `booking_requests` | Brevo SMTP | DONE/VALIDATED |
| Emails transacionais | Backend | N/A | `EmailService`, `BookingNotificationService`, `SiteNotificationService` | N/A | Brevo SMTP | DONE/VALIDATED; Feature 4 IN PROGRESS nesta branch |
| Notificações admin/artista | Admin/Backend | `AdminArea` | `SiteNotificationService`, `BookingNotificationService`, `AdminPortfolioController` | `profiles.notification_email`, `app_users` | Brevo SMTP | IN PROGRESS em `feat/notifications-and-email` |
| Reminders de eventos | Maintenance/Admin ops | N/A | `BookingReminderService`, `MaintenanceController` | `booking_requests.reminder_sent_at` | Scheduler + SMTP | DONE/VALIDATED |
| Upload admin media | Admin | `AdminArea`, `MaterialManagement` | `MediaController`, `MediaStorage` | R2/local | R2 em prod | DONE |
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
- Avatares atuais aceitam upload privado; a proposta futura é avaliar avatares predefinidos.
- `profiles.notification_email` é informação operacional privada: só endpoints admin podem ler/escrever; endpoints públicos de perfis/portfólio não expõem este campo.
- Notificações operacionais usam admins ativos da base de dados (`role=ADMIN`, `active=true`), não `ADMIN_EMAIL`; `ADMIN_EMAIL` continua bootstrap-only.
- O conteúdo inicial dos três perfis usa assets estáticos em `frontend/public/content/profiles/`; a metadata permanece editável na BD e uploads posteriores continuam no fluxo R2 existente.
- O admin content management V2 permite gerir portfolio publicado/oculto, editar materiais, ocultar/mostrar contactos e reorganiza o painel num backoffice responsivo sem alterar o comportamento público.
- O portfolio público e admin permanece ordenado cronologicamente por `eventDate DESC`, com `id DESC` como fallback; `display_order` não é usado como ordenação canónica do portfolio.
- O design system frontend usa tokens CSS globais, Tailwind CSS v4 via Vite e fontes Fontsource self-hosted; CSS Modules continuam a existir onde ajudam a encapsular comportamento visual especializado.

## Limitações Funcionais Conhecidas

- O frontend é uma SPA client-rendered; já existem rotas públicas por perfil, mas SEO individual continua limitado sem renderização estática/SSR ou páginas pré-renderizadas.
- Email transacional está ativo em produção, mas a entregabilidade deve continuar monitorizada.
- Reminders de booking já têm Scheduler em produção; manter o cron interno do Spring desativado no profile prod.
- Analytics não deve ser ativado sem consentimento e revisão da política de cookies.
- A base de dados está documentada até Flyway V22, incluindo `profiles.notification_email` e `profile_social_links`.
- Uploads públicos feitos pelo admin ainda não têm ownership persistente em `media_objects`; limpeza automática de media pública órfã/R2 fica como follow-up para evitar apagar URLs reutilizados.
