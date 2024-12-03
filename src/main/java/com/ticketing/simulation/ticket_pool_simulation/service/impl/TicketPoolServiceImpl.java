package com.ticketing.simulation.ticket_pool_simulation.service.impl;

import com.ticketing.simulation.ticket_pool_simulation.model.entity.Configuration;
import com.ticketing.simulation.ticket_pool_simulation.model.entity.Ticket;
import com.ticketing.simulation.ticket_pool_simulation.repository.TicketRepository;
import com.ticketing.simulation.ticket_pool_simulation.service.interfaces.TicketPoolService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@Slf4j
@RequiredArgsConstructor
public class TicketPoolServiceImpl implements TicketPoolService {
    private final TicketRepository ticketRepository;
    private final Map<String, BlockingQueue<Ticket>> ticketPools = new ConcurrentHashMap<>();
    private final Map<String, Configuration> configs = new ConcurrentHashMap<>();
    private final Map<String, AtomicBoolean> runningStatus = new ConcurrentHashMap<>();

    @Override
    public synchronized void addTickets(String eventId, int count) {
        AtomicBoolean isRunning = runningStatus.get(eventId);
        if (isRunning == null || !isRunning.get()) {
            throw new IllegalStateException("Ticket pool is not running for event: " + eventId);
        }

        BlockingQueue<Ticket> ticketPool = ticketPools.get(eventId);
        Configuration config = configs.get(eventId);

        if (ticketPool == null || config == null) {
            throw new IllegalStateException("Ticket pool not initialized for event: " + eventId);
        }

        if (ticketPool.size() + count > config.getMaxTicketCapacity()) {
            log.warn("Cannot add {} tickets for event {}, pool at capacity", count, eventId);
            return;
        }

        for (int i = 0; i < count && isRunning.get(); i++) {
            Ticket ticket = Ticket.builder()
                    .eventId(eventId)
                    .status(Ticket.TicketStatus.AVAILABLE)
                    .createdAt(LocalDateTime.now())
                    .build();

            try {
                if (!ticketPool.offer(ticket, 100, TimeUnit.MILLISECONDS)) {
                    log.debug("Skipping ticket addition due to timeout");
                    break;
                }
                ticketRepository.save(ticket);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.debug("Interrupted while adding tickets for event: {}", eventId);
                break;
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
            Ticket ticket = ticketPool.poll(100, TimeUnit.MILLISECONDS);
            if (ticket == null) {
                return null; // No ticket available
            }

            ticket.setStatus(Ticket.TicketStatus.PURCHASED);
            ticket.setPurchasedAt(LocalDateTime.now());
            ticket.setPurchasedBy(customerId);
            ticketRepository.save(ticket);

            log.info("Ticket purchased by customer {} for event {}. Remaining: {}",
                    customerId, eventId, ticketPool.size());
            return ticket;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.debug("Interrupted while purchasing ticket for event: {}", eventId);
            return null;
        }
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