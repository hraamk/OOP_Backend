package com.ticketing.simulation.ticket_pool_simulation.service.impl;

import com.ticketing.simulation.ticket_pool_simulation.model.dto.SimulationStatus;
import com.ticketing.simulation.ticket_pool_simulation.model.entity.Configuration;
import com.ticketing.simulation.ticket_pool_simulation.model.entity.Event;
import com.ticketing.simulation.ticket_pool_simulation.repository.ConfigurationRepository;
import com.ticketing.simulation.ticket_pool_simulation.repository.EventRepository;
import com.ticketing.simulation.ticket_pool_simulation.service.interfaces.SimulationService;
import com.ticketing.simulation.ticket_pool_simulation.service.interfaces.TicketPoolService;
import com.ticketing.simulation.ticket_pool_simulation.service.thread.CustomerThread;
import com.ticketing.simulation.ticket_pool_simulation.service.thread.VendorThread;
import com.ticketing.simulation.ticket_pool_simulation.exception.ResourceNotFoundException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
@RequiredArgsConstructor
public class SimulationServiceImpl implements SimulationService {
    private final TicketPoolService ticketPoolService;
    private final ConfigurationRepository configRepository;
    private final EventRepository eventRepository;

    private final Map<String, SimulationContext> activeSimulations = new ConcurrentHashMap<>();

    @Data
    @AllArgsConstructor
    private static class SimulationContext {
        private Configuration config;
        private List<VendorThread> vendorThreads;
        private List<CustomerThread> customerThreads;
        private List<Thread> runningThreads;
        private volatile boolean paused;
    }

    @Override
    public SimulationStatus startSimulation(String eventId, Configuration config) {
        try {
            log.info("Starting simulation for event: {}", eventId);

            // Check if simulation already exists
            if (activeSimulations.containsKey(eventId)) {
                log.warn("Simulation already exists for event: {}", eventId);
                return getSimulationStatus(eventId);
            }

            // Validate event existence
            Event event = eventRepository.findById(eventId)
                    .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + eventId));

            // Initialize configuration
            config.setEventId(eventId);
            config.setRunning(true);
            config.setPaused(false);

            // Save configuration
            try {
                config = configRepository.save(config);
            } catch (Exception e) {
                log.error("Error saving configuration: {}", e.getMessage());
                throw new RuntimeException("Failed to save configuration", e);
            }

            // Initialize ticket pool
            try {
                ticketPoolService.initialize(eventId, config);
            } catch (Exception e) {
                log.error("Error initializing ticket pool: {}", e.getMessage());
                throw new RuntimeException("Failed to initialize ticket pool", e);
            }

            List<VendorThread> vendorThreads = new ArrayList<>();
            List<CustomerThread> customerThreads = new ArrayList<>();
            List<Thread> runningThreads = new ArrayList<>();

            // Start vendor threads
            for (int i = 0; i < config.getVendorCount(); i++) {
                try {
                    VendorThread vendorThread = new VendorThread(
                            ticketPoolService,
                            eventId,
                            config.getTicketReleaseRate(),
                            60000L / config.getTicketReleaseRate()
                    );
                    vendorThreads.add(vendorThread);
                    Thread thread = new Thread(vendorThread, "Vendor-" + eventId + "-" + i);
                    runningThreads.add(thread);
                    thread.start();
                } catch (Exception e) {
                    log.error("Error starting vendor thread {}: {}", i, e.getMessage());
                }
            }

            // Start customer threads
            for (int i = 0; i < config.getCustomerCount(); i++) {
                try {
                    CustomerThread customerThread = new CustomerThread(
                            ticketPoolService,
                            eventId,
                            "Customer-" + i,
                            60000L / config.getCustomerRetrievalRate()
                    );
                    customerThreads.add(customerThread);
                    Thread thread = new Thread(customerThread, "Customer-" + eventId + "-" + i);
                    runningThreads.add(thread);
                    thread.start();
                } catch (Exception e) {
                    log.error("Error starting customer thread {}: {}", i, e.getMessage());
                }
            }

            // Store simulation context
            SimulationContext context = new SimulationContext(
                    config, vendorThreads, customerThreads, runningThreads, false
            );
            activeSimulations.put(eventId, context);

            log.info("Successfully started simulation for event {} with {} vendors and {} customers",
                    eventId, vendorThreads.size(), customerThreads.size());

