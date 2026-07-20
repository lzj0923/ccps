package com.ccps.backend.controller;

import java.util.List;
import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ccps.backend.dto.AdminOwnerResponse;
import com.ccps.backend.dto.AdminOwnerSummaryResponse;
import com.ccps.backend.dto.AdminOwnerCreateRequest;
import com.ccps.backend.dto.AdminProjectOption;
import com.ccps.backend.dto.AdminPropertyCreateRequest;
import com.ccps.backend.dto.AdminOwnerResponse.Property;
import com.ccps.backend.dto.AdminPropertyUpdateRequest;
import com.ccps.backend.service.AdminOwnerService;

import jakarta.validation.Valid;

@Validated
@RestController
@RequestMapping("/api/admin/owners")
public class AdminOwnerController {
    private final AdminOwnerService service;

    public AdminOwnerController(AdminOwnerService service) {
        this.service = service;
    }

    @GetMapping
    public List<AdminOwnerResponse> findOwners() {
        return service.findOwners();
    }

    @GetMapping("/summary")
    public AdminOwnerSummaryResponse findSummary() {
        return service.findSummary();
    }

    @PostMapping
    public ResponseEntity<AdminOwnerResponse> createOwner(@Valid @RequestBody AdminOwnerCreateRequest request) {
        AdminOwnerResponse response = service.createOwner(request);
        return ResponseEntity.created(URI.create("/api/admin/owners/" + response.id())).body(response);
    }

    @GetMapping("/projects")
    public List<AdminProjectOption> findProjects() {
        return service.findProjects();
    }

    @PostMapping("/{ownerId}/properties")
    public ResponseEntity<Property> createProperty(@PathVariable Long ownerId,
                                                   @Valid @RequestBody AdminPropertyCreateRequest request) {
        Property response = service.createProperty(ownerId, request);
        return ResponseEntity.created(URI.create("/api/admin/owners/" + ownerId + "/properties/" + response.ownerUnitId())).body(response);
    }

    @GetMapping("/{ownerId}/properties/{ownerUnitId}")
    public Property findProperty(@PathVariable Long ownerId, @PathVariable Long ownerUnitId) {
        return service.findProperty(ownerId, ownerUnitId);
    }

    @PutMapping("/{ownerId}/properties/{ownerUnitId}")
    public Property updateProperty(@PathVariable Long ownerId,
                                   @PathVariable Long ownerUnitId,
                                   @Valid @RequestBody AdminPropertyUpdateRequest request) {
        return service.updateProperty(ownerId, ownerUnitId, request);
    }
}
