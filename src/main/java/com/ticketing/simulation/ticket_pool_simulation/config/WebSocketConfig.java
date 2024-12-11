package com.ticketing.simulation.ticket_pool_simulation.config;

import com.ticketing.simulation.ticket_pool_simulation.service.impl.WebSocketLogger;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.TextMessage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(new CustomWebSocketHandler(), "/ws")
                .setAllowedOrigins("*"); // Configure allowed origins as needed
    }
}

@Component
class CustomWebSocketHandler extends TextWebSocketHandler {
    public CustomWebSocketHandler() {
        WebSocketLogger.getInstance().subscribe(this::broadcast);
    }

    private final List<WebSocketSession> clients = new ArrayList<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        clients.add(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        clients.remove(session);
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // Handle incoming messages
        String payload = message.getPayload();

        // Echo the message back (example response)
        session.sendMessage(new TextMessage("Server received: " + payload));
    }

    private void broadcast(String msg) {
        for (WebSocketSession session : clients) {
            try {
                session.sendMessage(new TextMessage(msg));
            } catch (IOException ex) {
                System.out.println("Could not send message to client!");
            }
        }
    }
}