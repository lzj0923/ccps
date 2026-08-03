package com.ccps.backend.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.ccps.backend.dto.AdminPropertyOwnershipRequest;
import com.ccps.backend.dto.AdminPropertyOwnershipResponse;
import com.ccps.backend.mapper.AdminPropertyOwnershipMapper;
import com.ccps.backend.mapper.AdminPropertyOwnershipMapper.NewOwnership;
import com.ccps.backend.mapper.AdminPropertyOwnershipMapper.Row;

@Service
public class AdminPropertyOwnershipService {
    private final AdminPropertyOwnershipMapper mapper;
    public AdminPropertyOwnershipService(AdminPropertyOwnershipMapper mapper) { this.mapper=mapper; }

    @Transactional(readOnly=true)
    public List<AdminPropertyOwnershipResponse> list(Long unitId) {
        requireUnit(unitId);
        return mapper.list(unitId).stream().map(this::response).toList();
    }

    @Transactional
    public AdminPropertyOwnershipResponse create(Long unitId, AdminPropertyOwnershipRequest request) {
        requireUnit(unitId); requireOwner(request.ownerId()); validateDates(request);
        Row existing=mapper.findByOwner(unitId,request.ownerId());
        if(existing!=null && "active".equals(existing.getStatus())) throw conflict("This owner already holds the property");
        Long excludeId=existing==null?0L:existing.getOwnershipId();
        validateTotal(unitId,excludeId,request.ownershipPercent());
        boolean primary=request.primary()||mapper.activeCount(unitId)==0;
        if(primary) mapper.clearPrimary(unitId);
        Long id;
        if(existing!=null){
            if(mapper.reactivate(unitId,existing.getOwnershipId(),request.ownerId(),request.ownershipPercent(),primary,request.startDate(),request.endDate())!=1) throw conflict("Unable to restore ownership");
            id=existing.getOwnershipId();
        }else{
            NewOwnership row=new NewOwnership(); row.setOwnerId(request.ownerId()); row.setUnitId(unitId);
            row.setOwnershipPercent(request.ownershipPercent()); row.setPrimary(primary);
            row.setStartDate(request.startDate()); row.setEndDate(request.endDate());
            if(mapper.insert(row)!=1||row.getOwnershipId()==null) throw conflict("Unable to create ownership");
            id=row.getOwnershipId();
        }
        ensurePrimary(unitId);
        return response(require(unitId,id));
    }

    @Transactional
    public AdminPropertyOwnershipResponse update(Long unitId,Long ownershipId,AdminPropertyOwnershipRequest request){
        requireUnit(unitId); Row current=require(unitId,ownershipId); validateDates(request);
        if(!current.getOwnerId().equals(request.ownerId())) throw bad("Owner cannot be replaced; create another ownership instead");
        validateTotal(unitId,ownershipId,request.ownershipPercent());
        if(request.primary()) mapper.clearPrimary(unitId);
        if(mapper.update(unitId,ownershipId,request.ownershipPercent(),request.primary(),request.startDate(),request.endDate())!=1) throw conflict("Ownership was changed by another request");
        ensurePrimary(unitId);
        return response(require(unitId,ownershipId));
    }

    @Transactional
    public void delete(Long unitId,Long ownershipId){
        requireUnit(unitId); Row current=require(unitId,ownershipId);
        if(!"active".equals(current.getStatus())) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Active ownership not found");
        if(mapper.activeCount(unitId)<=1) throw conflict("The last active owner cannot be removed");
        if(mapper.deactivate(unitId,ownershipId)!=1) throw conflict("Ownership was changed by another request");
        ensurePrimary(unitId);
    }

    private void validateTotal(Long unitId,Long excludeId,BigDecimal percent){
        BigDecimal total=mapper.activePercentExcluding(unitId,excludeId).add(percent);
        if(total.compareTo(new BigDecimal("100.00"))>0) throw bad("Total active ownership cannot exceed 100%");
    }
    private void validateDates(AdminPropertyOwnershipRequest request){
        if(request.startDate()!=null&&request.endDate()!=null&&request.endDate().isBefore(request.startDate())) throw bad("End date cannot be before start date");
    }
    private void ensurePrimary(Long unitId){if(mapper.primaryCount(unitId)==0)mapper.assignFirstPrimary(unitId);}
    private void requireUnit(Long unitId){if(mapper.unitExists(unitId)!=1)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Property unit not found");}
    private void requireOwner(Long ownerId){if(mapper.activeOwnerExists(ownerId)!=1)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Active owner not found");}
    private Row require(Long unitId,Long id){Row row=mapper.find(unitId,id);if(row==null)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Ownership not found");return row;}
    private AdminPropertyOwnershipResponse response(Row row){return new AdminPropertyOwnershipResponse(row.getOwnershipId(),row.getUnitId(),row.getOwnerId(),row.getOwnerNo(),row.getOwnerName(),row.getIdentityNo(),row.getMobilePhone(),row.getEmail(),row.getOwnershipPercent(),row.isPrimaryFlag(),row.getStartDate(),row.getEndDate(),row.getStatus());}
    private ResponseStatusException bad(String value){return new ResponseStatusException(HttpStatus.BAD_REQUEST,value);}
    private ResponseStatusException conflict(String value){return new ResponseStatusException(HttpStatus.CONFLICT,value);}
}
