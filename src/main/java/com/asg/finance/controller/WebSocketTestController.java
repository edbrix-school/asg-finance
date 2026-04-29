package com.asg.finance.controller;

import com.asg.finance.service.WebSocketMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
public class WebSocketTestController {

    private final WebSocketMessageService webSocketMessageService;

    @GetMapping("/websocket-info")
    public String getWebSocketInfo() {
        return "WebSocket endpoint should be available at: /ws";
    }

    @PostMapping("/websocket/{transactionPoid}")
    public String testWebSocket(@PathVariable Long transactionPoid, @RequestParam String message) {
        webSocketMessageService.sendInfoMessage(transactionPoid, message);
        return "Message sent to WebSocket: " + message;
    }

    @PostMapping("/websocket/{transactionPoid}/complete")
    public String testWebSocketComplete(@PathVariable Long transactionPoid, @RequestParam String message) {
        webSocketMessageService.sendCompletionMessage(transactionPoid, message, true);
        return "Completion message sent to WebSocket: " + message;
    }
}