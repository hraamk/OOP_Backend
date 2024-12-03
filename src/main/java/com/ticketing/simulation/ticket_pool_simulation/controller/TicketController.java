package com.ticketing.simulation.ticket_pool_simulation.controller;

import com.ticketing.simulation.ticket_pool_simulation.model.entity.Ticket;
import com.ticketing.simulation.ticket_pool_simulation.repository.TicketRepository;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
public class TicketController {

    @Autowired
    TicketRepository ticketRepository;

    @GetMapping("/tickets")
    public List<Ticket> getTickets() {
        return ticketRepository.findAll();
    }

    @PostMapping("/saveTicket")
    public Ticket saveTicket(@RequestBody Ticket ticket) {
        return ticketRepository.save(ticket);

    }
}
