package com.ccps.backend.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ccps.backend.config.AuthInterceptor;
import com.ccps.backend.dto.AdminOffMarketPropertyPageResponse;
import com.ccps.backend.dto.AdminPropertyManagementHistoryResponse;
import com.ccps.backend.dto.AdminPropertyOffMarketRequest;
import com.ccps.backend.dto.AdminPropertyRelistRequest;
import com.ccps.backend.service.AdminPropertyArchiveService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/rental-listings")
public class AdminRentalListingController {
    private final AdminPropertyArchiveService service;

    public AdminRentalListingController(AdminPropertyArchiveService service) { this.service = service; }

    @GetMapping("/off-market")
    public AdminOffMarketPropertyPageResponse findOffMarket(@RequestParam(defaultValue="1") int page,
            @RequestParam(defaultValue="10") int pageSize,@RequestParam(required=false) String keyword,
            @RequestParam(required=false) String reasonCode){return service.list(page,pageSize,keyword,reasonCode);}

    @PostMapping("/{unitId}/off-market")
    public ResponseEntity<Void> offMarket(@PathVariable Long unitId,@Valid @RequestBody AdminPropertyOffMarketRequest body,
            HttpServletRequest request){service.offMarket(unitId,body.reasonCode(),body.note(),AuthInterceptor.userId(request));return ResponseEntity.noContent().build();}

    @PostMapping("/{unitId}/relist")
    public ResponseEntity<Void> relist(@PathVariable Long unitId,@Valid @RequestBody AdminPropertyRelistRequest body,
            HttpServletRequest request){service.relist(unitId,body.note(),AuthInterceptor.userId(request));return ResponseEntity.noContent().build();}

    @GetMapping("/{unitId}/history")
    public List<AdminPropertyManagementHistoryResponse> history(@PathVariable Long unitId){return service.history(unitId);}
}
