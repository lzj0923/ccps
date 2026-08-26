package com.ccps.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.ccps.backend.config.AuthInterceptor;
import com.ccps.backend.dto.OwnerRentIncomeResponse;
import com.ccps.backend.dto.RentReceiptConfirmationResponse;
import com.ccps.backend.service.OwnerRentIncomeService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/owner/rent-income")
public class OwnerRentIncomeController {
    private final OwnerRentIncomeService service;

    public OwnerRentIncomeController(OwnerRentIncomeService service) {
        this.service = service;
    }

    @GetMapping
    public OwnerRentIncomeResponse currentOwnerRentIncome(
            HttpServletRequest request,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) String status) {
        return service.getRentIncome(AuthInterceptor.userId(request), year, month, projectId, status);
    }

    @PostMapping("/{invoiceId}/confirm-receipt")
    public RentReceiptConfirmationResponse confirmReceipt(
            @PathVariable Long invoiceId,
            HttpServletRequest request) {
        throw new ResponseStatusException(HttpStatus.GONE,
                "租金收款只能由财务确认模块确认");
    }
}
