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

@Service
public class BookingService {

    private final BookingRepository bookings;
    private final AppUserRepository users;
    private final ProfileRepository profiles;
    private final BookingNotificationService notifications;

    public BookingService(
            BookingRepository bookings,
            AppUserRepository users,
            ProfileRepository profiles,
            BookingNotificationService notifications) {
        this.bookings = bookings;
        this.users = users;
        this.profiles = profiles;
        this.notifications = notifications;
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
        return bookings.findAvailabilitySlots(profileSlug, Set.of(BookingStatus.PENDING, BookingStatus.ACCEPTED), from, to);
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
            notifications.sendDecisionNotification(savedBooking);
            return savedBooking;
        }

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Este pedido já não está pendente de decisão da administração");
        }

        // Locking the profile serializes decisions for the same artist. It avoids two
        // administrators accepting different proposals for the same date concurrently.
        Profile profile = profiles.findByIdAndActiveTrueForUpdate(booking.getProfile().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "O perfil deste artista já não está disponível"));

        if (command.status() == BookingStatus.ACCEPTED) {
            LocalDate finalEventDate = command.eventDate() == null ? booking.getEventDate() : command.eventDate();
            LocalTime finalStartTime = command.hasScheduleOverride() ? command.startTime() : booking.getStartTime();
            LocalTime finalEndTime = command.hasScheduleOverride() ? command.endTime() : booking.getEndTime();

            validateProposalDate(finalEventDate);
            validateTimeWindow(finalStartTime, finalEndTime);
            assertNoAcceptedScheduleConflict(profile.getId(), finalEventDate, finalStartTime, finalEndTime, booking.getId());
            assertNoCounterValues(command);
            validateAgreedBudget(command.agreedBudget());
            booking.accept(finalEventDate, finalStartTime, finalEndTime, command.agreedBudget(), command.message());
        } else if (command.status() == BookingStatus.COUNTER_PROPOSED) {
            assertNoAgreedBudget(command);
            validateCounterProposal(command, profile, booking.getId());
            booking.counterPropose(command.message(), command.counterBudget(), command.counterEventDate());
        } else {
            assertNoCounterValues(command);
            assertNoAgreedBudget(command);
            booking.decline(command.message());
        }

        Booking savedBooking = bookings.save(booking);
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

        if (decision == CounterProposalDecision.DECLINED) {
            booking.declineCounterProposal();
            Booking savedBooking = bookings.save(booking);
            notifications.sendCounterProposalResponseConfirmation(savedBooking, decision);
            return savedBooking;
        }

        Profile profile = profiles.findByIdAndActiveTrueForUpdate(booking.getProfile().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "O perfil deste artista já não está disponível"));
        LocalDate acceptedEventDate = booking.getCounterEventDate() == null
                ? booking.getEventDate()
                : booking.getCounterEventDate();
        BigDecimal acceptedBudget = booking.getCounterBudget() == null
                ? booking.getBudget()
                : booking.getCounterBudget();

        validateProposalDate(acceptedEventDate);
        assertNoAcceptedScheduleConflict(
                profile.getId(),
                acceptedEventDate,
                booking.getStartTime(),
                booking.getEndTime(),
                booking.getId());

        booking.acceptCounterProposal(acceptedEventDate, acceptedBudget);
        Booking savedBooking = bookings.save(booking);
        notifications.sendCounterProposalResponseConfirmation(savedBooking, decision);
        return savedBooking;
    }

    @Transactional
    public Booking cancelMine(String email, Long bookingId, String message) {
        AppUser user = findActiveUser(email);
        Booking booking = bookings.findByIdWithProfile(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido de agendamento não encontrado"));

        if (!booking.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Não tens permissão para cancelar este pedido");
        }

        if (booking.getStatus() != BookingStatus.PENDING && booking.getStatus() != BookingStatus.COUNTER_PROPOSED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Este pedido já não pode ser cancelado pelo cliente");
        }

        String cancellationMessage = message == null || message.isBlank()
                ? "Pedido cancelado pelo cliente."
                : "Pedido cancelado pelo cliente: " + message.trim();
        booking.cancel(cancellationMessage);
        Booking savedBooking = bookings.save(booking);
        notifications.sendDecisionNotification(savedBooking);
        return savedBooking;
    }

    private void validateCounterProposal(DecisionBookingCommand command, Profile profile, Long bookingId) {
        if (command.counterBudget() == null && command.counterEventDate() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Indica um novo orçamento, uma nova data, ou ambos na contraproposta");
        }

        if (command.counterBudget() != null && command.counterBudget().signum() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O orçamento da contraproposta tem de ser superior a zero");
        }

        if (command.counterEventDate() != null) {
            if (!command.counterEventDate().isAfter(LocalDate.now())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A nova data da contraproposta tem de ser futura");
            }
            assertNoAcceptedScheduleConflict(profile.getId(), command.counterEventDate(), null, null, bookingId);
        }
    }

    private void assertNoCounterValues(DecisionBookingCommand command) {
        if (command.counterBudget() != null || command.counterEventDate() != null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Os dados de contraproposta só podem ser enviados para uma contraproposta");
        }
    }

    private static void validateAgreedBudget(BigDecimal agreedBudget) {
        if (agreedBudget != null && agreedBudget.signum() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O orçamento acordado tem de ser superior a zero");
        }
    }

    private static void assertNoAgreedBudget(DecisionBookingCommand command) {
        if (command.agreedBudget() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O orçamento acordado só pode ser guardado ao confirmar o pedido");
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
                        Set.of(BookingStatus.ACCEPTED))
                .stream()
                .filter(existing -> currentBookingId == null || !existing.getId().equals(currentBookingId))
                .anyMatch(existing -> overlaps(existing.getStartTime(), existing.getEndTime(), startTime, endTime));

        if (hasConflict) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "O artista já tem um evento aceite nesse horário. Escolhe outro intervalo.");
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
            LocalDate counterEventDate) {

        boolean hasScheduleOverride() {
            return eventDate != null || startTime != null || endTime != null;
        }
    }
}
