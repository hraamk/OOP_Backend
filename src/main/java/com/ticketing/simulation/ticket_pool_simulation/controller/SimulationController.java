package com.ticketing.simulation.ticket_pool_simulation.controller;

import com.ticketing.simulation.ticket_pool_simulation.model.dto.SimulationStatus;
import com.ticketing.simulation.ticket_pool_simulation.model.entity.Configuration;
import com.ticketing.simulation.ticket_pool_simulation.service.interfaces.SimulationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/simulation")
@CrossOrigin(origins = "*")
@Slf4j
@RequiredArgsConstructor
public class SimulationController {

    private final SimulationService simulationService;

    @PostMapping("/start")
    public ResponseEntity<?> startSimulation(@RequestBody Configuration config) {
        try {
            if (simulationService.getCurrentConfig() != null &&
                    simulationService.getCurrentConfig().isRunning()) {
                return ResponseEntity.badRequest()
                        .body("Simulation is already running. Please stop it first.");
            }

            config.setRunning(true);
            simulationService.startSimulation(config);
            return ResponseEntity.ok("Simulation started successfully");
        } catch (Exception e) {
            log.error("Error starting simulation", e);
            return ResponseEntity.internalServerError()
                    .body("Failed to start simulation: " + e.getMessage());
        }
    }

    @PostMapping("/stop")
    public ResponseEntity<?> stopSimulation() {
        try {
            if (simulationService.getCurrentConfig() == null ||
                    !simulationService.getCurrentConfig().isRunning()) {
                return ResponseEntity.badRequest()
                        .body("No simulation is currently running");
            }

            simulationService.stopSimulation();
            return ResponseEntity.ok("Simulation stopped successfully");
        } catch (Exception e) {
            log.error("Error stopping simulation", e);
            return ResponseEntity.internalServerError()
                    .body("Failed to stop simulation: " + e.getMessage());
        }
    }

    @PostMapping("/pause")
    public ResponseEntity<?> pauseSimulation() {
        try {
            if (simulationService.getCurrentConfig() == null ||
                    !simulationService.getCurrentConfig().isRunning()) {
                return ResponseEntity.badRequest()
                        .body("No simulation is currently running");
            }

            simulationService.pauseSimulation();
            return ResponseEntity.ok("Simulation paused successfully");
        } catch (Exception e) {
            log.error("Error pausing simulation", e);
            return ResponseEntity.internalServerError()
                    .body("Failed to pause simulation: " + e.getMessage());
        }
    }

    @PostMapping("/resume")
    public ResponseEntity<?> resumeSimulation() {
        try {
            if (simulationService.getCurrentConfig() == null) {
                return ResponseEntity.badRequest()
                        .body("No simulation has been started");
            }

            simulationService.resumeSimulation();
            return ResponseEntity.ok("Simulation resumed successfully");
        } catch (Exception e) {
            log.error("Error resuming simulation", e);
            return ResponseEntity.internalServerError()
                    .body("Failed to resume simulation: " + e.getMessage());
        }
    }

    @GetMapping("/status")
    public ResponseEntity<?> getSimulationStatus() {
        try {
            Configuration currentConfig = simulationService.getCurrentConfig();
            if (currentConfig == null) {
                return ResponseEntity.ok()
                        .body(new SimulationStatus(false, 0, 0, 0));
            }

            return ResponseEntity.ok(currentConfig);
        } catch (Exception e) {
            log.error("Error fetching simulation status", e);
            return ResponseEntity.internalServerError()
                    .body("Failed to fetch simulation status: " + e.getMessage());
        }
    }
}