package pt.saltosnaspalhacadas.backend.booking.api;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import pt.saltosnaspalhacadas.backend.booking.BookingEventType;
import pt.saltosnaspalhacadas.backend.booking.BookingService;
import pt.saltosnaspalhacadas.backend.booking.CounterProposalDecision;

@RestController
@RequestMapping("/api/v1/bookings")
public class BookingController {

    private final BookingService bookings;

    public BookingController(BookingService bookings) {
        this.bookings = bookings;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    BookingResponse createBooking(Authentication authentication, @Valid @RequestBody CreateBookingRequest request) {
        return BookingResponse.from(bookings.create(currentEmail(authentication), new BookingService.CreateBookingCommand(
                request.profileSlug(),
                request.eventDate(),
                request.startTime(),
                request.endTime(),
                request.eventType(),
                request.customEventType(),
                request.weddingCoupleNames(),
                request.location(),
                request.contactName(),
                request.contactEmail(),
                request.contactPhone(),
                request.description(),
                request.notes())));
    }

    @GetMapping("/mine")
    List<BookingResponse> findMyBookings(Authentication authentication) {
        return bookings.findMine(currentEmail(authentication)).stream().map(BookingResponse::from).toList();
    }

    @PutMapping("/{bookingId}/counter-proposal/decision")
    BookingResponse respondToCounterProposal(
            Authentication authentication,
            @PathVariable Long bookingId,
            @Valid @RequestBody CounterProposalDecisionRequest request) {
        return BookingResponse.from(bookings.respondToCounterProposal(
                currentEmail(authentication),
                bookingId,
                request.decision()));
    }

    @PutMapping("/{bookingId}/cancel")
    BookingResponse cancelBooking(
            Authentication authentication,
            @PathVariable Long bookingId,
            @Valid @RequestBody(required = false) CancelBookingRequest request) {
        return BookingResponse.from(bookings.cancelMine(
                currentEmail(authentication),
                bookingId,
                request == null ? null : request.message()));
    }

    private static String currentEmail(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Inicia sessão para continuar");
        }
        return authentication.getName();
    }

    record CreateBookingRequest(
            @NotBlank(message = "Escolhe o perfil do artista")
            @Size(max = 100, message = "O perfil escolhido é inválido")
            @Pattern(regexp = "[a-z0-9]+(?:-[a-z0-9]+)*", message = "O perfil escolhido é inválido")
            String profileSlug,
            @NotNull(message = "Escolhe a data do evento")
            @FutureOrPresent(message = "A data do evento não pode ser no passado")
            LocalDate eventDate,
            LocalTime startTime,
            LocalTime endTime,
            @NotNull(message = "Escolhe o tipo de evento")
            BookingEventType eventType,
            @Size(max = 120, message = "O tipo de evento pode ter no máximo 120 caracteres")
            String customEventType,
            @Size(max = 180, message = "Os nomes dos noivos podem ter no máximo 180 caracteres")
            String weddingCoupleNames,
            @NotBlank(message = "Indica o local do evento")
            @Size(max = 180, message = "O local pode ter no máximo 180 caracteres")
            String location,
            @NotBlank(message = "Indica o teu nome")
            @Size(max = 120, message = "O nome pode ter no máximo 120 caracteres")
            String contactName,
            @NotBlank(message = "Indica o teu email de contacto")
            @Email(message = "Indica um email válido")
            @Size(max = 254, message = "O email pode ter no máximo 254 caracteres")
            String contactEmail,
            @NotBlank(message = "Indica um contacto telefónico")
            @Size(max = 40, message = "O contacto telefónico é demasiado longo")
            @Pattern(regexp = "(?=(?:\\D*\\d){6,}\\D*$)[0-9+(). -]{6,40}", message = "Indica um contacto telefónico válido")
            String contactPhone,
            @NotBlank(message = "Descreve o evento e os serviços pretendidos")
            @Size(max = 2000, message = "A descrição pode ter no máximo 2000 caracteres")
            String description,
            @Size(max = 1000, message = "As notas podem ter no máximo 1000 caracteres")
            String notes) {
    }

    record CounterProposalDecisionRequest(
            @NotNull(message = "Escolhe se aceitas ou recusas a contraproposta")
            CounterProposalDecision decision) {
    }

    record CancelBookingRequest(
            @Size(max = 1000, message = "A mensagem pode ter no máximo 1000 caracteres")
            String message) {
    }
}
