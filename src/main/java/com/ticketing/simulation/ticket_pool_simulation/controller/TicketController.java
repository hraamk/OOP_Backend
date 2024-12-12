package com.ticketing.simulation.ticket_pool_simulation.controller;

import com.ticketing.simulation.ticket_pool_simulation.model.dto.TicketDTO;
import com.ticketing.simulation.ticket_pool_simulation.model.entity.Ticket;
import com.ticketing.simulation.ticket_pool_simulation.service.interfaces.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")  // Configure appropriately for production
public class TicketController {

    private final TicketService ticketService;

    @GetMapping("/event/{eventId}")
    public ResponseEntity<List<TicketDTO>> getTicketsByEventId(
            @PathVariable String eventId,
            @RequestParam(required = false) Ticket.TicketStatus status
    ) {
        List<Ticket> tickets;
        if (status != null) {
            tickets = ticketService.getTicketsByEventIdAndStatus(eventId, status);
        } else {
            tickets = ticketService.getTicketsByEventId(eventId);
        }

        List<TicketDTO> ticketDTOs = tickets.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ticketDTOs);
    }

    @GetMapping("/event/{eventId}/available")
    public ResponseEntity<List<TicketDTO>> getAvailableTickets(@PathVariable String eventId) {
        List<Ticket> availableTickets = ticketService.getAvailableTickets(eventId);
        List<TicketDTO> ticketDTOs = availableTickets.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ticketDTOs);
    }

    @GetMapping("/event/{eventId}/count")
    public ResponseEntity<Integer> getTicketCount(
            @PathVariable String eventId,
            @RequestParam(required = false) Ticket.TicketStatus status
    ) {
        int count;
        if (status != null) {
            count = ticketService.getTicketCountByStatus(eventId, status);
        } else {
            count = ticketService.getTotalTicketCount(eventId);
        }

        return ResponseEntity.ok(count);
    }

    private TicketDTO convertToDTO(Ticket ticket) {
        return TicketDTO.builder()
                .id(ticket.getId())
                .eventId(ticket.getEventId())
                .price(ticket.getPrice())
                .status(ticket.getStatus())
                .createdAt(ticket.getCreatedAt())
                .purchasedAt(ticket.getPurchasedAt())
                .purchasedBy(ticket.getPurchasedBy())
                .build();
    }
}