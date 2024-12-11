package com.ticketing.simulation.ticket_pool_simulation.service.interfaces;

import com.ticketing.simulation.ticket_pool_simulation.model.entity.Configuration;
import com.ticketing.simulation.ticket_pool_simulation.model.entity.Ticket;

import java.util.Set;

/**
 * Service interface for managing ticket pools for events.
 * Provides operations for initializing, managing, and monitoring ticket pools.
 */
public interface TicketPoolService {
    /**
     * Initializes a new ticket pool for the specified event.
     *
     * @param eventId Unique identifier for the event
     * @param config Configuration containing pool settings including capacity
     */
    void initialize(String eventId, Configuration config);

    /**
     * Adds a specified number of tickets to the pool for an event.
     *
     * @param eventId Unique identifier for the event
     * @param count Number of tickets to add
     */
    void addTickets(String eventId, int count);

    /**
     * Attempts to purchase a ticket for a customer from the specified event pool.
     *
     * @param eventId Unique identifier for the event
     * @param customerId Identifier of the customer purchasing the ticket
     * @return The purchased ticket if successful, null if no ticket is available
     */
    Ticket purchaseTicket(String eventId, String customerId);

    /**
     * Returns the current number of available tickets in the pool.
     *
     * @param eventId Unique identifier for the event
     * @return Number of available tickets
     */
    int getAvailableTicketCount(String eventId);

    /**
     * Checks if the ticket pool has reached its maximum capacity.
     *
     * @param eventId Unique identifier for the event
     * @return true if the pool is full, false otherwise
     */
    boolean isPoolFull(String eventId);

    /**
     * Shuts down a specific event's ticket pool and releases its resources.
     *
     * @param eventId Unique identifier for the event to shut down
     */
    void shutdownEvent(String eventId);

    /**
     * Shuts down the entire ticket pool service and releases all resources.
     * Should be called during application shutdown.
     */
    void shutdown();

    /**
     * Returns a set of all active event IDs.
     *
     * @return Set of active event IDs
     */
    Set<String> getActiveEventIds();

    /**
     * Returns the maximum capacity of the ticket pool for the specified event.
     *
     * @param eventId Unique identifier for the event
     * @return Maximum number of tickets the pool can hold
     */
    int getMaxCapacity(String eventId);
}