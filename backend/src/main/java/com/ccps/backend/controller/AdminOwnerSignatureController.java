package com.ccps.backend.controller;

import java.util.List;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.ccps.backend.config.AuthInterceptor;
import com.ccps.backend.mapper.OwnerSignatureMapper.Recipient;
import com.ccps.backend.service.OwnerSignatureService;

@RestController
@RequestMapping("/api/admin/e-signatures/requests")
public class AdminOwnerSignatureController {
    private final OwnerSignatureService service;
    public AdminOwnerSignatureController(OwnerSignatureService service) { this.service = service; }
    @GetMapping("/{id}/owner-recipients") public List<Recipient> recipients(@PathVariable Long id) {
        return service.recipients(id);
    }
    @PostMapping("/{id}/owner-app") public ResponseEntity<Void> dispatch(@PathVariable Long id,
            @Valid @RequestBody Dispatch payload, HttpServletRequest request) {
        service.dispatch(AuthInterceptor.userId(request), id, payload.ownerId());
        return ResponseEntity.noContent().build();
    }
    public record Dispatch(@NotNull @Positive Long ownerId) {}
}
