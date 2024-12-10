package com.ticketing.simulation.ticket_pool_simulation.service.impl;

import com.ticketing.simulation.ticket_pool_simulation.model.entity.Configuration;
import com.ticketing.simulation.ticket_pool_simulation.model.entity.Ticket;
import com.ticketing.simulation.ticket_pool_simulation.repository.TicketRepository;
import com.ticketing.simulation.ticket_pool_simulation.service.interfaces.TicketPoolService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

@Service
@Slf4j
@RequiredArgsConstructor
public class TicketPoolServiceImpl implements TicketPoolService {
    private final TicketRepository ticketRepository;
    private final Map<String, BlockingQueue<Ticket>> ticketPools = new ConcurrentHashMap<>();
    private final Map<String, Configuration> configs = new ConcurrentHashMap<>();
    private final Map<String, AtomicBoolean> runningStatus = new ConcurrentHashMap<>();

    private static final int BATCH_SIZE = 10;
    private static final int TIMEOUT_MS = 100;

    @Override
    public void addTickets(String eventId, int count) {
        AtomicBoolean isRunning = runningStatus.get(eventId);
        if (isRunning == null || !isRunning.get()) {
            throw new IllegalStateException("Ticket pool is not running for event: " + eventId);
        }

        BlockingQueue<Ticket> ticketPool = ticketPools.get(eventId);
        Configuration config = configs.get(eventId);

        if (ticketPool == null || config == null) {
            throw new IllegalStateException("Ticket pool not initialized for event: " + eventId);
        }

        // Check capacity before creating tickets
        int availableCapacity = config.getMaxTicketCapacity() - ticketPool.size();
        if (availableCapacity <= 0) {
            log.debug("Ticket pool at capacity for event: {}", eventId);
            return;
        }

        int ticketsToAdd = Math.min(count, availableCapacity);
        List<Ticket> ticketBatch = new ArrayList<>();

        for (int i = 0; i < ticketsToAdd && isRunning.get(); i++) {
            Ticket ticket = Ticket.builder()
                    .eventId(eventId)
                    .status(Ticket.TicketStatus.AVAILABLE)
                    .createdAt(LocalDateTime.now())
                    .build();
            ticketBatch.add(ticket);

            // Process batch when it reaches BATCH_SIZE or is the last batch
            if (ticketBatch.size() >= BATCH_SIZE || i == ticketsToAdd - 1) {
                try {
                    // Save batch to database
                    List<Ticket> savedTickets = ticketRepository.saveAll(ticketBatch);

                    // Add saved tickets to queue
                    for (Ticket savedTicket : savedTickets) {
                        if (!ticketPool.offer(savedTicket, TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
                            log.debug("Queue offer timeout for event: {}", eventId);
                            return;
                        }
                    }

                    ticketBatch.clear();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.debug("Interrupted while adding tickets for event: {}", eventId);
                    return;
                } catch (Exception e) {
                    log.error("Error saving ticket batch for event {}: {}", eventId, e.getMessage());
                    // Continue with next batch
                }
            }
        }
    }

    @Override
    public Ticket purchaseTicket(String eventId, String customerId) {
        AtomicBoolean isRunning = runningStatus.get(eventId);
        if (isRunning == null || !isRunning.get()) {
            throw new IllegalStateException("Ticket pool is not running for event: " + eventId);
        }

        BlockingQueue<Ticket> ticketPool = ticketPools.get(eventId);
        if (ticketPool == null) {
            throw new IllegalStateException("Ticket pool not initialized for event: " + eventId);
        }

        try {
            Ticket ticket = ticketPool.poll(TIMEOUT_MS, TimeUnit.MILLISECONDS);
            if (ticket == null) {
                return null;
            }

            try {
                ticket.setStatus(Ticket.TicketStatus.PURCHASED);
                ticket.setPurchasedAt(LocalDateTime.now());
                ticket.setPurchasedBy(customerId);

                // Retry logic for database operations
                return retryOperation(() -> ticketRepository.save(ticket));
            } catch (Exception e) {
                // If save fails, try to put the ticket back in the pool
                ticketPool.offer(ticket);
                throw e;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.debug("Interrupted while purchasing ticket for event: {}", eventId);
            return null;
        }
    }

    // Add retry utility method
    private <T> T retryOperation(Supplier<T> operation) {
        int maxRetries = 3;
        int retryDelay = 100; // ms

        for (int i = 0; i < maxRetries; i++) {
            try {
                return operation.get();
            } catch (Exception e) {
                if (i == maxRetries - 1) {
                    throw e;
                }
                try {
                    Thread.sleep(retryDelay * (i + 1));
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted during retry", ie);
                }
            }
        }
        throw new RuntimeException("Failed after " + maxRetries + " retries");
    }

    @Override
    public void initialize(String eventId, Configuration config) {
        ticketPools.put(eventId, new LinkedBlockingQueue<>(config.getMaxTicketCapacity()));
        configs.put(eventId, config);
        runningStatus.put(eventId, new AtomicBoolean(true));
        log.info("Initialized ticket pool for event {} with capacity: {}",
                eventId, config.getMaxTicketCapacity());
    }

    @Override
    public int getAvailableTicketCount(String eventId) {
        BlockingQueue<Ticket> ticketPool = ticketPools.get(eventId);
        return ticketPool != null ? ticketPool.size() : 0;
    }

    @Override
    public boolean isPoolFull(String eventId) {
        BlockingQueue<Ticket> ticketPool = ticketPools.get(eventId);
        Configuration config = configs.get(eventId);

        if (ticketPool == null || config == null) {
            return false;
        }

        return ticketPool.size() >= config.getMaxTicketCapacity();
    }

    @Override
    public void shutdown(String eventId) {
        AtomicBoolean status = runningStatus.get(eventId);
        if (status != null) {
            status.set(false);
        }

        BlockingQueue<Ticket> ticketPool = ticketPools.remove(eventId);
        configs.remove(eventId);
        runningStatus.remove(eventId);

        if (ticketPool != null) {
            ticketPool.clear();
            log.info("Ticket pool shutdown for event: {}", eventId);
        }
    }
}