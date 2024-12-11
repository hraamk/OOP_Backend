package com.ticketing.simulation.ticket_pool_simulation.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class SimulationEventHandlerService {
    private final ApplicationEventPublisher eventPublisher;

    public void publishSimulationStopEvent(String eventId) {
        log.info("Publishing simulation stop event for event ID: {}", eventId);
        eventPublisher.publishEvent(new SimulationStopEvent(this, eventId));
    }
}


