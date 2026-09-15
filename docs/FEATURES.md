# Funcionalidades

Last verified: 2026-09-15.

## Matriz

| Feature | Público/Cliente/Admin | Frontend principal | Backend | Persistência | Dependência externa | Estado |
| --- | --- | --- | --- | --- | --- | --- |
| Perfis de artistas | Público/Admin | `ProfileSelector`, `PortfolioPage`, `AdminArea` | `ProfileController`, `AdminPortfolioController` | `profiles` | R2 para imagens | DONE |
| Portfólio | Público/Admin | `PortfolioPage`, `PortfolioCard`, admin | `PortfolioController`, `AdminPortfolioController` | `portfolio_items` | R2/media pública | DONE |
| Reviews | Público/Cliente/Admin | `ReviewsSection`, admin | `ReviewController`, `AdminReviewController` | `reviews` | Nenhuma | DONE |
| Contactos | Público/Admin | `ContactPage`, admin | `ContactController`, `AdminContactController` | `contacts` | Nenhuma | DONE |
| Materiais | Público/Admin | `MaterialsPage`, admin | `MaterialController`, `AdminMaterialController` | `materials` | R2/media pública | DONE |
| Registo/login | Público | `AuthPage`, `AuthContext` | `AuthController`, `JwtService` | `app_users` | Turnstile | DONE |
| Forgot/reset password | Público | `AuthPage` | `AuthController`, `PasswordResetToken` | `password_reset_tokens` | Email pendente | Parcial: token existe, email real pendente |
| Conta do cliente | Cliente | `AccountPage` | `AuthController` | `app_users` | R2 para avatar | DONE |
| Alteração de password | Cliente | `AccountPage` | `AuthController` | `app_users` | Nenhuma | DONE |
| Export RGPD | Cliente | `AccountPage` | `AccountLifecycleService` | users, bookings, posts, favorites, reviews | Nenhuma | DONE técnico |
| Eliminação de conta | Cliente | `AccountPage` | `AccountLifecycleService` | Anonimiza/apaga dados relacionados | R2 para apagar media | DONE técnico |
| Favoritos | Cliente | `FavoritesPage`, `AuthContext` | `FavoriteController`, `FavoriteService` | `user_favorites` | Nenhuma | DONE |
| Booking | Cliente | `BookingPage` | `BookingController`, `BookingService` | `booking_requests` | Email pendente | DONE sem SMTP real |
| Disponibilidade | Público | `BookingPage` | `ProfileAvailabilityController` | `booking_requests` | Nenhuma | DONE |
| Decisões/counter-proposals | Cliente/Admin | `BookingPage`, admin | `BookingController`, `AdminBookingController` | `booking_requests` | Email pendente | DONE |
| Cancelamentos | Cliente/Admin | `BookingPage`, admin | `BookingController`, `BookingService` | `booking_requests` | Email pendente | DONE |
| Emails transacionais | Backend | N/A | `EmailService`, `BookingNotificationService` | N/A | Brevo/SMTP | IN PROGRESS |
| Reminders de eventos | Maintenance/Admin ops | N/A | `BookingReminderService`, `MaintenanceController` | `booking_requests.reminder_sent_at` | Scheduler + SMTP | BLOCKED BY SMTP |
| Upload admin media | Admin | `AdminArea`, `MaterialManagement` | `MediaController`, `MediaStorage` | R2/local | R2 em prod | DONE |
| Upload cliente | Cliente | `ClientContentPage` | `ClientContentMediaController`, `ClientContentMediaService` | `media_objects` | R2 | DONE |
| Media privada | Cliente/Admin | `AuthenticatedMedia` | `PrivateMediaController` | `media_objects` | R2 private bucket | DONE |
| Media pública | Público | Imagens/vídeos no site | `PublicMediaController` | paths públicos | R2 public bucket | DONE |
| Moderação partilhas | Admin | `AdminArea` | `AdminClientContentController` | `client_content_posts`, `media_objects` | R2 copy/delete | DONE |
| Cleanup media privada | Maintenance | N/A | `ClientContentMediaService`, `MaintenanceController` | `media_objects` | Scheduler + R2 | DONE |
| Chat suporte | Público | `SupportChat` | `SupportChatController`, `SupportChatService` | Sem persistência própria confirmada | OpenAI opcional | DONE local; IA opcional |
| Privacy/Terms/Cookies | Público | `LegalPage`, `Footer` | N/A | `localStorage` para consentimento | Nenhuma | DONE técnico |
| FAQ | Público | `FAQPage` | N/A | N/A | Nenhuma | DONE |
| SEO metadata | Público | `index.html`, `App.tsx` | N/A | N/A | Cloudflare Pages | Parcial por ser SPA sem rotas reais |
| Sitemap/robots/404 | Público | `public/` | N/A | N/A | Cloudflare Pages | DONE |
| Turnstile | Público auth | `Turnstile`, `AuthPage` | `TurnstileService` | N/A | Cloudflare Siteverify | DONE/VALIDATED |

## Regras Importantes

- Publicações de clientes começam privadas e só ficam públicas depois de aprovação.
- Outro cliente não recebe informação sobre existência de media privada alheia; os casos não autorizados devolvem `404`.
- O backend usa DTOs/records e validação Jakarta; propriedades JSON desconhecidas são rejeitadas globalmente.
- Passwords usam BCrypt; tokens de reset são guardados como hash SHA-256.
- Avatares atuais aceitam upload privado; a proposta futura é avaliar avatares predefinidos.

## Limitações Funcionais Conhecidas

- O frontend é SPA sem rotas públicas por perfil, limitando SEO individual de artistas.
- Brevo/SMTP ainda não está ativo; emails podem ser preparados/logados pelo backend sem envio real quando email está disabled.
- O Scheduler de booking reminders ainda não deve ser criado.
- Analytics não deve ser ativado sem consentimento e revisão da política de cookies.
