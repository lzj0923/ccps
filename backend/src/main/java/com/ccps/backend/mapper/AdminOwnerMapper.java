package com.ccps.backend.mapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.ccps.backend.dto.AdminProjectOption;
import com.ccps.backend.dto.AdminOwnerSummaryResponse;

@Mapper
public interface AdminOwnerMapper {

    @Select("""
            SELECT
              (SELECT COUNT(*) FROM owners WHERE status = 'active') AS ownerCount,
              (SELECT COUNT(*) FROM owner_units WHERE status = 'active') AS propertyCount,
              (SELECT COUNT(*) FROM owner_units
               WHERE status = 'active' AND asset_stage = 'OPERATING') AS operatingPropertyCount,
              (SELECT COUNT(DISTINCT ou.owner_id)
               FROM owner_units ou
               JOIN owners o ON o.id = ou.owner_id AND o.status = 'active'
               LEFT JOIN (
                 SELECT pc.owner_unit_id,
                        MAX(pc.purchase_price) AS purchase_price,
                        COALESCE(SUM(pi.amount_paid), 0) AS paid_amount
                 FROM purchase_contracts pc
                 LEFT JOIN payment_plans pp ON pp.purchase_contract_id = pc.id
                   AND pp.status = 'active'
                 LEFT JOIN payment_installments pi ON pi.payment_plan_id = pp.id
                 WHERE pc.status IN ('active', 'completed')
                 GROUP BY pc.owner_unit_id
               ) pay ON pay.owner_unit_id = ou.id
               WHERE ou.status = 'active'
                 AND ou.asset_stage = 'PRE_HANDOVER'
                 AND GREATEST(COALESCE(pay.purchase_price, 0) - COALESCE(pay.paid_amount, 0), 0) > 0
              ) AS unpaidOwnerCount,
              (SELECT COUNT(DISTINCT ou.owner_id)
               FROM owner_units ou
               JOIN owners o ON o.id = ou.owner_id AND o.status = 'active'
               JOIN reserve_accounts ra ON ra.owner_unit_id = ou.id AND ra.status = 'active'
               WHERE ou.status = 'active'
                 AND ou.asset_stage = 'OPERATING'
                 AND ra.current_balance < ra.minimum_balance
              ) AS lowReserveOwnerCount
            """)
    AdminOwnerSummaryResponse findSummary();

