package com.ticketing.simulation.ticket_pool_simulation.service.interfaces;

import com.ticketing.simulation.ticket_pool_simulation.model.entity.Ticket;
import com.ticketing.simulation.ticket_pool_simulation.model.entity.Configuration;

import java.util.List;

public interface TicketPoolService {
    void addTickets(int count);
    Ticket purchaseTicket(String customerId);
    int getAvailableTicketCount();
    boolean isPoolFull();
    void initialize(Configuration config);
    void shutdown();
}
