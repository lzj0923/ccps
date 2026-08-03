package com.ccps.backend.controller;

import java.net.URI;
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

import com.ccps.backend.dto.AdminPropertyOwnershipRequest;
import com.ccps.backend.dto.AdminPropertyOwnershipResponse;
import com.ccps.backend.service.AdminPropertyOwnershipService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/units/{unitId}/ownerships")
public class AdminPropertyOwnershipController {
    private final AdminPropertyOwnershipService service;
    public AdminPropertyOwnershipController(AdminPropertyOwnershipService service){this.service=service;}

    @GetMapping public List<AdminPropertyOwnershipResponse> list(@PathVariable Long unitId){return service.list(unitId);}

    @PostMapping public ResponseEntity<AdminPropertyOwnershipResponse> create(@PathVariable Long unitId,
            @Valid @RequestBody AdminPropertyOwnershipRequest request){
        var result=service.create(unitId,request);
        return ResponseEntity.created(URI.create("/api/admin/units/"+unitId+"/ownerships/"+result.ownershipId())).body(result);
    }

    @PutMapping("/{ownershipId}") public AdminPropertyOwnershipResponse update(@PathVariable Long unitId,
            @PathVariable Long ownershipId,@Valid @RequestBody AdminPropertyOwnershipRequest request){return service.update(unitId,ownershipId,request);}

    @DeleteMapping("/{ownershipId}") public ResponseEntity<Void> delete(@PathVariable Long unitId,@PathVariable Long ownershipId){
        service.delete(unitId,ownershipId);return ResponseEntity.noContent().build();
    }
}
