package com.ticketing.simulation.ticket_pool_simulation.service.interfaces;

import com.ticketing.simulation.ticket_pool_simulation.model.entity.Configuration;
import com.ticketing.simulation.ticket_pool_simulation.model.entity.Ticket;

public interface TicketPoolService {
    void initialize(String eventId, Configuration config);
    void addTickets(String eventId, int count);
    Ticket purchaseTicket(String eventId, String customerId);
    int getAvailableTicketCount(String eventId);
    boolean isPoolFull(String eventId);
    void shutdown(String eventId);
}