    String OWNER_PROPERTY_SELECT = """
            SELECT
              o.id AS owner_id,
              o.full_name,
              o.identity_no,
              o.phone,
              o.email,
              o.status AS owner_status,
              ou.id AS owner_unit_id,
              ou.ownership_percent,
              ou.is_primary AS primary_ownership,
              ou.start_date,
              ou.end_date,
              ou.asset_stage,
              ou.expected_handover_date,
              ou.actual_handover_date,
              svc.services,
              u.id AS unit_id,
              u.project_id,
              u.building,
              u.floor_no,
              u.unit_no,
              u.unit_type,
              u.area_sqm,
              u.bedroom_count,
              u.listing_status,
              p.name AS project_name,
              p.address,
              p.city,
              COALESCE(pay.purchase_price, 0) AS purchase_price,
              CASE WHEN NOT (ou.asset_stage = 'PRE_HANDOVER') THEN COALESCE(pay.purchase_price, 0)
                   ELSE COALESCE(pay.paid_amount, 0) END AS paid_amount,
              CASE WHEN NOT (ou.asset_stage = 'PRE_HANDOVER') THEN 0
                   ELSE GREATEST(COALESCE(pay.purchase_price, 0) - COALESCE(pay.paid_amount, 0), 0) END AS remaining_amount,
              CASE
                WHEN u.id IS NULL THEN 'not_configured'
                WHEN NOT (ou.asset_stage = 'PRE_HANDOVER') THEN 'not_applicable'
                WHEN COALESCE(pay.total_installment_count, 0) = 0 THEN 'not_configured'
                WHEN COALESCE(pay.paid_amount, 0) = COALESCE(pay.purchase_price, 0)
                  OR COALESCE(pay.paid_amount, 0) > COALESCE(pay.purchase_price, 0) THEN 'paid'
                WHEN COALESCE(pay.has_overdue, 0) = 1 THEN 'overdue'
                WHEN pay.next_due_date BETWEEN CURRENT_DATE AND DATE_ADD(CURRENT_DATE, INTERVAL 30 DAY) THEN 'due_soon'
                ELSE 'paying'
              END AS payment_status
            FROM owners o
            LEFT JOIN owner_units ou ON ou.owner_id = o.id AND ou.status = 'active'
            LEFT JOIN units u ON u.id = ou.unit_id
            LEFT JOIN projects p ON p.id = u.project_id
            LEFT JOIN (
              SELECT owner_unit_id,
                     GROUP_CONCAT(service_type ORDER BY service_type SEPARATOR ',') AS services
              FROM owner_unit_services
              WHERE status = 'active'
              GROUP BY owner_unit_id
            ) svc ON svc.owner_unit_id = ou.id
            LEFT JOIN (
              SELECT
                pc.owner_unit_id,
                MAX(pc.purchase_price) AS purchase_price,
                COALESCE(SUM(pi.amount_paid), 0) AS paid_amount,
                COUNT(pi.id) AS total_installment_count,
                MIN(CASE WHEN NOT (pi.amount_paid >= pi.amount_due) THEN pi.due_date END) AS next_due_date,
                MAX(CASE WHEN NOT (pi.amount_paid >= pi.amount_due) AND CURRENT_DATE > pi.due_date THEN 1 ELSE 0 END) AS has_overdue
              FROM purchase_contracts pc
              JOIN owner_units pou ON pou.id = pc.owner_unit_id
              LEFT JOIN payment_plans pp ON pp.purchase_contract_id = pc.id
                AND pp.status = 'active' AND pou.asset_stage = 'PRE_HANDOVER'
              LEFT JOIN payment_installments pi ON pi.payment_plan_id = pp.id
              WHERE pc.status IN ('active', 'completed')
              GROUP BY pc.owner_unit_id
            ) pay ON pay.owner_unit_id = ou.id
            """;

    @Select(OWNER_PROPERTY_SELECT + """
            ORDER BY o.full_name, p.name, u.unit_no
            """)
    List<OwnerPropertyRow> findOwnersWithProperties();

    String PROPERTY_PAGE_WHERE = """
            <where>
              owner_unit_id IS NOT NULL
              <if test="keyword != null and keyword != ''">
                AND CONCAT_WS(' ', project_name, unit_no, full_name, phone, unit_type) LIKE CONCAT('%', #{keyword}, '%')
              </if>
              <if test="projectName != null and projectName != ''">AND project_name = #{projectName}</if>
              <if test="rentalStatus == 'pre_handover'">AND asset_stage = 'PRE_HANDOVER'</if>
              <if test="rentalStatus == 'pending_rental'">AND asset_stage = 'OPERATING' AND FIND_IN_SET('RENTAL', COALESCE(services, '')) &gt; 0 AND listing_status &lt;&gt; 'rented'</if>
              <if test="rentalStatus == 'rented'">AND asset_stage = 'OPERATING' AND listing_status = 'rented'</if>
              <if test="rentalStatus == 'not_for_rent'">AND asset_stage = 'OPERATING' AND listing_status &lt;&gt; 'rented' AND FIND_IN_SET('RENTAL', COALESCE(services, '')) = 0</if>
            </where>
            """;

    @Select({"<script>", "SELECT * FROM (", OWNER_PROPERTY_SELECT, ") property_rows",
            PROPERTY_PAGE_WHERE,
            "ORDER BY project_name, unit_no, owner_id LIMIT #{limit} OFFSET #{offset}", "</script>"})
    List<OwnerPropertyRow> findPropertyPage(@Param("keyword") String keyword,
            @Param("projectName") String projectName, @Param("rentalStatus") String rentalStatus,
            @Param("limit") int limit, @Param("offset") int offset);

    @Select({"<script>", "SELECT COUNT(*) FROM (", OWNER_PROPERTY_SELECT, ") property_rows",
            PROPERTY_PAGE_WHERE, "</script>"})
    Long countPropertyPage(@Param("keyword") String keyword,
            @Param("projectName") String projectName, @Param("rentalStatus") String rentalStatus);

