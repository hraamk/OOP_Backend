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
import jakarta.annotation.PreDestroy;
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
import java.util.stream.IntStream;


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

        public synchronized SimulationStatus getStatus() {
            return new SimulationStatus(
                    config.isRunning(),
                    vendorThreads.size(),
                    customerThreads.size(),
                    runningThreads.size()
            );
        }

        public synchronized void addVendorThread(VendorThread vendorThread, Thread thread) {
            vendorThreads.add(vendorThread);
            runningThreads.add(thread);
        }

        public synchronized void addCustomerThread(CustomerThread customerThread, Thread thread) {
            customerThreads.add(customerThread);
            runningThreads.add(thread);
        }

        public synchronized void removeVendorThreads(List<VendorThread> vendors, List<Thread> threads) {
            vendorThreads.removeAll(vendors);
            runningThreads.removeAll(threads);
        }

        public synchronized void removeCustomerThreads(List<CustomerThread> customers, List<Thread> threads) {
            customerThreads.removeAll(customers);
            runningThreads.removeAll(threads);
        }
    }

    @Override
    public synchronized SimulationStatus startSimulation(String eventId, Configuration config) {
        if (activeSimulations.containsKey(eventId)) {
            log.warn("Simulation already exists for event: {}", eventId);
            return activeSimulations.get(eventId).getStatus();
        }

        try {
            SimulationContext context = initializeSimulation(eventId, config);
            activeSimulations.put(eventId, context);
            log.info("Successfully started simulation for event {} with {} vendors and {} customers",
                    eventId, context.getVendorThreads().size(), context.getCustomerThreads().size());
            return context.getStatus();
        } catch (Exception e) {
            log.error("Error initializing simulation: {}", e.getMessage());
            cleanupFailedStart(eventId);
            throw new RuntimeException("Failed to start simulation", e);
        }
    }

    private SimulationContext initializeSimulation(String eventId, Configuration config) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + eventId));

        // Initialize configuration
        config.setEventId(eventId);
        config.setRunning(true);
        config.setPaused(false);
        config = configRepository.save(config);

        // Initialize ticket pool
        ticketPoolService.initialize(eventId, config);

        // Start threads
        List<VendorThread> vendorThreads = startVendorThreads(eventId, config);
        List<CustomerThread> customerThreads = startCustomerThreads(eventId, config);
        List<Thread> allThreads = new ArrayList<>();

        // Start vendor threads
        allThreads.addAll(vendorThreads.stream()
                .map(v -> new Thread(v, "Vendor-" + eventId + "-" + vendorThreads.indexOf(v)))
                .peek(Thread::start)
                .toList());

        // Start customer threads
        allThreads.addAll(customerThreads.stream()
                .map(c -> new Thread(c, "Customer-" + eventId + "-" + customerThreads.indexOf(c)))
                .peek(Thread::start)
                .toList());

        return new SimulationContext(config, vendorThreads, customerThreads, allThreads, false);
    }

    private List<VendorThread> startVendorThreads(String eventId, Configuration config) {
        return IntStream.range(0, config.getVendorCount())
                .mapToObj(i -> new VendorThread(
                        ticketPoolService,
                        eventId,
                        config.getTicketReleaseRate(),
                        60000L / config.getTicketReleaseRate()
                ))
                .collect(Collectors.toList());
    }

    private List<CustomerThread> startCustomerThreads(String eventId, Configuration config) {
        return IntStream.range(0, config.getCustomerCount())
                .mapToObj(i -> new CustomerThread(
                        ticketPoolService,
                        eventId,
                        "Customer-" + i,
                        60000L / config.getCustomerRetrievalRate()
                ))
                .collect(Collectors.toList());
    }

    @Override
    public synchronized void stopSimulation(String eventId) {
        SimulationContext context = activeSimulations.get(eventId);
        if (context == null) {
            throw new IllegalStateException("No simulation running for event: " + eventId);
        }

        // Stop all threads
        context.getVendorThreads().forEach(VendorThread::stop);
        context.getCustomerThreads().forEach(CustomerThread::stop);
        context.getRunningThreads().forEach(Thread::interrupt);

        // Cleanup resources
        ticketPoolService.shutdownEvent(eventId);  // Changed from shutdown(eventId)

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
    public synchronized void pauseSimulation(String eventId) {
        SimulationContext context = activeSimulations.get(eventId);
        if (context == null) {
            throw new IllegalStateException("No simulation running for event: " + eventId);
        }

        if (context.isPaused()) {
            log.warn("Simulation for event {} is already paused", eventId);
            return;
        }

        context.setPaused(true);
        context.getConfig().setPaused(true);
        configRepository.save(context.getConfig());

        context.getVendorThreads().forEach(VendorThread::pause);
        context.getCustomerThreads().forEach(CustomerThread::pause);

        log.info("Paused simulation for event: {}", eventId);
    }

    @Override
    public synchronized void resumeSimulation(String eventId) {
        SimulationContext context = activeSimulations.get(eventId);
        if (context == null) {
            throw new IllegalStateException("No simulation running for event: " + eventId);
        }

        if (!context.isPaused()) {
            log.warn("Simulation for event {} is not paused", eventId);
            return;
        }

        context.setPaused(false);
        context.getConfig().setPaused(false);
        configRepository.save(context.getConfig());

        context.getVendorThreads().forEach(VendorThread::resume);
        context.getCustomerThreads().forEach(CustomerThread::resume);

        log.info("Resumed simulation for event: {}", eventId);
    }

    @Override
    public synchronized SimulationStatus increaseVendorCount(String eventId, int count) {
        SimulationContext context = activeSimulations.get(eventId);
        if (context == null) {
            throw new IllegalStateException("No simulation running for event: " + eventId);
        }

        Configuration config = context.getConfig();
        int currentCount = config.getVendorCount();

        for (int i = 0; i < count; i++) {
            try {
                VendorThread vendorThread = new VendorThread(
                        ticketPoolService,
                        eventId,
                        config.getTicketReleaseRate(),
                        60000L / config.getTicketReleaseRate()
                );
                Thread thread = new Thread(vendorThread,
                        String.format("Vendor-%s-%d", eventId, currentCount + i));

                if (context.isPaused()) {
                    vendorThread.pause();
                }

                thread.start();
                context.addVendorThread(vendorThread, thread);
            } catch (Exception e) {
                log.error("Error creating new vendor thread: {}", e.getMessage());
            }
        }

        config.setVendorCount(currentCount + count);
        configRepository.save(config);

        return context.getStatus();
    }

    @Override
    public synchronized SimulationStatus decreaseVendorCount(String eventId, int count) {
        SimulationContext context = activeSimulations.get(eventId);
        if (context == null) {
            throw new IllegalStateException("No simulation running for event: " + eventId);
        }

        Configuration config = context.getConfig();
        if (config.getVendorCount() <= count) {
            count = config.getVendorCount() - 1; // Keep at least one vendor
        }

        if (count <= 0) {
            return context.getStatus();
        }

        List<VendorThread> vendorsToRemove = context.getVendorThreads()
                .subList(context.getVendorThreads().size() - count, context.getVendorThreads().size());
        List<Thread> threadsToRemove = new ArrayList<>();

        vendorsToRemove.forEach(vendor -> {
            vendor.stop();
            context.getRunningThreads().stream()
                    .filter(thread -> thread.getName().contains("Vendor") &&
                            thread.getName().endsWith(vendor.toString()))
                    .findFirst()
                    .ifPresent(threadsToRemove::add);
        });

        context.removeVendorThreads(vendorsToRemove, threadsToRemove);
        threadsToRemove.forEach(Thread::interrupt);

        config.setVendorCount(config.getVendorCount() - count);
        configRepository.save(config);

        return context.getStatus();
    }

    @Override
    public synchronized SimulationStatus increaseCustomerCount(String eventId, int count) {
        SimulationContext context = activeSimulations.get(eventId);
        if (context == null) {
            throw new IllegalStateException("No simulation running for event: " + eventId);
        }

        Configuration config = context.getConfig();
        int currentCount = config.getCustomerCount();

        for (int i = 0; i < count; i++) {
            try {
                CustomerThread customerThread = new CustomerThread(
                        ticketPoolService,
                        eventId,
                        String.format("Customer-%d", currentCount + i),
                        60000L / config.getCustomerRetrievalRate()
                );
                Thread thread = new Thread(customerThread,
                        String.format("Customer-%s-%d", eventId, currentCount + i));

                if (context.isPaused()) {
                    customerThread.pause();
                }

                thread.start();
                context.addCustomerThread(customerThread, thread);
            } catch (Exception e) {
                log.error("Error creating new customer thread: {}", e.getMessage());
            }
        }

        config.setCustomerCount(currentCount + count);
        configRepository.save(config);

        return context.getStatus();
    }

    @Override
    public synchronized SimulationStatus decreaseCustomerCount(String eventId, int count) {
        SimulationContext context = activeSimulations.get(eventId);
        if (context == null) {
            throw new IllegalStateException("No simulation running for event: " + eventId);
        }

        Configuration config = context.getConfig();
        if (config.getCustomerCount() <= count) {
            count = config.getCustomerCount() - 1; // Keep at least one customer
        }

        if (count <= 0) {
            return context.getStatus();
        }

        List<CustomerThread> customersToRemove = context.getCustomerThreads()
                .subList(context.getCustomerThreads().size() - count, context.getCustomerThreads().size());
        List<Thread> threadsToRemove = new ArrayList<>();

        customersToRemove.forEach(customer -> {
            customer.stop();
            context.getRunningThreads().stream()
                    .filter(thread -> thread.getName().contains("Customer") &&
                            thread.getName().endsWith(customer.toString()))
                    .findFirst()
                    .ifPresent(threadsToRemove::add);
        });

        context.removeCustomerThreads(customersToRemove, threadsToRemove);
        threadsToRemove.forEach(Thread::interrupt);

        config.setCustomerCount(config.getCustomerCount() - count);
        configRepository.save(config);

        return context.getStatus();
    }

    @Override
    public Configuration getConfiguration(String eventId) {
        SimulationContext context = activeSimulations.get(eventId);
        if (context == null) {
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
    public SimulationStatus getSimulationStatus(String eventId) {
        SimulationContext context = activeSimulations.get(eventId);
        if (context != null) {
            return context.getStatus();
        }

        Configuration config = configRepository.findByEventId(eventId).stream()
                .findFirst()
                .orElse(null);

        if (config == null) {
            return new SimulationStatus(false, 0, 0, 0);
        }

        return new SimulationStatus(
                config.isRunning(),
                config.getVendorCount(),
                config.getCustomerCount(),
                0
        );
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

    private void cleanupFailedStart(String eventId) {
        try {
            SimulationContext context = activeSimulations.remove(eventId);
            if (context != null) {
                context.getVendorThreads().forEach(VendorThread::stop);
                context.getCustomerThreads().forEach(CustomerThread::stop);
                context.getRunningThreads().forEach(Thread::interrupt);
            }

            ticketPoolService.shutdownEvent(eventId);  // Changed from shutdown(eventId)

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
    @PreDestroy
    public void cleanup() {
        cleanupSimulations();
        ticketPoolService.shutdown();  // Add global shutdown call
    }

}
