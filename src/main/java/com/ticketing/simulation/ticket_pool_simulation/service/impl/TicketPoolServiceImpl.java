package com.ticketing.simulation.ticket_pool_simulation.service.impl;

import com.ticketing.simulation.ticket_pool_simulation.model.dto.TicketPoolUpdate;
import com.ticketing.simulation.ticket_pool_simulation.model.entity.Configuration;
import com.ticketing.simulation.ticket_pool_simulation.model.entity.Ticket;
import com.ticketing.simulation.ticket_pool_simulation.repository.TicketRepository;
import com.ticketing.simulation.ticket_pool_simulation.service.interfaces.TicketPoolService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@Slf4j
@RequiredArgsConstructor
public class TicketPoolServiceImpl implements TicketPoolService {
    private final TicketRepository ticketRepository;
    private final Map<String, BlockingQueue<Ticket>> ticketPools = new ConcurrentHashMap<>();
    private final Map<String, Configuration> configs = new ConcurrentHashMap<>();
    private final Map<String, AtomicBoolean> runningStatus = new ConcurrentHashMap<>();
    private final ScheduledExecutorService updateExecutor = Executors.newSingleThreadScheduledExecutor();
    private final Map<String, Long> lastUpdateTime = new ConcurrentHashMap<>();

    private static final int BATCH_SIZE = 10;
    private static final int QUEUE_TIMEOUT_MS = 100;
    private static final int DB_RETRY_COUNT = 3;
    private static final int DB_RETRY_DELAY_MS = 100;
    private static final long UPDATE_THROTTLE_MS = 100;

    private void schedulePoolUpdate(String eventId) {
        Long lastUpdate = lastUpdateTime.get(eventId);
        long currentTime = System.currentTimeMillis();

        if (lastUpdate == null || currentTime - lastUpdate >= UPDATE_THROTTLE_MS) {
            lastUpdateTime.put(eventId, currentTime);

            updateExecutor.schedule(() -> {
                try {
                    sendPoolUpdate(eventId);
                } catch (Exception e) {
                    log.error("Failed to send scheduled update for event {}: {}", eventId, e.getMessage());
                }
            }, UPDATE_THROTTLE_MS, TimeUnit.MILLISECONDS);
        }
    }

    @Override
    public void initialize(String eventId, Configuration config) {
        synchronized(getLock(eventId)) {
            log.info("Initializing ticket pool for event {} with capacity {}",
                    eventId, config.getMaxTicketCapacity());

            ticketPools.put(eventId, new LinkedBlockingQueue<>(config.getMaxTicketCapacity()));
            configs.put(eventId, config);
            runningStatus.put(eventId, new AtomicBoolean(true));

            schedulePoolUpdate(eventId);
        }
    }

    private void sendPoolUpdate(String eventId) {
        try {
            BlockingQueue<Ticket> pool = ticketPools.get(eventId);
            Configuration config = configs.get(eventId);

            if (pool != null && config != null) {
                TicketPoolUpdate update = new TicketPoolUpdate(
                        eventId,
                        pool.size(),
                        config.getMaxTicketCapacity()
                );
                //messagingTemplate.convertAndSend("/topic/tickets/" + eventId, update);
                WebSocketLogger.getInstance().log(update.getEventId() + "," +
                        update.getTotalCapacity() + "," + update.getAvailableTickets());

                log.debug("Sent ticket pool update for event {}: available={}, capacity={}",
                        eventId, update.getAvailableTickets(), update.getTotalCapacity());
            }
        } catch (Exception e) {
            log.error("Failed to send ticket pool update for event {}: {}", eventId, e.getMessage());
        }
    }

    private Object getLock(String eventId) {
        return ticketPools.computeIfAbsent(eventId, k -> new LinkedBlockingQueue<>());
    }


    @Override
    public synchronized void addTickets(String eventId, int count) {
        log.info("Attempting to add {} tickets to event {}", count, eventId);
        validateEventState(eventId);
        BlockingQueue<Ticket> ticketPool = getTicketPool(eventId);
        Configuration config = getConfig(eventId);

        int availableCapacity = getAvailableCapacity(ticketPool, config);
        if (availableCapacity <= 0) {
            log.warn("Ticket pool at capacity for event: {}. Current size: {}, Max capacity: {}",
                    eventId, ticketPool.size(), config.getMaxTicketCapacity());
            return;
        }

        int ticketsToAdd = Math.min(count, availableCapacity);
        log.debug("Will attempt to add {} tickets (adjusted for capacity) to event {}",
                ticketsToAdd, eventId);
        processTicketBatches(eventId, ticketsToAdd, ticketPool);
    }

