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
    private static final int MAX_RETRIES = 3;

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
            if (Thread.interrupted()) {
                break;
            }

            try {
                if (!paused) {
                    int retries = 0;
                    boolean success = false;

                    while (!success && retries < MAX_RETRIES && running && !paused) {
                        try {
                            Ticket ticket = ticketPoolService.purchaseTicket(eventId, customerId);
                            if (ticket == null) {
                                // No ticket available, break retry loop
                                break;
                            }
                            success = true;
                        } catch (Exception e) {
                            retries++;
                            if (retries >= MAX_RETRIES) {
                                log.error("Failed to purchase ticket after {} retries for customer: {}",
                                        MAX_RETRIES, customerId);
                                break;
                            }
                            Thread.sleep(100 * retries); // Exponential backoff
                        }
                    }
                }
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("Error in customer thread {}: {}", customerId, e.getMessage());
                // Continue running but log the error
            }
        }
        log.info("Customer thread {} stopped", customerId);
    }

}