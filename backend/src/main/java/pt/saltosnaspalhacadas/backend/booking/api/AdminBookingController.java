package pt.saltosnaspalhacadas.backend.booking.api;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import pt.saltosnaspalhacadas.backend.booking.BookingService;
import pt.saltosnaspalhacadas.backend.booking.BookingStatus;

@RestController
@RequestMapping("/api/v1/admin/bookings")
public class AdminBookingController {

    private final BookingService bookings;

    public AdminBookingController(BookingService bookings) {
        this.bookings = bookings;
    }

    @GetMapping
    List<BookingResponse> findBookings(@RequestParam(required = false) BookingStatus status) {
        return bookings.findForAdmin(status).stream().map(BookingResponse::from).toList();
    }

    @PutMapping("/{bookingId}/decision")
    BookingResponse decide(
            @PathVariable Long bookingId,
            @Valid @RequestBody DecisionBookingRequest request) {
        return BookingResponse.from(bookings.decide(bookingId, new BookingService.DecisionBookingCommand(
                request.status(),
                request.message(),
                request.counterBudget(),
                request.counterEventDate())));
    }

    record DecisionBookingRequest(
            @NotNull(message = "Escolhe a decisão para esta proposta")
            BookingStatus status,
            @Size(max = 1000, message = "A mensagem pode ter no máximo 1000 caracteres")
            String message,
            @DecimalMin(value = "0.01", message = "O orçamento da contraproposta tem de ser superior a zero")
            @Digits(integer = 8, fraction = 2, message = "O orçamento só pode ter duas casas decimais")
            BigDecimal counterBudget,
            @Future(message = "A nova data da contraproposta tem de ser futura")
            LocalDate counterEventDate) {
    }
}
