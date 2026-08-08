package com.ccps.backend.controller;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ccps.backend.dto.AdminProjectCreateRequest;
import com.ccps.backend.dto.AdminProjectManagementResponse;
import com.ccps.backend.service.AdminProjectService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/projects")
public class AdminProjectController {
    private final AdminProjectService service;

    public AdminProjectController(AdminProjectService service) {
        this.service = service;
    }

    @GetMapping
    public AdminProjectManagementResponse findAll(@RequestParam(defaultValue = "1") int page,
                                                   @RequestParam(defaultValue = "5") int pageSize,
                                                   @RequestParam(required = false) String keyword,
                                                   @RequestParam(required = false) String status) {
        return service.findAll(page, pageSize, keyword, status);
    }

    @PostMapping
    public ResponseEntity<AdminProjectManagementResponse.Project> create(
            @Valid @RequestBody AdminProjectCreateRequest request) {
        AdminProjectManagementResponse.Project response = service.create(request);
        return ResponseEntity.created(URI.create("/api/admin/projects/" + response.id())).body(response);
    }

    @PutMapping("/{id}")
    public AdminProjectManagementResponse.Project update(@PathVariable Long id,
                                                          @Valid @RequestBody AdminProjectCreateRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
