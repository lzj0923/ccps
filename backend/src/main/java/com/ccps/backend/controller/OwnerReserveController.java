package com.ccps.backend.controller;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import org.springframework.core.io.FileSystemResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.ccps.backend.config.AuthInterceptor;
import com.ccps.backend.dto.OwnerReserveResponse;
import com.ccps.backend.dto.ReserveTopupSubmissionResponse;
import com.ccps.backend.service.OwnerReserveService;
import com.ccps.backend.service.ReserveTopupService;
import com.ccps.backend.service.ReserveTopupService.Download;
import com.ccps.backend.service.ReserveTopupService.Submission;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/owner/reserve")
public class OwnerReserveController {
    private final OwnerReserveService service;
    private final ReserveTopupService topupService;

    public OwnerReserveController(OwnerReserveService service, ReserveTopupService topupService) {
        this.service = service;
        this.topupService = topupService;
    }

    @GetMapping
    public OwnerReserveResponse overview(
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            HttpServletRequest request) {
        return service.getReserve(AuthInterceptor.userId(request), projectId, type, startDate, endDate);
    }

    @PostMapping(value = "/topups", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ReserveTopupSubmissionResponse submitTopup(
            @RequestParam Long reserveAccountId,
            @RequestParam BigDecimal amount,
            @RequestParam LocalDate paymentDate,
            @RequestParam String paymentMethod,
            @RequestParam String bankName,
            @RequestParam String reference,
            @RequestParam String payerName,
            @RequestParam(required = false) String note,
            @RequestParam("files") List<MultipartFile> files,
            HttpServletRequest request) {
        return topupService.submit(AuthInterceptor.userId(request), new Submission(reserveAccountId, amount, paymentDate,
                paymentMethod, bankName, reference, payerName, note), files);
    }

    @GetMapping("/documents/{documentId}")
    public ResponseEntity<FileSystemResource> document(
            @PathVariable Long documentId,
            @RequestParam(defaultValue = "false") boolean download,
            HttpServletRequest request) {
        Download file = topupService.download(AuthInterceptor.userId(request), documentId);
        MediaType mediaType;
        try { mediaType = MediaType.parseMediaType(file.mimeType()); }
        catch (IllegalArgumentException ignored) { mediaType = MediaType.APPLICATION_OCTET_STREAM; }
        ContentDisposition disposition = ContentDisposition.builder(download ? "attachment" : "inline")
                .filename(file.originalName(), StandardCharsets.UTF_8).build();
        return ResponseEntity.ok().contentType(mediaType).contentLength(file.size())
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(new FileSystemResource(file.path()));
    }

}