    @Override
    public synchronized Ticket purchaseTicket(String eventId, String customerId) {
        log.info("Attempting to purchase ticket for customer {} in event {}", customerId, eventId);
        validateEventState(eventId);
        BlockingQueue<Ticket> ticketPool = getTicketPool(eventId);

        try {
            log.debug("Polling ticket pool for event {}. Current pool size: {}",
                    eventId, ticketPool.size());
            Ticket ticket = ticketPool.poll(QUEUE_TIMEOUT_MS, TimeUnit.MILLISECONDS);

            if (ticket == null) {
                log.debug("No ticket available for event {} within timeout period", eventId);
                return null;
            }

            return processPurchase(ticket, customerId, ticketPool);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Interrupted while purchasing ticket for event {}", eventId);
            return null;
        }
    }

    @Override
    public void shutdown() {
        log.info("Shutting down ticket pool service");

        try {
            // Shutdown all active events first
            Set<String> activeEvents = new HashSet<>(ticketPools.keySet());
            activeEvents.forEach(this::shutdownEvent);

            // Shutdown the executor service
            updateExecutor.shutdown();
            if (!updateExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                updateExecutor.shutdownNow();
            }

            // Clear any remaining resources
            ticketPools.clear();
            configs.clear();
            runningStatus.clear();
            lastUpdateTime.clear();

            log.info("Ticket pool service shutdown completed");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            updateExecutor.shutdownNow();
            log.error("Interrupted during shutdown", e);
        }
    }

    @Override
    public void shutdownEvent(String eventId) {
        log.info("Shutting down ticket pool for event {}", eventId);

        // Remove event-specific resources
        BlockingQueue<Ticket> ticketPool = ticketPools.remove(eventId);
        configs.remove(eventId);
        runningStatus.remove(eventId);

        if (ticketPool != null) {
            int remainingTickets = ticketPool.size();
            ticketPool.clear();
            log.info("Cleared ticket pool for event {}. Removed {} remaining tickets",
                    eventId, remainingTickets);

            // Send final update before shutdown
            sendPoolUpdate(eventId);
        }
    }


    @Override
    public int getAvailableTicketCount(String eventId) {
        BlockingQueue<Ticket> ticketPool = ticketPools.get(eventId);
        int count = ticketPool != null ? ticketPool.size() : 0;
        log.debug("Available ticket count for event {}: {}", eventId, count);
        return count;
    }

    @Override
    public boolean isPoolFull(String eventId) {
        BlockingQueue<Ticket> ticketPool = ticketPools.get(eventId);
        Configuration config = configs.get(eventId);

        if (ticketPool == null || config == null) {
            log.debug("Cannot check if pool is full - pool or config not found for event {}", eventId);
            return false;
        }

        boolean isFull = ticketPool.size() >= config.getMaxTicketCapacity();
        log.debug("Checked pool fullness for event {}: {} (size: {}, capacity: {})",
                eventId, isFull, ticketPool.size(), config.getMaxTicketCapacity());
        return isFull;
    }

    @Override
    public Set<String> getActiveEventIds() {
        Set<String> activeEvents = new HashSet<>(ticketPools.keySet());
        log.debug("Retrieved {} active event IDs", activeEvents.size());
        return activeEvents;
    }

    @Override
    public int getMaxCapacity(String eventId) {
        Configuration config = configs.get(eventId);
        int capacity = config != null ? config.getMaxTicketCapacity() : 0;
        log.debug("Max capacity for event {}: {}", eventId, capacity);
        return capacity;
    }

    private void processTicketBatches(String eventId, int totalCount, BlockingQueue<Ticket> ticketPool) {
        log.debug("Starting batch processing for {} tickets in event {}", totalCount, eventId);
        List<Ticket> currentBatch = new ArrayList<>(BATCH_SIZE);
        int processedCount = 0;

        while (processedCount < totalCount && isRunning(eventId)) {
            int remainingCount = totalCount - processedCount;
            int batchSize = Math.min(BATCH_SIZE, remainingCount);

            log.debug("Processing batch of size {} ({}/{} tickets)",
                    batchSize, processedCount + batchSize, totalCount);

            currentBatch.addAll(createTicketBatch(eventId, batchSize));

            if (!processBatch(currentBatch, ticketPool, eventId)) {
                log.warn("Batch processing failed, stopping ticket creation for event {}", eventId);
                break;
            }

            processedCount += currentBatch.size();
            currentBatch.clear();
        }

        log.info("Completed batch processing. Added {} tickets to event {}", processedCount, eventId);
    }

    private List<Ticket> createTicketBatch(String eventId, int count) {
        log.debug("Creating batch of {} tickets for event {}", count, eventId);
        List<Ticket> tickets = IntStream.range(0, count)
                .mapToObj(i -> Ticket.builder()
                        .eventId(eventId)
                        .status(Ticket.TicketStatus.AVAILABLE)
                        .createdAt(LocalDateTime.now())
                        .build())
                .collect(Collectors.toList());
        log.debug("Created {} ticket objects in memory", tickets.size());
        return tickets;
    }

