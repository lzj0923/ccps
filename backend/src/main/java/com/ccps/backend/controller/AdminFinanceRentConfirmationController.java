package com.ccps.backend.controller;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.ccps.backend.config.AuthInterceptor;
import com.ccps.backend.dto.AdminRecordCreateResponse;
import com.ccps.backend.dto.AdminRentCollectionBatchRequest;
import com.ccps.backend.dto.AdminRentCollectionRequest;
import com.ccps.backend.service.AdminTenancyService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

/** The only HTTP entry point allowed to confirm rent collections. */
@RestController
@RequestMapping("/api/admin/finance/rent")
public class AdminFinanceRentConfirmationController {
    private final AdminTenancyService service;

    public AdminFinanceRentConfirmationController(AdminTenancyService service) {
        this.service = service;
    }

    @PostMapping(value = "/invoices/{invoiceId}/confirm", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AdminRecordCreateResponse confirm(@PathVariable Long invoiceId,
            @Valid @RequestPart("payload") AdminRentCollectionRequest payload,
            @RequestPart(value = "proof", required = false) MultipartFile proof,
            HttpServletRequest request) {
        return service.confirmRentCollection(AuthInterceptor.userId(request), invoiceId, payload, proof);
    }

    @PostMapping("/invoices/batch-confirm")
    public List<AdminRecordCreateResponse> confirmBatch(
            @Valid @RequestBody AdminRentCollectionBatchRequest payload,
            HttpServletRequest request) {
        return service.confirmRentCollections(AuthInterceptor.userId(request), payload);
    }
}
