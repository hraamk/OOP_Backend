package com.ticketing.simulation.ticket_pool_simulation.model.repository;

import com.ticketing.simulation.ticket_pool_simulation.model.entity.Ticket;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface TicketRepo extends MongoRepository<Ticket, String>{

}
