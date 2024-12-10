package com.ticketing.simulation.ticket_pool_simulation.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SimulationStatus {
    private boolean running;
    private int vendorCount;
    private int customerCount;
    private int activeThreadCount;

    @Override
    public String toString() {
        return String.format(
                "SimulationStatus(running=%s, vendors=%d, customers=%d, activeThreads=%d)",
                running, vendorCount, customerCount, activeThreadCount
        );
    }
}