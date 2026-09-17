package com.ccps.backend.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface AdminLeaseAgreementDetailsMapper {
    @Select("SELECT COUNT(*) FROM leases WHERE id=#{leaseId}")
    int countLease(@Param("leaseId") Long leaseId);

    @Select("SELECT agreement_details FROM leases WHERE id=#{leaseId}")
    String findDetails(@Param("leaseId") Long leaseId);

    @Update("UPDATE leases SET agreement_details=#{details} WHERE id=#{leaseId}")
    int saveDetails(@Param("leaseId") Long leaseId, @Param("details") String details);
}
