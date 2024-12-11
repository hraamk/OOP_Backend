package com.ticketing.simulation.ticket_pool_simulation.service.impl;

import org.springframework.context.ApplicationEvent;

public class SimulationStopEvent extends ApplicationEvent {
    private final String eventId;

    public SimulationStopEvent(Object source, String eventId) {
        super(source);
        this.eventId = eventId;
    }

    public String getEventId() {
        return eventId;
    }
}