    private boolean processBatch(List<Ticket> batch, BlockingQueue<Ticket> ticketPool, String eventId) {
        try {
            log.debug("Saving batch of {} tickets to database for event {}", batch.size(), eventId);
            List<Ticket> savedTickets = retryOperation(() -> ticketRepository.saveAll(batch));
            log.debug("Successfully saved {} tickets to database", savedTickets.size());

            boolean added = addTicketsToPool(savedTickets, ticketPool, eventId);
            if (!added) {
                log.warn("Failed to add all tickets to pool for event {}", eventId);
            } else {
                // Send update after successful batch addition
                sendPoolUpdate(eventId);
            }
            return added;
        } catch (Exception e) {
            log.error("Failed to process ticket batch for event {}: {}", eventId, e.getMessage(), e);
            return false;
        }
    }

    private boolean addTicketsToPool(List<Ticket> tickets, BlockingQueue<Ticket> ticketPool, String eventId) {
        log.debug("Adding {} tickets to pool for event {}", tickets.size(), eventId);
        int addedCount = 0;

        for (Ticket ticket : tickets) {
            try {
                if (!ticketPool.offer(ticket, QUEUE_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
                    log.warn("Queue offer timeout for event: {}. Added {}/{} tickets",
                            eventId, addedCount, tickets.size());
                    return false;
                }
                addedCount++;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Interrupted while adding tickets to pool. Added {}/{} tickets",
                        addedCount, tickets.size());
                return false;
            }
        }

        log.debug("Successfully added all {} tickets to pool", tickets.size());
        return true;
    }

    private Ticket processPurchase(Ticket ticket, String customerId, BlockingQueue<Ticket> ticketPool) {
        try {
            log.debug("Processing purchase of ticket {} for customer {}", ticket.getId(), customerId);
            ticket.setStatus(Ticket.TicketStatus.PURCHASED);
            ticket.setPurchasedAt(LocalDateTime.now());
            ticket.setPurchasedBy(customerId);

            Ticket savedTicket = retryOperation(() -> ticketRepository.save(ticket));
            log.info("Successfully purchased ticket {} for customer {}",
                    savedTicket.getId(), customerId);

            // Send update after successful purchase
            sendPoolUpdate(ticket.getEventId());
            return savedTicket;
        } catch (Exception e) {
            log.error("Failed to process ticket purchase for customer {}: {}",
                    customerId, e.getMessage(), e);
            log.debug("Returning ticket to pool after failed purchase");
            ticketPool.offer(ticket);
            return null;
        }
    }

    private <T> T retryOperation(Supplier<T> operation) {
        for (int i = 0; i < DB_RETRY_COUNT; i++) {
            try {
                return operation.get();
            } catch (Exception e) {
                if (i == DB_RETRY_COUNT - 1) {
                    log.error("Operation failed after {} retries", DB_RETRY_COUNT);
                    throw e;
                }
                log.warn("Operation failed, attempt {}/{}. Retrying after delay...",
                        i + 1, DB_RETRY_COUNT);
                try {
                    Thread.sleep(DB_RETRY_DELAY_MS * (i + 1));
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted during retry", ie);
                }
            }
        }
        throw new RuntimeException("Failed after " + DB_RETRY_COUNT + " retries");
    }

    private void validateEventState(String eventId) {
        AtomicBoolean status = runningStatus.get(eventId);
        if (status == null || !status.get()) {
            log.error("Attempted to access inactive ticket pool for event {}", eventId);
            throw new IllegalStateException("Ticket pool is not running for event: " + eventId);
        }
        log.trace("Validated event state for {}", eventId);
    }

    private BlockingQueue<Ticket> getTicketPool(String eventId) {
        BlockingQueue<Ticket> pool = ticketPools.get(eventId);
        if (pool == null) {
            log.error("Ticket pool not found for event {}", eventId);
            throw new IllegalStateException("Ticket pool not initialized for event: " + eventId);
        }
        log.trace("Retrieved ticket pool for event {} with size {}", eventId, pool.size());
        return pool;
    }

    private Configuration getConfig(String eventId) {
        Configuration config = configs.get(eventId);
        if (config == null) {
            log.error("Configuration not found for event {}", eventId);
            throw new IllegalStateException("Configuration not found for event: " + eventId);
        }
        log.trace("Retrieved configuration for event {}", eventId);
        return config;
    }

    private boolean isRunning(String eventId) {
        AtomicBoolean status = runningStatus.get(eventId);
        boolean running = status != null && status.get();
        log.trace("Checked running status for event {}: {}", eventId, running);
        return running;
    }

    private int getAvailableCapacity(BlockingQueue<Ticket> ticketPool, Configuration config) {
        int capacity = config.getMaxTicketCapacity() - ticketPool.size();
        log.trace("Available capacity for ticket pool: {} (max: {}, current: {})",
                capacity, config.getMaxTicketCapacity(), ticketPool.size());
        return capacity;
    }
}
