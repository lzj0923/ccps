package com.ccps.backend.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ccps.backend.service.WhatsAppWebhookService;

@RestController
@RequestMapping("/api/webhooks/whatsapp")
public class WhatsAppWebhookController {
    private final WhatsAppWebhookService webhookService;

    public WhatsAppWebhookController(WhatsAppWebhookService webhookService) {
        this.webhookService = webhookService;
    }

    @GetMapping(produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> verify(
            @RequestParam(name = "hub.mode", required = false) String mode,
            @RequestParam(name = "hub.verify_token", required = false) String verifyToken,
            @RequestParam(name = "hub.challenge", required = false) String challenge) {
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_PLAIN)
                .body(webhookService.verifySubscription(mode, verifyToken, challenge));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> receive(
            @RequestBody byte[] payload,
            @RequestHeader(name = "X-Hub-Signature-256", required = false) String signature) {
        webhookService.receive(payload, signature);
        return ResponseEntity.ok().build();
    }
}
