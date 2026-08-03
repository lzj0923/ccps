package com.ccps.backend.controller;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.List;

import org.springframework.core.io.InputStreamResource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.ccps.backend.config.AuthInterceptor;
import com.ccps.backend.dto.AdminPropertyCashflowResponse;
import com.ccps.backend.service.AdminPropertyCashflowService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/admin/owners/{ownerId}/properties/{ownerUnitId}/income-expenses")
public class AdminPropertyCashflowController {
    private final AdminPropertyCashflowService service;public AdminPropertyCashflowController(AdminPropertyCashflowService service){this.service=service;}
    @GetMapping public List<AdminPropertyCashflowResponse> list(@PathVariable Long ownerId,@PathVariable Long ownerUnitId){return service.list(ownerId,ownerUnitId);}
    @PostMapping(consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public AdminPropertyCashflowResponse create(@PathVariable Long ownerId,@PathVariable Long ownerUnitId,@RequestParam String direction,@RequestParam String category,@RequestParam String description,@RequestParam BigDecimal amount,@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate occurredOn,@RequestParam String paymentMethod,@RequestParam String confirmationStatus,@RequestPart(value="file",required=false)MultipartFile file,HttpServletRequest request){return service.create(AuthInterceptor.userId(request),ownerId,ownerUnitId,direction,category,description,amount,occurredOn,paymentMethod,confirmationStatus,file);}
    @PutMapping(value="/{cashflowId}",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public AdminPropertyCashflowResponse update(@PathVariable Long ownerId,@PathVariable Long ownerUnitId,@PathVariable Long cashflowId,@RequestParam String direction,@RequestParam String category,@RequestParam String description,@RequestParam BigDecimal amount,@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate occurredOn,@RequestParam String paymentMethod,@RequestParam String confirmationStatus,@RequestPart(value="file",required=false)MultipartFile file,HttpServletRequest request){return service.update(AuthInterceptor.userId(request),ownerId,ownerUnitId,cashflowId,direction,category,description,amount,occurredOn,paymentMethod,confirmationStatus,file);}
    @DeleteMapping("/{cashflowId}") public ResponseEntity<Void> delete(@PathVariable Long ownerId,@PathVariable Long ownerUnitId,@PathVariable Long cashflowId,HttpServletRequest request){service.delete(AuthInterceptor.userId(request),ownerId,ownerUnitId,cashflowId);return ResponseEntity.noContent().build();}
    @GetMapping("/{cashflowId}/proof") public ResponseEntity<InputStreamResource> download(@PathVariable Long ownerId,@PathVariable Long ownerUnitId,@PathVariable Long cashflowId)throws IOException{AdminPropertyCashflowService.Download download=service.download(ownerId,ownerUnitId,cashflowId);return ResponseEntity.ok().contentType(MediaType.parseMediaType(download.mimeType())).contentLength(download.size()).header(HttpHeaders.CONTENT_DISPOSITION,ContentDisposition.attachment().filename(download.originalName()).build().toString()).body(new InputStreamResource(Files.newInputStream(download.path())));}
}
