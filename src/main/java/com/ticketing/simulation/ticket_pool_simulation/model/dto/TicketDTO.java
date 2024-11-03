package com.ticketing.simulation.ticket_pool_simulation.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.ticketing.simulation.ticket_pool_simulation.model.entity.TicketStatus;
import com.ticketing.simulation.ticket_pool_simulation.model.entity.Ticket;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketDTO {
    private String id;

    // Event details
    private String eventId;
    private String eventName;
    private LocalDateTime eventDateTime;
    private String eventVenue;

    // Ticket details
    private Double price;
    private TicketStatus status;

    // References
    private String vendorId;
    private String customerId;

    // Transaction details
    private String transactionId;
    private LocalDateTime purchaseTime;

    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime madeAvailableAt;

    // Conversion methods
    public static TicketDTO fromEntity(Ticket ticket) {
        return TicketDTO.builder()
                .id(ticket.getId())
                .eventId(ticket.getEventId())
                .eventName(ticket.getEventName())
                .eventDateTime(ticket.getEventDateTime())
                .eventVenue(ticket.getEventVenue())
                .price(ticket.getPrice())
                .status(ticket.getStatus())
                .vendorId(ticket.getVendorId())
                .customerId(ticket.getCustomerId())
                .transactionId(ticket.getTransactionId())
                .purchaseTime(ticket.getPurchaseTime())
                .createdAt(ticket.getCreatedAt())
                .madeAvailableAt(ticket.getMadeAvailableAt())
                .build();
    }

    public Ticket toEntity() {
        return Ticket.builder()
                .id(this.id)
                .eventId(this.eventId)
                .eventName(this.eventName)
                .eventDateTime(this.eventDateTime)
                .eventVenue(this.eventVenue)
                .price(this.price)
                .status(this.status)
                .vendorId(this.vendorId)
                .customerId(this.customerId)
                .transactionId(this.transactionId)
                .purchaseTime(this.purchaseTime)
                .createdAt(this.createdAt)
                .madeAvailableAt(this.madeAvailableAt)
                .build();
    }

    // Constructor for essential fields
    public TicketDTO(String eventId, String vendorId) {
        this.eventId = eventId;
        this.vendorId = vendorId;
        this.status = TicketStatus.CREATED;
        this.createdAt = LocalDateTime.now();
    }

    // Utility methods
    public boolean isAvailable() {
        return status == TicketStatus.AVAILABLE;
    }

    public void markAsSold(String customerId, String transactionId) {
        this.status = TicketStatus.SOLD;
        this.customerId = customerId;
        this.transactionId = transactionId;
        this.purchaseTime = LocalDateTime.now();
    }

    public void setEventDetails() {
        this.eventName = "Event Name";
        this.eventVenue = "Event Venue";
    }
}