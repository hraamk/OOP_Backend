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
    private int totalTickets;
    private int ticketReleaseRate;  // tickets per minute
    private int customerRetrievalRate;  // attempts per minute
    private int maxTicketCapacity;
    private int vendorCount;
    private int customerCount;
    private boolean isRunning;

}

