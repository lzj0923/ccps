package com.ccps.backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.ccps.backend.config.AuthInterceptor;
import com.ccps.backend.dto.OwnerNotificationResponse;
import com.ccps.backend.dto.EmailBindingRequest;
import com.ccps.backend.dto.EmailSubscriptionToggleRequest;
import com.ccps.backend.dto.EmailVerificationRequest;
import com.ccps.backend.dto.OwnerNotificationResponse.Channel;
import com.ccps.backend.service.EmailNotificationService;
import com.ccps.backend.service.OwnerNotificationService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/owner/notifications")
public class OwnerNotificationController {
    private final OwnerNotificationService service;
    private final EmailNotificationService emailService;

    public OwnerNotificationController(OwnerNotificationService service, EmailNotificationService emailService) {
        this.service = service;
        this.emailService = emailService;
    }

    @GetMapping
    public OwnerNotificationResponse overview(HttpServletRequest request) {
        return service.getNotifications(AuthInterceptor.userId(request));
    }

    @PostMapping("/{notificationId}/read")
    public ResponseEntity<Void> markRead(@PathVariable Long notificationId, HttpServletRequest request) {
        try {
            service.markRead(AuthInterceptor.userId(request), notificationId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        }
    }

    @PostMapping("/read-all")
    public ResponseEntity<Void> markAllRead(HttpServletRequest request) {
        service.markAllRead(AuthInterceptor.userId(request));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/email/request-code")
    public Channel requestEmailCode(@Valid @RequestBody EmailBindingRequest body, HttpServletRequest request) {
        return emailService.requestVerification(AuthInterceptor.userId(request), body);
    }

    @PostMapping("/email/verify")
    public Channel verifyEmail(@Valid @RequestBody EmailVerificationRequest body, HttpServletRequest request) {
        return emailService.verify(AuthInterceptor.userId(request), body);
    }

    @PutMapping("/email/subscription")
    public Channel toggleEmail(@Valid @RequestBody EmailSubscriptionToggleRequest body, HttpServletRequest request) {
        return emailService.toggle(AuthInterceptor.userId(request), body);
    }
}
