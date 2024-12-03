package com.ticketing.simulation.ticket_pool_simulation.repository;

import com.ticketing.simulation.ticket_pool_simulation.model.entity.Event;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import java.time.LocalDateTime;
import java.util.List;

public interface EventRepository extends MongoRepository<Event, String> {
    List<Event> findByEventDateGreaterThan(LocalDateTime date);

    @Query("{ 'eventDate': { $gt: ?0 }, 'totalTickets': { $gt: 0 } }")
    List<Event> findUpcomingEventsWithAvailableTickets(LocalDateTime date);

    List<Event> findByTotalTicketsGreaterThan(int minTickets);
}