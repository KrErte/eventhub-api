package com.example.eventhub.common;

import java.util.Map;

/**
 * The request is valid but conflicts with the current state (e.g. no seats left). Mapped to HTTP 409 by
 * {@link GlobalExceptionHandler}. {@code properties} are added to the ProblemDetail as extra fields; use an
 * {@code "errors"} map ({@code field → message}) when the conflict belongs to a specific input field.
 */
public class ConflictException extends RuntimeException {

    private final Map<String, Object> properties;

    public ConflictException(String message) {
        this(message, Map.of());
    }

    public ConflictException(String message, Map<String, Object> properties) {
        super(message);
        this.properties = Map.copyOf(properties);
    }

    public Map<String, Object> getProperties() {
        return properties;
    }
}
