package com.ticketing.simulation.ticket_pool_simulation.service.thread;


import com.ticketing.simulation.ticket_pool_simulation.service.interfaces.TicketPoolService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class VendorThread implements Runnable {
    private final TicketPoolService ticketPoolService;
    private final String eventId;
    private final int ticketsPerMinute;
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
                if (!paused && !ticketPoolService.isPoolFull(eventId)) {
                    int retries = 0;
                    boolean success = false;

                    while (!success && retries < MAX_RETRIES && running && !paused) {
                        try {
                            ticketPoolService.addTickets(eventId, 1);
                            success = true;
                        } catch (Exception e) {
                            retries++;
                            if (retries >= MAX_RETRIES) {
                                log.error("Failed to add tickets after {} retries for event: {}",
                                        MAX_RETRIES, eventId);
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
                log.error("Error in vendor thread for event {}: {}", eventId, e.getMessage());
                // Continue running but log the error
            }
        }
        log.info("Vendor thread for event {} stopped", eventId);
    }
}
