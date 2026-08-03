package com.ccps.backend.service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.ccps.backend.dto.AdminVendorRequest;
import com.ccps.backend.dto.AdminVendorResponse;
import com.ccps.backend.mapper.AdminVendorMapper;
import com.ccps.backend.mapper.AdminVendorMapper.NewVendor;

@Service
public class AdminVendorService {
    private final AdminVendorMapper mapper;
    private final AdminAuditService auditService;

    @Autowired
    public AdminVendorService(AdminVendorMapper mapper, AdminAuditService auditService) {
        this.mapper = mapper;
        this.auditService = auditService;
    }

    AdminVendorService(AdminVendorMapper mapper) { this(mapper, null); }

    @Transactional(readOnly = true)
    public List<AdminVendorResponse> list() { return mapper.findAll(); }

    @Transactional
    public AdminVendorResponse create(Long actorId, AdminVendorRequest request) {
        validateName(request);
        NewVendor vendor = fromRequest(request);
        vendor.setVendorCode("VND-" + LocalDate.now().toString().replace("-", "") + "-"
                + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase());
        vendor.setStatus("active");
        mapper.insert(vendor);
        AdminVendorResponse created = mapper.findById(vendor.getId());
        audit(actorId, "create_vendor", null, created);
        return created;
    }

    @Transactional
    public AdminVendorResponse update(Long actorId, Long id, AdminVendorRequest request) {
        validateName(request);
        AdminVendorResponse before = ensureExists(id);
        NewVendor vendor = fromRequest(request);
        vendor.setId(id);
        vendor.setStatus("inactive".equalsIgnoreCase(request.status()) ? "inactive" : "active");
        mapper.update(vendor);
        AdminVendorResponse updated = mapper.findById(id);
        audit(actorId, "update_vendor", before, updated);
        return updated;
    }

    @Transactional
    public void deactivate(Long actorId, Long id) {
        AdminVendorResponse before = ensureExists(id);
        mapper.setStatus(id, "inactive");
        audit(actorId, "deactivate_vendor", before, new AdminVendorResponse(before.id(), before.vendorCode(),
                before.name(), before.contactName(), before.phone(), before.email(), "inactive"));
    }

    private NewVendor fromRequest(AdminVendorRequest request) {
        NewVendor vendor = new NewVendor();
        vendor.setName(request.name().trim());
        vendor.setContactName(trimToNull(request.contactName()));
        vendor.setPhone(trimToNull(request.phone()));
        vendor.setEmail(trimToNull(request.email()));
        return vendor;
    }

    private void validateName(AdminVendorRequest request) {
        if (request == null || request.name() == null || request.name().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vendor name is required");
        }
    }

    private AdminVendorResponse ensureExists(Long id) {
        AdminVendorResponse vendor = id == null ? null : mapper.findById(id);
        if (vendor == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Vendor not found");
        }
        return vendor;
    }

    private void audit(Long actorId, String action, AdminVendorResponse before, AdminVendorResponse after) {
        if (auditService != null && after != null) {
            auditService.record(actorId, action, "vendor", after.id(), snapshot(before), snapshot(after));
        }
    }

    private String snapshot(AdminVendorResponse vendor) {
        if (vendor == null) return null;
        return "{\"vendorCode\":\"" + escape(vendor.vendorCode()) + "\",\"name\":\"" + escape(vendor.name())
                + "\",\"contactName\":\"" + escape(vendor.contactName()) + "\",\"phone\":\""
                + escape(vendor.phone()) + "\",\"email\":\"" + escape(vendor.email()) + "\",\"status\":\""
                + escape(vendor.status()) + "\"}";
    }

    private String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
