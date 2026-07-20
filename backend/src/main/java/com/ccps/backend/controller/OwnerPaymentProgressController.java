package com.ccps.backend.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.ccps.backend.config.AuthInterceptor;
import com.ccps.backend.dto.PaymentProgressResponse;
import com.ccps.backend.dto.PaymentProofSubmissionResponse;
import com.ccps.backend.service.PaymentProgressService;
import com.ccps.backend.service.PaymentProofService;
import com.ccps.backend.service.PaymentProofService.Submission;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/owner/payment-progress")
public class OwnerPaymentProgressController {
    private final PaymentProgressService service;
    private final PaymentProofService paymentProofService;

    public OwnerPaymentProgressController(PaymentProgressService service, PaymentProofService paymentProofService) {
        this.service = service;
        this.paymentProofService = paymentProofService;
    }

    @GetMapping("/{ownerUnitId}")
    public PaymentProgressResponse paymentProgress(@PathVariable Long ownerUnitId, HttpServletRequest request) {
        return service.getPaymentProgress(AuthInterceptor.userId(request), ownerUnitId);
    }

    @PostMapping(value = "/{ownerUnitId}/proofs", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public PaymentProofSubmissionResponse submitProof(
            @PathVariable Long ownerUnitId,
            @RequestParam Long installmentId,
            @RequestParam BigDecimal amount,
            @RequestParam LocalDate paymentDate,
            @RequestParam String paymentMethod,
            @RequestParam String bankName,
            @RequestParam String reference,
            @RequestParam String payerName,
            @RequestParam(required = false) String note,
            @RequestParam("files") List<MultipartFile> files,
            HttpServletRequest request) {
        Submission submission = new Submission(installmentId, amount, paymentDate, paymentMethod,
                bankName, reference, payerName, note);
        return paymentProofService.submit(AuthInterceptor.userId(request), ownerUnitId, submission, files);
    }
}
