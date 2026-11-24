package com.example.eventhub.event;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record EventResponse(
        UUID id,
        String title,
        @Nullable String description,
        Instant startsAt,
        String venue,
        int capacity,
        int availableSeats) {

    static EventResponse from(Event event) {
        return new EventResponse(
                Objects.requireNonNull(event.getId(), "event must be persisted"),
                event.getTitle(),
                event.getDescription(),
                event.getStartsAt(),
                event.getVenue(),
                event.getCapacity(),
                event.getAvailableSeats());
    }
}
