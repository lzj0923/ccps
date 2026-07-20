package com.ccps.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ccps.backend.dto.AdminPropertyPageResponse;
import com.ccps.backend.service.AdminOwnerService;

@RestController
@RequestMapping("/api/admin/properties")
public class AdminPropertyController {
    private final AdminOwnerService service;

    public AdminPropertyController(AdminOwnerService service) {
        this.service = service;
    }

    @GetMapping
    public AdminPropertyPageResponse findProperties(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String projectName,
            @RequestParam(required = false) String rentalStatus) {
        return service.findProperties(page, pageSize, keyword, projectName, rentalStatus);
    }
}
