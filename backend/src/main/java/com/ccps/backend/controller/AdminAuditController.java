package com.ccps.backend.controller;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ccps.backend.dto.AdminAuditOptionsResponse;
import com.ccps.backend.dto.AdminAuditResponse;
import com.ccps.backend.service.AdminAuditService;

@RestController
@RequestMapping("/api/admin/audit")
public class AdminAuditController {
    private final AdminAuditService service;

    public AdminAuditController(AdminAuditService service) { this.service = service; }

    @GetMapping
    public AdminAuditResponse list(@RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize, @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String action, @RequestParam(required = false) Long actorId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return service.list(page, pageSize, keyword, action, actorId, startDate, endDate);
    }

    @GetMapping("/options")
    public AdminAuditOptionsResponse options() { return service.options(); }
}