            return new SimulationStatus(
                    true,
                    vendorThreads.size(),
                    customerThreads.size(),
                    runningThreads.size()
            );
        } catch (Exception e) {
            log.error("Error in startSimulation for event {}: {}", eventId, e.getMessage(), e);
            // Cleanup any partially started resources
            cleanupFailedStart(eventId);
            return new SimulationStatus(false, 0, 0, 0);
        }
    }

    @Override
    public void stopSimulation(String eventId) {
        SimulationContext context = activeSimulations.get(eventId);
        if (context == null) {
            throw new IllegalStateException("No simulation running for event: " + eventId);
        }

        // Stop all threads
        context.getVendorThreads().forEach(VendorThread::stop);
        context.getCustomerThreads().forEach(CustomerThread::stop);
        context.getRunningThreads().forEach(Thread::interrupt);

        // Cleanup resources
        ticketPoolService.shutdown(eventId);

        // Update configuration
        Configuration config = context.getConfig();
        config.setRunning(false);
        config.setPaused(false);
        configRepository.save(config);

        // Remove from active simulations
        activeSimulations.remove(eventId);

        log.info("Stopped simulation for event: {}", eventId);
    }

    @Override
    public void pauseSimulation(String eventId) {
        SimulationContext context = activeSimulations.get(eventId);
        if (context == null) {
            throw new IllegalStateException("No simulation running for event: " + eventId);
        }

        if (context.isPaused()) {
            log.warn("Simulation for event {} is already paused", eventId);
            return;
        }

        // Pause all threads
        context.setPaused(true);
        context.getConfig().setPaused(true);
        configRepository.save(context.getConfig());

        context.getVendorThreads().forEach(VendorThread::pause);
        context.getCustomerThreads().forEach(CustomerThread::pause);

        log.info("Paused simulation for event: {}", eventId);
    }

    @Override
    public void resumeSimulation(String eventId) {
        SimulationContext context = activeSimulations.get(eventId);
        if (context == null) {
            throw new IllegalStateException("No simulation running for event: " + eventId);
        }

        if (!context.isPaused()) {
            log.warn("Simulation for event {} is not paused", eventId);
            return;
        }

        // Resume all threads
        context.setPaused(false);
        context.getConfig().setPaused(false);
        configRepository.save(context.getConfig());

        context.getVendorThreads().forEach(VendorThread::resume);
        context.getCustomerThreads().forEach(CustomerThread::resume);

        log.info("Resumed simulation for event: {}", eventId);
    }

    @Override
    public Configuration getConfiguration(String eventId) {
        SimulationContext context = activeSimulations.get(eventId);
        if (context == null) {
            // Changed from findByEventId().orElse(null) to handle List return type
            List<Configuration> configs = configRepository.findByEventId(eventId);
            return configs.isEmpty() ? null : configs.get(0);
        }
        return context.getConfig();
    }


    @Override
    public List<Configuration> getAllSimulations() {
        return activeSimulations.values().stream()
                .map(SimulationContext::getConfig)
                .collect(Collectors.toList());
    }


    @Override
    public void cleanupSimulations() {
        List<String> activeEventIds = new ArrayList<>(activeSimulations.keySet());
        for (String eventId : activeEventIds) {
            try {
                stopSimulation(eventId);
            } catch (Exception e) {
                log.error("Error cleaning up simulation for event: {}", eventId, e);
            }
        }
        activeSimulations.clear();
        log.info("Cleaned up all simulations");
    }

    @Override
    public SimulationStatus getSimulationStatus(String eventId) {
        SimulationContext context = activeSimulations.get(eventId);

        // Changed to handle List return type
        Configuration config = context != null ?
                context.getConfig() :
                configRepository.findByEventId(eventId).stream()
                        .findFirst()
                        .orElse(null);

        if (config == null) {
            return new SimulationStatus(false, 0, 0, 0);
        }

        return new SimulationStatus(
                config.isRunning(),
                config.getVendorCount(),
                config.getCustomerCount(),
                context != null ? context.getRunningThreads().size() : 0
        );
    }

    private void cleanupFailedStart(String eventId) {
        try {
            SimulationContext context = activeSimulations.remove(eventId);
            if (context != null) {
                // Stop all threads
                context.getVendorThreads().forEach(VendorThread::stop);
                context.getCustomerThreads().forEach(CustomerThread::stop);
                context.getRunningThreads().forEach(Thread::interrupt);
            }

            // Cleanup ticket pool
            ticketPoolService.shutdown(eventId);

            // Updated to handle List return type
            List<Configuration> configs = configRepository.findByEventId(eventId);
            if (!configs.isEmpty()) {
                Configuration config = configs.get(0);
                config.setRunning(false);
                config.setPaused(false);
                configRepository.save(config);
            }
        } catch (Exception e) {
            log.error("Error during cleanup for event {}: {}", eventId, e.getMessage());
        }
    }
}
