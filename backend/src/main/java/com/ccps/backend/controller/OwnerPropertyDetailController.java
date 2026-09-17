package com.ccps.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ccps.backend.config.AuthInterceptor;
import com.ccps.backend.dto.OwnerPropertyDetailResponse;
import com.ccps.backend.service.OwnerPropertyDetailService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/owner/properties/{ownerUnitId}/detail")
public class OwnerPropertyDetailController {
    private final OwnerPropertyDetailService service;
    public OwnerPropertyDetailController(OwnerPropertyDetailService service){this.service=service;}
    @GetMapping public OwnerPropertyDetailResponse get(@PathVariable Long ownerUnitId,HttpServletRequest request){return service.get(AuthInterceptor.userId(request),ownerUnitId);}
}
