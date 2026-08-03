package com.ccps.backend.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.List;

import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.ByteArrayResource;
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
import com.ccps.backend.dto.AdminPropertyHandoverReportResponse;
import com.ccps.backend.service.AdminPropertyHandoverReportService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/admin/owners/{ownerId}/properties/{ownerUnitId}/handover-reports")
public class AdminPropertyHandoverReportController {
    private final AdminPropertyHandoverReportService service;
    public AdminPropertyHandoverReportController(AdminPropertyHandoverReportService service){this.service=service;}

    @GetMapping
    public List<AdminPropertyHandoverReportResponse> list(@PathVariable Long ownerId,@PathVariable Long ownerUnitId){return service.list(ownerId,ownerUnitId);}

    @PostMapping(consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public AdminPropertyHandoverReportResponse create(@PathVariable Long ownerId,@PathVariable Long ownerUnitId,
            @RequestParam String title,@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate reportDate,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate trackingStartDate,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate trackingEndDate,
            @RequestParam(required=false) String remarks,@RequestParam(defaultValue="false") boolean completed,
            @RequestParam(required=false) String contentJson,@RequestParam(required=false) String photoMeta,
            @RequestPart(value="file",required=false) MultipartFile file,@RequestPart(value="photos",required=false) List<MultipartFile> photos,HttpServletRequest request){
        return service.create(AuthInterceptor.userId(request),ownerId,ownerUnitId,title,reportDate,trackingStartDate,trackingEndDate,remarks,contentJson,completed,file,photos,photoMeta);
    }

    @PutMapping(value="/{reportId}",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public AdminPropertyHandoverReportResponse update(@PathVariable Long ownerId,@PathVariable Long ownerUnitId,@PathVariable Long reportId,
            @RequestParam String title,@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate reportDate,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate trackingStartDate,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate trackingEndDate,
            @RequestParam(required=false) String remarks,@RequestParam(defaultValue="false") boolean completed,
            @RequestParam(required=false) String contentJson,@RequestParam(required=false) String photoMeta,
            @RequestPart(value="file",required=false) MultipartFile file,@RequestPart(value="photos",required=false) List<MultipartFile> photos){
        return service.update(ownerId,ownerUnitId,reportId,title,reportDate,trackingStartDate,trackingEndDate,remarks,contentJson,completed,file,photos,photoMeta);
    }

    /**
     * Multipart form uploads are not parsed consistently for PUT by all
     * servlet containers. Keep PUT for backwards compatibility and expose the
     * same update operation through POST for the web client.
     */
    @PostMapping(value="/{reportId}",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public AdminPropertyHandoverReportResponse updatePost(@PathVariable Long ownerId,@PathVariable Long ownerUnitId,@PathVariable Long reportId,
            @RequestParam String title,@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate reportDate,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate trackingStartDate,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate trackingEndDate,
            @RequestParam(required=false) String remarks,@RequestParam(defaultValue="false") boolean completed,
            @RequestParam(required=false) String contentJson,@RequestParam(required=false) String photoMeta,
            @RequestPart(value="file",required=false) MultipartFile file,@RequestPart(value="photos",required=false) List<MultipartFile> photos){
        return service.update(ownerId,ownerUnitId,reportId,title,reportDate,trackingStartDate,trackingEndDate,remarks,contentJson,completed,file,photos,photoMeta);
    }

    @DeleteMapping("/{reportId}")
    public ResponseEntity<Void> delete(@PathVariable Long ownerId,@PathVariable Long ownerUnitId,@PathVariable Long reportId){service.delete(ownerId,ownerUnitId,reportId);return ResponseEntity.noContent().build();}

    @GetMapping("/{reportId}/file")
    public ResponseEntity<?> download(@PathVariable Long ownerId,@PathVariable Long ownerUnitId,@PathVariable Long reportId)throws IOException{
        AdminPropertyHandoverReportService.Download download=service.download(ownerId,ownerUnitId,reportId);
        var headers=ResponseEntity.ok().contentType(MediaType.parseMediaType(download.mimeType())).contentLength(download.size())
                .header(HttpHeaders.CONTENT_DISPOSITION,ContentDisposition.attachment().filename(download.originalName()).build().toString());
        if(download.bytes()!=null)return headers.body(new ByteArrayResource(download.bytes()));
        return headers.body(new InputStreamResource(Files.newInputStream(download.path())));
    }
}
