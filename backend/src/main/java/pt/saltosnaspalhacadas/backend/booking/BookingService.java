package pt.saltosnaspalhacadas.backend.booking;

import java.math.BigDecimal;
import java.time.LocalDate;
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

    public BookingService(BookingRepository bookings, AppUserRepository users, ProfileRepository profiles) {
        this.bookings = bookings;
        this.users = users;
        this.profiles = profiles;
    }

    @Transactional
    public Booking create(String email, CreateBookingCommand command) {
        validateProposalDate(command.eventDate());

        AppUser user = findActiveUser(email);
        Profile profile = profiles.findBySlugAndActiveTrue(command.profileSlug())
                .orElseThrow(() -> new ProfileNotFoundException(command.profileSlug()));

        if (bookings.existsByProfileIdAndEventDateAndStatus(
                profile.getId(), command.eventDate(), BookingStatus.ACCEPTED)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "O artista já tem um evento aceite nesta data. Escolhe outro dia.");
        }

        if (bookings.existsByUserIdAndProfileIdAndEventDateAndStatusIn(
                user.getId(),
                profile.getId(),
                command.eventDate(),
                Set.of(BookingStatus.PENDING, BookingStatus.COUNTER_PROPOSED))) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Já tens uma proposta ativa para este artista nesta data.");
        }

        return bookings.save(new Booking(
                user,
                profile,
                command.eventDate(),
                command.eventType(),
                command.contactName(),
                command.contactPhone(),
                command.budget(),
                command.description(),
                command.notes()));
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
    public List<LocalDate> findBookedDates(String profileSlug, LocalDate from, LocalDate to) {
        profiles.findBySlugAndActiveTrue(profileSlug).orElseThrow(() -> new ProfileNotFoundException(profileSlug));
        validateDateRange(from, to);
        return bookings.findBookedDates(profileSlug, BookingStatus.ACCEPTED, from, to);
    }

    @Transactional
    public Booking decide(Long bookingId, DecisionBookingCommand command) {
        Booking booking = bookings.findByIdWithProfile(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Proposta de agendamento não encontrada"));

        if (command.status() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Escolhe a decisão para esta proposta");
        }

        if (command.status() == BookingStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Escolhe aceitar, recusar ou enviar uma contraproposta");
        }

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Esta proposta já não está pendente de decisão da administração");
        }

        // Locking the profile serializes decisions for the same artist. It avoids two
        // administrators accepting different proposals for the same date concurrently.
        Profile profile = profiles.findByIdAndActiveTrueForUpdate(booking.getProfile().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "O perfil deste artista já não está disponível"));

        if (command.status() == BookingStatus.ACCEPTED) {
            validateProposalDate(booking.getEventDate());
            assertDateIsFree(profile.getId(), booking.getEventDate(), booking.getId());
            assertNoCounterValues(command);
        } else if (command.status() == BookingStatus.COUNTER_PROPOSED) {
            validateCounterProposal(command, profile, booking.getId());
        } else {
            assertNoCounterValues(command);
        }

        booking.decide(
                command.status(),
                command.message(),
                command.counterBudget(),
                command.counterEventDate());
        return bookings.save(booking);
    }

    @Transactional
    public Booking respondToCounterProposal(String email, Long bookingId, CounterProposalDecision decision) {
        AppUser user = findActiveUser(email);
        Booking booking = bookings.findByIdWithProfile(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Proposta de agendamento não encontrada"));

        if (decision == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Escolhe se aceitas ou recusas a contraproposta");
        }

        if (!booking.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Não tens permissão para responder a esta proposta");
        }

        if (booking.getStatus() != BookingStatus.COUNTER_PROPOSED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Esta proposta não tem uma contraproposta pendente");
        }

        if (decision == CounterProposalDecision.DECLINED) {
            booking.declineCounterProposal();
            return bookings.save(booking);
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
        if (acceptedBudget == null || acceptedBudget.signum() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O orçamento acordado tem de ser superior a zero");
        }
        assertDateIsFree(profile.getId(), acceptedEventDate, booking.getId());

        booking.acceptCounterProposal(acceptedEventDate, acceptedBudget);
        return bookings.save(booking);
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
            assertDateIsFree(profile.getId(), command.counterEventDate(), bookingId);
        }
    }

    private void assertNoCounterValues(DecisionBookingCommand command) {
        if (command.counterBudget() != null || command.counterEventDate() != null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Os dados de contraproposta só podem ser enviados para uma contraproposta");
        }
    }

    private void assertDateIsFree(Long profileId, LocalDate eventDate, Long currentBookingId) {
        if (bookings.existsByProfileIdAndEventDateAndStatusAndIdNot(
                profileId,
                eventDate,
                BookingStatus.ACCEPTED,
                currentBookingId)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "O artista já tem um evento aceite nesta data. Escolhe outro dia.");
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
            BookingEventType eventType,
            String contactName,
            String contactPhone,
            BigDecimal budget,
            String description,
            String notes) {
    }

    public record DecisionBookingCommand(
            BookingStatus status,
            String message,
            BigDecimal counterBudget,
            LocalDate counterEventDate) {
    }
}
