package com.asg.finance.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

@Component
@Slf4j
public class WebSocketEventListener {

    @EventListener
    public void handleSessionConnect(SessionConnectEvent event) {
        StompHeaderAccessor sha = StompHeaderAccessor.wrap(event.getMessage());
        log.info("WebSocket STOMP CONNECT received. sessionId={}", sha.getSessionId());
    }

    @EventListener
    public void handleSessionConnected(SessionConnectedEvent event) {
        StompHeaderAccessor sha = StompHeaderAccessor.wrap(event.getMessage());
        log.info("WebSocket STOMP CONNECTED. sessionId={}", sha.getSessionId());
    }

    @EventListener
    public void handleSessionSubscribe(SessionSubscribeEvent event) {
        StompHeaderAccessor sha = StompHeaderAccessor.wrap(event.getMessage());
        log.info("WebSocket STOMP SUBSCRIBE. sessionId={}, destination={}",
                sha.getSessionId(), sha.getDestination());
    }

    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        log.info("WebSocket STOMP DISCONNECT. sessionId={}, closeStatus={}",
                event.getSessionId(), event.getCloseStatus());
    }
}
