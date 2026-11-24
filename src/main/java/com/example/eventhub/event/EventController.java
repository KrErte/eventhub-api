package com.example.eventhub.event;

import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/events")
class EventController {

    private final EventService eventService;

    EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping
    List<EventResponse> list() {
        return eventService.findAll();
    }

    @GetMapping("/{id}")
    EventResponse get(@PathVariable UUID id) {
        return eventService.findById(id);
    }
}
