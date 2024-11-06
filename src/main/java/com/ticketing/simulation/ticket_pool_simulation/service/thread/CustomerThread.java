package com.ticketing.simulation.ticket_pool_simulation.service.thread;

import com.ticketing.simulation.ticket_pool_simulation.model.entity.Ticket;
import com.ticketing.simulation.ticket_pool_simulation.service.interfaces.TicketPoolService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class CustomerThread implements Runnable {
    private final TicketPoolService ticketPoolService;
    private final String customerId;
    private final long purchaseInterval;
    private volatile boolean running = true;

    @Override
    public void run() {
        while (running) {
            try {
                Ticket ticket = ticketPoolService.purchaseTicket(customerId);
                log.info("Customer {} purchased ticket {}", customerId, ticket.getId());
                Thread.sleep(purchaseInterval);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Customer thread interrupted");
                break;
            } catch (Exception e) {
                log.error("Error purchasing ticket", e);
            }
        }
    }

    public void stop() {
        running = false;
    }
}