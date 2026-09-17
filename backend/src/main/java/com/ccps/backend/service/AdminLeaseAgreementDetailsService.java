package com.ccps.backend.service;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ccps.backend.mapper.AdminLeaseAgreementDetailsMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class AdminLeaseAgreementDetailsService {
    private final AdminLeaseAgreementDetailsMapper mapper;
    private final ObjectMapper objectMapper;

    public AdminLeaseAgreementDetailsService(AdminLeaseAgreementDetailsMapper mapper, ObjectMapper objectMapper) {
        this.mapper = mapper; this.objectMapper = objectMapper;
    }

    public Map<String, String> find(Long leaseId) {
        requireLease(leaseId);
        String json = mapper.findDetails(leaseId);
        if (json == null || json.isBlank()) return Map.of();
        try { return objectMapper.readValue(json, new TypeReference<LinkedHashMap<String, String>>() { }); }
        catch (Exception exception) { throw new IllegalStateException("租赁合同补充资料无法读取", exception); }
    }

    @Transactional
    public Map<String, String> save(Long leaseId, Map<String, String> fields) {
        requireLease(leaseId);
        Map<String, String> normalized = new LinkedHashMap<>();
        if (fields != null) fields.forEach((key, value) -> {
            if (key != null && !key.isBlank()) normalized.put(key.trim(), value == null ? "" : value.trim());
        });
        try { mapper.saveDetails(leaseId, objectMapper.writeValueAsString(normalized)); return normalized; }
        catch (Exception exception) { throw new IllegalStateException("租赁合同补充资料无法保存", exception); }
    }

    private void requireLease(Long leaseId) {
        if (leaseId == null || mapper.countLease(leaseId) == 0) throw new IllegalArgumentException("租约不存在");
    }
}
