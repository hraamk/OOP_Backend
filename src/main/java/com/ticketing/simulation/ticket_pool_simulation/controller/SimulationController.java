package com.ticketing.simulation.ticket_pool_simulation.controller;

import com.ticketing.simulation.ticket_pool_simulation.model.dto.SimulationStatus;
import com.ticketing.simulation.ticket_pool_simulation.model.entity.Configuration;
import com.ticketing.simulation.ticket_pool_simulation.service.interfaces.SimulationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    @PostMapping("/{eventId}/start")
    public ResponseEntity<?> startSimulation(@PathVariable String eventId,
                                             @RequestBody Configuration config) {
        try {
            Configuration currentConfig = simulationService.getSimulationStatus(eventId);
            if (currentConfig != null && currentConfig.isRunning()) {
                return ResponseEntity.badRequest()
                        .body("Simulation for event " + eventId + " is already running. Please stop it first.");
            }

            simulationService.startSimulation(eventId, config);
            return ResponseEntity.ok("Simulation started successfully for event: " + eventId);
        } catch (Exception e) {
            log.error("Error starting simulation for event: {}", eventId, e);
            return ResponseEntity.internalServerError()
                    .body("Failed to start simulation: " + e.getMessage());
        }
    }

    @PostMapping("/{eventId}/stop")
    public ResponseEntity<?> stopSimulation(@PathVariable String eventId) {
        try {
            Configuration currentConfig = simulationService.getSimulationStatus(eventId);
            if (currentConfig == null || !currentConfig.isRunning()) {
                return ResponseEntity.badRequest()
                        .body("No simulation is currently running for event: " + eventId);
            }

            simulationService.stopSimulation(eventId);
            return ResponseEntity.ok("Simulation stopped successfully for event: " + eventId);
        } catch (Exception e) {
            log.error("Error stopping simulation for event: {}", eventId, e);
            return ResponseEntity.internalServerError()
                    .body("Failed to stop simulation: " + e.getMessage());
        }
    }

    @PostMapping("/{eventId}/pause")
    public ResponseEntity<?> pauseSimulation(@PathVariable String eventId) {
        try {
            Configuration currentConfig = simulationService.getSimulationStatus(eventId);
            if (currentConfig == null || !currentConfig.isRunning()) {
                return ResponseEntity.badRequest()
                        .body("No simulation is currently running for event: " + eventId);
            }

            simulationService.pauseSimulation(eventId);
            return ResponseEntity.ok("Simulation paused successfully for event: " + eventId);
        } catch (Exception e) {
            log.error("Error pausing simulation for event: {}", eventId, e);
            return ResponseEntity.internalServerError()
                    .body("Failed to pause simulation: " + e.getMessage());
        }
    }

    @PostMapping("/{eventId}/resume")
    public ResponseEntity<?> resumeSimulation(@PathVariable String eventId) {
        try {
            Configuration currentConfig = simulationService.getSimulationStatus(eventId);
            if (currentConfig == null) {
                return ResponseEntity.badRequest()
                        .body("No simulation exists for event: " + eventId);
            }

            if (!currentConfig.isPaused()) {
                return ResponseEntity.badRequest()
                        .body("Simulation for event " + eventId + " is not paused");
            }

            simulationService.resumeSimulation(eventId);
            return ResponseEntity.ok("Simulation resumed successfully for event: " + eventId);
        } catch (Exception e) {
            log.error("Error resuming simulation for event: {}", eventId, e);
            return ResponseEntity.internalServerError()
                    .body("Failed to resume simulation: " + e.getMessage());
        }
    }

    @GetMapping("/{eventId}/status")
    public ResponseEntity<?> getSimulationStatus(@PathVariable String eventId) {
        try {
            Configuration currentConfig = simulationService.getSimulationStatus(eventId);
            if (currentConfig == null) {
                return ResponseEntity.ok()
                        .body(new SimulationStatus(false, 0, 0, 0));
            }

            return ResponseEntity.ok(currentConfig);
        } catch (Exception e) {
            log.error("Error fetching simulation status for event: {}", eventId, e);
            return ResponseEntity.internalServerError()
                    .body("Failed to fetch simulation status: " + e.getMessage());
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