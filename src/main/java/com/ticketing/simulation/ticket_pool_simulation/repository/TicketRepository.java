package com.ticketing.simulation.ticket_pool_simulation.repository;


import com.ticketing.simulation.ticket_pool_simulation.model.entity.Ticket;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface TicketRepository extends MongoRepository<Ticket, String> {
    List<Ticket> findByStatus(Ticket.TicketStatus status);
}