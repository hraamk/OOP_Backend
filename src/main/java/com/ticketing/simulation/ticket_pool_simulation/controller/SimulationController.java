package com.ticketing.simulation.ticket_pool_simulation.controller;

import com.ticketing.simulation.ticket_pool_simulation.model.dto.SimulationStatus;
import com.ticketing.simulation.ticket_pool_simulation.model.entity.Configuration;
import com.ticketing.simulation.ticket_pool_simulation.service.interfaces.SimulationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/simulation")
@CrossOrigin(origins = "*")
@Slf4j
@RequiredArgsConstructor
public class SimulationController {

    private final SimulationService simulationService;

    @PostMapping(value = "/{eventId}/start", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SimulationStatus> startSimulation(
            @PathVariable String eventId,
            @RequestBody Configuration config) {
        try {
            log.info("Starting simulation for event: {} with config: {}", eventId, config);

            // Check if simulation is already running
            Configuration currentConfig = simulationService.getConfiguration(eventId);
            if (currentConfig != null && currentConfig.isRunning()) {
                SimulationStatus currentStatus = simulationService.getSimulationStatus(eventId);
                log.warn("Simulation already running for event: {}", eventId);
                return ResponseEntity.ok(currentStatus);
            }

            // Start new simulation
            SimulationStatus status = simulationService.startSimulation(eventId, config);
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            log.error("Error starting simulation for event: {}", eventId, e);
            return ResponseEntity.ok(new SimulationStatus(false, 0, 0, 0));
        }
    }

    @PostMapping("/{eventId}/stop")
    public ResponseEntity<SimulationStatus> stopSimulation(@PathVariable String eventId) {
        try {
            Configuration config = simulationService.getConfiguration(eventId);
            if (config == null || !config.isRunning()) {
                return ResponseEntity.badRequest()
                        .body(new SimulationStatus(false, 0, 0, 0));
            }

            simulationService.stopSimulation(eventId);
            return ResponseEntity.ok(new SimulationStatus(false, 0, 0, 0));
        } catch (Exception e) {
            log.error("Error stopping simulation for event: {}", eventId, e);
            return ResponseEntity.internalServerError()
                    .body(new SimulationStatus(false, 0, 0, 0));
        }
    }

    @PostMapping("/{eventId}/pause")
    public ResponseEntity<SimulationStatus> pauseSimulation(@PathVariable String eventId) {
        try {
            Configuration currentConfig = simulationService.getConfiguration(eventId);
            if (currentConfig == null || !currentConfig.isRunning()) {
                return ResponseEntity.badRequest()
                        .body(new SimulationStatus(false, 0, 0, 0));
            }

            simulationService.pauseSimulation(eventId);
            return ResponseEntity.ok(simulationService.getSimulationStatus(eventId));
        } catch (Exception e) {
            log.error("Error pausing simulation for event: {}", eventId, e);
            return ResponseEntity.internalServerError()
                    .body(new SimulationStatus(false, 0, 0, 0));
        }
    }

    @PostMapping("/{eventId}/resume")
    public ResponseEntity<SimulationStatus> resumeSimulation(@PathVariable String eventId) {
        try {
            Configuration currentConfig = simulationService.getConfiguration(eventId);
            if (currentConfig == null) {
                return ResponseEntity.badRequest()
                        .body(new SimulationStatus(false, 0, 0, 0));
            }

            if (!currentConfig.isPaused()) {
                return ResponseEntity.badRequest()
                        .body(simulationService.getSimulationStatus(eventId));
            }

            simulationService.resumeSimulation(eventId);
            return ResponseEntity.ok(simulationService.getSimulationStatus(eventId));
        } catch (Exception e) {
            log.error("Error resuming simulation for event: {}", eventId, e);
            return ResponseEntity.internalServerError()
                    .body(new SimulationStatus(false, 0, 0, 0));
        }
    }

    @GetMapping("/{eventId}/status")
    public ResponseEntity<SimulationStatus> getStatus(@PathVariable String eventId) {
        try {
            // Always return OK (200) with a status, even if it's the default one
            SimulationStatus status = simulationService.getSimulationStatus(eventId);
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            log.error("Error getting simulation status for event: {}", eventId, e);
            // Return default status with 200 instead of 500
            return ResponseEntity.ok(new SimulationStatus(false, 0, 0, 0));
        }
    }


    @GetMapping("/all")
    public ResponseEntity<?> getAllSimulations() {
        try {
            List<Configuration> simulations = simulationService.getAllSimulations();
            return ResponseEntity.ok(simulations);
        } catch (Exception e) {
            log.error("Error fetching all simulations", e);
            return ResponseEntity.internalServerError()
                    .body("Failed to fetch simulations: " + e.getMessage());
        }
    }
}