package com.example.eventhub.common;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

class GlobalExceptionHandlerTest {

    private final MockMvcTester mvc = MockMvcTester.create(MockMvcBuilders
            .standaloneSetup(new FailingController())
            .setControllerAdvice(new GlobalExceptionHandler())
            .build());

    @Test
    void notFoundBecomesProblemDetail() {
        assertThat(mvc.get().uri("/test/missing"))
                .hasStatus(HttpStatus.NOT_FOUND)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyJson()
                .hasPathSatisfying("$.title", title -> assertThat(title).asString().isEqualTo("Not Found"))
                .hasPathSatisfying("$.detail", detail -> assertThat(detail).asString().isEqualTo("Event 42 not found"));
    }

    @Test
    void validationErrorListsFieldErrors() {
        assertThat(mvc.post().uri("/test/validated")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"\"}"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyJson()
                .hasPathSatisfying("$.errors.name", message -> assertThat(message).asString().isNotBlank());
    }

    @Test
    void unexpectedErrorDoesNotLeakInternals() {
        assertThat(mvc.get().uri("/test/boom"))
                .hasStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyText()
                .doesNotContain("secret internal detail");
    }

    record NameRequest(@NotBlank String name) {
    }

    @RestController
    static class FailingController {

        @GetMapping("/test/missing")
        String missing() {
            throw new NotFoundException("Event 42 not found");
        }

        @PostMapping("/test/validated")
        String validated(@Valid @RequestBody NameRequest request) {
            return request.name();
        }

        @GetMapping("/test/boom")
        String boom() {
            throw new IllegalStateException("secret internal detail");
        }
    }
}
