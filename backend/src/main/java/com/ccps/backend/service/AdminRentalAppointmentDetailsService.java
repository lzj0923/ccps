package com.ccps.backend.service;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ccps.backend.mapper.AdminRentalAppointmentDetailsMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class AdminRentalAppointmentDetailsService {
    private final AdminRentalAppointmentDetailsMapper mapper;
    private final ObjectMapper objectMapper;

    public AdminRentalAppointmentDetailsService(AdminRentalAppointmentDetailsMapper mapper, ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }

    public Map<String, String> find(Long mandateId) {
        requireMandate(mandateId);
        String json = mapper.findDetails(mandateId);
        if (json == null || json.isBlank()) return Map.of();
        try {
            return objectMapper.readValue(json, new TypeReference<LinkedHashMap<String, String>>() { });
        } catch (Exception exception) {
            throw new IllegalStateException("租赁委任书补充资料无法读取", exception);
        }
    }

    @Transactional
    public Map<String, String> save(Long mandateId, Map<String, String> fields) {
        requireMandate(mandateId);
        Map<String, String> normalized = new LinkedHashMap<>();
        if (fields != null) fields.forEach((key, value) -> {
            if (key != null && !key.isBlank()) normalized.put(key.trim(), value == null ? "" : value.trim());
        });
        try {
            mapper.saveDetails(mandateId, objectMapper.writeValueAsString(normalized));
            return normalized;
        } catch (Exception exception) {
            throw new IllegalStateException("租赁委任书补充资料无法保存", exception);
        }
    }

    private void requireMandate(Long mandateId) {
        if (mandateId == null || mapper.countMandate(mandateId) == 0) throw new IllegalArgumentException("出租委托不存在");
    }
}
