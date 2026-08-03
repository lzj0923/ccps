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
import com.ccps.backend.dto.AdminPropertyImportantMessageRequest;
import com.ccps.backend.dto.AdminPropertyImportantMessageResponse;
import com.ccps.backend.service.AdminPropertyImportantMessageService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/admin/owners/{ownerId}/properties/{ownerUnitId}/important-messages")
public class AdminPropertyImportantMessageController {
    private final AdminPropertyImportantMessageService service;public AdminPropertyImportantMessageController(AdminPropertyImportantMessageService service){this.service=service;}
    @GetMapping public List<AdminPropertyImportantMessageResponse> list(@PathVariable Long ownerId,@PathVariable Long ownerUnitId){return service.list(ownerId,ownerUnitId);}
    @PostMapping public AdminPropertyImportantMessageResponse create(@PathVariable Long ownerId,@PathVariable Long ownerUnitId,@RequestBody AdminPropertyImportantMessageRequest body,HttpServletRequest request){return service.create(AuthInterceptor.userId(request),ownerId,ownerUnitId,body);}
    @PutMapping("/{messageId}") public AdminPropertyImportantMessageResponse update(@PathVariable Long ownerId,@PathVariable Long ownerUnitId,@PathVariable Long messageId,@RequestBody AdminPropertyImportantMessageRequest body){return service.update(ownerId,ownerUnitId,messageId,body);}
    @DeleteMapping("/{messageId}") public ResponseEntity<Void> delete(@PathVariable Long ownerId,@PathVariable Long ownerUnitId,@PathVariable Long messageId){service.delete(ownerId,ownerUnitId,messageId);return ResponseEntity.noContent().build();}
}
