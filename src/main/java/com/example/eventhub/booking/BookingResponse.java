package com.example.eventhub.booking;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record BookingResponse(UUID id, UUID eventId, String name, String email, int seats, Instant createdAt) {

    static BookingResponse from(Booking booking) {
        return new BookingResponse(
                Objects.requireNonNull(booking.getId(), "booking must be persisted"),
                booking.getEventId(),
                booking.getName(),
                booking.getEmail(),
                booking.getSeats(),
                booking.getCreatedAt());
    }
}
