package com.ticketing.simulation.ticket_pool_simulation.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class WebSocketLogger {
    private static WebSocketLogger instance;
    private final List<Consumer<String>> subscribers = new ArrayList<>();
    private WebSocketLogger(){}

    public static WebSocketLogger getInstance() {
        if (instance == null) instance = new WebSocketLogger();
        return instance;
    }

    public void log(String message) {
        for (Consumer<String> subscriber : subscribers) {
            subscriber.accept(message);
        }
    }

    public void subscribe(Consumer<String> subscriber) {
        subscribers.add(subscriber);
    }
}
