package pt.saltosnaspalhacadas.backend.booking.api;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import pt.saltosnaspalhacadas.backend.booking.Booking;
import pt.saltosnaspalhacadas.backend.booking.BookingStatus;

public record AvailabilityResponse(List<LocalDate> bookedDates, List<AvailabilitySlotResponse> slots) {

    public static AvailabilityResponse from(List<Booking> bookings) {
        List<AvailabilitySlotResponse> slots = bookings.stream()
                .map(AvailabilitySlotResponse::from)
                .toList();
        List<LocalDate> bookedDates = slots.stream()
                .filter(slot -> slot.status() == BookingStatus.ACCEPTED)
                .filter(slot -> slot.startTime() == null || slot.endTime() == null)
                .map(AvailabilitySlotResponse::date)
                .distinct()
                .toList();
        return new AvailabilityResponse(bookedDates, slots);
    }

    public record AvailabilitySlotResponse(
            LocalDate date,
            LocalTime startTime,
            LocalTime endTime,
            BookingStatus status) {

        static AvailabilitySlotResponse from(Booking booking) {
            return new AvailabilitySlotResponse(
                    booking.getEventDate(),
                    booking.getStartTime(),
                    booking.getEndTime(),
                    booking.getStatus());
        }
    }
}
