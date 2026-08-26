package com.ccps.backend.service;

import java.util.List;
import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.ccps.backend.dto.AdminRentalModeRequest;
import com.ccps.backend.dto.AdminRentalSpaceRequest;
import com.ccps.backend.dto.AdminRentalSpaceResponse;
import com.ccps.backend.mapper.AdminRentalSpaceMapper;
import com.ccps.backend.mapper.AdminRentalSpaceMapper.Room;

@Service
public class AdminRentalSpaceService {
    private final AdminRentalSpaceMapper mapper;
    public AdminRentalSpaceService(AdminRentalSpaceMapper mapper){this.mapper=mapper;}

    @Transactional
    public List<AdminRentalSpaceResponse> find(Long unitId){
        requireUnit(unitId);
        mapper.ensureWholeUnitSpace(unitId);
        return mapper.findByUnit(unitId);
    }

    @Transactional
    public Long create(Long unitId,AdminRentalSpaceRequest request){
        requireUnit(unitId);
        if(mapper.countOpenLeasesByType(unitId,"whole_unit")>0)
            throw new ResponseStatusException(HttpStatus.CONFLICT,"整套租约尚未结束，不能新增合租房间");
        String code=normalizeCode(request.spaceCode());
        if(mapper.countCode(unitId,code,null)>0) throw new ResponseStatusException(HttpStatus.CONFLICT,"出租空间编号已存在");
        Room room=room(null,unitId,request,code);
        if(mapper.insert(room)!=1||room.getId()==null) throw new ResponseStatusException(HttpStatus.CONFLICT,"新增出租空间失败");
        mapper.updateMode(unitId,"shared");
        return room.getId();
    }

    @Transactional
    public void update(Long unitId,Long spaceId,AdminRentalSpaceRequest request){
        requireUnit(unitId); String code=normalizeCode(request.spaceCode());
        if(mapper.countCode(unitId,code,spaceId)>0) throw new ResponseStatusException(HttpStatus.CONFLICT,"出租空间编号已存在");
        if("disabled".equals(request.status())&&mapper.countOpenLeases(spaceId)>0)
            throw new ResponseStatusException(HttpStatus.CONFLICT,"该空间仍有生效租约，不能停用");
        if(mapper.update(room(spaceId,unitId,request,code))!=1) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"出租空间不存在");
    }

    @Transactional
    public void disable(Long unitId,Long spaceId){
        if(mapper.countOpenLeases(spaceId)>0) throw new ResponseStatusException(HttpStatus.CONFLICT,"该空间仍有生效租约，不能停用");
        if(mapper.disable(unitId,spaceId)!=1) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"出租空间不存在");
    }

    @Transactional
    public void changeMode(Long unitId,AdminRentalModeRequest request){
        requireUnit(unitId);
        if("shared".equals(request.rentalMode())&&mapper.countOpenLeasesByType(unitId,"whole_unit")>0)
            throw new ResponseStatusException(HttpStatus.CONFLICT,"请先结束整套租约，再启用合租");
        if("whole_unit".equals(request.rentalMode())&&mapper.countOpenLeasesByType(unitId,"room")>0)
            throw new ResponseStatusException(HttpStatus.CONFLICT,"仍有房间租约，不能切换为整租");
        mapper.updateMode(unitId,request.rentalMode());
        if("whole_unit".equals(request.rentalMode())) mapper.ensureWholeUnitSpace(unitId);
    }

    private void requireUnit(Long unitId){if(mapper.countUnit(unitId)!=1)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"房产不存在");}
    private String normalizeCode(String value){return value.trim().toUpperCase(Locale.ROOT);}
    private Room room(Long id,Long unitId,AdminRentalSpaceRequest r,String code){
        Room x=new Room();x.setId(id);x.setUnitId(unitId);x.setSpaceCode(code);x.setSpaceName(r.spaceName().trim());
        x.setCapacity(r.capacity()==null?1:r.capacity());x.setAreaSqm(r.areaSqm());x.setRecommendedRent(r.recommendedRent());
        x.setStatus(r.status()==null?"active":r.status());return x;
    }
}
