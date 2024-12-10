package com.ticketing.simulation.ticket_pool_simulation.service.interfaces;

import com.ticketing.simulation.ticket_pool_simulation.model.dto.SimulationStatus;
import com.ticketing.simulation.ticket_pool_simulation.model.entity.Configuration;
import java.util.List;

public interface SimulationService {
    SimulationStatus startSimulation(String eventId, Configuration config);
    void stopSimulation(String eventId);
    void pauseSimulation(String eventId);
    void resumeSimulation(String eventId);
    Configuration getConfiguration(String eventId);
    SimulationStatus getSimulationStatus(String eventId);
    List<Configuration> getAllSimulations();
    void cleanupSimulations();

    // New methods for managing vendor and customer counts
    SimulationStatus increaseVendorCount(String eventId, int count);
    SimulationStatus decreaseVendorCount(String eventId, int count);
    SimulationStatus increaseCustomerCount(String eventId, int count);
    SimulationStatus decreaseCustomerCount(String eventId, int count);
}