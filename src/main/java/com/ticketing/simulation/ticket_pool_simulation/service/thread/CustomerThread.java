package com.ticketing.simulation.ticket_pool_simulation.service.thread;

import com.ticketing.simulation.ticket_pool_simulation.model.entity.Ticket;
import com.ticketing.simulation.ticket_pool_simulation.service.interfaces.TicketPoolService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class CustomerThread implements Runnable {
    private final TicketPoolService ticketPoolService;
    private final String eventId;
    private final String customerId;
    private final long sleepTime;
    private volatile boolean running = true;
    private volatile boolean paused = false;

    public void stop() {
        running = false;
    }

    public void pause() {
        paused = true;
    }

    public void resume() {
        paused = false;
    }

    @Override
    public void run() {
        while (running) {
            try {
                if (!paused) {
                    Ticket ticket = ticketPoolService.purchaseTicket(eventId, customerId);
                    if (ticket == null) {
                        // No ticket available or interrupted, wait before retrying
                        Thread.sleep(sleepTime);
                        continue;
                    }
                }
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("Error in customer thread: {}", e.getMessage());
                break;
            }
        }
        log.info("Customer thread {} stopped", customerId);
    }
}