package com.ccps.backend.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.ccps.backend.dto.OwnerPropertyDetailResponse;
import com.ccps.backend.mapper.OwnerPropertyDetailMapper;

@Service
public class OwnerPropertyDetailService {
    private final OwnerPropertyDetailMapper mapper;
    public OwnerPropertyDetailService(OwnerPropertyDetailMapper mapper) { this.mapper=mapper; }

    public OwnerPropertyDetailResponse get(Long userId, Long ownerUnitId) {
        var p=mapper.findProperty(userId,ownerUnitId);
        if(p==null) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Owner property not found");
        var bank=mapper.findLatestBank(ownerUnitId);
        return new OwnerPropertyDetailResponse(
                new OwnerPropertyDetailResponse.Property(p.getOwnerUnitId(),p.getProjectName(),p.getAddress(),p.getCountryCode(),p.getBuilding(),p.getFloorNo(),p.getUnitNo(),p.getAreaSqm()),
                bank==null?null:new OwnerPropertyDetailResponse.BankAccount(bank.getId(),bank.getItemName(),bank.getPaymentName(),mask(bank.getAccountNo()),bank.getBankAddress(),bank.getBranchCode(),bank.getSwiftCode(),Boolean.TRUE.equals(bank.getOverseasBank())),
                mapper.findMandates(ownerUnitId).stream().map(r->new OwnerPropertyDetailResponse.Mandate(r.getId(),r.getMandateNo(),r.getMandateType(),r.getStartDate(),r.getEndDate(),r.getStatus())).toList(),
                mapper.findLeases(ownerUnitId).stream().map(r->new OwnerPropertyDetailResponse.Lease(r.getId(),r.getLeaseNo(),r.getTenantName(),r.getMonthlyRent(),r.getDepositAmount(),r.getStartDate(),r.getEndDate(),r.getStatus(),r.getContractDocumentId()==null?null:mapper.findPreviewContractId(r.getContractDocumentId()))).toList(),
                mapper.findPhotos(ownerUnitId).stream().map(r->new OwnerPropertyDetailResponse.Photo(r.getId(),r.getLeaseId(),r.getRentalStage(),r.getVersionMonth(),r.getDocumentId(),r.getTitle(),r.getCategory(),r.getDescription(),Boolean.TRUE.equals(r.getCover()),r.getOriginalName(),r.getMimeType(),r.getCreatedAt())).toList());
    }

    static String mask(String value) {
        if(value==null||value.isBlank()) return "—";
        String compact=value.replaceAll("\\s+","");
        if(compact.length()<=4) return "••••"+compact;
        return "•••• •••• "+compact.substring(compact.length()-4);
    }
}
