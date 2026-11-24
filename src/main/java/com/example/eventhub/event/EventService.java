package com.example.eventhub.event;

import com.example.eventhub.common.NotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Public entry point of the event feature. Other features use this, never {@link EventRepository}.
 */
@Service
@Transactional(readOnly = true)
public class EventService {

    private final EventRepository repository;

    EventService(EventRepository repository) {
        this.repository = repository;
    }

    public List<EventResponse> findAll() {
        return repository.findAll(Sort.by("startsAt")).stream()
                .map(EventResponse::from)
                .toList();
    }

    public EventResponse findById(UUID id) {
        return repository.findById(id)
                .map(EventResponse::from)
                .orElseThrow(() -> new NotFoundException("Event " + id + " not found"));
    }
}
