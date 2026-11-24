package com.example.eventhub.event;

import com.example.eventhub.common.ConflictException;
import com.example.eventhub.common.NotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Public entry point of the event feature. Other features use this, never {@link EventRepository}.
 */
@Service
@Transactional(readOnly = true)
public class EventService {

    private final EventRepository repository;

    EventService(EventRepository repository) {
        this.repository = repository;
    }

    public List<EventResponse> findAll() {
        return repository.findAll(Sort.by("startsAt")).stream()
                .map(EventResponse::from)
                .toList();
    }

    public EventResponse findById(UUID id) {
        return repository.findById(id)
                .map(EventResponse::from)
                .orElseThrow(() -> new NotFoundException("Event " + id + " not found"));
    }

    /**
     * Creates an event. The request must already be validated ({@code @Valid} in the controller).
     */
    @Transactional
    public EventResponse create(CreateEventRequest request) {
        Event event = new Event(
                Objects.requireNonNull(request.title()).strip(),
                blankToNull(request.description()),
                Objects.requireNonNull(request.startsAt()),
                Objects.requireNonNull(request.venue()).strip(),
                Objects.requireNonNull(request.capacity()));
        return EventResponse.from(repository.save(event));
    }

    /**
     * Reserves seats for a booking. Must run inside the caller's transaction so that the reservation is
     * rolled back if the booking itself cannot be saved.
     * <p>
     * The check and the increment are one conditional {@code UPDATE}: the database locks the event row for
     * the statement, so concurrent bookings are serialised and can never exceed the capacity.
     *
     * @throws NotFoundException  if the event does not exist
     * @throws ConflictException if the event has started or there are not enough free seats
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void reserveSeats(UUID eventId, int seats) {
        Instant now = Instant.now();
        if (repository.reserveSeats(eventId, seats, now) == 1) {
            return;
        }
        Event event = repository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event " + eventId + " not found"));
        if (!event.getStartsAt().isAfter(now)) {
            throw new ConflictException("Sündmus on juba alanud, broneerida ei saa");
        }
        int available = event.getAvailableSeats();
        throw new ConflictException(
                available == 0 ? "Vabu kohti pole enam" : "Vabu kohti pole piisavalt (alles " + available + ")",
                Map.of("availableSeats", available));
    }

    private static @Nullable String blankToNull(@Nullable String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
