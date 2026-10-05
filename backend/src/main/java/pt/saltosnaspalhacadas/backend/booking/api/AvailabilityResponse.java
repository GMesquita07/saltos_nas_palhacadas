package pt.saltosnaspalhacadas.backend.booking.api;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import pt.saltosnaspalhacadas.backend.booking.Booking;
import pt.saltosnaspalhacadas.backend.booking.BookingStatus;

public record AvailabilityResponse(List<LocalDate> bookedDates, List<AvailabilitySlotResponse> slots) {

    public static AvailabilityResponse from(List<Booking> bookings, LocalDate from, LocalDate to) {
        List<AvailabilitySlotResponse> slots = bookings.stream()
                .flatMap(booking -> AvailabilitySlotResponse.from(booking).stream())
                .filter(slot -> !slot.date().isBefore(from) && !slot.date().isAfter(to))
                .toList();
        List<LocalDate> bookedDates = slots.stream()
                .filter(slot -> slot.status() == BookingStatus.ACCEPTED
                        || slot.status() == BookingStatus.COUNTER_PROPOSED)
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

        static List<AvailabilitySlotResponse> from(Booking booking) {
            AvailabilitySlotResponse canonical = new AvailabilitySlotResponse(
                    booking.getEventDate(),
                    booking.getStartTime(),
                    booking.getEndTime(),
                    booking.getStatus());

            if (booking.getStatus() != BookingStatus.COUNTER_PROPOSED
                    || (booking.getCounterEventDate() == null
                        && booking.getCounterStartTime() == null
                        && booking.getCounterEndTime() == null)) {
                return List.of(canonical);
            }

            AvailabilitySlotResponse proposed = new AvailabilitySlotResponse(
                    booking.getCounterEventDate() == null
                            ? booking.getEventDate()
                            : booking.getCounterEventDate(),
                    booking.getCounterStartTime() == null
                            ? booking.getStartTime()
                            : booking.getCounterStartTime(),
                    booking.getCounterEndTime() == null
                            ? booking.getEndTime()
                            : booking.getCounterEndTime(),
                    BookingStatus.COUNTER_PROPOSED);

            return List.of(canonical, proposed);
        }
    }
}
