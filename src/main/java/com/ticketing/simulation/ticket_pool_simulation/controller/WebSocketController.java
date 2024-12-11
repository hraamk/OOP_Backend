package com.ticketing.simulation.ticket_pool_simulation.controller;

import com.ticketing.simulation.ticket_pool_simulation.model.dto.TicketPoolUpdate;
import com.ticketing.simulation.ticket_pool_simulation.service.interfaces.TicketPoolService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;

import java.util.Set;

@Controller
@RequiredArgsConstructor
@Slf4j
public class WebSocketController {
    private final TicketPoolService ticketPoolService;

    //  @Scheduled(fixedRate = 1000)
    public void sendTicketPoolUpdates() {
        try {
            Set<String> activeEvents = ticketPoolService.getActiveEventIds();
            log.debug("Found {} active events for updates", activeEvents.size());

            activeEvents.forEach(eventId -> {
                try {
                    int availableTickets = ticketPoolService.getAvailableTicketCount(eventId);
                    int maxCapacity = ticketPoolService.getMaxCapacity(eventId);

                    TicketPoolUpdate update = new TicketPoolUpdate(eventId, availableTickets, maxCapacity);
                    // messagingTemplate.convertAndSend("/topic/tickets/" + eventId, update);

                    log.debug("Sent update for event {}: available={}, capacity={}",
                            eventId, availableTickets, maxCapacity);
                } catch (Exception e) {
                    log.error("Failed to process update for event {}: {}", eventId, e.getMessage());
                }
            });
        } catch (Exception e) {
            log.error("Error in scheduled update task: {}", e.getMessage(), e);
        }
    }

    @MessageMapping("/test")
    @SendTo("/topic/test")
    public String handleTestMessage(String message) {
        log.info("Received test message: {}", message);
        return "Server received: " + message;
    }
}