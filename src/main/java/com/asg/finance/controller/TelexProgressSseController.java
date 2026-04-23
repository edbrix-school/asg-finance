package com.asg.finance.controller;

import com.asg.finance.service.WebSocketMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@RestController
@RequestMapping("/v1/telex-progress")
@RequiredArgsConstructor
@Slf4j
public class TelexProgressSseController {

    // Store SSE emitters for each transaction
    private final ConcurrentHashMap<Long, CopyOnWriteArrayList<SseEmitter>> transactionEmitters = new ConcurrentHashMap<>();

    @GetMapping(value = "/{transactionPoid}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamProgress(@PathVariable Long transactionPoid) {
        SseEmitter emitter = new SseEmitter(300000L); // 5 minutes timeout
        
        // Add emitter to the list for this transaction
        transactionEmitters.computeIfAbsent(transactionPoid, k -> new CopyOnWriteArrayList<>()).add(emitter);
        
        // Handle completion and errors
        emitter.onCompletion(() -> removeEmitter(transactionPoid, emitter));
        emitter.onTimeout(() -> removeEmitter(transactionPoid, emitter));
        emitter.onError((ex) -> removeEmitter(transactionPoid, emitter));
        
        // Send initial connection message
        try {
            emitter.send(SseEmitter.event()
                .name("connected")
                .data("Connected to progress stream for transaction: " + transactionPoid));
        } catch (IOException e) {
            log.error("Error sending initial SSE message", e);
            removeEmitter(transactionPoid, emitter);
        }
        
        return emitter;
    }

    @PostMapping("/{transactionPoid}/send-message")
    public String sendMessage(@PathVariable Long transactionPoid, 
                             @RequestParam String message, 
                             @RequestParam(defaultValue = "INFO") String status) {
        sendProgressMessage(transactionPoid, message, status);
        return "Message sent to " + getEmitterCount(transactionPoid) + " clients";
    }

    public void sendProgressMessage(Long transactionPoid, String message, String status) {
        CopyOnWriteArrayList<SseEmitter> emitters = transactionEmitters.get(transactionPoid);
        if (emitters != null && !emitters.isEmpty()) {
            String timestamp = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
            
            String eventData = String.format(
                "{\"transactionPoid\":%d,\"message\":\"%s\",\"status\":\"%s\",\"timestamp\":\"%s\"}", 
                transactionPoid, message.replace("\"", "\\\""), status, timestamp
            );
            
            emitters.removeIf(emitter -> {
                try {
                    emitter.send(SseEmitter.event()
                        .name("progress")
                        .data(eventData));
                    return false; // Keep emitter
                } catch (IOException e) {
                    log.debug("SSE emitter failed, removing: {}", e.getMessage());
                    return true; // Remove emitter
                }
            });
        }
    }

    private void removeEmitter(Long transactionPoid, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> emitters = transactionEmitters.get(transactionPoid);
        if (emitters != null) {
            emitters.remove(emitter);
            if (emitters.isEmpty()) {
                transactionEmitters.remove(transactionPoid);
            }
        }
    }

    private int getEmitterCount(Long transactionPoid) {
        CopyOnWriteArrayList<SseEmitter> emitters = transactionEmitters.get(transactionPoid);
        return emitters != null ? emitters.size() : 0;
    }
}