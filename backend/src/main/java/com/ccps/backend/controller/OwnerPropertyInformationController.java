package com.ccps.backend.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ccps.backend.config.AuthInterceptor;
import com.ccps.backend.dto.OwnerPropertyInformationResponse;
import com.ccps.backend.service.OwnerPropertyInformationService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/owner/properties/{ownerUnitId}/information")
public class OwnerPropertyInformationController {
    private final OwnerPropertyInformationService service;

    public OwnerPropertyInformationController(OwnerPropertyInformationService service) {
        this.service = service;
    }

    @GetMapping
    public List<OwnerPropertyInformationResponse> list(
            @PathVariable Long ownerUnitId, HttpServletRequest request) {
        return service.list(AuthInterceptor.userId(request), ownerUnitId);
    }
}
