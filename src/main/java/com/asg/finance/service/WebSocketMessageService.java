package com.asg.finance.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebSocketMessageService {

    private final SimpMessagingTemplate messagingTemplate;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    // Reference to SSE controller for dual messaging
    private com.asg.finance.controller.TelexProgressSseController sseController;

    public void setSseController(com.asg.finance.controller.TelexProgressSseController sseController) {
        this.sseController = sseController;
    }

    /**
     * Send progress message to specific transaction
     */
    public void sendProgressMessage(Long transactionPoid, String message, String status) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("transactionPoid", transactionPoid);
        payload.put("message", message);
        payload.put("status", status); // SUCCESS, ERROR, INFO, WARNING
        payload.put("timestamp", LocalDateTime.now().format(TIME_FORMATTER));
        
        // Send via WebSocket
        try {
            String destination = "/topic/telex-progress/" + transactionPoid;
            messagingTemplate.convertAndSend(destination, payload);
            log.debug("Sent WebSocket message to {}: {}", destination, message);
        } catch (Exception e) {
            log.warn("Failed to send WebSocket message: {}", e.getMessage());
        }
        
        // Also send via SSE if available
        if (sseController != null) {
            try {
                sseController.sendProgressMessage(transactionPoid, message, status);
            } catch (Exception e) {
                log.warn("Failed to send SSE message: {}", e.getMessage());
            }
        }
    }

    /**
     * Send info message
     */
    public void sendInfoMessage(Long transactionPoid, String message) {
        sendProgressMessage(transactionPoid, message, "INFO");
    }

    /**
     * Send success message
     */
    public void sendSuccessMessage(Long transactionPoid, String message) {
        sendProgressMessage(transactionPoid, message, "SUCCESS");
    }

    /**
     * Send error message
     */
    public void sendErrorMessage(Long transactionPoid, String message) {
        sendProgressMessage(transactionPoid, message, "ERROR");
    }

    /**
     * Send warning message
     */
    public void sendWarningMessage(Long transactionPoid, String message) {
        sendProgressMessage(transactionPoid, message, "WARNING");
    }

    /**
     * Send completion message with final result
     */
    public void sendCompletionMessage(Long transactionPoid, String finalResult, boolean isSuccess) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("transactionPoid", transactionPoid);
        payload.put("message", finalResult);
        payload.put("status", isSuccess ? "COMPLETED_SUCCESS" : "COMPLETED_ERROR");
        payload.put("timestamp", LocalDateTime.now().format(TIME_FORMATTER));
        payload.put("completed", true);
        
        // Send via WebSocket
        try {
            String destination = "/topic/telex-progress/" + transactionPoid;
            messagingTemplate.convertAndSend(destination, payload);
            log.info("Sent completion message to {}: {}", destination, finalResult);
        } catch (Exception e) {
            log.warn("Failed to send WebSocket completion message: {}", e.getMessage());
        }
        
        // Also send via SSE if available
        if (sseController != null) {
            try {
                sseController.sendProgressMessage(transactionPoid, finalResult, 
                    isSuccess ? "COMPLETED_SUCCESS" : "COMPLETED_ERROR");
            } catch (Exception e) {
                log.warn("Failed to send SSE completion message: {}", e.getMessage());
            }
        }
    }
}