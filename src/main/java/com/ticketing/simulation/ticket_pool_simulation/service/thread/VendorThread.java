package com.ticketing.simulation.ticket_pool_simulation.service.thread;


import com.ticketing.simulation.ticket_pool_simulation.service.interfaces.TicketPoolService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class VendorThread implements Runnable {
    private final TicketPoolService ticketPoolService;
    private final int ticketsPerRelease;
    private final long releaseInterval;
    private volatile boolean running = true;

    @Override
    public void run() {
        while (running) {
            try {
                if (!ticketPoolService.isPoolFull()) {
                    ticketPoolService.addTickets(ticketsPerRelease);
                    log.info("Released {} tickets", ticketsPerRelease);
                } else {
                    log.debug("Ticket pool full, waiting...");
                }
                Thread.sleep(releaseInterval);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Vendor thread interrupted");
                break;
            } catch (Exception e) {
                log.error("Error releasing tickets", e);
            }
        }
    }

    public void stop() {
        running = false;
    }
}