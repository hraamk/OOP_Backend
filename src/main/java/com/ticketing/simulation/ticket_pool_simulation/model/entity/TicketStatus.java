package com.ticketing.simulation.ticket_pool_simulation.model.entity;

public enum TicketStatus {
    AVAILABLE,    // Ticket is available for purchase
    CREATED,     // Ticket is generated but not yet available
    SOLD        // Ticket has been purchased
}