package com.ticketing.simulation.ticket_pool_simulation.service.interfaces;

import com.ticketing.simulation.ticket_pool_simulation.model.entity.Ticket;
import java.util.List;

public interface TicketService {
    List<Ticket> getTicketsByEventId(String eventId);
    List<Ticket> getTicketsByEventIdAndStatus(String eventId, Ticket.TicketStatus status);
    List<Ticket> getAvailableTickets(String eventId);
    int getTicketCountByStatus(String eventId, Ticket.TicketStatus status);
    int getTotalTicketCount(String eventId);
}