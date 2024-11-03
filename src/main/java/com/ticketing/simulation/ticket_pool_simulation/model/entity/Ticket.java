package com.ticketing.simulation.ticket_pool_simulation.model.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection ="TicketInfo")
public class Ticket {
    private String id;

    // Event details
    private String eventId;
    private String eventName;
    private LocalDateTime eventDateTime;
    private String eventVenue;

    // Ticket details
    private Double price;     // Optional, for seated events
    private TicketStatus status;

    // References
    private String vendorId;          // ID of the vendor who created the ticket
    private String customerId;        // ID of the customer who purchased the ticket (null if not purchased)

    // Transaction details
    private String transactionId;     // Set when ticket is purchased
    private LocalDateTime purchaseTime;

    private LocalDateTime createdAt;
    private LocalDateTime madeAvailableAt;

    public Ticket(String eventId, String vendorId) {
        this.eventId = eventId;
        this.vendorId = vendorId;
        this.status = TicketStatus.CREATED;
        this.createdAt = LocalDateTime.now();
    }


    // Business methods
    public boolean isAvailable() {
        return status == TicketStatus.AVAILABLE;
    }

    public void setEventDetails(){
        this.eventName = "Event Name";
        this.eventVenue = "Event Venue";
    }
    public void markAsSold(String customerId, String transactionId) {
        this.status = TicketStatus.SOLD;
        this.customerId = customerId;
        this.transactionId = transactionId;
        this.purchaseTime = LocalDateTime.now();
    }

    // Create a copy of the ticket (useful for batch creation)
   /* public Ticket createCopy() {
        return Ticket.builder()
                .eventId(this.eventId)
                .eventName(this.eventName)
                .eventDateTime(this.eventDateTime)
                .eventVenue(this.eventVenue)
                .ticketCategory(this.ticketCategory)
                .price(this.price)
                .vendorId(this.vendorId)
                .status(TicketStatus.AVAILABLE)
                .isValid(true)
                .build();
                }

    */

}
