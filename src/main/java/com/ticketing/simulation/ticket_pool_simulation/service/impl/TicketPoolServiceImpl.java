package com.ticketing.simulation.ticket_pool_simulation.service.impl;

import com.ticketing.simulation.ticket_pool_simulation.model.entity.Configuration;
import com.ticketing.simulation.ticket_pool_simulation.model.entity.Ticket;
import com.ticketing.simulation.ticket_pool_simulation.repository.TicketRepository;
import com.ticketing.simulation.ticket_pool_simulation.service.interfaces.TicketPoolService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Service
@Slf4j
@RequiredArgsConstructor
public class TicketPoolServiceImpl implements TicketPoolService {
    private final TicketRepository ticketRepository;
    private BlockingQueue<Ticket> ticketPool;
    private Configuration config;
    private volatile boolean isRunning = false;

    @Override
    public void initialize(Configuration config) {
        this.config = config;
        this.ticketPool = new LinkedBlockingQueue<>(config.getMaxTicketCapacity());
        this.isRunning = true;
        log.info("Initialized ticket pool with capacity: {}", config.getMaxTicketCapacity());
    }

    @Override
    public synchronized void addTickets(int count) {
        if (!isRunning) {
            throw new IllegalStateException("Ticket pool is not running");
        }

        if (ticketPool.size() + count > config.getMaxTicketCapacity()) {
            log.warn("Cannot add {} tickets, pool at capacity", count);
            return;
        }

        for (int i = 0; i < count; i++) {
            Ticket ticket = Ticket.builder()
                    .status(Ticket.TicketStatus.AVAILABLE)
                    .createdAt(LocalDateTime.now())
                    .build();

            try {
                ticketPool.put(ticket);
                ticketRepository.save(ticket);
                log.debug("Added ticket to pool. Current size: {}", ticketPool.size());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Interrupted while adding tickets", e);
                break;
            }
        }
    }

    @Override
    public Ticket purchaseTicket(String customerId) {
        if (!isRunning) {
            throw new IllegalStateException("Ticket pool is not running");
        }

        try {
            Ticket ticket = ticketPool.take();
            ticket.setStatus(Ticket.TicketStatus.PURCHASED);
            ticket.setPurchasedAt(LocalDateTime.now());
            ticket.setPurchasedBy(customerId);
            ticketRepository.save(ticket);

            log.info("Ticket purchased by customer {}. Remaining: {}",
                    customerId, ticketPool.size());
            return ticket;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while purchasing ticket", e);
            throw new RuntimeException("Failed to purchase ticket", e);
        }
    }

    @Override
    public int getAvailableTicketCount() {
        return ticketPool.size();
    }

    @Override
    public boolean isPoolFull() {
        return ticketPool.size() >= config.getMaxTicketCapacity();
    }

    @Override
    public void shutdown() {
        isRunning = false;
        ticketPool.clear();
        log.info("Ticket pool shutdown");
    }
}