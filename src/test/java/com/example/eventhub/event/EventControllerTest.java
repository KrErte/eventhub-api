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
    void malformedIdIsBadRequestProblem() {
        assertThat(mvc.get().uri("/api/events/not-a-uuid"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
    }
}
