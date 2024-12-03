package com.ticketing.simulation.ticket_pool_simulation.controller;

import com.ticketing.simulation.ticket_pool_simulation.model.entity.Event;
import com.ticketing.simulation.ticket_pool_simulation.repository.EventRepository;
import com.ticketing.simulation.ticket_pool_simulation.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/events")
@CrossOrigin(origins = "*")
@Slf4j
@RequiredArgsConstructor
public class EventController {

    private final EventRepository eventRepository;

    @PostMapping
    public ResponseEntity<?> createEvent(@RequestBody Event event) {
        try {
            Event savedEvent = eventRepository.save(event);
            log.info("Created event: {}", savedEvent.getName());
            return ResponseEntity.ok(savedEvent);
        } catch (Exception e) {
            log.error("Error creating event", e);
            return ResponseEntity.internalServerError()
                    .body("Failed to create event: " + e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<?> getAllEvents() {
        try {
            List<Event> events = eventRepository.findAll();
            return ResponseEntity.ok(events);
        } catch (Exception e) {
            log.error("Error fetching events", e);
            return ResponseEntity.internalServerError()
                    .body("Failed to fetch events: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getEvent(@PathVariable String id) {
        try {
            Event event = eventRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + id));
            return ResponseEntity.ok(event);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error fetching event: {}", id, e);
            return ResponseEntity.internalServerError()
                    .body("Failed to fetch event: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateEvent(@PathVariable String id, @RequestBody Event eventDetails) {
        try {
            Event event = eventRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + id));

            event.setName(eventDetails.getName());
            event.setDescription(eventDetails.getDescription());
            event.setTotalTickets(eventDetails.getTotalTickets());
            event.setPrice(eventDetails.getPrice());
            event.setEventDate(eventDetails.getEventDate());

            Event updatedEvent = eventRepository.save(event);
            log.info("Updated event: {}", updatedEvent.getName());
            return ResponseEntity.ok(updatedEvent);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error updating event: {}", id, e);
            return ResponseEntity.internalServerError()
                    .body("Failed to update event: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteEvent(@PathVariable String id) {
        try {
            Event event = eventRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + id));

            eventRepository.delete(event);
            log.info("Deleted event: {}", event.getName());
            return ResponseEntity.ok().build();
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error deleting event: {}", id, e);
            return ResponseEntity.internalServerError()
                    .body("Failed to delete event: " + e.getMessage());
        }
    }

    @GetMapping("/upcoming")
    public ResponseEntity<?> getUpcomingEvents() {
        try {
            List<Event> events = eventRepository.findUpcomingEventsWithAvailableTickets(
                    java.time.LocalDateTime.now());
            return ResponseEntity.ok(events);
        } catch (Exception e) {
            log.error("Error fetching upcoming events", e);
            return ResponseEntity.internalServerError()
                    .body("Failed to fetch upcoming events: " + e.getMessage());
        }
    }
}