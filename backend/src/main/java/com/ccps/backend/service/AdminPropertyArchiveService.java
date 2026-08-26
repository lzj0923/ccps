package com.ccps.backend.service;

import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.ccps.backend.dto.AdminOffMarketPropertyPageResponse;
import com.ccps.backend.dto.AdminPropertyManagementHistoryResponse;
import com.ccps.backend.mapper.AdminPropertyArchiveMapper;
import com.ccps.backend.mapper.AdminPropertyArchiveMapper.UnitState;

@Service
public class AdminPropertyArchiveService {
    private static final Set<String> REASONS=Set.of("OWNER_REQUEST","TEMPORARY_HOLD","RENOVATION","SOLD_OR_TRANSFERRED","DATA_CLEANUP","OTHER");
    private final AdminPropertyArchiveMapper mapper;
    private final AdminAuditService auditService;
    public AdminPropertyArchiveService(AdminPropertyArchiveMapper mapper,AdminAuditService auditService){this.mapper=mapper;this.auditService=auditService;}

    @Transactional(readOnly=true)
    public AdminOffMarketPropertyPageResponse list(int requestedPage,int requestedPageSize,String keyword,String reasonCode){
        int size=Math.max(1,Math.min(requestedPageSize,100)); String search=normalize(keyword); String reason=normalize(reasonCode);
        long total=mapper.count(search,reason); int pages=Math.max(1,(int)Math.ceil((double)total/size)); int page=Math.max(1,Math.min(requestedPage,pages));
        return new AdminOffMarketPropertyPageResponse(mapper.findPage(search,reason,size,(page-1)*size),
                new AdminOffMarketPropertyPageResponse.Page(total,page,size,pages),
                new AdminOffMarketPropertyPageResponse.Summary(mapper.count(null,null),mapper.countThisMonth(),mapper.countWithDocuments(),mapper.countCanRelist()));
    }

    @Transactional
    public void offMarket(Long unitId,String reasonCode,String note,Long actorId){
        String reason=normalize(reasonCode); if(!REASONS.contains(reason)) throw bad("Invalid off-market reason");
        UnitState current=require(unitId); if("off_market".equals(current.getRentalListingStatus())) throw conflict("Rental listing is already off market");
        if(mapper.countActiveRentalServices(unitId)==0) throw conflict("Property has no active rental service");
        String normalizedNote=normalize(note); if(mapper.markOffMarket(unitId,reason,normalizedNote,actorId)!=1) throw conflict("Property status changed; reload and try again");
        mapper.insertHistory(unitId,"off_market",reason,normalizedNote,actorId);
        auditService.record(actorId,"RENTAL_LISTING_OFF_MARKET","unit",unitId,"{\"rentalListingStatus\":\"listed\"}","{\"rentalListingStatus\":\"off_market\",\"reasonCode\":\""+reason+"\"}");
    }

    @Transactional
    public void relist(Long unitId,String note,Long actorId){
        UnitState current=require(unitId); if(!"off_market".equals(current.getRentalListingStatus())) throw conflict("Rental listing is not off market");
        if(mapper.countRelistableOwnerships(unitId)==0) throw conflict("Property has no active owner or has been disposed");
        if(mapper.countActiveRentalServices(unitId)==0) throw conflict("Property has no active rental service");
        if(mapper.countActiveLeases(unitId)>0) throw conflict("Occupied rental listing cannot be relisted");
        String normalizedNote=normalize(note); if(mapper.relist(unitId)!=1) throw conflict("Property status changed; reload and try again");
        mapper.insertHistory(unitId,"relisted",null,normalizedNote,actorId);
        auditService.record(actorId,"RENTAL_LISTING_RELISTED","unit",unitId,"{\"rentalListingStatus\":\"off_market\"}","{\"rentalListingStatus\":\"listed\"}");
    }

    @Transactional(readOnly=true)
    public List<AdminPropertyManagementHistoryResponse> history(Long unitId){if(mapper.findUnit(unitId)==null)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Property not found");return mapper.history(unitId);}
    private UnitState require(Long id){UnitState row=mapper.lockUnit(id);if(row==null)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Property not found");return row;}
    private String normalize(String value){return value==null||value.isBlank()?null:value.trim();}
    private ResponseStatusException bad(String value){return new ResponseStatusException(HttpStatus.BAD_REQUEST,value);}
    private ResponseStatusException conflict(String value){return new ResponseStatusException(HttpStatus.CONFLICT,value);}
}
