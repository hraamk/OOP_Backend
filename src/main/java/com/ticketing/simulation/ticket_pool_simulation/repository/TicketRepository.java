package com.ticketing.simulation.ticket_pool_simulation.repository;

import com.ticketing.simulation.ticket_pool_simulation.model.entity.Ticket;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import java.time.LocalDateTime;
import java.util.List;

public interface TicketRepository extends MongoRepository<Ticket, String> {
    List<Ticket> findByEventId(String eventId);

    List<Ticket> findByEventIdAndStatus(String eventId, Ticket.TicketStatus status);

    int countByEventIdAndStatus(String eventId, Ticket.TicketStatus status);

    @Query("{ 'eventId': ?0, 'status': 'AVAILABLE' }")
    List<Ticket> findAvailableTickets(String eventId);

    List<Ticket> findByPurchasedBy(String customerId);

    List<Ticket> findByEventIdAndPurchasedAtBetween(
            String eventId,
            LocalDateTime startDate,
            LocalDateTime endDate
    );

    @Query(value = "{ 'eventId': ?0 }", delete = true)
    void deleteAllByEventId(String eventId);
}