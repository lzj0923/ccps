package com.ccps.backend.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.ccps.backend.config.AuthInterceptor;
import com.ccps.backend.dto.AdminLeasePaymentResponse;
import com.ccps.backend.dto.AdminLeasePaymentUpdateRequest;
import com.ccps.backend.service.AdminLeasePaymentService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/tenancy/leases/{leaseId}/payments")
public class AdminLeasePaymentController {
    private final AdminLeasePaymentService service;public AdminLeasePaymentController(AdminLeasePaymentService service){this.service=service;}
    @GetMapping public List<AdminLeasePaymentResponse> list(@PathVariable Long leaseId){return service.list(leaseId);}
    @PutMapping("/{paymentId}") public AdminLeasePaymentResponse update(@PathVariable Long leaseId,@PathVariable Long paymentId,@Valid @RequestBody AdminLeasePaymentUpdateRequest body,HttpServletRequest request){throw new ResponseStatusException(HttpStatus.GONE,"已确认租金须在财务确认模块退回后才能修改");}
    @DeleteMapping("/{paymentId}") public ResponseEntity<Void> delete(@PathVariable Long leaseId,@PathVariable Long paymentId,HttpServletRequest request){throw new ResponseStatusException(HttpStatus.GONE,"已确认租金须在财务确认模块退回后才能作废");}
}
