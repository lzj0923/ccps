package com.ccps.backend.mapper;

import org.apache.ibatis.annotations.*;
import com.ccps.backend.dto.OwnerAccountResponse;

@Mapper
public interface OwnerAccountMapper {
    @Select("SELECT full_name, mobile_phone, home_phone, office_phone, mailing_address FROM owners WHERE user_id=#{userId} AND status='active'")
    OwnerAccountResponse find(@Param("userId") Long userId);

    @Update("""
        UPDATE owners SET mobile_phone=#{contact.mobilePhone}, home_phone=#{contact.homePhone},
          office_phone=#{contact.officePhone}, mailing_address=#{contact.mailingAddress}
        WHERE user_id=#{userId} AND status='active'
        """)
    int updateContact(@Param("userId") Long userId, @Param("contact") OwnerAccountResponse contact);

    @Select("SELECT password_hash FROM users WHERE id=#{userId} AND account_type='OWNER' AND status='active'")
    String passwordHash(@Param("userId") Long userId);

    @Update("UPDATE users SET password_hash=#{newHash} WHERE id=#{userId} AND account_type='OWNER' AND status='active' AND password_hash=#{oldHash}")
    int changePassword(@Param("userId") Long userId, @Param("oldHash") String oldHash, @Param("newHash") String newHash);
}
