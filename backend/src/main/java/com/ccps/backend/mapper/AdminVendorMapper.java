package com.ccps.backend.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.ccps.backend.dto.AdminVendorResponse;

@Mapper
public interface AdminVendorMapper {
    @Select("SELECT id, vendor_code, name, contact_name, phone, email, status FROM vendors ORDER BY status = 'active' DESC, name")
    List<AdminVendorResponse> findAll();

    @Select("SELECT id, vendor_code, name, contact_name, phone, email, status FROM vendors WHERE id = #{id} LIMIT 1")
    AdminVendorResponse findById(@Param("id") Long id);

    @Insert("""
            INSERT INTO vendors (vendor_code, name, contact_name, phone, email, status)
            VALUES (#{vendorCode}, #{name}, #{contactName}, #{phone}, #{email}, #{status})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(NewVendor vendor);

    @Update("""
            UPDATE vendors
            SET name = #{name}, contact_name = #{contactName}, phone = #{phone},
                email = #{email}, status = #{status}
            WHERE id = #{id}
            """)
    int update(NewVendor vendor);

    @Update("UPDATE vendors SET status = #{status} WHERE id = #{id}")
    int setStatus(@Param("id") Long id, @Param("status") String status);

    class NewVendor {
        private Long id;
        private String vendorCode;
        private String name;
        private String contactName;
        private String phone;
        private String email;
        private String status;

        public Long getId() { return id; }
        public void setId(Long value) { id = value; }
        public String getVendorCode() { return vendorCode; }
        public void setVendorCode(String value) { vendorCode = value; }
        public String getName() { return name; }
        public void setName(String value) { name = value; }
        public String getContactName() { return contactName; }
        public void setContactName(String value) { contactName = value; }
        public String getPhone() { return phone; }
        public void setPhone(String value) { phone = value; }
        public String getEmail() { return email; }
        public void setEmail(String value) { email = value; }
        public String getStatus() { return status; }
        public void setStatus(String value) { status = value; }
    }
}
