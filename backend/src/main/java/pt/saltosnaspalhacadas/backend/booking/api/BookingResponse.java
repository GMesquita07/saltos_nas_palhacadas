package pt.saltosnaspalhacadas.backend.booking.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import pt.saltosnaspalhacadas.backend.booking.Booking;
import pt.saltosnaspalhacadas.backend.booking.BookingEventType;
import pt.saltosnaspalhacadas.backend.booking.BookingStatus;

public record BookingResponse(
        Long id,
        String profileSlug,
        String profileName,
        LocalDate eventDate,
        BookingEventType eventType,
        String contactName,
        String contactPhone,
        BigDecimal budget,
        String description,
        String notes,
        BookingStatus status,
        CounterProposalResponse counterProposal,
        String message,
        Instant createdAt,
        Instant updatedAt) {

    public static BookingResponse from(Booking booking) {
        CounterProposalResponse counterProposal = booking.getStatus() == BookingStatus.COUNTER_PROPOSED
                ? new CounterProposalResponse(booking.getCounterBudget(), booking.getCounterEventDate())
                : null;

        return new BookingResponse(
                booking.getId(),
                booking.getProfile().getSlug(),
                booking.getProfile().getName(),
                booking.getEventDate(),
                booking.getEventType(),
                booking.getContactName(),
                booking.getContactPhone(),
                booking.getBudget(),
                booking.getDescription(),
                booking.getNotes(),
                booking.getStatus(),
                counterProposal,
                booking.getAdminMessage(),
                booking.getCreatedAt(),
                booking.getUpdatedAt());
    }

    public record CounterProposalResponse(BigDecimal budget, LocalDate eventDate) {
    }
}
