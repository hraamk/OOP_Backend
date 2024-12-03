package com.ticketing.simulation.ticket_pool_simulation.repository;

import com.ticketing.simulation.ticket_pool_simulation.model.entity.Configuration;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ConfigurationRepository extends MongoRepository<Configuration, String> {}