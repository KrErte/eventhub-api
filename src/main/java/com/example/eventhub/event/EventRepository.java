package com.example.eventhub.event;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface EventRepository extends JpaRepository<Event, UUID> {
}
