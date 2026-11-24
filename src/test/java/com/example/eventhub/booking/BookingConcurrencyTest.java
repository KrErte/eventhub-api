package com.example.eventhub.booking;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.eventhub.TestcontainersConfiguration;
import com.example.eventhub.common.ConflictException;
import com.example.eventhub.event.CreateEventRequest;
import com.example.eventhub.event.EventService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.IntFunction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Many simultaneous bookings must never oversell an event or create duplicate bookings.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class BookingConcurrencyTest {

    private static final int THREADS = 10;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private EventService eventService;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void cleanDatabase() {
        jdbc.execute("TRUNCATE booking, event");
    }

    private UUID futureEvent(int capacity) {
        return eventService.create(new CreateEventRequest(
                "Populaarne", null, Instant.now().plus(7, ChronoUnit.DAYS), "Tallinn", capacity)).id();
    }

    /** Fires THREADS bookings at the same moment and returns how many succeeded. */
    private int bookConcurrently(UUID eventId, IntFunction<String> emailForThread) throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> results = new ArrayList<>();
        try (ExecutorService pool = Executors.newFixedThreadPool(THREADS)) {
            for (int i = 0; i < THREADS; i++) {
                String email = emailForThread.apply(i);
                Callable<Boolean> attempt = () -> {
                    start.await();
                    try {
                        bookingService.create(eventId, new CreateBookingRequest("Mari", email, 1));
                        return true;
                    } catch (ConflictException e) {
                        return false;
                    }
                };
                results.add(pool.submit(attempt));
            }
            start.countDown();
            int successes = 0;
            for (Future<Boolean> result : results) {
                try {
                    if (result.get()) {
                        successes++;
                    }
                } catch (ExecutionException e) {
                    throw new AssertionError("Unexpected failure", e.getCause());
                }
            }
            return successes;
        }
    }

    private int bookedSeats(UUID eventId) {
        return jdbc.queryForObject("SELECT booked_seats FROM event WHERE id = ?", Integer.class, eventId);
    }

    private int bookingCount(UUID eventId) {
        return jdbc.queryForObject("SELECT count(*) FROM booking WHERE event_id = ?", Integer.class, eventId);
    }

    @Test
    void lastSeatIsSoldExactlyOnce() throws Exception {
        UUID eventId = futureEvent(1);

        int successes = bookConcurrently(eventId, i -> "user" + i + "@example.com");

        assertThat(successes).isEqualTo(1);
        assertThat(bookedSeats(eventId)).isEqualTo(1);
        assertThat(bookingCount(eventId)).isEqualTo(1);
    }

    @Test
    void sameEmailBooksOnlyOnceAndRejectedAttemptsReleaseTheirSeats() throws Exception {
        UUID eventId = futureEvent(50);

        int successes = bookConcurrently(eventId, i -> i % 2 == 0 ? "mari@example.com" : "MARI@example.com");

        assertThat(successes).isEqualTo(1);
        assertThat(bookedSeats(eventId)).isEqualTo(1);
        assertThat(bookingCount(eventId)).isEqualTo(1);
    }
}
