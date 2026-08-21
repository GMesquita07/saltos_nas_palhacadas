package pt.saltosnaspalhacadas.backend.booking.api;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
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
                request.eventType(),
                request.contactName(),
                request.contactPhone(),
                request.budget(),
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
            @NotNull(message = "Escolhe o tipo de evento")
            BookingEventType eventType,
            @NotBlank(message = "Indica o teu nome")
            @Size(max = 120, message = "O nome pode ter no máximo 120 caracteres")
            String contactName,
            @NotBlank(message = "Indica um contacto telefónico")
            @Size(max = 40, message = "O contacto telefónico é demasiado longo")
            @Pattern(regexp = "(?=(?:\\D*\\d){6,}\\D*$)[0-9+(). -]{6,40}", message = "Indica um contacto telefónico válido")
            String contactPhone,
            @NotNull(message = "Indica uma proposta de orçamento")
            @DecimalMin(value = "0.01", message = "O orçamento tem de ser superior a zero")
            @Digits(integer = 8, fraction = 2, message = "O orçamento só pode ter duas casas decimais")
            BigDecimal budget,
            @NotBlank(message = "Descreve o evento")
            @Size(max = 2000, message = "A descrição pode ter no máximo 2000 caracteres")
            String description,
            @Size(max = 1000, message = "As notas podem ter no máximo 1000 caracteres")
            String notes) {
    }

    record CounterProposalDecisionRequest(
            @NotNull(message = "Escolhe se aceitas ou recusas a contraproposta")
            CounterProposalDecision decision) {
    }
}
