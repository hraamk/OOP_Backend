package com.ticketing.simulation.ticket_pool_simulation.service.interfaces;

import com.ticketing.simulation.ticket_pool_simulation.model.entity.Configuration;
import com.ticketing.simulation.ticket_pool_simulation.model.dto.SimulationStatus;

public interface SimulationService {
    void startSimulation(Configuration config);
    void stopSimulation();
    Configuration getCurrentConfig();
    void pauseSimulation();
    void resumeSimulation();

}

