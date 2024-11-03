package com.ticketing.simulation.ticket_pool_simulation.controller;

import com.ticketing.simulation.ticket_pool_simulation.model.entity.Ticket;
import com.ticketing.simulation.ticket_pool_simulation.repository.TicketRepo;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
public class TicketController {

    @Autowired
    TicketRepo ticketRepo;

    @GetMapping("/tickets")
    public List<Ticket> getTickets() {
        return ticketRepo.findAll();
    }

    @PostMapping("/saveTicket")
    public Ticket saveTicket(@RequestBody Ticket ticket) {
        return ticketRepo.save(ticket);

    }
}
