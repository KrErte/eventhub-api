package com.example.eventhub.event;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * JPA entity. Package-private on purpose: other features and the API only see {@link EventResponse}.
 */
@Entity
class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private @Nullable UUID id;

    @Column(nullable = false, length = 200)
    private String title;

    private @Nullable String description;

    @Column(nullable = false)
    private Instant startsAt;

    @Column(nullable = false, length = 200)
    private String venue;

    @Column(nullable = false)
    private int capacity;

    /** Changed only by {@link EventRepository#reserveSeats}, never through the entity. */
    @Column(nullable = false, updatable = false)
    private int bookedSeats;

    Event(String title, @Nullable String description, Instant startsAt, String venue, int capacity) {
        this.title = title;
        this.description = description;
        this.startsAt = startsAt;
        this.venue = venue;
        this.capacity = capacity;
    }

    /** For JPA only. */
    protected Event() {
        this.title = "";
        this.startsAt = Instant.EPOCH;
        this.venue = "";
    }

    @Nullable UUID getId() {
        return id;
    }

    String getTitle() {
        return title;
    }

    @Nullable String getDescription() {
        return description;
    }

    Instant getStartsAt() {
        return startsAt;
    }

    String getVenue() {
        return venue;
    }

    int getCapacity() {
        return capacity;
    }

    int getAvailableSeats() {
        return capacity - bookedSeats;
    }
}
