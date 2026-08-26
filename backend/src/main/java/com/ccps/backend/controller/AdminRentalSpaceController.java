package com.ccps.backend.controller;

import java.net.URI;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ccps.backend.dto.AdminRentalModeRequest;
import com.ccps.backend.dto.AdminRentalSpaceRequest;
import com.ccps.backend.dto.AdminRentalSpaceResponse;
import com.ccps.backend.service.AdminRentalSpaceService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/units/{unitId}/rental-spaces")
public class AdminRentalSpaceController {
    private final AdminRentalSpaceService service;
    public AdminRentalSpaceController(AdminRentalSpaceService service){this.service=service;}
    @GetMapping public List<AdminRentalSpaceResponse> find(@PathVariable Long unitId){return service.find(unitId);}
    @PostMapping public ResponseEntity<Map<String,Long>> create(@PathVariable Long unitId,@Valid @RequestBody AdminRentalSpaceRequest request){
        Long id=service.create(unitId,request);return ResponseEntity.created(URI.create("/api/admin/units/"+unitId+"/rental-spaces/"+id)).body(Map.of("id",id));
    }
    @PutMapping("/{spaceId}") public ResponseEntity<Void> update(@PathVariable Long unitId,@PathVariable Long spaceId,@Valid @RequestBody AdminRentalSpaceRequest request){service.update(unitId,spaceId,request);return ResponseEntity.noContent().build();}
    @DeleteMapping("/{spaceId}") public ResponseEntity<Void> disable(@PathVariable Long unitId,@PathVariable Long spaceId){service.disable(unitId,spaceId);return ResponseEntity.noContent().build();}
    @PutMapping("/mode") public ResponseEntity<Void> mode(@PathVariable Long unitId,@Valid @RequestBody AdminRentalModeRequest request){service.changeMode(unitId,request);return ResponseEntity.noContent().build();}
}
