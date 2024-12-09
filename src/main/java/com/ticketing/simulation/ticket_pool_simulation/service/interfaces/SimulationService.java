package com.ticketing.simulation.ticket_pool_simulation.service.interfaces;

import com.ticketing.simulation.ticket_pool_simulation.model.dto.SimulationStatus;
import com.ticketing.simulation.ticket_pool_simulation.model.entity.Configuration;
import java.util.List;

public interface SimulationService {
    SimulationStatus startSimulation(String eventId, Configuration config);  // Changed return type to SimulationStatus
    void stopSimulation(String eventId);
    void pauseSimulation(String eventId);
    void resumeSimulation(String eventId);
    Configuration getConfiguration(String eventId);
    SimulationStatus getSimulationStatus(String eventId);
    List<Configuration> getAllSimulations();
    void cleanupSimulations();
}