// ConfigurationController.java
package com.ticketing.simulation.ticket_pool_simulation.controller;

import com.ticketing.simulation.ticket_pool_simulation.model.entity.Configuration;
import com.ticketing.simulation.ticket_pool_simulation.repository.ConfigurationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/configurations")
@CrossOrigin(origins = "*")
@Slf4j
public class ConfigurationController {

    private final ConfigurationRepository configurationRepository;

    public ConfigurationController(ConfigurationRepository configurationRepository) {
        this.configurationRepository = configurationRepository;
    }

    @GetMapping
    public ResponseEntity<List<Configuration>> getAllConfigurations() {
        try {
            List<Configuration> configurations = configurationRepository.findAll();
            return ResponseEntity.ok(configurations);
        } catch (Exception e) {
            log.error("Error fetching all configurations: {}", e.getMessage(), e);
            return ResponseEntity.ok(Collections.emptyList());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Configuration> getConfigurationById(@PathVariable String id) {
        try {
            return configurationRepository.findById(id)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.ok(null));
        } catch (Exception e) {
            log.error("Error fetching configuration by id {}: {}", id, e.getMessage(), e);
            return ResponseEntity.ok(null);
        }
    }

    @GetMapping("/event/{eventId}")
    public ResponseEntity<List<Configuration>> getConfigurationsByEventId(@PathVariable String eventId) {
        try {
            log.info("Fetching configurations for event: {}", eventId);
            List<Configuration> configurations = configurationRepository.findByEventId(eventId);
            return ResponseEntity.ok(configurations);
        } catch (Exception e) {
            log.error("Error fetching configurations for event {}: {}", eventId, e.getMessage(), e);
            return ResponseEntity.ok(Collections.emptyList());
        }
    }

    @GetMapping("/event/{eventId}/running")
    public ResponseEntity<Configuration> getRunningConfigurationByEventId(@PathVariable String eventId) {
        try {
            return configurationRepository.findRunningConfigurationByEventId(eventId)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.ok(null));
        } catch (Exception e) {
            log.error("Error fetching running configuration for event {}: {}", eventId, e.getMessage(), e);
            return ResponseEntity.ok(null);
        }
    }

    @PostMapping
    public ResponseEntity<Configuration> createConfiguration(@RequestBody Configuration configuration) {
        try {
            if (configuration.getId() != null && configurationRepository.existsById(configuration.getId())) {
                log.warn("Attempted to create configuration with existing id: {}", configuration.getId());
                return ResponseEntity.ok(null);
            }
            Configuration savedConfig = configurationRepository.save(configuration);
            log.info("Created new configuration with id: {}", savedConfig.getId());
            return ResponseEntity.ok(savedConfig);
        } catch (Exception e) {
            log.error("Error creating configuration: {}", e.getMessage(), e);
            return ResponseEntity.ok(null);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Configuration> updateConfiguration(
            @PathVariable String id,
            @RequestBody Configuration configuration) {
        try {
            if (!configurationRepository.existsById(id)) {
                log.warn("Attempted to update non-existent configuration: {}", id);
                return ResponseEntity.ok(null);
            }
            configuration.setId(id);
            Configuration updatedConfig = configurationRepository.save(configuration);
            log.info("Updated configuration: {}", id);
            return ResponseEntity.ok(updatedConfig);
        } catch (Exception e) {
            log.error("Error updating configuration {}: {}", id, e.getMessage(), e);
            return ResponseEntity.ok(null);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteConfiguration(@PathVariable String id) {
        try {
            if (!configurationRepository.existsById(id)) {
                log.warn("Attempted to delete non-existent configuration: {}", id);
                return ResponseEntity.ok().build();
            }
            configurationRepository.deleteById(id);
            log.info("Deleted configuration: {}", id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error deleting configuration {}: {}", id, e.getMessage(), e);
            return ResponseEntity.ok().build();
        }
    }
}