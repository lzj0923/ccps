package com.ccps.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ccps.backend.config.AuthInterceptor;
import com.ccps.backend.dto.AdminReminderResponse;
import com.ccps.backend.dto.AdminReminderRuleRequest;
import com.ccps.backend.dto.AdminReminderRunResponse;
import com.ccps.backend.dto.AdminRentCollectionHoldRequest;
import com.ccps.backend.dto.AdminRentCollectionWorkflowResponse;
import com.ccps.backend.service.AdminRentCollectionWorkflowService;
import com.ccps.backend.service.AdminReminderService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/reminders")
public class AdminReminderController {
    private final AdminReminderService service;
    private final AdminRentCollectionWorkflowService rentCollectionService;

    public AdminReminderController(AdminReminderService service,
                                   AdminRentCollectionWorkflowService rentCollectionService) {
        this.service = service;
        this.rentCollectionService = rentCollectionService;
    }

    @GetMapping
    public AdminReminderResponse overview() {
        return service.overview();
    }

    @PostMapping("/rules")
    public AdminReminderResponse.Rule createRule(@Valid @RequestBody AdminReminderRuleRequest body,
                                                  HttpServletRequest request) {
        return service.createRule(AuthInterceptor.userId(request), body);
    }

    @PutMapping("/rules/{ruleId}")
    public AdminReminderResponse.Rule updateRule(@PathVariable Long ruleId,
                                                  @Valid @RequestBody AdminReminderRuleRequest body,
                                                  HttpServletRequest request) {
        return service.updateRule(AuthInterceptor.userId(request), ruleId, body);
    }

    @PutMapping("/rules/{ruleId}/enabled")
    public ResponseEntity<Void> setEnabled(@PathVariable Long ruleId, @RequestParam boolean enabled,
                                            HttpServletRequest request) {
        service.setEnabled(AuthInterceptor.userId(request), ruleId, enabled);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/rules/{ruleId}")
    public ResponseEntity<Void> deleteRule(@PathVariable Long ruleId, HttpServletRequest request) {
        service.deleteRule(AuthInterceptor.userId(request), ruleId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/rules/{ruleId}/run")
    public AdminReminderRunResponse runRule(@PathVariable Long ruleId, HttpServletRequest request) {
        return new AdminReminderRunResponse(service.runRule(AuthInterceptor.userId(request), ruleId));
    }

    @PostMapping("/run")
    public AdminReminderRunResponse runAll(HttpServletRequest request) {
        return new AdminReminderRunResponse(service.runEnabledRules(AuthInterceptor.userId(request)));
    }

    @PostMapping("/deliveries/{deliveryId}/retry")
    public ResponseEntity<Void> retryDelivery(@PathVariable Long deliveryId) {
        service.retryDelivery(deliveryId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/rent-collection")
    public AdminRentCollectionWorkflowResponse rentCollectionOverview() {
        return rentCollectionService.overview();
    }

    @PostMapping("/rent-collection/{invoiceId}/stages/{stage}/send")
    public AdminRentCollectionWorkflowResponse.Item sendRentCollectionStage(
            @PathVariable Long invoiceId, @PathVariable String stage, HttpServletRequest request) {
        return rentCollectionService.sendStage(AuthInterceptor.userId(request), invoiceId, stage);
    }

    @PutMapping("/rent-collection/{invoiceId}/hold")
    public AdminRentCollectionWorkflowResponse.Item holdRentCollection(
            @PathVariable Long invoiceId, @Valid @RequestBody AdminRentCollectionHoldRequest body,
            HttpServletRequest request) {
        return rentCollectionService.hold(AuthInterceptor.userId(request), invoiceId, body.reason());
    }

    @DeleteMapping("/rent-collection/{invoiceId}/hold")
    public AdminRentCollectionWorkflowResponse.Item resumeRentCollection(
            @PathVariable Long invoiceId, HttpServletRequest request) {
        return rentCollectionService.resume(AuthInterceptor.userId(request), invoiceId);
    }
}
