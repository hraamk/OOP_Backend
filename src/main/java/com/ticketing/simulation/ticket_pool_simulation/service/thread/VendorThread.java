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
                if (!paused && !ticketPoolService.isPoolFull(eventId)) {
                    ticketPoolService.addTickets(eventId, 1);
                }
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}
