package com.ticketing.simulation.ticket_pool_simulation.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "configurations")
public class Configuration {
    @Id
    private String id;
    private String eventId;  // Reference to associated event
    private String templateName;
    private int vendorCount;
    private int totalTickets;
    private int customerCount;
    private int ticketReleaseRate;
    private int customerRetrievalRate;
    private int maxTicketCapacity;
    private boolean running;
    private boolean paused;
}