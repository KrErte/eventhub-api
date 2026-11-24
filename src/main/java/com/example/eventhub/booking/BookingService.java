package com.example.eventhub.booking;

import com.example.eventhub.common.ConflictException;
import com.example.eventhub.event.EventService;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookingService {

    static final String DUPLICATE_EMAIL = "Selle e-postiga on sellele sündmusele juba broneeritud";

    private final BookingRepository repository;
    private final EventService eventService;

    BookingService(BookingRepository repository, EventService eventService) {
        this.repository = repository;
        this.eventService = eventService;
    }

    /**
     * Books seats for an event. Everything happens in one transaction: if any step fails, the reserved
     * seats are released again.
     * <ol>
     *   <li>Reserve the seats on the event (atomic; also locks the event row until commit, so bookings for
     *       the same event run one after another).</li>
     *   <li>Reject a second booking with the same e-mail. Because of the row lock this check cannot race;
     *       the unique index {@code uq_booking_event_email} is a safety net.</li>
     *   <li>Save the booking.</li>
     * </ol>
     * The request must already be validated ({@code @Valid} in the controller).
     */
    @Transactional
    public BookingResponse create(UUID eventId, CreateBookingRequest request) {
        int seats = Objects.requireNonNull(request.seats());
        String email = Objects.requireNonNull(request.email()).strip().toLowerCase(Locale.ROOT);

        eventService.reserveSeats(eventId, seats);

        if (repository.existsByEventIdAndEmail(eventId, email)) {
            throw duplicateEmail();
        }
        Booking booking = new Booking(
                eventId, Objects.requireNonNull(request.name()).strip(), email, seats, Instant.now());
        try {
            return BookingResponse.from(repository.saveAndFlush(booking));
        } catch (DataIntegrityViolationException e) {
            if (isDuplicateEmail(e)) {
                throw duplicateEmail();
            }
            throw e;
        }
    }

    private static boolean isDuplicateEmail(DataIntegrityViolationException e) {
        String message = e.getMostSpecificCause().getMessage();
        return message != null && message.contains("uq_booking_event_email");
    }

    private static ConflictException duplicateEmail() {
        return new ConflictException(DUPLICATE_EMAIL, Map.of("errors", Map.of("email", DUPLICATE_EMAIL)));
    }
}
