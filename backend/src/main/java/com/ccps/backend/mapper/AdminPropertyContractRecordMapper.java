package com.ccps.backend.mapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.math.BigDecimal;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface AdminPropertyContractRecordMapper {
    String COLUMNS = "pcr.id, pcr.owner_unit_id AS ownerUnitId, pcr.lease_id AS leaseId, "
            + "l.lease_no AS leaseNo, t.full_name AS tenantName, l.start_date AS leaseStart, l.end_date AS leaseEnd, "
            + "l.monthly_rent AS monthlyRent, l.status AS leaseStatus, pcr.contract_type AS contractType, "
            + "pcr.contract_no AS contractNo, pcr.signed_date AS signedDate, pcr.valid_from AS validFrom, "
            + "pcr.valid_to AS validTo, pcr.status, pcr.notes, pcr.original_name AS originalName, "
            + "pcr.storage_key AS storageKey, pcr.mime_type AS mimeType, pcr.file_size AS fileSize, "
            + "pcr.created_at AS createdAt, pcr.updated_at AS updatedAt";
    String JOINS = " FROM property_contract_records pcr LEFT JOIN leases l ON l.id=pcr.lease_id LEFT JOIN tenants t ON t.id=l.tenant_id ";

    @Select("SELECT COUNT(*) FROM owner_units WHERE id=#{ownerUnitId} AND owner_id=#{ownerId}")
    int ownsProperty(@Param("ownerId") Long ownerId, @Param("ownerUnitId") Long ownerUnitId);

    @Select("SELECT " + COLUMNS + JOINS + " WHERE pcr.owner_unit_id=#{ownerUnitId} AND pcr.contract_type<>'L_LEASE' AND (pcr.contract_type='A_HANDOVER_ASSISTANCE' OR NOT EXISTS (SELECT 1 FROM property_contract_records newer WHERE newer.owner_unit_id=pcr.owner_unit_id AND newer.contract_type=pcr.contract_type AND newer.lease_id <=> pcr.lease_id AND newer.id>pcr.id)) ORDER BY pcr.signed_date DESC, pcr.id DESC")
    List<Row> list(@Param("ownerUnitId") Long ownerUnitId);

    @Select("""
            SELECT l.id, ou.id AS ownerUnitId, l.id AS leaseId, l.lease_no AS leaseNo,
                   t.full_name AS tenantName, l.start_date AS leaseStart, l.end_date AS leaseEnd,
                   l.monthly_rent AS monthlyRent, l.status AS leaseStatus, 'L_LEASE' AS contractType,
                   l.lease_no AS contractNo,
                   CASE WHEN EXISTS (SELECT 1 FROM electronic_signature_requests esr
                                     WHERE esr.source_document_id=l.contract_document_id
                                       AND esr.entity_type='lease' AND esr.entity_id=l.id
                                       AND esr.status='signed'
                                       AND EXISTS (SELECT 1 FROM documents signed_doc WHERE signed_doc.id=esr.signed_document_id AND signed_doc.status NOT IN ('voided','superseded'))) THEN l.start_date ELSE NULL END AS signedDate,
                   l.start_date AS validFrom,
                   l.end_date AS validTo,
                   CASE WHEN EXISTS (SELECT 1 FROM electronic_signature_requests esr
                                     WHERE esr.source_document_id=l.contract_document_id
                                       AND esr.entity_type='lease' AND esr.entity_id=l.id
                                       AND esr.status='signed'
                                       AND EXISTS (SELECT 1 FROM documents signed_doc WHERE signed_doc.id=esr.signed_document_id AND signed_doc.status NOT IN ('voided','superseded'))) THEN 'completed'
                        ELSE 'pending' END AS status,
                   NULL AS notes, d.original_name AS originalName, d.storage_key AS storageKey,
                   d.mime_type AS mimeType, d.file_size AS fileSize,
                   l.created_at AS createdAt, l.updated_at AS updatedAt
            FROM owner_units ou
            JOIN leases l ON l.unit_id=ou.unit_id
            JOIN tenants t ON t.id=l.tenant_id
            LEFT JOIN documents d ON d.id=l.contract_document_id AND d.document_type='lease'
            WHERE ou.id=#{ownerUnitId}
            ORDER BY l.start_date DESC, l.id DESC
            """)
    List<Row> listLeaseContracts(@Param("ownerUnitId") Long ownerUnitId);

    @Select("SELECT " + COLUMNS + JOINS + " WHERE pcr.id=#{id} AND pcr.owner_unit_id=#{ownerUnitId}")
    Row find(@Param("ownerUnitId") Long ownerUnitId, @Param("id") Long id);

    @Select("SELECT " + COLUMNS + JOINS + " WHERE pcr.owner_unit_id=#{ownerUnitId} AND pcr.contract_no=#{contractNo}")
    Row findByContractNo(@Param("ownerUnitId") Long ownerUnitId, @Param("contractNo") String contractNo);

    @Select("SELECT " + COLUMNS + JOINS + " WHERE pcr.owner_unit_id=#{ownerUnitId} AND pcr.lease_id=#{leaseId} AND pcr.contract_type=#{contractType}")
    Row findByLeaseAndType(@Param("ownerUnitId") Long ownerUnitId, @Param("leaseId") Long leaseId,
            @Param("contractType") String contractType);

    @Select("SELECT " + COLUMNS + JOINS + " WHERE pcr.owner_unit_id=#{ownerUnitId} AND pcr.lease_id IS NULL AND pcr.contract_type=#{contractType} ORDER BY pcr.id DESC LIMIT 1")
    Row findByPropertyAndType(@Param("ownerUnitId") Long ownerUnitId, @Param("contractType") String contractType);

    @Select("""
            SELECT l.id AS leaseId, l.rental_mandate_id AS rentalMandateId, l.lease_no AS leaseNo, l.tenant_id AS tenantId, t.full_name AS tenantName,
                   l.start_date AS startDate, l.end_date AS endDate, l.monthly_rent AS monthlyRent,
                   l.deposit_amount AS depositAmount, l.payment_day AS paymentDay, l.status,
                   CASE WHEN l.contract_document_id IS NULL THEN FALSE ELSE TRUE END AS linked,
                   CASE WHEN l.contract_document_id IS NULL THEN 'not_generated'
                        WHEN EXISTS(SELECT 1 FROM electronic_signature_requests sr WHERE sr.entity_type='lease' AND sr.entity_id=l.id AND sr.source_document_id=l.contract_document_id AND sr.status='signed') THEN 'signed'
                        WHEN EXISTS(SELECT 1 FROM electronic_signature_requests sr WHERE sr.entity_type='lease' AND sr.entity_id=l.id AND sr.source_document_id=l.contract_document_id AND sr.status IN ('pending','sent','viewed')) THEN 'pending'
                        ELSE 'ready_to_sign' END AS signatureStatus
            FROM owner_units ou
            JOIN leases l ON l.unit_id=ou.unit_id
            JOIN tenants t ON t.id=l.tenant_id
            WHERE ou.id=#{ownerUnitId}
            ORDER BY l.start_date DESC, l.id DESC
            """)
    List<LeaseOptionRow> leaseOptions(@Param("ownerUnitId") Long ownerUnitId);

    @Select("SELECT COUNT(*) FROM owner_units ou JOIN leases l ON l.unit_id=ou.unit_id WHERE ou.id=#{ownerUnitId} AND l.id=#{leaseId}")
    int leaseBelongsToProperty(@Param("ownerUnitId") Long ownerUnitId, @Param("leaseId") Long leaseId);

    @Insert("""
            INSERT INTO property_contract_records
              (owner_unit_id, lease_id, contract_type, contract_no, signed_date, valid_from, valid_to, status, notes,
               original_name, storage_key, mime_type, file_size, created_by)
            VALUES
              (#{ownerUnitId}, #{leaseId}, #{contractType}, #{contractNo}, #{signedDate}, #{validFrom}, #{validTo}, #{status}, #{notes},
               #{originalName}, #{storageKey}, #{mimeType}, #{fileSize}, #{createdBy})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Row row);

    @Update("""
            UPDATE property_contract_records SET
              lease_id=#{leaseId}, contract_type=#{contractType}, contract_no=#{contractNo}, signed_date=#{signedDate},
              valid_from=#{validFrom}, valid_to=#{validTo}, status=#{status}, notes=#{notes},
              original_name=#{originalName}, storage_key=#{storageKey}, mime_type=#{mimeType}, file_size=#{fileSize}
            WHERE id=#{id} AND owner_unit_id=#{ownerUnitId}
            """)
    int update(Row row);

    @Delete("DELETE FROM property_contract_records WHERE id=#{id} AND owner_unit_id=#{ownerUnitId}")
    int delete(@Param("ownerUnitId") Long ownerUnitId, @Param("id") Long id);

    class Row {
        private Long id;
        private Long ownerUnitId;
        private Long leaseId;
        private String leaseNo;
        private String tenantName;
        private LocalDate leaseStart;
        private LocalDate leaseEnd;
        private BigDecimal monthlyRent;
        private String leaseStatus;
        private String contractType;
        private String contractNo;
        private LocalDate signedDate;
        private LocalDate validFrom;
        private LocalDate validTo;
        private String status;
        private String notes;
        private String originalName;
        private String storageKey;
        private String mimeType;
        private Long fileSize;
        private Long createdBy;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public Long getId() { return id; }
        public void setId(Long value) { id = value; }
        public Long getOwnerUnitId() { return ownerUnitId; }
        public void setOwnerUnitId(Long value) { ownerUnitId = value; }
        public Long getLeaseId() { return leaseId; }
        public void setLeaseId(Long value) { leaseId = value; }
        public String getLeaseNo() { return leaseNo; }
        public void setLeaseNo(String value) { leaseNo = value; }
        public String getTenantName() { return tenantName; }
        public void setTenantName(String value) { tenantName = value; }
        public LocalDate getLeaseStart() { return leaseStart; }
        public void setLeaseStart(LocalDate value) { leaseStart = value; }
        public LocalDate getLeaseEnd() { return leaseEnd; }
        public void setLeaseEnd(LocalDate value) { leaseEnd = value; }
        public BigDecimal getMonthlyRent() { return monthlyRent; }
        public void setMonthlyRent(BigDecimal value) { monthlyRent = value; }
        public String getLeaseStatus() { return leaseStatus; }
        public void setLeaseStatus(String value) { leaseStatus = value; }
        public String getContractType() { return contractType; }
        public void setContractType(String value) { contractType = value; }
        public String getContractNo() { return contractNo; }
        public void setContractNo(String value) { contractNo = value; }
        public LocalDate getSignedDate() { return signedDate; }
        public void setSignedDate(LocalDate value) { signedDate = value; }
        public LocalDate getValidFrom() { return validFrom; }
        public void setValidFrom(LocalDate value) { validFrom = value; }
        public LocalDate getValidTo() { return validTo; }
        public void setValidTo(LocalDate value) { validTo = value; }
        public String getStatus() { return status; }
        public void setStatus(String value) { status = value; }
        public String getNotes() { return notes; }
        public void setNotes(String value) { notes = value; }
        public String getOriginalName() { return originalName; }
        public void setOriginalName(String value) { originalName = value; }
        public String getStorageKey() { return storageKey; }
        public void setStorageKey(String value) { storageKey = value; }
        public String getMimeType() { return mimeType; }
        public void setMimeType(String value) { mimeType = value; }
        public Long getFileSize() { return fileSize; }
        public void setFileSize(Long value) { fileSize = value; }
        public Long getCreatedBy() { return createdBy; }
        public void setCreatedBy(Long value) { createdBy = value; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime value) { createdAt = value; }
        public LocalDateTime getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(LocalDateTime value) { updatedAt = value; }
    }

    class LeaseOptionRow {
        private Long leaseId;
        private Long rentalMandateId;
        private String leaseNo;
        private Long tenantId;
        private String tenantName;
        private LocalDate startDate;
        private LocalDate endDate;
        private BigDecimal monthlyRent;
        private BigDecimal depositAmount;
        private Integer paymentDay;
        private String status;
        private boolean linked;
        private String signatureStatus;
        public Long getLeaseId() { return leaseId; } public void setLeaseId(Long value) { leaseId=value; }
        public Long getRentalMandateId() { return rentalMandateId; } public void setRentalMandateId(Long value) { rentalMandateId=value; }
        public String getLeaseNo() { return leaseNo; } public void setLeaseNo(String value) { leaseNo=value; }
        public Long getTenantId() { return tenantId; } public void setTenantId(Long value) { tenantId=value; }
        public String getTenantName() { return tenantName; } public void setTenantName(String value) { tenantName=value; }
        public LocalDate getStartDate() { return startDate; } public void setStartDate(LocalDate value) { startDate=value; }
        public LocalDate getEndDate() { return endDate; } public void setEndDate(LocalDate value) { endDate=value; }
        public BigDecimal getMonthlyRent() { return monthlyRent; } public void setMonthlyRent(BigDecimal value) { monthlyRent=value; }
        public BigDecimal getDepositAmount() { return depositAmount; } public void setDepositAmount(BigDecimal value) { depositAmount=value; }
        public Integer getPaymentDay() { return paymentDay; } public void setPaymentDay(Integer value) { paymentDay=value; }
        public String getStatus() { return status; } public void setStatus(String value) { status=value; }
        public boolean isLinked() { return linked; } public void setLinked(boolean value) { linked=value; }
        public String getSignatureStatus() { return signatureStatus; } public void setSignatureStatus(String value) { signatureStatus=value; }
    }
}
