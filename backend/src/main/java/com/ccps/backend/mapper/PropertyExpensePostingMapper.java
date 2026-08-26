package com.ccps.backend.mapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface PropertyExpensePostingMapper {
    @Select("SELECT ou.id AS ownerUnitId,ou.owner_id AS ownerId,ou.unit_id AS unitId FROM owner_units ou WHERE ou.id=#{ownerUnitId} AND ou.status='active'")
    PropertyContext findContext(@Param("ownerUnitId") Long ownerUnitId);

    @Select("SELECT owner_unit_id AS ownerUnitId,profile_json AS profileJson FROM property_basic_profiles")
    List<ProfileRow> findProfiles();

    @Select("""
            SELECT rm.id AS mandateId, rm.owner_unit_id AS ownerUnitId,
                   rm.management_fee AS managementFee, rm.commission_percent AS commissionPercent,
                   COALESCE(SUM(ri.amount_due), 0) AS rentBase
            FROM rental_mandates rm
            LEFT JOIN leases l ON l.rental_mandate_id = rm.id
            LEFT JOIN rent_invoices ri ON ri.lease_id = l.id AND ri.billing_month = #{billingMonth}
            WHERE rm.status = 'active'
              AND (COALESCE(rm.commission_percent, 0) > 0 OR COALESCE(rm.management_fee, 0) > 0)
              AND rm.start_date <= #{today}
              AND (rm.end_date IS NULL OR rm.end_date >= #{today})
            GROUP BY rm.id, rm.owner_unit_id, rm.management_fee, rm.commission_percent
            ORDER BY rm.id
            """)
    List<MandateFeeRow> findActiveMandateFees(@Param("today") LocalDate today,
            @Param("billingMonth") LocalDate billingMonth);

    @Select("""
            SELECT rm.id AS mandateId, rm.owner_unit_id AS ownerUnitId,
                   rm.management_fee AS managementFee, rm.commission_percent AS commissionPercent,
                   COALESCE(SUM(ri.amount_due), 0) AS rentBase
            FROM rental_mandates rm
            LEFT JOIN leases l ON l.rental_mandate_id = rm.id
            LEFT JOIN rent_invoices ri ON ri.lease_id = l.id AND ri.billing_month = #{periodStart}
            WHERE rm.id = #{mandateId} AND rm.status = 'active'
              AND rm.start_date <= #{periodEnd}
              AND (rm.end_date IS NULL OR rm.end_date >= #{periodStart})
            GROUP BY rm.id, rm.owner_unit_id, rm.management_fee, rm.commission_percent
            """)
    MandateFeeRow findMandateFee(@Param("mandateId") Long mandateId,
            @Param("periodStart") LocalDate periodStart, @Param("periodEnd") LocalDate periodEnd);

    @Select("SELECT pep.finance_record_id AS financeRecordId,fr.confirmation_status AS confirmationStatus FROM property_expense_postings pep JOIN finance_records fr ON fr.id=pep.finance_record_id WHERE pep.owner_unit_id=#{ownerUnitId} AND pep.charge_key=#{chargeKey} AND pep.period_key=#{periodKey} FOR UPDATE")
    PostingRow lockPosting(@Param("ownerUnitId") Long ownerUnitId,@Param("chargeKey") String chargeKey,@Param("periodKey") String periodKey);

    @Insert("INSERT INTO finance_records (transaction_no,record_type,unit_id,owner_id,amount,currency,transaction_date,payment_method,payment_status,confirmation_status,sync_status,created_by) VALUES (#{transactionNo},'property_expense',#{unitId},#{ownerId},#{amount},'MYR',#{occurredOn},'internal_accrual','unpaid','pending','not_synced',#{actorId})")
    @Options(useGeneratedKeys=true,keyProperty="id")
    int insertFinance(FinanceWrite row);

    @Insert("INSERT INTO cashflow_entries (finance_record_id,unit_id,owner_id,direction,category,description,occurred_on,attachment_status) VALUES (#{financeRecordId},#{unitId},#{ownerId},'expense',#{category},#{description},#{occurredOn},'not_required')")
    int insertCashflow(@Param("financeRecordId") Long financeRecordId,@Param("unitId") Long unitId,@Param("ownerId") Long ownerId,@Param("category") String category,@Param("description") String description,@Param("occurredOn") LocalDate occurredOn);

    @Insert("INSERT INTO property_expense_postings (owner_unit_id,charge_key,charge_name,period_key,finance_record_id) VALUES (#{ownerUnitId},#{chargeKey},#{chargeName},#{periodKey},#{financeRecordId})")
    int insertPosting(@Param("ownerUnitId") Long ownerUnitId,@Param("chargeKey") String chargeKey,@Param("chargeName") String chargeName,@Param("periodKey") String periodKey,@Param("financeRecordId") Long financeRecordId);

    @Update("UPDATE finance_records SET amount=#{amount},transaction_date=#{occurredOn} WHERE id=#{financeRecordId} AND confirmation_status='pending'")
    int updatePendingFinance(@Param("financeRecordId") Long financeRecordId,@Param("amount") BigDecimal amount,@Param("occurredOn") LocalDate occurredOn);

    @Update("UPDATE cashflow_entries SET category=#{category},description=#{description},occurred_on=#{occurredOn} WHERE finance_record_id=#{financeRecordId}")
    int updateCashflow(@Param("financeRecordId") Long financeRecordId,@Param("category") String category,@Param("description") String description,@Param("occurredOn") LocalDate occurredOn);

    class PropertyContext { private Long ownerUnitId,ownerId,unitId; public Long getOwnerUnitId(){return ownerUnitId;} public void setOwnerUnitId(Long v){ownerUnitId=v;} public Long getOwnerId(){return ownerId;} public void setOwnerId(Long v){ownerId=v;} public Long getUnitId(){return unitId;} public void setUnitId(Long v){unitId=v;} }
    class ProfileRow { private Long ownerUnitId; private String profileJson; public Long getOwnerUnitId(){return ownerUnitId;} public void setOwnerUnitId(Long v){ownerUnitId=v;} public String getProfileJson(){return profileJson;} public void setProfileJson(String v){profileJson=v;} }
    class MandateFeeRow { private Long mandateId,ownerUnitId; private BigDecimal managementFee,commissionPercent,rentBase; public Long getMandateId(){return mandateId;} public void setMandateId(Long v){mandateId=v;} public Long getOwnerUnitId(){return ownerUnitId;} public void setOwnerUnitId(Long v){ownerUnitId=v;} public BigDecimal getManagementFee(){return managementFee;} public void setManagementFee(BigDecimal v){managementFee=v;} public BigDecimal getCommissionPercent(){return commissionPercent;} public void setCommissionPercent(BigDecimal v){commissionPercent=v;} public BigDecimal getRentBase(){return rentBase;} public void setRentBase(BigDecimal v){rentBase=v;} }
    class PostingRow { private Long financeRecordId; private String confirmationStatus; public Long getFinanceRecordId(){return financeRecordId;} public void setFinanceRecordId(Long v){financeRecordId=v;} public String getConfirmationStatus(){return confirmationStatus;} public void setConfirmationStatus(String v){confirmationStatus=v;} }
    class FinanceWrite { private Long id,unitId,ownerId,actorId; private String transactionNo; private BigDecimal amount; private LocalDate occurredOn; public Long getId(){return id;} public void setId(Long v){id=v;} public Long getUnitId(){return unitId;} public void setUnitId(Long v){unitId=v;} public Long getOwnerId(){return ownerId;} public void setOwnerId(Long v){ownerId=v;} public Long getActorId(){return actorId;} public void setActorId(Long v){actorId=v;} public String getTransactionNo(){return transactionNo;} public void setTransactionNo(String v){transactionNo=v;} public BigDecimal getAmount(){return amount;} public void setAmount(BigDecimal v){amount=v;} public LocalDate getOccurredOn(){return occurredOn;} public void setOccurredOn(LocalDate v){occurredOn=v;} }
}
