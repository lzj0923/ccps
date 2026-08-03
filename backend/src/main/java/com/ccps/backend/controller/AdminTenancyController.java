package com.ccps.backend.controller;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.core.io.FileSystemResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.ccps.backend.config.AuthInterceptor;
import com.ccps.backend.dto.AdminLeaseCreateRequest;
import com.ccps.backend.dto.AdminLeaseCloseRequest;
import com.ccps.backend.dto.AdminLeaseInvoiceCreateRequest;
import com.ccps.backend.dto.AdminLeaseRentInvoiceResponse;
import com.ccps.backend.dto.AdminLeaseUpdateRequest;
import com.ccps.backend.dto.AdminLeaseTransferRequest;
import com.ccps.backend.dto.AdminRentFinanceResponse;
import com.ccps.backend.dto.AdminRentCollectionResponse;
import com.ccps.backend.dto.AdminRentCollectionRequest;
import com.ccps.backend.dto.AdminRecordCreateResponse;
import com.ccps.backend.dto.AdminTenancyOptionsResponse;
import com.ccps.backend.dto.AdminTenancyResponse;
import com.ccps.backend.dto.AdminTenantCreateRequest;
import com.ccps.backend.dto.AdminTenantDirectoryResponse;
import com.ccps.backend.dto.AdminTenantDetailResponse;
import com.ccps.backend.service.AdminTenancyService;
import com.ccps.backend.service.AdminTenancyService.Download;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/tenancy")
public class AdminTenancyController {
    private final AdminTenancyService service;
    public AdminTenancyController(AdminTenancyService service) { this.service = service; }

    @GetMapping
    public AdminTenancyResponse find(@RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String projectName,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        return service.find(page, pageSize, keyword, projectName, status, startDate, endDate);
    }

    @GetMapping("/options") public AdminTenancyOptionsResponse options() { return service.options(); }

    @GetMapping("/tenants")
    public AdminTenantDirectoryResponse tenants(@RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize, @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        return service.findTenantDirectory(page, pageSize, keyword, status);
    }

    @GetMapping("/leases/{leaseId}/rent-invoices")
    public List<AdminLeaseRentInvoiceResponse> leaseRentInvoices(@PathVariable Long leaseId) {
        return service.findLeaseInvoiceDetails(leaseId);
    }

    @GetMapping("/rent-reviews")
    public AdminRentFinanceResponse rentReviews(@RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize, @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String projectName, @RequestParam(required = false) String status,
            @RequestParam(required = false) LocalDate startDate, @RequestParam(required = false) LocalDate endDate,
            @RequestParam(required = false) LocalDate billingMonth, @RequestParam(required = false) String tenantName,
            @RequestParam(required = false) String unitNo) {
        return service.findRentFinance(page, pageSize, keyword, projectName, status, startDate, endDate, billingMonth, tenantName, unitNo);
    }

    @GetMapping("/rent-reviews/projects")
    public List<String> rentReviewProjects() { return service.rentFinanceProjects(); }

    @GetMapping("/rent-collections")
    public AdminRentCollectionResponse rentCollections(@RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize, @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String projectName, @RequestParam(required = false) String status,
            @RequestParam(required = false) LocalDate startDate, @RequestParam(required = false) LocalDate endDate) {
        return service.findRentCollections(page, pageSize, keyword, projectName, status, startDate, endDate);
    }

    @GetMapping("/rent-collections/projects")
    public List<String> rentCollectionProjects() { return service.rentCollectionProjects(); }

