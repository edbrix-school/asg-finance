package com.asg.finance.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@Slf4j
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        log.info("Configuring message broker...");
        // Enable a simple memory-based message broker to carry messages back to the client
        config.enableSimpleBroker("/topic", "/queue");
        // Designate the "/app" prefix for messages that are bound for @MessageMapping methods
        config.setApplicationDestinationPrefixes("/app");
        log.info("Message broker configured successfully");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        log.info("Registering STOMP endpoints...");
        
        // Register the "/ws" endpoint for WebSocket connections with SockJS fallback
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS()
                .setHeartbeatTime(25000);
        
        // Also register without SockJS for direct WebSocket connections
        registry.addEndpoint("/websocket")
                .setAllowedOriginPatterns("*");
                
        log.info("STOMP endpoints registered successfully: /ws (with SockJS), /websocket (direct)");
    }
}
