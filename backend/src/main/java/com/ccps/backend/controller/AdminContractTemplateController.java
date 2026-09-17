package com.ccps.backend.controller;

import java.io.IOException;
import java.util.Map;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import com.ccps.backend.config.AuthInterceptor;
import com.ccps.backend.dto.ContractTemplateGenerateRequest;
import com.ccps.backend.service.AdminPropertyPhotoService;
import com.ccps.backend.service.AdminPropertyHandoverChecklistService;
import com.ccps.backend.service.ContractTemplatePdfService;
import com.ccps.backend.service.TenancyAgreementPdfService;
import com.ccps.backend.service.RentalManagementTemplatePdfService;
import com.ccps.backend.web.DownloadContentDisposition;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/contract-templates")
public class AdminContractTemplateController {
    private final ContractTemplatePdfService service;
    private final TenancyAgreementPdfService tenancyAgreementPdfService;
    private final AdminPropertyPhotoService propertyPhotoService;
    private final AdminPropertyHandoverChecklistService handoverChecklistService;
    private final RentalManagementTemplatePdfService rentalManagementTemplateService;

    public AdminContractTemplateController(ContractTemplatePdfService service,
            TenancyAgreementPdfService tenancyAgreementPdfService,
            AdminPropertyPhotoService propertyPhotoService,
            AdminPropertyHandoverChecklistService handoverChecklistService,
            RentalManagementTemplatePdfService rentalManagementTemplateService) {
        this.service = service;
        this.tenancyAgreementPdfService = tenancyAgreementPdfService;
        this.propertyPhotoService = propertyPhotoService;
        this.handoverChecklistService = handoverChecklistService;
        this.rentalManagementTemplateService = rentalManagementTemplateService;
    }

