package com.ticketing.simulation.ticket_pool_simulation.service.impl;

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
    public void startSimulation(String eventId, Configuration config) {
        if (activeSimulations.containsKey(eventId)) {
            throw new IllegalStateException("Simulation for event " + eventId + " is already running");
        }

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + eventId));

        config.setEventId(eventId);
        config.setRunning(true);
        config.setPaused(false);
        configRepository.save(config);

        List<VendorThread> vendorThreads = new ArrayList<>();
        List<CustomerThread> customerThreads = new ArrayList<>();
        List<Thread> runningThreads = new ArrayList<>();

        ticketPoolService.initialize(eventId, config);

        // Start vendor threads
        for (int i = 0; i < config.getVendorCount(); i++) {
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
        }

        // Start customer threads
        for (int i = 0; i < config.getCustomerCount(); i++) {
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
        }

        activeSimulations.put(eventId, new SimulationContext(
                config, vendorThreads, customerThreads, runningThreads, false));

        log.info("Started simulation for event {} with {} vendors and {} customers",
                eventId, config.getVendorCount(), config.getCustomerCount());
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
        context.getConfig().setRunning(false);
        context.getConfig().setPaused(false);
        configRepository.save(context.getConfig());

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
    public Configuration getSimulationStatus(String eventId) {
        SimulationContext context = activeSimulations.get(eventId);
        if (context == null) {
            return configRepository.findByEventId(eventId).orElse(null);
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
}