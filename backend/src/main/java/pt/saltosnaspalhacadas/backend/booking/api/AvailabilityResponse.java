package pt.saltosnaspalhacadas.backend.booking.api;

import java.time.LocalDate;
import java.util.List;

public record AvailabilityResponse(List<LocalDate> bookedDates) {
}
