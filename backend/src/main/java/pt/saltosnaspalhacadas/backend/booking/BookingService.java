package pt.saltosnaspalhacadas.backend.booking;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import pt.saltosnaspalhacadas.backend.profile.Profile;
import pt.saltosnaspalhacadas.backend.profile.ProfileNotFoundException;
import pt.saltosnaspalhacadas.backend.profile.ProfileRepository;
import pt.saltosnaspalhacadas.backend.user.AppUser;
import pt.saltosnaspalhacadas.backend.user.AppUserRepository;
import pt.saltosnaspalhacadas.backend.usernotification.UserNotificationService;

@Service
public class BookingService {

    private final BookingRepository bookings;
    private final AppUserRepository users;
    private final ProfileRepository profiles;
    private final BookingNotificationService notifications;
    private final UserNotificationService userNotifications;

    public BookingService(
            BookingRepository bookings,
            AppUserRepository users,
            ProfileRepository profiles,
            BookingNotificationService notifications,
            UserNotificationService userNotifications) {
        this.bookings = bookings;
        this.users = users;
        this.profiles = profiles;
        this.notifications = notifications;
        this.userNotifications = userNotifications;
    }

    @Transactional
    public Booking create(String email, CreateBookingCommand command) {
        validateProposalDate(command.eventDate());
        validateTimeWindow(command.startTime(), command.endTime());
        validateEventSpecificFields(command);

        AppUser user = findActiveUser(email);
        Profile profile = profiles.findBySlugAndActiveTrue(command.profileSlug())
                .orElseThrow(() -> new ProfileNotFoundException(command.profileSlug()));

        assertNoAcceptedScheduleConflict(
                profile.getId(),
                command.eventDate(),
                command.startTime(),
                command.endTime(),
                null);

        assertNoDuplicateActiveRequest(
                user.getId(),
                profile.getId(),
                command.eventDate(),
                command.startTime(),
                command.endTime());

        Booking booking = bookings.save(new Booking(
                user,
                profile,
                command.eventDate(),
                command.startTime(),
                command.endTime(),
                command.eventType(),
                command.customEventType(),
                command.weddingCoupleNames(),
                command.location(),
                command.contactName(),
                command.contactEmail(),
                command.contactPhone(),
                command.description(),
                command.notes()));
        userNotifications.createForBookingCreated(booking);
        notifications.sendReceivedConfirmation(booking);
        return booking;
    }

    @Transactional(readOnly = true)
    public List<Booking> findMine(String email) {
        return bookings.findAllByUserIdWithProfileOrderByCreatedAtDesc(findActiveUser(email).getId());
    }

    @Transactional(readOnly = true)
    public List<Booking> findForAdmin(BookingStatus status) {
        return bookings.findAllForAdmin(status);
    }

    @Transactional(readOnly = true)
    public List<Booking> findAvailability(String profileSlug, LocalDate from, LocalDate to) {
        profiles.findBySlugAndActiveTrue(profileSlug).orElseThrow(() -> new ProfileNotFoundException(profileSlug));
        validateDateRange(from, to);
        return bookings.findAvailabilitySlots(
                profileSlug,
                Set.of(BookingStatus.PENDING, BookingStatus.ACCEPTED, BookingStatus.COUNTER_PROPOSED),
                BookingStatus.COUNTER_PROPOSED,
                from,
                to);
    }

