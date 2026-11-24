package com.example.eventhub.event;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

interface EventRepository extends JpaRepository<Event, UUID> {

    /**
     * Atomically adds {@code seats} to the booked count if the event has not started and enough seats are
     * free. Returns 1 on success, 0 otherwise (unknown event, already started, or not enough seats).
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE Event e SET e.bookedSeats = e.bookedSeats + :seats
            WHERE e.id = :id AND e.startsAt > :now AND e.bookedSeats + :seats <= e.capacity
            """)
    int reserveSeats(UUID id, int seats, Instant now);
}
