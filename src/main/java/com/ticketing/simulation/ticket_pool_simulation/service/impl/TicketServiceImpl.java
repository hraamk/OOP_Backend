package com.ticketing.simulation.ticket_pool_simulation.service.impl;

import com.ticketing.simulation.ticket_pool_simulation.model.entity.Ticket;
import com.ticketing.simulation.ticket_pool_simulation.repository.TicketRepository;
import com.ticketing.simulation.ticket_pool_simulation.service.interfaces.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TicketServiceImpl implements TicketService {

    private final TicketRepository ticketRepository;

    @Override
    public List<Ticket> getTicketsByEventId(String eventId) {
        return ticketRepository.findByEventId(eventId);
    }

    @Override
    public List<Ticket> getTicketsByEventIdAndStatus(String eventId, Ticket.TicketStatus status) {
        return ticketRepository.findByEventIdAndStatus(eventId, status);
    }

    @Override
    public List<Ticket> getAvailableTickets(String eventId) {
        return ticketRepository.findAvailableTickets(eventId);
    }

    @Override
    public int getTicketCountByStatus(String eventId, Ticket.TicketStatus status) {
        return ticketRepository.countByEventIdAndStatus(eventId, status);
    }

    @Override
    public int getTotalTicketCount(String eventId) {
        return getTicketsByEventId(eventId).size();
    }
}