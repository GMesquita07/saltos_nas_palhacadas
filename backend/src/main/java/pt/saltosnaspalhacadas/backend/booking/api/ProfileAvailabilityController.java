package pt.saltosnaspalhacadas.backend.booking.api;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import pt.saltosnaspalhacadas.backend.booking.BookingService;

@RestController
@RequestMapping("/api/v1/profiles/{slug}/availability")
public class ProfileAvailabilityController {

    private final BookingService bookings;

    public ProfileAvailabilityController(BookingService bookings) {
        this.bookings = bookings;
    }

    @GetMapping
    AvailabilityResponse findAvailability(
            @PathVariable String slug,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return AvailabilityResponse.from(bookings.findAvailability(slug, from, to));
    }
}
