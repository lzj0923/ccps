package com.ccps.backend.mapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface OwnerPropertyDetailMapper {
    @Select("""
            SELECT ou.id AS owner_unit_id, p.name AS project_name, p.address, p.country_code,
                   u.building, u.floor_no, u.unit_no, u.area_sqm
            FROM owners o
            JOIN owner_units ou ON ou.owner_id=o.id AND ou.status='active'
            JOIN units u ON u.id=ou.unit_id
            JOIN projects p ON p.id=u.project_id
            WHERE o.user_id=#{userId} AND o.status='active' AND ou.id=#{ownerUnitId}
            """)
    PropertyRow findProperty(@Param("userId") Long userId, @Param("ownerUnitId") Long ownerUnitId);

    @Select("""
            SELECT id,item_name,payment_name,account_no,bank_address,branch_code,swift_code,is_overseas_bank
            FROM property_bank_accounts WHERE owner_unit_id=#{ownerUnitId} ORDER BY id DESC LIMIT 1
            """)
    BankRow findLatestBank(@Param("ownerUnitId") Long ownerUnitId);

    @Select("""
            SELECT id,mandate_no,mandate_type,start_date,end_date,status
            FROM rental_mandates WHERE owner_unit_id=#{ownerUnitId}
            ORDER BY FIELD(status,'active','pending_review','suspended','expired','terminated','draft'),created_at DESC
            """)
    List<MandateRow> findMandates(@Param("ownerUnitId") Long ownerUnitId);

    @Select("""
            SELECT l.id,l.lease_no,t.full_name AS tenant_name,l.monthly_rent,l.deposit_amount,
                   l.start_date,l.end_date,l.status,l.contract_document_id
            FROM owner_units ou JOIN leases l ON l.unit_id=ou.unit_id JOIN tenants t ON t.id=l.tenant_id
            WHERE ou.id=#{ownerUnitId} ORDER BY l.start_date DESC,l.id DESC
            """)
    List<LeaseRow> findLeases(@Param("ownerUnitId") Long ownerUnitId);

    // Called only after property ownership is verified. Keep the stored source relation intact.
    @Select("""
            SELECT COALESCE(
              (SELECT signed_document.id
               FROM electronic_signature_requests signature_request
               JOIN documents signed_document ON signed_document.id=signature_request.signed_document_id
               WHERE signature_request.source_document_id=source_document.id
                 AND signature_request.status='signed'
                 AND COALESCE(signed_document.status,'') NOT IN ('voided','superseded')
               ORDER BY signature_request.id DESC LIMIT 1),
              CASE WHEN COALESCE(source_document.status,'') NOT IN ('voided','superseded')
                   THEN source_document.id ELSE NULL END)
            FROM documents source_document WHERE source_document.id=#{sourceDocumentId}
            """)
    Long findPreviewContractId(@Param("sourceDocumentId") Long sourceDocumentId);

    @Select("""
            SELECT pp.id,pp.lease_id,pp.rental_stage,pp.version_month,pp.document_id,pp.title,
                   pp.category,pp.description,pp.is_cover AS cover,d.original_name,d.mime_type,pp.created_at
            FROM property_photos pp JOIN documents d ON d.id=pp.document_id
            WHERE pp.owner_unit_id=#{ownerUnitId} AND COALESCE(d.status,'') NOT IN ('voided','superseded')
            ORDER BY pp.is_cover DESC,pp.version_month DESC,pp.sort_order,pp.id DESC
            """)
    List<PhotoRow> findPhotos(@Param("ownerUnitId") Long ownerUnitId);

    class PropertyRow { private Long ownerUnitId; private String projectName,address,countryCode,building,floorNo,unitNo; private BigDecimal areaSqm;
        public Long getOwnerUnitId(){return ownerUnitId;} public void setOwnerUnitId(Long v){ownerUnitId=v;} public String getProjectName(){return projectName;} public void setProjectName(String v){projectName=v;} public String getAddress(){return address;} public void setAddress(String v){address=v;} public String getCountryCode(){return countryCode;} public void setCountryCode(String v){countryCode=v;} public String getBuilding(){return building;} public void setBuilding(String v){building=v;} public String getFloorNo(){return floorNo;} public void setFloorNo(String v){floorNo=v;} public String getUnitNo(){return unitNo;} public void setUnitNo(String v){unitNo=v;} public BigDecimal getAreaSqm(){return areaSqm;} public void setAreaSqm(BigDecimal v){areaSqm=v;} }
    class BankRow { private Long id; private String itemName,paymentName,accountNo,bankAddress,branchCode,swiftCode; private Boolean overseasBank;
        public Long getId(){return id;} public void setId(Long v){id=v;} public String getItemName(){return itemName;} public void setItemName(String v){itemName=v;} public String getPaymentName(){return paymentName;} public void setPaymentName(String v){paymentName=v;} public String getAccountNo(){return accountNo;} public void setAccountNo(String v){accountNo=v;} public String getBankAddress(){return bankAddress;} public void setBankAddress(String v){bankAddress=v;} public String getBranchCode(){return branchCode;} public void setBranchCode(String v){branchCode=v;} public String getSwiftCode(){return swiftCode;} public void setSwiftCode(String v){swiftCode=v;} public Boolean getOverseasBank(){return overseasBank;} public void setOverseasBank(Boolean v){overseasBank=v;} }
    class MandateRow { private Long id; private String mandateNo,mandateType,status; private LocalDate startDate,endDate;
        public Long getId(){return id;} public void setId(Long v){id=v;} public String getMandateNo(){return mandateNo;} public void setMandateNo(String v){mandateNo=v;} public String getMandateType(){return mandateType;} public void setMandateType(String v){mandateType=v;} public LocalDate getStartDate(){return startDate;} public void setStartDate(LocalDate v){startDate=v;} public LocalDate getEndDate(){return endDate;} public void setEndDate(LocalDate v){endDate=v;} public String getStatus(){return status;} public void setStatus(String v){status=v;} }
    class LeaseRow { private Long id,contractDocumentId; private String leaseNo,tenantName,status; private BigDecimal monthlyRent,depositAmount; private LocalDate startDate,endDate;
        public Long getId(){return id;} public void setId(Long v){id=v;} public Long getContractDocumentId(){return contractDocumentId;} public void setContractDocumentId(Long v){contractDocumentId=v;} public String getLeaseNo(){return leaseNo;} public void setLeaseNo(String v){leaseNo=v;} public String getTenantName(){return tenantName;} public void setTenantName(String v){tenantName=v;} public String getStatus(){return status;} public void setStatus(String v){status=v;} public BigDecimal getMonthlyRent(){return monthlyRent;} public void setMonthlyRent(BigDecimal v){monthlyRent=v;} public BigDecimal getDepositAmount(){return depositAmount;} public void setDepositAmount(BigDecimal v){depositAmount=v;} public LocalDate getStartDate(){return startDate;} public void setStartDate(LocalDate v){startDate=v;} public LocalDate getEndDate(){return endDate;} public void setEndDate(LocalDate v){endDate=v;} }
    class PhotoRow { private Long id,leaseId,documentId; private String rentalStage,versionMonth,title,category,description,originalName,mimeType; private Boolean cover; private LocalDateTime createdAt;
        public Long getId(){return id;} public void setId(Long v){id=v;} public Long getLeaseId(){return leaseId;} public void setLeaseId(Long v){leaseId=v;} public Long getDocumentId(){return documentId;} public void setDocumentId(Long v){documentId=v;} public String getRentalStage(){return rentalStage;} public void setRentalStage(String v){rentalStage=v;} public String getVersionMonth(){return versionMonth;} public void setVersionMonth(String v){versionMonth=v;} public String getTitle(){return title;} public void setTitle(String v){title=v;} public String getCategory(){return category;} public void setCategory(String v){category=v;} public String getDescription(){return description;} public void setDescription(String v){description=v;} public String getOriginalName(){return originalName;} public void setOriginalName(String v){originalName=v;} public String getMimeType(){return mimeType;} public void setMimeType(String v){mimeType=v;} public Boolean getCover(){return cover;} public void setCover(Boolean v){cover=v;} public LocalDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(LocalDateTime v){createdAt=v;} }
}