    @Select(OWNER_PROPERTY_SELECT + """
            WHERE o.id = #{ownerId}
            ORDER BY p.name, u.unit_no
            """)
    List<OwnerPropertyRow> findOwnerById(@Param("ownerId") Long ownerId);

    @Select(OWNER_PROPERTY_SELECT + """
            WHERE o.id = #{ownerId}
              AND ou.id = #{ownerUnitId}
            """)
    OwnerPropertyRow findOwnerProperty(@Param("ownerId") Long ownerId,
                                       @Param("ownerUnitId") Long ownerUnitId);

    @Select("""
            SELECT COUNT(*)
            FROM units
            WHERE project_id = #{projectId}
              AND unit_no = #{unitNo}
              AND id <> #{unitId}
            """)
    int countUnitNumberConflicts(@Param("projectId") Long projectId,
                                 @Param("unitNo") String unitNo,
                                 @Param("unitId") Long unitId);

    @Select("SELECT COUNT(*) FROM owners WHERE identity_no = #{identityNo}")
    int countOwnersByIdentityNo(@Param("identityNo") String identityNo);

    @Select("SELECT COUNT(*) FROM owners WHERE LOWER(email) = LOWER(#{email})")
    int countOwnersByEmail(@Param("email") String email);

    @Insert("""
            INSERT INTO owners (user_id, full_name, identity_no, phone, email, status)
            VALUES (#{userId}, #{fullName}, #{identityNo}, #{phone}, #{email}, #{status})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertOwner(NewOwner owner);

    @Select("""
            SELECT id, project_code AS code, name, city
            FROM projects
            WHERE status = 'active'
            ORDER BY name
            """)
    List<AdminProjectOption> findActiveProjects();

    @Select("SELECT COUNT(*) FROM owners WHERE id = #{ownerId} AND status = 'active'")
    int countActiveOwner(@Param("ownerId") Long ownerId);

    @Select("SELECT COUNT(*) FROM projects WHERE id = #{projectId} AND status = 'active'")
    int countActiveProject(@Param("projectId") Long projectId);

    @Select("SELECT COUNT(*) FROM units WHERE project_id = #{projectId} AND unit_no = #{unitNo}")
    int countUnitNumberExists(@Param("projectId") Long projectId, @Param("unitNo") String unitNo);

    @Insert("""
            INSERT INTO units (project_id, building, floor_no, unit_no, unit_type, area_sqm, bedroom_count, listing_status)
            VALUES (#{projectId}, #{building}, #{floorNo}, #{unitNo}, #{unitType}, #{areaSqm}, #{bedroomCount}, #{listingStatus})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertUnit(NewUnit unit);

    @Insert("""
            INSERT INTO owner_units (owner_id, unit_id, ownership_percent, is_primary, start_date,
                                     asset_stage, expected_handover_date, actual_handover_date, status)
            VALUES (#{ownerId}, #{unitId}, #{ownershipPercent}, #{primaryOwnership}, #{startDate},
                    #{assetStage}, #{expectedHandoverDate}, #{actualHandoverDate}, 'active')
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertOwnerUnit(NewOwnerUnit ownerUnit);

    @Insert("""
            INSERT INTO purchase_contracts (owner_unit_id, contract_no, purchase_price, currency, signed_date, handover_date, status)
            VALUES (#{ownerUnitId}, #{contractNo}, #{purchasePrice}, 'MYR', #{signedDate}, #{handoverDate}, #{status})
            """)
    int insertPurchaseContract(NewPurchaseContract contract);

    @Update("""
            UPDATE owner_units
            SET asset_stage = #{assetStage},
                expected_handover_date = #{expectedHandoverDate},
                actual_handover_date = #{actualHandoverDate},
                end_date = CASE WHEN #{assetStage} = 'DISPOSED' THEN COALESCE(end_date, CURRENT_DATE) ELSE end_date END
            WHERE id = #{ownerUnitId}
            """)
    int updateOwnerUnitLifecycle(@Param("ownerUnitId") Long ownerUnitId,
                                 @Param("assetStage") String assetStage,
                                 @Param("expectedHandoverDate") LocalDate expectedHandoverDate,
                                 @Param("actualHandoverDate") LocalDate actualHandoverDate);

    @Update("""
            UPDATE owner_unit_services
            SET status = 'ended', ended_at = CURRENT_DATE
            WHERE owner_unit_id = #{ownerUnitId} AND status <> 'ended'
            """)
    int endOwnerUnitServices(@Param("ownerUnitId") Long ownerUnitId);

    @Insert("""
            INSERT INTO owner_unit_services (owner_unit_id, service_type, status, started_at, ended_at)
            VALUES (#{ownerUnitId}, #{serviceType}, 'active', CURRENT_DATE, NULL)
            ON DUPLICATE KEY UPDATE status = 'active', ended_at = NULL
            """)
    int activateOwnerUnitService(@Param("ownerUnitId") Long ownerUnitId,
                                 @Param("serviceType") String serviceType);

    @Insert("""
            INSERT INTO reserve_accounts (owner_unit_id, minimum_balance, current_balance, status, low_balance_alert_enabled)
            SELECT #{ownerUnitId}, 0, 0, 'active', 1
            WHERE NOT EXISTS (SELECT 1 FROM reserve_accounts WHERE owner_unit_id = #{ownerUnitId})
            """)
    int ensureReserveAccount(@Param("ownerUnitId") Long ownerUnitId);

    @Update("""
            UPDATE reserve_accounts
            SET status = #{status}
            WHERE owner_unit_id = #{ownerUnitId}
            """)
    int updateReserveStatus(@Param("ownerUnitId") Long ownerUnitId, @Param("status") String status);

    @Update("""
            UPDATE payment_installments pi
            JOIN payment_plans pp ON pp.id = pi.payment_plan_id
            JOIN purchase_contracts pc ON pc.id = pp.purchase_contract_id
            SET pi.amount_paid = pi.amount_due, pi.status = 'paid'
            WHERE pc.owner_unit_id = #{ownerUnitId} AND pp.status = 'active'
            """)
    int markActiveInstallmentsPaid(@Param("ownerUnitId") Long ownerUnitId);

    @Update("""
            UPDATE payment_plans pp
            JOIN purchase_contracts pc ON pc.id = pp.purchase_contract_id
            SET pp.status = 'historical'
            WHERE pc.owner_unit_id = #{ownerUnitId} AND pp.status = 'active'
            """)
    int archiveActivePaymentPlans(@Param("ownerUnitId") Long ownerUnitId);

    @Update("""
            UPDATE purchase_contracts
            SET status = 'completed', handover_date = COALESCE(handover_date, #{actualHandoverDate})
            WHERE owner_unit_id = #{ownerUnitId} AND status = 'active'
            """)
    int completePurchaseContracts(@Param("ownerUnitId") Long ownerUnitId,
                                  @Param("actualHandoverDate") LocalDate actualHandoverDate);

    @Update("""
            UPDATE units
            SET building = #{building},
                floor_no = #{floorNo},
                unit_no = #{unitNo},
                unit_type = #{unitType},
                area_sqm = #{areaSqm},
                bedroom_count = #{bedroomCount},
                listing_status = #{listingStatus}
            WHERE id = #{unitId}
            """)
    int updateUnit(@Param("unitId") Long unitId,
                   @Param("building") String building,
                   @Param("floorNo") String floorNo,
                   @Param("unitNo") String unitNo,
                   @Param("unitType") String unitType,
                   @Param("areaSqm") BigDecimal areaSqm,
                   @Param("bedroomCount") Integer bedroomCount,
                   @Param("listingStatus") String listingStatus);

    @Update("""
            UPDATE purchase_contracts
            SET purchase_price = #{purchasePrice}
            WHERE owner_unit_id = #{ownerUnitId}
              AND status <> 'cancelled'
            ORDER BY id DESC
            LIMIT 1
            """)
    int updateActivePurchasePrice(@Param("ownerUnitId") Long ownerUnitId,
                                  @Param("purchasePrice") BigDecimal purchasePrice);

    class OwnerPropertyRow {
        private Long ownerId;
        private String fullName;
        private String identityNo;
        private String phone;
        private String email;
        private String ownerStatus;
        private Long ownerUnitId;
        private BigDecimal ownershipPercent;
        private Boolean primaryOwnership;
        private LocalDate startDate;
        private LocalDate endDate;
        private String assetStage;
        private LocalDate expectedHandoverDate;
        private LocalDate actualHandoverDate;
        private String services;
        private Long unitId;
        private Long projectId;
        private String building;
        private String floorNo;
        private String unitNo;
        private String unitType;
        private BigDecimal areaSqm;
        private Integer bedroomCount;
        private String listingStatus;
        private String projectName;
        private String address;
        private String city;
        private BigDecimal purchasePrice;
        private BigDecimal paidAmount;
        private BigDecimal remainingAmount;
        private String paymentStatus;

        public Long getOwnerId() { return ownerId; }
        public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }
        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }
        public String getIdentityNo() { return identityNo; }
        public void setIdentityNo(String identityNo) { this.identityNo = identityNo; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getOwnerStatus() { return ownerStatus; }
        public void setOwnerStatus(String ownerStatus) { this.ownerStatus = ownerStatus; }
        public Long getOwnerUnitId() { return ownerUnitId; }
        public void setOwnerUnitId(Long ownerUnitId) { this.ownerUnitId = ownerUnitId; }
        public BigDecimal getOwnershipPercent() { return ownershipPercent; }
        public void setOwnershipPercent(BigDecimal ownershipPercent) { this.ownershipPercent = ownershipPercent; }
        public Boolean getPrimaryOwnership() { return primaryOwnership; }
        public void setPrimaryOwnership(Boolean primaryOwnership) { this.primaryOwnership = primaryOwnership; }
        public LocalDate getStartDate() { return startDate; }
        public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
        public LocalDate getEndDate() { return endDate; }
        public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
        public String getAssetStage() { return assetStage; }
        public void setAssetStage(String assetStage) { this.assetStage = assetStage; }
        public LocalDate getExpectedHandoverDate() { return expectedHandoverDate; }
        public void setExpectedHandoverDate(LocalDate expectedHandoverDate) { this.expectedHandoverDate = expectedHandoverDate; }
        public LocalDate getActualHandoverDate() { return actualHandoverDate; }
        public void setActualHandoverDate(LocalDate actualHandoverDate) { this.actualHandoverDate = actualHandoverDate; }
        public String getServices() { return services; }
        public void setServices(String services) { this.services = services; }
        public Long getUnitId() { return unitId; }
        public void setUnitId(Long unitId) { this.unitId = unitId; }
        public Long getProjectId() { return projectId; }
        public void setProjectId(Long projectId) { this.projectId = projectId; }
        public String getBuilding() { return building; }
        public void setBuilding(String building) { this.building = building; }
        public String getFloorNo() { return floorNo; }
        public void setFloorNo(String floorNo) { this.floorNo = floorNo; }
        public String getUnitNo() { return unitNo; }
        public void setUnitNo(String unitNo) { this.unitNo = unitNo; }
        public String getUnitType() { return unitType; }
        public void setUnitType(String unitType) { this.unitType = unitType; }
        public BigDecimal getAreaSqm() { return areaSqm; }
        public void setAreaSqm(BigDecimal areaSqm) { this.areaSqm = areaSqm; }
        public Integer getBedroomCount() { return bedroomCount; }
        public void setBedroomCount(Integer bedroomCount) { this.bedroomCount = bedroomCount; }
        public String getListingStatus() { return listingStatus; }
        public void setListingStatus(String listingStatus) { this.listingStatus = listingStatus; }
        public String getProjectName() { return projectName; }
        public void setProjectName(String projectName) { this.projectName = projectName; }
        public String getAddress() { return address; }
        public void setAddress(String address) { this.address = address; }
        public String getCity() { return city; }
        public void setCity(String city) { this.city = city; }
        public BigDecimal getPurchasePrice() { return purchasePrice; }
        public void setPurchasePrice(BigDecimal purchasePrice) { this.purchasePrice = purchasePrice; }
        public BigDecimal getPaidAmount() { return paidAmount; }
        public void setPaidAmount(BigDecimal paidAmount) { this.paidAmount = paidAmount; }
        public BigDecimal getRemainingAmount() { return remainingAmount; }
        public void setRemainingAmount(BigDecimal remainingAmount) { this.remainingAmount = remainingAmount; }
        public String getPaymentStatus() { return paymentStatus; }
        public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }
    }

    class NewOwner {
        private Long id;
        private Long userId;
        private String fullName;
        private String identityNo;
        private String phone;
        private String email;
        private String status;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }
        public String getIdentityNo() { return identityNo; }
        public void setIdentityNo(String identityNo) { this.identityNo = identityNo; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }

    class NewUnit {
        private Long id;
        private Long projectId;
        private String building;
        private String floorNo;
        private String unitNo;
        private String unitType;
        private BigDecimal areaSqm;
        private Integer bedroomCount;
        private String listingStatus;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Long getProjectId() { return projectId; }
        public void setProjectId(Long projectId) { this.projectId = projectId; }
        public String getBuilding() { return building; }
        public void setBuilding(String building) { this.building = building; }
        public String getFloorNo() { return floorNo; }
        public void setFloorNo(String floorNo) { this.floorNo = floorNo; }
        public String getUnitNo() { return unitNo; }
        public void setUnitNo(String unitNo) { this.unitNo = unitNo; }
        public String getUnitType() { return unitType; }
        public void setUnitType(String unitType) { this.unitType = unitType; }
        public BigDecimal getAreaSqm() { return areaSqm; }
        public void setAreaSqm(BigDecimal areaSqm) { this.areaSqm = areaSqm; }
        public Integer getBedroomCount() { return bedroomCount; }
        public void setBedroomCount(Integer bedroomCount) { this.bedroomCount = bedroomCount; }
        public String getListingStatus() { return listingStatus; }
        public void setListingStatus(String listingStatus) { this.listingStatus = listingStatus; }
    }

    class NewOwnerUnit {
        private Long id;
        private Long ownerId;
        private Long unitId;
        private BigDecimal ownershipPercent;
        private Boolean primaryOwnership;
        private LocalDate startDate;
        private String assetStage;
        private LocalDate expectedHandoverDate;
        private LocalDate actualHandoverDate;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Long getOwnerId() { return ownerId; }
        public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }
        public Long getUnitId() { return unitId; }
        public void setUnitId(Long unitId) { this.unitId = unitId; }
        public BigDecimal getOwnershipPercent() { return ownershipPercent; }
        public void setOwnershipPercent(BigDecimal ownershipPercent) { this.ownershipPercent = ownershipPercent; }
        public Boolean getPrimaryOwnership() { return primaryOwnership; }
        public void setPrimaryOwnership(Boolean primaryOwnership) { this.primaryOwnership = primaryOwnership; }
        public LocalDate getStartDate() { return startDate; }
        public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
        public String getAssetStage() { return assetStage; }
        public void setAssetStage(String assetStage) { this.assetStage = assetStage; }
        public LocalDate getExpectedHandoverDate() { return expectedHandoverDate; }
        public void setExpectedHandoverDate(LocalDate expectedHandoverDate) { this.expectedHandoverDate = expectedHandoverDate; }
        public LocalDate getActualHandoverDate() { return actualHandoverDate; }
        public void setActualHandoverDate(LocalDate actualHandoverDate) { this.actualHandoverDate = actualHandoverDate; }
    }

    class NewPurchaseContract {
        private Long ownerUnitId;
        private String contractNo;
        private BigDecimal purchasePrice;
        private LocalDate signedDate;
        private LocalDate handoverDate;
        private String status;

        public Long getOwnerUnitId() { return ownerUnitId; }
        public void setOwnerUnitId(Long ownerUnitId) { this.ownerUnitId = ownerUnitId; }
        public String getContractNo() { return contractNo; }
        public void setContractNo(String contractNo) { this.contractNo = contractNo; }
        public BigDecimal getPurchasePrice() { return purchasePrice; }
        public void setPurchasePrice(BigDecimal purchasePrice) { this.purchasePrice = purchasePrice; }
        public LocalDate getSignedDate() { return signedDate; }
        public void setSignedDate(LocalDate signedDate) { this.signedDate = signedDate; }
        public LocalDate getHandoverDate() { return handoverDate; }
        public void setHandoverDate(LocalDate handoverDate) { this.handoverDate = handoverDate; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }
}