    @PostMapping(value = "/{templateType}/generate", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<byte[]> generate(@PathVariable("templateType") String templateType,
            @Valid @RequestBody ContractTemplateGenerateRequest request, HttpServletRequest servletRequest) {
        RentalManagementTemplatePdfService.TemplateType rentalManagementType = parseRentalManagementType(templateType);
        if (rentalManagementType != null) {
            byte[] pdf;
            try {
                pdf = rentalManagementTemplateService.generate(rentalManagementType, request.fields());
            } catch (IllegalArgumentException exception) {
                throw new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
            }
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF).contentLength(pdf.length)
                    .header(HttpHeaders.CONTENT_DISPOSITION, DownloadContentDisposition.attachment(
                            rentalManagementTemplateService.fileName(rentalManagementType, request.fields())))
                    .body(pdf);
        }
        if ("tenancy-agreement".equalsIgnoreCase(templateType.trim())) {
            List<TenancyAgreementPdfService.PropertyPhotoAsset> photos = List.of();
            List<TenancyAgreementPdfService.InventoryItem> inventory = List.of();
            String leaseId = request.fields().get("leaseId");
            try {
                Long id = Long.valueOf(leaseId == null ? "" : leaseId.trim());
                photos = propertyPhotoService.regularAssetsForLease(id, request.fields().get("photoIds")).stream()
                        .map(photo -> new TenancyAgreementPdfService.PropertyPhotoAsset(photo.path(), photo.mimeType(),
                                photo.sortOrder(), photo.coverFlag())).toList();
                inventory = handoverChecklistService.rowsForLease(id, request.fields().get("handoverChecklistIds")).stream()
                        .map(row -> new TenancyAgreementPdfService.InventoryItem(row.getCategory(), row.getItemName(),
                                row.getDefaultQuantity())).toList();
            } catch (NumberFormatException exception) {
                throw new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.BAD_REQUEST, "租赁合同缺少有效的租约关联", exception);
            }
            try {
                tenancyAgreementPdfService.validateRequiredFields(request.fields());
            } catch (IllegalArgumentException exception) {
                throw new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
            }
            if (inventory.isEmpty()) {
                throw new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.BAD_REQUEST, "租赁合同请至少选择一项交接清单");
            }
            if (photos.isEmpty()) {
                throw new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.BAD_REQUEST, "租赁合同请至少选择一张房产照片");
            }
            byte[] pdf = tenancyAgreementPdfService.generate(request.fields(), photos, inventory);
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
                    .contentLength(pdf.length)
                    .header(HttpHeaders.CONTENT_DISPOSITION, DownloadContentDisposition.attachment(
                            tenancyAgreementPdfService.fileName(request.fields())))
                    .body(pdf);
        }
        ContractTemplatePdfService.TemplateType type = parseType(templateType);
        try {
            service.validateRequiredFields(type, request.fields());
        } catch (IllegalArgumentException exception) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        }
        byte[] pdf = service.generate(type, service.data(request.fields()));
        String fileName = generatedFileName(type, request.fields());
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF).contentLength(pdf.length)
                .header(HttpHeaders.CONTENT_DISPOSITION, DownloadContentDisposition.attachment(fileName)).body(pdf);
    }

    @GetMapping("/{templateType}/version")
    public RentalManagementTemplatePdfService.TemplateVersion currentVersion(
            @PathVariable("templateType") String templateType) {
        RentalManagementTemplatePdfService.TemplateType type = requireRentalManagementType(templateType);
        return rentalManagementTemplateService.currentVersion(type);
    }

    @PostMapping(value = "/{templateType}/template", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public RentalManagementTemplatePdfService.TemplateVersion replaceTemplate(
            @PathVariable("templateType") String templateType, @RequestPart("file") MultipartFile file,
            HttpServletRequest servletRequest) {
        AuthInterceptor.userId(servletRequest);
        return rentalManagementTemplateService.replace(requireRentalManagementType(templateType), file);
    }

    @GetMapping(value = "/{templateType}/template", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> currentTemplate(@PathVariable("templateType") String templateType) {
        RentalManagementTemplatePdfService.TemplateType type = requireRentalManagementType(templateType);
        byte[] pdf = rentalManagementTemplateService.currentTemplate(type);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF).contentLength(pdf.length)
                .header(HttpHeaders.CONTENT_DISPOSITION, DownloadContentDisposition.inline(
                        rentalManagementTemplateService.currentVersion(type).originalName()))
                .body(pdf);
    }

    @GetMapping("/{templateType}/layout")
    public RentalManagementTemplatePdfService.TemplateLayout currentLayout(
            @PathVariable("templateType") String templateType) {
        return rentalManagementTemplateService.currentLayout(requireRentalManagementType(templateType));
    }

    @PutMapping("/{templateType}/layout")
    public RentalManagementTemplatePdfService.TemplateLayout saveLayout(
            @PathVariable("templateType") String templateType,
            @RequestBody RentalManagementTemplatePdfService.TemplateLayout layout,
            HttpServletRequest servletRequest) {
        AuthInterceptor.userId(servletRequest);
        try {
            return rentalManagementTemplateService.saveLayout(requireRentalManagementType(templateType), layout);
        } catch (IllegalArgumentException exception) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        }
    }

    private ContractTemplatePdfService.TemplateType parseType(String value) {
        return switch (value.trim().toLowerCase()) {
            case "otr", "offer-to-rent", "letter-offer-to-rent" -> ContractTemplatePdfService.TemplateType.OTR;
            case "authorization", "appointment", "letter-of-appointment-to-rent" -> ContractTemplatePdfService.TemplateType.AUTHORIZATION;
            default -> throw new IllegalArgumentException("Unsupported contract template: " + value);
        };
    }

    private RentalManagementTemplatePdfService.TemplateType requireRentalManagementType(String value) {
        RentalManagementTemplatePdfService.TemplateType type = parseRentalManagementType(value);
        if (type == null) throw new IllegalArgumentException("Unsupported rental management template: " + value);
        return type;
    }

    private RentalManagementTemplatePdfService.TemplateType parseRentalManagementType(String value) {
        return switch (value.trim().toLowerCase()) {
            case "property-management-agreement", "pma", "management-agreement" ->
                    RentalManagementTemplatePdfService.TemplateType.PROPERTY_MANAGEMENT_AGREEMENT;
            case "management-authorization", "authorization-to-manage" ->
                    RentalManagementTemplatePdfService.TemplateType.MANAGEMENT_AUTHORIZATION;
            case "termination-letter", "termination_notice", "termination-letter-with-landlord" ->
                    RentalManagementTemplatePdfService.TemplateType.TERMINATION_LETTER;
            case "rental-remittance", "remittance-of-rental", "rental-remittance-letter" ->
                    RentalManagementTemplatePdfService.TemplateType.RENTAL_REMITTANCE;
            default -> null;
        };
    }

    private String generatedFileName(ContractTemplatePdfService.TemplateType type, Map<String, String> fields) {
        String party = safeFilePart(type == ContractTemplatePdfService.TemplateType.OTR
                ? firstNotBlank(fields.get("tenantName"), fields.get("landlordName"))
                : fields.get("landlordName"));
        String project = safeFilePart(fields.get("projectName"));
        String unit = safeFilePart(fields.get("unitNo"));
        String documentName = "租赁委托书";
        String reference = safeFilePart(fields.get("caseNo"));
        return java.util.stream.Stream.of(party, project, unit, documentName, reference)
                .filter(part -> !part.isBlank()).reduce((left, right) -> left + "-" + right).orElse(documentName) + ".pdf";
    }

    private String firstNotBlank(String preferred, String fallback) {
        return preferred != null && !preferred.isBlank() ? preferred : fallback;
    }

    private String safeFilePart(String value) {
        if (value == null) return "";
        String safe = value.trim().replaceAll("[\\\\/:*?\"<>|\\r\\n]+", "-").replaceAll("\\s+", " ");
        return safe.length() > 60 ? safe.substring(0, 60) : safe;
    }
}
