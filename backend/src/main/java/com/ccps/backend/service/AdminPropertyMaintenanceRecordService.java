package com.ccps.backend.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.ccps.backend.dto.AdminPropertyMaintenanceRecordRequest;
import com.ccps.backend.dto.AdminPropertyMaintenanceRecordResponse;
import com.ccps.backend.mapper.AdminMaintenanceMapper;
import com.ccps.backend.mapper.AdminPropertyMaintenanceRecordMapper;
import com.ccps.backend.mapper.AdminPropertyMaintenanceRecordMapper.NewRecord;

@Service
public class AdminPropertyMaintenanceRecordService {
    private final AdminPropertyMaintenanceRecordMapper mapper;
    private final AdminMaintenanceMapper auditMapper;

    public AdminPropertyMaintenanceRecordService(AdminPropertyMaintenanceRecordMapper mapper,
            AdminMaintenanceMapper auditMapper) {
        this.mapper = mapper;
        this.auditMapper = auditMapper;
    }

    @Transactional(readOnly = true)
    public List<AdminPropertyMaintenanceRecordResponse> list(Long ownerId, Long ownerUnitId) {
        requireProperty(ownerId, ownerUnitId);
        return mapper.findAll(ownerUnitId);
    }

    @Transactional
    public AdminPropertyMaintenanceRecordResponse create(Long actorId, Long ownerId, Long ownerUnitId,
            AdminPropertyMaintenanceRecordRequest request) {
        requireProperty(ownerId, ownerUnitId);
        NewRecord record = values(null, ownerUnitId, request);
        record.setRecordNo("PMR-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase());
        record.setActorId(actorId);
        mapper.insert(record);
        auditMapper.insertCreateAudit(actorId, "create_maintenance_record", "property_maintenance_record",
                record.getId(), "{\"recordNo\":\"" + record.getRecordNo() + "\"}");
        return find(ownerUnitId, record.getId());
    }

    @Transactional
    public AdminPropertyMaintenanceRecordResponse update(Long actorId, Long ownerId, Long ownerUnitId, Long id,
            AdminPropertyMaintenanceRecordRequest request) {
        requireProperty(ownerId, ownerUnitId); find(ownerUnitId, id);
        NewRecord record = values(id, ownerUnitId, request);
        if (mapper.update(record) != 1) throw new ResponseStatusException(HttpStatus.CONFLICT, "Record changed");
        auditMapper.insertCreateAudit(actorId, "update_maintenance_record", "property_maintenance_record", id, null);
        return find(ownerUnitId, id);
    }

    @Transactional
    public void delete(Long actorId, Long ownerId, Long ownerUnitId, Long id) {
        requireProperty(ownerId, ownerUnitId); find(ownerUnitId, id);
        if (mapper.delete(ownerUnitId, id) != 1) throw new ResponseStatusException(HttpStatus.CONFLICT, "Record changed");
        auditMapper.insertCreateAudit(actorId, "delete_maintenance_record", "property_maintenance_record", id, null);
    }

    @Transactional
    public void syncCompletedWorkOrder(Long actorId, Long workOrderId, String resultSummary) {
        mapper.upsertFromWorkOrder(workOrderId, resultSummary, actorId);
    }

    private void requireProperty(Long ownerId, Long ownerUnitId) {
        if (ownerId == null || ownerUnitId == null || mapper.countProperty(ownerId, ownerUnitId) != 1) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Property not found");
        }
    }

    private AdminPropertyMaintenanceRecordResponse find(Long ownerUnitId, Long id) {
        AdminPropertyMaintenanceRecordResponse result = mapper.findById(ownerUnitId, id);
        if (result == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Maintenance record not found");
        return result;
    }

    private NewRecord values(Long id, Long ownerUnitId, AdminPropertyMaintenanceRecordRequest request) {
        NewRecord record = new NewRecord(); record.setId(id); record.setOwnerUnitId(ownerUnitId);
        record.setCategory(request.category()); record.setTitle(request.title());
        record.setMaintenanceDate(request.maintenanceDate()); record.setDurationMinutes(request.durationMinutes());
        record.setDetails(request.details()); record.setResultSummary(request.resultSummary());
        record.setNextMaintenanceDate(request.nextMaintenanceDate()); return record;
    }
}
