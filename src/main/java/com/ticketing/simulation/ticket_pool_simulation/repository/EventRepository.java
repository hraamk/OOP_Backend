package com.ticketing.simulation.ticket_pool_simulation.repository;

import com.ticketing.simulation.ticket_pool_simulation.model.entity.Event;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface EventRepository extends MongoRepository<Event, String> {}