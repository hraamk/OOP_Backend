package com.ticketing.simulation.ticket_pool_simulation.service.impl;

import com.ticketing.simulation.ticket_pool_simulation.model.entity.Configuration;
import com.ticketing.simulation.ticket_pool_simulation.repository.ConfigurationRepository;
import com.ticketing.simulation.ticket_pool_simulation.service.interfaces.SimulationService;
import com.ticketing.simulation.ticket_pool_simulation.service.interfaces.TicketPoolService;
import com.ticketing.simulation.ticket_pool_simulation.service.thread.CustomerThread;
import com.ticketing.simulation.ticket_pool_simulation.service.thread.VendorThread;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.ArrayList;

@Service
@Slf4j
@RequiredArgsConstructor
public class SimulationServiceImpl implements SimulationService {
    private final TicketPoolService ticketPoolService;
    private final ConfigurationRepository configRepository;
    private final List<VendorThread> vendorThreads = new ArrayList<>();
    private final List<CustomerThread> customerThreads = new ArrayList<>();
    private final List<Thread> runningThreads = new ArrayList<>();
    private Configuration currentConfig;

    @Override
    public void startSimulation(Configuration config) {
        ticketPoolService.initialize(config);
        this.currentConfig = config;
        configRepository.save(config);

        // Start vendor threads
        for (int i = 0; i < config.getVendorCount(); i++) {
            VendorThread vendorThread = new VendorThread(
                    ticketPoolService,
                    config.getTicketReleaseRate(),
                    60000L / config.getTicketReleaseRate()
            );
            vendorThreads.add(vendorThread);
            Thread thread = new Thread(vendorThread, "Vendor-" + i);
            runningThreads.add(thread);
            thread.start();
        }

        // Start customer threads
        for (int i = 0; i < config.getCustomerCount(); i++) {
            CustomerThread customerThread = new CustomerThread(
                    ticketPoolService,
                    "Customer-" + i,
                    60000L / config.getCustomerRetrievalRate()
            );
            customerThreads.add(customerThread);
            Thread thread = new Thread(customerThread, "Customer-" + i);
            runningThreads.add(thread);
            thread.start();
        }

        log.info("Started simulation with {} vendors and {} customers",
                config.getVendorCount(), config.getCustomerCount());
    }

    @Override
    public void stopSimulation() {
        vendorThreads.forEach(VendorThread::stop);
        customerThreads.forEach(CustomerThread::stop);
        runningThreads.forEach(Thread::interrupt);
        ticketPoolService.shutdown();

        vendorThreads.clear();
        customerThreads.clear();
        runningThreads.clear();

        if (currentConfig != null) {
            currentConfig.setRunning(false);
            configRepository.save(currentConfig);
        }

        log.info("Stopped simulation");
    }

    @Override
    public Configuration getCurrentConfig() {
        return currentConfig;
    }


    @Override
    public void pauseSimulation() {

    }

    @Override
    public void resumeSimulation() {

    }
}