    @PostMapping(value = "/rent-invoices/{invoiceId}/confirm", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AdminRecordCreateResponse confirmRentCollection(@PathVariable Long invoiceId,
            @Valid @RequestPart("payload") AdminRentCollectionRequest payload,
            @RequestPart(value = "proof", required = false) MultipartFile proof, HttpServletRequest request) {
        return service.confirmRentCollection(AuthInterceptor.userId(request), invoiceId, payload, proof);
    }

    @PostMapping("/tenants")
    public ResponseEntity<Map<String, Long>> createTenant(@Valid @RequestBody AdminTenantCreateRequest request) {
        Long id = service.createTenant(request);
        return ResponseEntity.created(URI.create("/api/admin/tenancy/tenants/" + id)).body(Map.of("id", id));
    }

    @GetMapping("/tenants/{tenantId}")
    public AdminTenantDetailResponse tenantDetail(@PathVariable Long tenantId) {
        return service.findTenantDetail(tenantId);
    }

    @PutMapping("/tenants/{tenantId}")
    public ResponseEntity<Void> updateTenant(@PathVariable Long tenantId,@Valid @RequestBody AdminTenantCreateRequest request) {
        service.updateTenant(tenantId,request);return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/tenants/{tenantId}")
    public ResponseEntity<Void> deleteTenant(@PathVariable Long tenantId) {
        service.deleteTenant(tenantId); return ResponseEntity.noContent().build();
    }

    @PostMapping("/leases")
    public ResponseEntity<Map<String, Long>> createLease(@Valid @RequestBody AdminLeaseCreateRequest request) {
        Long id = service.createLease(request);
        return ResponseEntity.created(URI.create("/api/admin/tenancy/leases/" + id)).body(Map.of("id", id));
    }

    @PostMapping("/leases/{leaseId}/rent-invoices")
    public ResponseEntity<Void> createLeaseInvoice(@PathVariable Long leaseId,
            @Valid @RequestBody AdminLeaseInvoiceCreateRequest request) {
        service.createLeaseInvoice(leaseId, request);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/leases/{leaseId}")
    public ResponseEntity<Void> updateLease(@PathVariable Long leaseId,
            @Valid @RequestBody AdminLeaseUpdateRequest payload, HttpServletRequest request) {
        service.updateLease(AuthInterceptor.userId(request), leaseId, payload);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/leases/{leaseId}/close")
    public ResponseEntity<Void> closeLease(@PathVariable Long leaseId,
            @Valid @RequestBody AdminLeaseCloseRequest payload, HttpServletRequest request) {
        service.closeLease(AuthInterceptor.userId(request), leaseId, payload);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/leases/{leaseId}/transfer")
    public ResponseEntity<Map<String, Long>> transferLease(@PathVariable Long leaseId,
            @Valid @RequestBody AdminLeaseTransferRequest payload, HttpServletRequest request) {
        Long id = service.transferLease(AuthInterceptor.userId(request), leaseId, payload);
        return ResponseEntity.created(URI.create("/api/admin/tenancy/leases/" + id)).body(Map.of("id", id));
    }

    @PostMapping(value = "/leases/{leaseId}/contract", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Long>> uploadContract(@PathVariable Long leaseId,
            @RequestParam("file") MultipartFile file, HttpServletRequest request) {
        Long id = service.uploadContract(AuthInterceptor.userId(request), leaseId, file);
        return ResponseEntity.ok(Map.of("id", id));
    }

    @GetMapping("/leases/{leaseId}/contract")
    public ResponseEntity<FileSystemResource> contract(@PathVariable Long leaseId,
            @RequestParam(defaultValue = "false") boolean download) {
        Download file = service.downloadContract(leaseId);
        MediaType mediaType;
        try { mediaType = MediaType.parseMediaType(file.mimeType()); }
        catch (IllegalArgumentException ignored) { mediaType = MediaType.APPLICATION_OCTET_STREAM; }
        ContentDisposition disposition = ContentDisposition.builder(download ? "attachment" : "inline")
                .filename(file.originalName(), StandardCharsets.UTF_8).build();
        return ResponseEntity.ok().contentType(mediaType).contentLength(file.size())
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(new FileSystemResource(file.path()));
    }

    @PostMapping(value = "/rent-payments/{financeRecordId}/proof", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Long>> uploadRentProof(@PathVariable Long financeRecordId,
            @RequestParam("file") MultipartFile file, HttpServletRequest request) {
        Long id = service.uploadRentProof(AuthInterceptor.userId(request), financeRecordId, file);
        return ResponseEntity.ok(Map.of("id", id));
    }

    @GetMapping("/rent-proofs/{documentId}")
    public ResponseEntity<FileSystemResource> rentProof(@PathVariable Long documentId,
            @RequestParam(defaultValue = "false") boolean download) {
        Download file = service.downloadRentProof(documentId);
        MediaType mediaType;
        try { mediaType = MediaType.parseMediaType(file.mimeType()); }
        catch (IllegalArgumentException ignored) { mediaType = MediaType.APPLICATION_OCTET_STREAM; }
        ContentDisposition disposition = ContentDisposition.builder(download ? "attachment" : "inline")
                .filename(file.originalName(), StandardCharsets.UTF_8).build();
        return ResponseEntity.ok().contentType(mediaType).contentLength(file.size())
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(new FileSystemResource(file.path()));
    }

    @GetMapping("/rent-payments/{financeRecordId}/receipt")
    public ResponseEntity<FileSystemResource> rentReceipt(@PathVariable Long financeRecordId) {
        Download file = service.downloadRentReceipt(financeRecordId);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF).contentLength(file.size())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(file.originalName(), StandardCharsets.UTF_8).build().toString())
                .body(new FileSystemResource(file.path()));
    }

    @PostMapping("/rent-invoices/{invoiceId}/reminders")
    public ResponseEntity<Void> remind(@PathVariable Long invoiceId) {
        service.sendReminder(invoiceId); return ResponseEntity.noContent().build();
    }
}
