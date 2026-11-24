package com.example.eventhub.booking;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface BookingRepository extends JpaRepository<Booking, UUID> {

    /** {@code email} must already be normalised (trimmed, lower case), as stored. */
    boolean existsByEventIdAndEmail(UUID eventId, String email);
}
