package com.ticketing.simulation.ticket_pool_simulation.repository;

import com.ticketing.simulation.ticket_pool_simulation.model.entity.Configuration;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ConfigurationRepository extends MongoRepository<Configuration, String> {
    List<Configuration> findByEventId(String eventId);  // Changed to return List

    @Query("{ 'eventId': ?0, 'running': true }")
    Optional<Configuration> findRunningConfigurationByEventId(String eventId);

    boolean existsByEventIdAndRunning(String eventId, boolean running);
}