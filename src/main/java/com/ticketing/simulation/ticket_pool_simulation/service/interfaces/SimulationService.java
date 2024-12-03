package com.ticketing.simulation.ticket_pool_simulation.service.interfaces;

import com.ticketing.simulation.ticket_pool_simulation.model.entity.Configuration;
import java.util.List;

public interface SimulationService {
    void startSimulation(String eventId, Configuration config);
    void stopSimulation(String eventId);
    void pauseSimulation(String eventId);
    void resumeSimulation(String eventId);
    Configuration getSimulationStatus(String eventId);
    List<Configuration> getAllSimulations();
    void cleanupSimulations();
}