    @Transactional
    public Booking decide(Long bookingId, DecisionBookingCommand command) {
        Booking booking = bookings.findByIdWithProfile(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido de agendamento não encontrado"));

        if (command.status() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Escolhe a decisão para este pedido");
        }

        if (command.status() == BookingStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Escolhe confirmar, rejeitar, alterar ou cancelar o pedido");
        }

        if (command.status() == BookingStatus.CANCELLED) {
            if (command.message() == null || command.message().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Indica a justificação do cancelamento");
            }
            if (booking.getStatus() == BookingStatus.CANCELLED) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Este agendamento já está cancelado");
            }
            booking.cancel(command.message());
            Booking savedBooking = bookings.save(booking);
            userNotifications.createForAdminDecision(savedBooking);
            notifications.sendDecisionNotification(savedBooking);
            return savedBooking;
        }

        if (command.status() == BookingStatus.ACCEPTED) {
            assertNoDecisionTerms(command);
            Profile profile = lockActiveProfile(booking);
            if (booking.getStatus() == BookingStatus.PENDING) {
                validateFinalScheduleAndConflict(booking, profile, booking.getEventDate(), booking.getStartTime(), booking.getEndTime());
                booking.acceptCurrent(command.message());
            } else if (booking.getStatus() == BookingStatus.COUNTER_PROPOSED
                    && booking.getCounterProposedBy() == CounterProposalAuthor.CUSTOMER) {
                LocalDate date = proposedDate(booking);
                LocalTime start = proposedStartTime(booking);
                LocalTime end = proposedEndTime(booking);
                validateFinalScheduleAndConflict(booking, profile, date, start, end);
                booking.acceptCounterProposalByAdmin(command.message());
            } else if (booking.getStatus() == BookingStatus.COUNTER_PROPOSED) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Não podes aceitar uma proposta enviada pela administração");
            } else {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Este pedido não pode ser aceite neste estado");
            }
        } else if (command.status() == BookingStatus.COUNTER_PROPOSED) {
            if (booking.getStatus() != BookingStatus.PENDING
                    && booking.getStatus() != BookingStatus.ACCEPTED
                    && booking.getStatus() != BookingStatus.COUNTER_PROPOSED) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Este pedido já não aceita contrapropostas");
            }
            assertNoAcceptedTerms(command);
            Profile profile = lockActiveProfile(booking);
            NormalizedCounterProposal proposal = validateCounterProposal(booking, command, profile);
            booking.counterPropose(
                    CounterProposalAuthor.ADMIN,
                    command.message(),
                    proposal.budget(),
                    proposal.eventDate(),
                    proposal.startTime(),
                    proposal.endTime());
        } else {
            assertNoDecisionTerms(command);
            if (booking.getStatus() != BookingStatus.PENDING
                    && !(booking.getStatus() == BookingStatus.COUNTER_PROPOSED
                        && booking.getCounterProposedBy() == CounterProposalAuthor.CUSTOMER)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Este pedido não pode ser recusado neste estado");
            }
            booking.decline(command.message());
        }

        Booking savedBooking = bookings.save(booking);
        userNotifications.createForAdminDecision(savedBooking);
        notifications.sendDecisionNotification(savedBooking);
        return savedBooking;
    }

    @Transactional
    public Booking respondToCounterProposal(String email, Long bookingId, CounterProposalDecision decision) {
        AppUser user = findActiveUser(email);
        Booking booking = bookings.findByIdWithProfile(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido de agendamento não encontrado"));

        if (decision == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Escolhe se aceitas ou recusas a contraproposta");
        }

        if (!booking.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Não tens permissão para responder a este pedido");
        }

        if (booking.getStatus() != BookingStatus.COUNTER_PROPOSED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Esta proposta não tem uma contraproposta pendente");
        }

        if (booking.getCounterProposedBy() != CounterProposalAuthor.ADMIN) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A equipa ainda tem de responder à tua contraproposta");
        }

        if (decision == CounterProposalDecision.DECLINED) {
            booking.declineCounterProposal();
            Booking savedBooking = bookings.save(booking);
            userNotifications.resolveCounterProposalAction(savedBooking);
            userNotifications.createForCustomerCounterDecision(savedBooking, decision);
            notifications.sendCounterProposalResponseConfirmation(savedBooking, decision);
            return savedBooking;
        }

        Profile profile = lockActiveProfile(booking);
        validateFinalScheduleAndConflict(
                booking,
                profile,
                proposedDate(booking),
                proposedStartTime(booking),
                proposedEndTime(booking));
        booking.acceptCounterProposalByCustomer();
        Booking savedBooking = bookings.save(booking);
        userNotifications.resolveCounterProposalAction(savedBooking);
        userNotifications.createForCustomerCounterDecision(savedBooking, decision);
        notifications.sendCounterProposalResponseConfirmation(savedBooking, decision);
        return savedBooking;
    }

    @Transactional
    public Booking counterProposeMine(String email, Long bookingId, CounterProposalCommand command) {
        AppUser user = findActiveUser(email);
        Booking booking = bookings.findByIdWithProfile(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido de agendamento não encontrado"));
        assertOwner(user, booking);

        if (booking.getStatus() != BookingStatus.COUNTER_PROPOSED
                || booking.getCounterProposedBy() != CounterProposalAuthor.ADMIN) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Não existe uma proposta da equipa à qual possas responder");
        }

        Profile profile = lockActiveProfile(booking);
        NormalizedCounterProposal proposal = validateCounterProposal(booking, command, profile);
        booking.counterPropose(
                CounterProposalAuthor.CUSTOMER,
                command.message(),
                proposal.budget(),
                proposal.eventDate(),
                proposal.startTime(),
                proposal.endTime());
        Booking savedBooking = bookings.save(booking);
        userNotifications.resolveCounterProposalAction(savedBooking);
        userNotifications.createForCustomerCounterProposal(savedBooking);
        notifications.sendCustomerCounterProposal(savedBooking);
        return savedBooking;
    }

    @Transactional
    public Booking cancelMine(String email, Long bookingId, String message) {
        AppUser user = findActiveUser(email);
        Booking booking = bookings.findByIdWithProfile(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido de agendamento não encontrado"));

        assertOwner(user, booking);

        if (booking.getStatus() != BookingStatus.PENDING
                && booking.getStatus() != BookingStatus.COUNTER_PROPOSED
                && booking.getStatus() != BookingStatus.ACCEPTED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Este pedido já não pode ser cancelado pelo cliente");
        }

        String cancellationMessage = message == null || message.isBlank()
                ? "Pedido cancelado pelo cliente."
                : "Pedido cancelado pelo cliente: " + message.trim();
        boolean resolvesAdminProposal = booking.getStatus() == BookingStatus.COUNTER_PROPOSED
                && booking.getCounterProposedBy() == CounterProposalAuthor.ADMIN;
        booking.cancel(cancellationMessage);
        Booking savedBooking = bookings.save(booking);
        if (resolvesAdminProposal) {
            userNotifications.resolveCounterProposalAction(savedBooking);
        }
        userNotifications.createForCustomerCancellation(savedBooking);
        notifications.sendDecisionNotification(savedBooking);
        return savedBooking;
    }

    private NormalizedCounterProposal validateCounterProposal(
            Booking booking,
            CounterProposalCommand command,
            Profile profile) {
        validateTimeWindow(command.counterStartTime(), command.counterEndTime());
        ProposalTerms canonical = canonicalTerms(booking);
        ProposalTerms baseline = booking.getStatus() == BookingStatus.COUNTER_PROPOSED
                ? effectiveCounterTerms(booking)
                : canonical;
        ProposalTerms candidate = new ProposalTerms(
                command.counterEventDate() == null ? canonical.eventDate() : command.counterEventDate(),
                command.counterStartTime() == null ? canonical.startTime() : command.counterStartTime(),
                command.counterEndTime() == null ? canonical.endTime() : command.counterEndTime(),
                command.counterBudget() == null ? canonical.budget() : command.counterBudget());

        if (sameTerms(candidate, baseline)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A contraproposta tem de alterar a data, o horário ou o orçamento da proposta pendente");
        }

        if (candidate.budget() != null && candidate.budget().signum() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O orçamento da contraproposta tem de ser superior a zero");
        }

        if (candidate.eventDate().isBefore(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A data da contraproposta não pode ser no passado");
        }

        validateProposalDate(candidate.eventDate());
        validateTimeWindow(candidate.startTime(), candidate.endTime());
        assertNoAcceptedScheduleConflict(
                profile.getId(),
                candidate.eventDate(),
                candidate.startTime(),
                candidate.endTime(),
                booking.getId());

        return new NormalizedCounterProposal(
                sameAmount(candidate.budget(), canonical.budget()) ? null : candidate.budget(),
                candidate.eventDate().equals(canonical.eventDate()) ? null : candidate.eventDate(),
                candidate.startTime() == null ? null
                        : candidate.startTime().equals(canonical.startTime())
                                && candidate.endTime().equals(canonical.endTime()) ? null : candidate.startTime(),
                candidate.endTime() == null ? null
                        : candidate.startTime().equals(canonical.startTime())
                                && candidate.endTime().equals(canonical.endTime()) ? null : candidate.endTime());
    }

    private static boolean sameAmount(BigDecimal left, BigDecimal right) {
        return left == null ? right == null : right != null && left.compareTo(right) == 0;
    }

    private static ProposalTerms canonicalTerms(Booking booking) {
        return new ProposalTerms(
                booking.getEventDate(),
                booking.getStartTime(),
                booking.getEndTime(),
                booking.getBudget());
    }

    private static ProposalTerms effectiveCounterTerms(Booking booking) {
        return new ProposalTerms(
                proposedDate(booking),
                proposedStartTime(booking),
                proposedEndTime(booking),
                booking.getCounterBudget() == null ? booking.getBudget() : booking.getCounterBudget());
    }

    private static boolean sameTerms(ProposalTerms left, ProposalTerms right) {
        return left.eventDate().equals(right.eventDate())
                && java.util.Objects.equals(left.startTime(), right.startTime())
                && java.util.Objects.equals(left.endTime(), right.endTime())
                && sameAmount(left.budget(), right.budget());
    }

    private Profile lockActiveProfile(Booking booking) {
        return profiles.findByIdAndActiveTrueForUpdate(booking.getProfile().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "O perfil deste artista já não está disponível"));
    }

    private void validateFinalScheduleAndConflict(
            Booking booking,
            Profile profile,
            LocalDate date,
            LocalTime start,
            LocalTime end) {
        validateProposalDate(date);
        validateTimeWindow(start, end);
        assertNoAcceptedScheduleConflict(profile.getId(), date, start, end, booking.getId());
    }

    private static LocalDate proposedDate(Booking booking) {
        return booking.getCounterEventDate() == null ? booking.getEventDate() : booking.getCounterEventDate();
    }

    private static LocalTime proposedStartTime(Booking booking) {
        return booking.getCounterStartTime() == null ? booking.getStartTime() : booking.getCounterStartTime();
    }

    private static LocalTime proposedEndTime(Booking booking) {
        return booking.getCounterEndTime() == null ? booking.getEndTime() : booking.getCounterEndTime();
    }

    private static void assertOwner(AppUser user, Booking booking) {
        if (!booking.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Não tens permissão para alterar este pedido");
        }
    }

    private static void assertNoDecisionTerms(DecisionBookingCommand command) {
        if (command.hasAcceptedTerms() || command.hasCounterTerms()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Aceitar ou recusar não permite alterar termos; usa Propor alterações");
        }
    }

    private static void assertNoAcceptedTerms(DecisionBookingCommand command) {
        if (command.hasAcceptedTerms()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Usa apenas os campos de contraproposta para propor alterações");
        }
    }

    private void assertNoDuplicateActiveRequest(
            Long userId,
            Long profileId,
            LocalDate eventDate,
            LocalTime startTime,
            LocalTime endTime) {
        boolean hasDuplicate = bookings.findUserBookingsOnDateWithStatuses(
                        userId,
                        profileId,
                        eventDate,
                        Set.of(BookingStatus.PENDING, BookingStatus.COUNTER_PROPOSED, BookingStatus.ACCEPTED))
                .stream()
                .anyMatch(existing -> overlaps(existing.getStartTime(), existing.getEndTime(), startTime, endTime));

        if (hasDuplicate) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Já tens um pedido ativo para este artista nesse horário.");
        }
    }

    private void assertNoAcceptedScheduleConflict(
            Long profileId,
            LocalDate eventDate,
            LocalTime startTime,
            LocalTime endTime,
            Long currentBookingId) {
        boolean hasConflict = bookings.findProfileBookingsOnDateWithStatuses(
                        profileId,
                        eventDate,
                        Set.of(BookingStatus.ACCEPTED, BookingStatus.COUNTER_PROPOSED))
                .stream()
                .filter(existing -> currentBookingId == null || !existing.getId().equals(currentBookingId))
                .anyMatch(existing -> overlaps(existing.getStartTime(), existing.getEndTime(), startTime, endTime));

        if (hasConflict) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "O artista já tem um evento confirmado ou em negociação nesse horário. Escolhe outro intervalo.");
        }
    }

    private static boolean overlaps(
            LocalTime existingStart,
            LocalTime existingEnd,
            LocalTime requestedStart,
            LocalTime requestedEnd) {
        if (existingStart == null || existingEnd == null || requestedStart == null || requestedEnd == null) {
            return true;
        }
        return requestedStart.isBefore(existingEnd) && existingStart.isBefore(requestedEnd);
    }

    private static void validateTimeWindow(LocalTime startTime, LocalTime endTime) {
        if ((startTime == null) != (endTime == null)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Indica a hora de início e a hora de fim, ou deixa ambas em branco");
        }
        if (startTime != null && !startTime.isBefore(endTime)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A hora de fim tem de ser posterior à hora de início");
        }
    }

    private static void validateEventSpecificFields(CreateBookingCommand command) {
        if (command.eventType() == BookingEventType.WEDDING
                && (command.weddingCoupleNames() == null || command.weddingCoupleNames().isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Indica os nomes dos noivos");
        }
        if (command.eventType() == BookingEventType.OTHER
                && (command.customEventType() == null || command.customEventType().isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Indica o tipo de evento");
        }
    }

    private static void validateProposalDate(LocalDate eventDate) {
        if (eventDate == null || eventDate.isBefore(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A data do evento não pode ser no passado");
        }
    }

    private static void validateDateRange(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Indica a data inicial e a data final do calendário");
        }
        if (from.isAfter(to)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A data inicial tem de ser anterior à data final");
        }
        if (from.plusYears(1).isBefore(to)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O intervalo do calendário não pode exceder um ano");
        }
    }

    private AppUser findActiveUser(String email) {
        return users.findByEmailAndActiveTrue(email.trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "A sessão já não é válida"));
    }

    public record CreateBookingCommand(
            String profileSlug,
            LocalDate eventDate,
            LocalTime startTime,
            LocalTime endTime,
            BookingEventType eventType,
            String customEventType,
            String weddingCoupleNames,
            String location,
            String contactName,
            String contactEmail,
            String contactPhone,
            String description,
            String notes) {
    }

    public record DecisionBookingCommand(
            BookingStatus status,
            String message,
            LocalDate eventDate,
            LocalTime startTime,
            LocalTime endTime,
            BigDecimal agreedBudget,
            BigDecimal counterBudget,
            LocalDate counterEventDate,
            LocalTime counterStartTime,
            LocalTime counterEndTime) implements CounterProposalCommand {

        boolean hasAcceptedTerms() {
            return eventDate != null || startTime != null || endTime != null || agreedBudget != null;
        }

        boolean hasCounterTerms() {
            return counterBudget != null || counterEventDate != null || counterStartTime != null || counterEndTime != null;
        }
    }

    public interface CounterProposalCommand {
        String message();
        BigDecimal counterBudget();
        LocalDate counterEventDate();
        LocalTime counterStartTime();
        LocalTime counterEndTime();
    }

    public record CustomerCounterProposalCommand(
            String message,
            BigDecimal counterBudget,
            LocalDate counterEventDate,
            LocalTime counterStartTime,
            LocalTime counterEndTime) implements CounterProposalCommand {
    }

    private record ProposalTerms(
            LocalDate eventDate,
            LocalTime startTime,
            LocalTime endTime,
            BigDecimal budget) {
    }

    private record NormalizedCounterProposal(
            BigDecimal budget,
            LocalDate eventDate,
            LocalTime startTime,
            LocalTime endTime) {
    }
}
