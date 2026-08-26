package com.ccps.backend.controller;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.time.LocalDate;

import org.springframework.core.io.FileSystemResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ccps.backend.config.AuthInterceptor;
import com.ccps.backend.dto.AdminFinanceBatchConfirmRequest;
import com.ccps.backend.dto.AdminFinanceBatchReopenRequest;
import com.ccps.backend.dto.AdminFinanceAllocationNoteRequest;
import com.ccps.backend.dto.AdminFinanceConfirmRequest;
import com.ccps.backend.dto.AdminFinanceDecisionRequest;
import com.ccps.backend.dto.AdminFinanceReviewResponse;
import com.ccps.backend.service.AdminFinanceReviewService;
import com.ccps.backend.service.AdminFinanceReviewService.Download;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/finance")
public class AdminFinanceReviewController {
    private final AdminFinanceReviewService service;

    public AdminFinanceReviewController(AdminFinanceReviewService service) {
        this.service = service;
    }

    @GetMapping("/reviews")
    public AdminFinanceReviewResponse findReviews(
            @RequestParam(defaultValue = "property") String type,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String projectName,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        return service.findReviews(type, page, pageSize, keyword, projectName, status, startDate, endDate);
    }

    @GetMapping("/projects")
    public List<String> findProjects(@RequestParam(defaultValue = "property") String type) {
        return service.findProjects(type);
    }

    @PostMapping("/reviews/{financeRecordId}/confirm")
    public ResponseEntity<Void> confirm(@PathVariable Long financeRecordId,
            @Valid @RequestBody AdminFinanceConfirmRequest request,
            HttpServletRequest httpRequest) {
        service.confirm(AuthInterceptor.userId(httpRequest), financeRecordId, request.transactionDate(),
                request.receiptDate(), request.note());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reviews/{financeRecordId}/reject")
    public ResponseEntity<Void> reject(@PathVariable Long financeRecordId,
            @Valid @RequestBody AdminFinanceDecisionRequest request,
            HttpServletRequest httpRequest) {
        service.reject(AuthInterceptor.userId(httpRequest), financeRecordId, request.note());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reviews/{financeRecordId}/reopen")
    public ResponseEntity<Void> reopen(@PathVariable Long financeRecordId,
            @Valid @RequestBody AdminFinanceDecisionRequest request,
            HttpServletRequest httpRequest) {
        service.reopen(AuthInterceptor.userId(httpRequest), financeRecordId, request.note());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/reviews/{financeRecordId}/allocation-note")
    public ResponseEntity<Void> updateAllocationNote(@PathVariable Long financeRecordId,
            @Valid @RequestBody AdminFinanceAllocationNoteRequest request,
            HttpServletRequest httpRequest) {
        service.updateAllocationNote(AuthInterceptor.userId(httpRequest), financeRecordId,
                request.note(), Boolean.TRUE.equals(request.reuseEnabled()));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reviews/batch-confirm")
    public ResponseEntity<Void> confirmBatch(@Valid @RequestBody AdminFinanceBatchConfirmRequest request,
            HttpServletRequest httpRequest) {
        service.confirmBatch(AuthInterceptor.userId(httpRequest), request.ids(), request.transactionDate(),
                request.receiptDate(), request.note(), request.referenceNo());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reviews/batch-reopen")
    public ResponseEntity<Void> reopenBatch(@Valid @RequestBody AdminFinanceBatchReopenRequest request,
            HttpServletRequest httpRequest) {
        service.reopenBatch(AuthInterceptor.userId(httpRequest), request.ids(), request.note());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/proofs/{documentId}")
    public ResponseEntity<FileSystemResource> proof(@PathVariable Long documentId,
            @RequestParam(defaultValue = "false") boolean download) {
        Download file = service.downloadProof(documentId);
        MediaType mediaType;
        try { mediaType = MediaType.parseMediaType(file.mimeType()); }
        catch (IllegalArgumentException ignored) { mediaType = MediaType.APPLICATION_OCTET_STREAM; }
        ContentDisposition disposition = ContentDisposition.builder(download ? "attachment" : "inline")
                .filename(file.originalName(), StandardCharsets.UTF_8).build();
        return ResponseEntity.ok()
                .contentType(mediaType)
                .contentLength(file.size())
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(new FileSystemResource(file.path()));
    }

    @GetMapping("/reviews/{financeRecordId}/document")
    public ResponseEntity<FileSystemResource> financeDocument(@PathVariable Long financeRecordId,
            @RequestParam String documentType) {
        Download file = service.downloadFinancialDocument(financeRecordId, documentType);
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(file.originalName(), StandardCharsets.UTF_8).build();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(file.size())
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(new FileSystemResource(file.path()));
    }

    @PostMapping("/reviews/documents/batch")
    public ResponseEntity<FileSystemResource> financeDocumentsBatch(@RequestParam String documentType,
            @RequestBody List<Long> financeRecordIds) {
        Download file = service.downloadFinancialDocuments(financeRecordIds, documentType);
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(file.originalName(), StandardCharsets.UTF_8).build();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.mimeType()))
                .contentLength(file.size())
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(new FileSystemResource(file.path()));
    }
}
