package com.ccps.backend.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ccps.backend.config.AuthInterceptor;
import com.ccps.backend.dto.AdminPropertyBankAccountRequest;
import com.ccps.backend.dto.AdminPropertyBankAccountResponse;
import com.ccps.backend.service.AdminPropertyBankAccountService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/admin/owners/{ownerId}/properties/{ownerUnitId}/bank-accounts")
public class AdminPropertyBankAccountController {
    private final AdminPropertyBankAccountService service;
    public AdminPropertyBankAccountController(AdminPropertyBankAccountService service){this.service=service;}
    @GetMapping public List<AdminPropertyBankAccountResponse> list(@PathVariable Long ownerId,@PathVariable Long ownerUnitId){return service.list(ownerId,ownerUnitId);}
    @PostMapping public AdminPropertyBankAccountResponse create(@PathVariable Long ownerId,@PathVariable Long ownerUnitId,@RequestBody AdminPropertyBankAccountRequest body,HttpServletRequest request){return service.create(AuthInterceptor.userId(request),ownerId,ownerUnitId,body);}
    @PutMapping("/{accountId}") public AdminPropertyBankAccountResponse update(@PathVariable Long ownerId,@PathVariable Long ownerUnitId,@PathVariable Long accountId,@RequestBody AdminPropertyBankAccountRequest body){return service.update(ownerId,ownerUnitId,accountId,body);}
    @DeleteMapping("/{accountId}") public ResponseEntity<Void> delete(@PathVariable Long ownerId,@PathVariable Long ownerUnitId,@PathVariable Long accountId){service.delete(ownerId,ownerUnitId,accountId);return ResponseEntity.noContent().build();}
}
