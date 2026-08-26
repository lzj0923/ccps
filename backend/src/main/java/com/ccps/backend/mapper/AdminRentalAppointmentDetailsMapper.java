package com.ccps.backend.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface AdminRentalAppointmentDetailsMapper {
    @Select("SELECT COUNT(*) FROM rental_mandates WHERE id = #{mandateId}")
    int countMandate(@Param("mandateId") Long mandateId);

    @Select("SELECT rental_appointment_details FROM rental_mandates WHERE id = #{mandateId}")
    String findDetails(@Param("mandateId") Long mandateId);

    @Update("UPDATE rental_mandates SET rental_appointment_details = #{details} WHERE id = #{mandateId}")
    int saveDetails(@Param("mandateId") Long mandateId, @Param("details") String details);
}
