package com.ccps.backend.service;

import com.ccps.backend.dto.AdminPropertyHandoverRequest;
import com.ccps.backend.dto.AdminPropertyHandoverResponse;
import com.ccps.backend.mapper.AdminPropertyHandoverMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Service
public class AdminPropertyHandoverService {
    private final AdminPropertyHandoverMapper mapper;
    public AdminPropertyHandoverService(AdminPropertyHandoverMapper mapper) { this.mapper = mapper; }

    public AdminPropertyHandoverResponse find(Long mandateId) {
        return mapper.findByMandateId(mandateId);
    }

    @Transactional
    public AdminPropertyHandoverResponse save(Long mandateId, AdminPropertyHandoverRequest request, Long actorId) {
        if (mapper.countEligibleMandate(mandateId) == 0) throw new ResponseStatusException(HttpStatus.CONFLICT, "只有啟用或暫停中的委託可以建立接管記錄");
        if (request.handoverDate() == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "交接日期不可為空");
        AdminPropertyHandoverMapper.HandoverRecord record = new AdminPropertyHandoverMapper.HandoverRecord();
        record.setMandateId(mandateId); record.setHandoverDate(request.handoverDate()); record.setConditionSummary(request.conditionSummary());
        record.setKeyCount(request.keyCount() == null ? 0 : request.keyCount()); record.setAccessCardCount(request.accessCardCount() == null ? 0 : request.accessCardCount());
        record.setWaterMeter(request.waterMeter()); record.setElectricityMeter(request.electricityMeter()); record.setInventory(request.inventory() == null ? "{}" : request.inventory());
        record.setReceivedBy(request.receivedBy()); record.setNotes(request.notes()); record.setStatus(Boolean.TRUE.equals(request.completed()) ? "completed" : "draft"); record.setCreatedBy(actorId); record.setCompletedBy(Boolean.TRUE.equals(request.completed()) ? actorId : null);
        mapper.upsert(record); AdminPropertyHandoverResponse saved = mapper.findByMandateId(mandateId);
        mapper.insertAudit(actorId, record.getStatus().equals("completed") ? "complete_handover" : "save_handover", saved == null ? record.getId() : saved.id(), "{\"mandateId\":" + mandateId + ",\"status\":\"" + record.getStatus() + "\"}");
        return saved;
    }
}
