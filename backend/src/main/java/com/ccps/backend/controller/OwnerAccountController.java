package com.ccps.backend.controller;

import org.springframework.web.bind.annotation.*;
import com.ccps.backend.config.AuthInterceptor;
import com.ccps.backend.dto.OwnerAccountResponse;
import com.ccps.backend.service.OwnerAccountService;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/owner/account")
public class OwnerAccountController {
    private final OwnerAccountService service;
    public OwnerAccountController(OwnerAccountService service) { this.service = service; }
    @GetMapping
    public OwnerAccountResponse get(HttpServletRequest request) { return service.get(AuthInterceptor.userId(request)); }
    @PutMapping
    public OwnerAccountResponse update(@RequestBody OwnerAccountService.Contact contact, HttpServletRequest request) {
        return service.update(AuthInterceptor.userId(request), contact);
    }
    @PutMapping("/password")
    public void password(@RequestBody OwnerAccountService.Password password, HttpServletRequest request) {
        service.changePassword(AuthInterceptor.userId(request), password);
    }
}
