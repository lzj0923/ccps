package com.ccps.backend.controller;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.List;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ccps.backend.config.AuthInterceptor;
import com.ccps.backend.dto.ContractTemplateGenerateRequest;
import com.ccps.backend.service.AdminPropertyPhotoService;
import com.ccps.backend.service.AdminPropertyHandoverChecklistService;
import com.ccps.backend.service.ContractTemplatePdfService;
import com.ccps.backend.service.TenancyAgreementPdfService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/contract-templates")
public class AdminContractTemplateController {
    private final ContractTemplatePdfService service;
    private final TenancyAgreementPdfService tenancyAgreementPdfService;
    private final AdminPropertyPhotoService propertyPhotoService;
    private final AdminPropertyHandoverChecklistService handoverChecklistService;

    public AdminContractTemplateController(ContractTemplatePdfService service,
            TenancyAgreementPdfService tenancyAgreementPdfService,
            AdminPropertyPhotoService propertyPhotoService,
            AdminPropertyHandoverChecklistService handoverChecklistService) {
        this.service = service;
        this.tenancyAgreementPdfService = tenancyAgreementPdfService;
        this.propertyPhotoService = propertyPhotoService;
        this.handoverChecklistService = handoverChecklistService;
    }

    @PostMapping(value = "/{templateType}/generate", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<byte[]> generate(@PathVariable("templateType") String templateType,
            @Valid @RequestBody ContractTemplateGenerateRequest request, HttpServletRequest servletRequest) {
        if ("tenancy-agreement".equalsIgnoreCase(templateType.trim())) {
            List<TenancyAgreementPdfService.PropertyPhotoAsset> photos = List.of();
            List<TenancyAgreementPdfService.InventoryItem> inventory = List.of();
            String leaseId = request.fields().get("leaseId");
            if (leaseId != null && !leaseId.isBlank()) {
                try {
                    Long id = Long.valueOf(leaseId);
                    photos = propertyPhotoService.regularAssetsForLease(id).stream()
                            .map(photo -> new TenancyAgreementPdfService.PropertyPhotoAsset(photo.path(), photo.mimeType(),
                                    photo.sortOrder(), photo.coverFlag())).toList();
                    inventory = handoverChecklistService.rowsForLease(id).stream()
                            .map(row -> new TenancyAgreementPdfService.InventoryItem(row.getCategory(), row.getItemName(),
                                    row.getDefaultQuantity())).toList();
                } catch (NumberFormatException ignored) { }
            }
            byte[] pdf = tenancyAgreementPdfService.generate(request.fields(), photos, inventory);
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
                    .contentLength(pdf.length)
                    .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                            .filename(tenancyAgreementPdfService.fileName(request.fields()), StandardCharsets.UTF_8)
                            .build().toString()).body(pdf);
        }
        ContractTemplatePdfService.TemplateType type = parseType(templateType);
        byte[] pdf = service.generate(type, service.data(request.fields()));
        String fileName = type == ContractTemplatePdfService.TemplateType.OTR
                ? "Letter-Offer-to-Rent.pdf" : "Letter-of-Appointment-to-Rent.pdf";
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF).contentLength(pdf.length)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(fileName, StandardCharsets.UTF_8).build().toString()).body(pdf);
    }

    private ContractTemplatePdfService.TemplateType parseType(String value) {
        return switch (value.trim().toLowerCase()) {
            case "otr", "offer-to-rent", "letter-offer-to-rent" -> ContractTemplatePdfService.TemplateType.OTR;
            case "authorization", "appointment", "letter-of-appointment-to-rent" -> ContractTemplatePdfService.TemplateType.AUTHORIZATION;
            default -> throw new IllegalArgumentException("Unsupported contract template: " + value);
        };
    }
}
