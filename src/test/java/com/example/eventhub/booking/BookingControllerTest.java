package com.example.eventhub.booking;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.eventhub.TestcontainersConfiguration;
import com.example.eventhub.event.CreateEventRequest;
import com.example.eventhub.event.EventService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class BookingControllerTest {

    @Autowired
    private MockMvcTester mvc;

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
                "Meetup", null, Instant.now().plus(30, ChronoUnit.DAYS), "Tallinn", capacity)).id();
    }

    private static String booking(String email, int seats) {
        return """
                {"name": "Mari Maasikas", "email": "%s", "seats": %d}
                """.formatted(email, seats);
    }

    private MockMvcTester.MockMvcRequestBuilder post(UUID eventId, String body) {
        return mvc.post().uri("/api/events/{id}/bookings", eventId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }

    @Test
    void createsBookingAndReducesAvailableSeats() {
        UUID eventId = futureEvent(10);

        var body = assertThat(post(eventId, booking("  Mari@Example.com ", 3)))
                .hasStatus(HttpStatus.CREATED)
                .bodyJson();
        body.extractingPath("$.eventId").isEqualTo(eventId.toString());
        body.extractingPath("$.name").isEqualTo("Mari Maasikas");
        body.extractingPath("$.email").isEqualTo("mari@example.com");
        body.extractingPath("$.seats").isEqualTo(3);

        assertThat(mvc.get().uri("/api/events/{id}", eventId))
                .bodyJson().extractingPath("$.availableSeats").isEqualTo(7);
        assertThat(mvc.get().uri("/api/events"))
                .bodyJson().extractingPath("$[0].availableSeats").isEqualTo(7);
    }

    @Test
    void rejectsBookingWhenNotEnoughSeats() {
        UUID eventId = futureEvent(2);
        assertThat(post(eventId, booking("a@example.com", 1))).hasStatus(HttpStatus.CREATED);

        var body = assertThat(post(eventId, booking("b@example.com", 2)))
                .hasStatus(HttpStatus.CONFLICT)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyJson();
        body.extractingPath("$.detail").isEqualTo("Vabu kohti pole piisavalt (alles 1)");
        body.extractingPath("$.availableSeats").isEqualTo(1);
    }

    @Test
    void rejectsSecondBookingWithSameEmailIgnoringCase() {
        UUID eventId = futureEvent(10);
        assertThat(post(eventId, booking("mari@example.com", 1))).hasStatus(HttpStatus.CREATED);

        assertThat(post(eventId, booking("MARI@example.com", 2)))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson()
                .extractingPath("$.errors.email")
                .isEqualTo("Selle e-postiga on sellele sündmusele juba broneeritud");

        assertThat(mvc.get().uri("/api/events/{id}", eventId))
                .bodyJson().extractingPath("$.availableSeats").isEqualTo(9);
    }

    @Test
    void sameEmailMayBookDifferentEvents() {
        assertThat(post(futureEvent(5), booking("mari@example.com", 1))).hasStatus(HttpStatus.CREATED);
        assertThat(post(futureEvent(5), booking("mari@example.com", 1))).hasStatus(HttpStatus.CREATED);
    }

    @Test
    void rejectsBookingForPastEvent() {
        UUID eventId = UUID.randomUUID();
        jdbc.update("INSERT INTO event (id, title, starts_at, venue, capacity) VALUES (?, 'Vana', now() - interval '1 day', 'Tartu', 10)",
                eventId);

        assertThat(post(eventId, booking("a@example.com", 1)))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson()
                .extractingPath("$.detail").isEqualTo("Sündmus on juba alanud, broneerida ei saa");
    }

    @Test
    void unknownEventIsNotFound() {
        assertThat(post(UUID.randomUUID(), booking("a@example.com", 1)))
                .hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void validatesBooking() {
        UUID eventId = futureEvent(10);

        var body = assertThat(post(eventId, """
                {"name": " ", "email": "not-an-email", "seats": 11}
                """))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson();
        body.extractingPath("$.errors.name").isEqualTo("Nimi on kohustuslik");
        body.extractingPath("$.errors.email").isEqualTo("E-posti aadress ei ole korrektne");
        body.extractingPath("$.errors.seats").isEqualTo("Korraga saab broneerida kuni 10 kohta");
    }
}
