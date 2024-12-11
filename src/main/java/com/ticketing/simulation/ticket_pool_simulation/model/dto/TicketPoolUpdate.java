package com.ticketing.simulation.ticket_pool_simulation.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TicketPoolUpdate {
    private String eventId;
    private int availableTickets;
    private int totalCapacity;
}