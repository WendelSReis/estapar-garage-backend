package com.estapar.garage.api.controller;

import com.estapar.garage.api.dto.WebhookEventRequest;
import com.estapar.garage.api.dto.WebhookEventResponse;
import com.estapar.garage.domain.service.WebhookProcessingService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/webhook")
public class WebhookController {

    private final WebhookProcessingService webhookProcessingService;

    public WebhookController(WebhookProcessingService webhookProcessingService) {
        this.webhookProcessingService = webhookProcessingService;
    }

    @PostMapping
    public ResponseEntity<WebhookEventResponse> receive(@Valid @RequestBody WebhookEventRequest request) {
        String message = webhookProcessingService.process(request);
        return ResponseEntity.ok(new WebhookEventResponse(message, Instant.now()));
    }
}
