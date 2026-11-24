package com.example.eventhub.booking;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * JPA entity. References the event only by id: the event feature's entity is private to that feature.
 */
@Entity
class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private @Nullable UUID id;

    @Column(nullable = false, updatable = false)
    private UUID eventId;

    @Column(nullable = false, length = 200)
    private String name;

    /** Stored trimmed and lower-cased; unique per event. */
    @Column(nullable = false, length = 320)
    private String email;

    @Column(nullable = false)
    private int seats;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    Booking(UUID eventId, String name, String email, int seats, Instant createdAt) {
        this.eventId = eventId;
        this.name = name;
        this.email = email;
        this.seats = seats;
        this.createdAt = createdAt;
    }

    /** For JPA only. */
    protected Booking() {
        this.eventId = new UUID(0, 0);
        this.name = "";
        this.email = "";
        this.createdAt = Instant.EPOCH;
    }

    @Nullable UUID getId() {
        return id;
    }

    UUID getEventId() {
        return eventId;
    }

    String getName() {
        return name;
    }

    String getEmail() {
        return email;
    }

    int getSeats() {
        return seats;
    }

    Instant getCreatedAt() {
        return createdAt;
    }
}
