package com.ccps.backend.mapper;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface AdminPropertyBankAccountMapper {
    String COLUMNS="a.id,a.owner_unit_id AS ownerUnitId,a.item_name AS itemName,a.payment_name AS paymentName,"
            +"a.account_no AS accountNo,a.remarks,a.created_by AS createdBy,"
            +"COALESCE(u.display_name,u.username,'系統') AS createdByName,a.created_at AS createdAt,a.updated_at AS updatedAt";
    String JOINS=" FROM property_bank_accounts a LEFT JOIN users u ON u.id=a.created_by ";

    @Select("SELECT COUNT(*) FROM owner_units WHERE id=#{ownerUnitId} AND owner_id=#{ownerId} AND status='active'")
    int ownsProperty(@Param("ownerId") Long ownerId,@Param("ownerUnitId") Long ownerUnitId);
    @Select("SELECT "+COLUMNS+JOINS+"WHERE a.owner_unit_id=#{ownerUnitId} ORDER BY a.id DESC")
    List<AccountRow> list(@Param("ownerUnitId") Long ownerUnitId);
    @Select("SELECT "+COLUMNS+JOINS+"WHERE a.owner_unit_id=#{ownerUnitId} AND a.id=#{accountId}")
    AccountRow find(@Param("ownerUnitId") Long ownerUnitId,@Param("accountId") Long accountId);
    @Insert("INSERT INTO property_bank_accounts (owner_unit_id,item_name,payment_name,account_no,remarks,created_by) "
            +"VALUES (#{ownerUnitId},#{itemName},#{paymentName},#{accountNo},#{remarks},#{createdBy})")
    @Options(useGeneratedKeys=true,keyProperty="id") int insert(AccountRow row);
    @Update("UPDATE property_bank_accounts SET item_name=#{itemName},payment_name=#{paymentName},account_no=#{accountNo},remarks=#{remarks} "
            +"WHERE id=#{id} AND owner_unit_id=#{ownerUnitId}") int update(AccountRow row);
    @Delete("DELETE FROM property_bank_accounts WHERE id=#{accountId} AND owner_unit_id=#{ownerUnitId}")
    int delete(@Param("ownerUnitId") Long ownerUnitId,@Param("accountId") Long accountId);

    class AccountRow {
        private Long id,ownerUnitId,createdBy; private String itemName,paymentName,accountNo,remarks,createdByName;
        private LocalDateTime createdAt,updatedAt;
        public Long getId(){return id;} public void setId(Long value){id=value;}
        public Long getOwnerUnitId(){return ownerUnitId;} public void setOwnerUnitId(Long value){ownerUnitId=value;}
        public Long getCreatedBy(){return createdBy;} public void setCreatedBy(Long value){createdBy=value;}
        public String getItemName(){return itemName;} public void setItemName(String value){itemName=value;}
        public String getPaymentName(){return paymentName;} public void setPaymentName(String value){paymentName=value;}
        public String getAccountNo(){return accountNo;} public void setAccountNo(String value){accountNo=value;}
        public String getRemarks(){return remarks;} public void setRemarks(String value){remarks=value;}
        public String getCreatedByName(){return createdByName;} public void setCreatedByName(String value){createdByName=value;}
        public LocalDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(LocalDateTime value){createdAt=value;}
        public LocalDateTime getUpdatedAt(){return updatedAt;} public void setUpdatedAt(LocalDateTime value){updatedAt=value;}
    }
}
