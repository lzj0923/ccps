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
import com.ccps.backend.dto.AdminPropertyHandoverChecklistItemRequest;
import com.ccps.backend.dto.AdminPropertyHandoverChecklistItemResponse;
import com.ccps.backend.service.AdminPropertyHandoverChecklistService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/owners/{ownerId}/properties/{ownerUnitId}/handover-checklist")
public class AdminPropertyHandoverChecklistController {
    private final AdminPropertyHandoverChecklistService service;
    public AdminPropertyHandoverChecklistController(AdminPropertyHandoverChecklistService service){this.service=service;}
    @GetMapping public List<AdminPropertyHandoverChecklistItemResponse> list(@PathVariable Long ownerId,@PathVariable Long ownerUnitId){return service.list(ownerId,ownerUnitId);}
    @PostMapping public AdminPropertyHandoverChecklistItemResponse create(@PathVariable Long ownerId,@PathVariable Long ownerUnitId,@Valid @RequestBody AdminPropertyHandoverChecklistItemRequest request){return service.create(ownerId,ownerUnitId,request);}
    @PutMapping("/{id}") public AdminPropertyHandoverChecklistItemResponse update(@PathVariable Long ownerId,@PathVariable Long ownerUnitId,@PathVariable Long id,@Valid @RequestBody AdminPropertyHandoverChecklistItemRequest request){return service.update(ownerId,ownerUnitId,id,request);}
    @DeleteMapping("/{id}") public ResponseEntity<Void> delete(@PathVariable Long ownerId,@PathVariable Long ownerUnitId,@PathVariable Long id){service.delete(ownerId,ownerUnitId,id);return ResponseEntity.noContent().build();}
}
