package com.ticketing.simulation.ticket_pool_simulation.model.dto;

import com.ticketing.simulation.ticket_pool_simulation.model.entity.Ticket;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class TicketDTO {
    private String id;
    private String eventId;
    private double price;
    private Ticket.TicketStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime purchasedAt;
    private String purchasedBy;
}