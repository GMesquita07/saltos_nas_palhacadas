package pt.saltosnaspalhacadas.backend.booking.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

import pt.saltosnaspalhacadas.backend.booking.Booking;
import pt.saltosnaspalhacadas.backend.booking.BookingEventType;
import pt.saltosnaspalhacadas.backend.booking.BookingStatus;

public record BookingResponse(
        Long id,
        String profileSlug,
        String profileName,
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
        BigDecimal budget,
        String description,
        String notes,
        BookingStatus status,
        CounterProposalResponse counterProposal,
        String message,
        Instant createdAt,
        Instant updatedAt) {

    public static BookingResponse from(Booking booking) {
        return from(booking, false);
    }

    public static BookingResponse fromAdmin(Booking booking) {
        return from(booking, true);
    }

    private static BookingResponse from(Booking booking, boolean includeBudget) {
        CounterProposalResponse counterProposal = booking.getStatus() == BookingStatus.COUNTER_PROPOSED
                ? new CounterProposalResponse(booking.getCounterBudget(), booking.getCounterEventDate())
                : null;

        return new BookingResponse(
                booking.getId(),
                booking.getProfile().getSlug(),
                booking.getProfile().getName(),
                booking.getEventDate(),
                booking.getStartTime(),
                booking.getEndTime(),
                booking.getEventType(),
                booking.getCustomEventType(),
                booking.getWeddingCoupleNames(),
                booking.getLocation(),
                booking.getContactName(),
                booking.getContactEmail(),
                booking.getContactPhone(),
                includeBudget ? booking.getBudget() : null,
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
