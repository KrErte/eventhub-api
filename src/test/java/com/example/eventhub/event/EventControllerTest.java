package com.example.eventhub.event;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.eventhub.TestcontainersConfiguration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class EventControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private EventRepository repository;

    @BeforeEach
    void cleanDatabase() {
        repository.deleteAll();
    }

    @Test
    void listsEventsOrderedByStartTimeWithoutAuthentication() {
        repository.save(new Event("Later", null, Instant.parse("2027-03-01T18:00:00Z"), "Tallinn", 50));
        repository.save(new Event("Sooner", "Opening night", Instant.parse("2027-01-15T19:00:00Z"), "Tartu", 100));

        var result = assertThat(mvc.get().uri("/api/events")).hasStatusOk().bodyJson();
        result.extractingPath("$").asArray().hasSize(2);
        result.extractingPath("$[0].title").isEqualTo("Sooner");
        result.extractingPath("$[0].description").isEqualTo("Opening night");
        result.extractingPath("$[0].startsAt").isEqualTo("2027-01-15T19:00:00Z");
        result.extractingPath("$[0].venue").isEqualTo("Tartu");
        result.extractingPath("$[0].capacity").isEqualTo(100);
        result.extractingPath("$[1].title").isEqualTo("Later");
    }

    @Test
    void returnsSingleEvent() {
        Event saved = repository.save(new Event("Jazz", null, Instant.parse("2027-02-01T20:00:00Z"), "Pärnu", 30));

        assertThat(mvc.get().uri("/api/events/{id}", saved.getId()))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.title").isEqualTo("Jazz");
    }

    @Test
    void unknownEventIsNotFoundProblem() {
        assertThat(mvc.get().uri("/api/events/{id}", UUID.randomUUID()))
                .hasStatus(HttpStatus.NOT_FOUND)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
    }

    @Test
    void createsEvent() {
        var result = assertThat(mvc.post().uri("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "  Uus meetup  ",
                                  "description": "",
                                  "startsAt": "2099-05-01T15:00:00Z",
                                  "venue": "Tallinn",
                                  "capacity": 40
                                }
                                """))
                .hasStatus(HttpStatus.CREATED);
        result.headers().hasHeaderSatisfying("Location",
                values -> assertThat(values.getFirst()).matches(".*/api/events/[0-9a-f-]{36}"));
        var body = result.bodyJson();
        body.extractingPath("$.title").isEqualTo("Uus meetup");
        body.extractingPath("$.description").isNull();
        body.extractingPath("$.startsAt").isEqualTo("2099-05-01T15:00:00Z");
        body.extractingPath("$.capacity").isEqualTo(40);

        assertThat(repository.findAll()).singleElement()
                .satisfies(event -> assertThat(event.getTitle()).isEqualTo("Uus meetup"));
    }

    @Test
    void rejectsInvalidEventWithFieldErrors() {
        var body = assertThat(mvc.post().uri("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": " ",
                                  "startsAt": "2000-01-01T10:00:00Z",
                                  "venue": "",
                                  "capacity": 0
                                }
                                """))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyJson();
        body.extractingPath("$.errors.title").isEqualTo("Pealkiri on kohustuslik");
        body.extractingPath("$.errors.startsAt").isEqualTo("Algusaeg peab olema tulevikus");
        body.extractingPath("$.errors.venue").isEqualTo("Toimumiskoht on kohustuslik");
        body.extractingPath("$.errors.capacity").isEqualTo("Kohtade arv peab olema vähemalt 1");

        assertThat(repository.count()).isZero();
    }

    @Test
    void rejectsMissingRequiredFields() {
        var body = assertThat(mvc.post().uri("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson();
        body.extractingPath("$.errors.startsAt").isEqualTo("Algusaeg on kohustuslik");
        body.extractingPath("$.errors.capacity").isEqualTo("Kohtade arv on kohustuslik");
    }

    @Test
    void malformedIdIsBadRequestProblem() {
        assertThat(mvc.get().uri("/api/events/not-a-uuid"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
    }
}
