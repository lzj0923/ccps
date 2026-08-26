package com.ccps.backend.mapper;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.ccps.backend.dto.AdminOffMarketPropertyPageResponse.Item;
import com.ccps.backend.dto.AdminPropertyManagementHistoryResponse;

@Mapper
public interface AdminPropertyArchiveMapper {
    String LIST_FROM = """
            FROM units u
            JOIN projects p ON p.id=u.project_id
            JOIN owner_units ou ON ou.id=(SELECT ou2.id FROM owner_units ou2 WHERE ou2.unit_id=u.id ORDER BY (ou2.status='active') DESC,ou2.is_primary DESC,ou2.id DESC LIMIT 1)
            JOIN owners o ON o.id=ou.owner_id
            LEFT JOIN users actor ON actor.id=u.rental_off_market_by
            WHERE u.rental_listing_status='off_market'
            """;

    String LIST_FILTER = """
            <if test="keyword != null and keyword != ''">AND CONCAT_WS(' ',p.name,p.city,u.building,u.floor_no,u.unit_no,u.unit_type,o.full_name,COALESCE(o.mobile_phone,o.phone)) LIKE CONCAT('%',#{keyword},'%')</if>
            <if test="reasonCode != null and reasonCode != ''">AND u.rental_off_market_reason_code=#{reasonCode}</if>
            """;

    @Select({"<script>", "SELECT u.id AS unitId,ou.id AS ownerUnitId,o.id AS ownerId,o.full_name AS ownerName,COALESCE(o.mobile_phone,o.phone) AS ownerPhone,",
            "p.name AS projectName,p.city,u.building,u.floor_no AS floorNo,u.unit_no AS unitNo,u.unit_type AS unitType,u.listing_status AS listingStatus,",
            "ou.asset_stage AS assetStage,u.rental_off_market_reason_code AS reasonCode,u.rental_off_market_note AS note,u.rental_off_market_at AS offMarketAt,COALESCE(actor.display_name,'系统') AS offMarketByName,",
            "(SELECT COUNT(*) FROM leases l WHERE l.unit_id=u.id) AS leaseCount,",
            "((SELECT COUNT(*) FROM property_attachments pa WHERE pa.owner_unit_id=ou.id)+(SELECT COUNT(*) FROM property_photos pp WHERE pp.owner_unit_id=ou.id)+(SELECT COUNT(*) FROM property_contract_records pcr WHERE pcr.owner_unit_id=ou.id)) AS documentCount,",
            "(SELECT COUNT(*) FROM maintenance_work_orders mo WHERE mo.unit_id=u.id) AS maintenanceCount", LIST_FROM, LIST_FILTER,
            "ORDER BY u.rental_off_market_at DESC,u.id DESC LIMIT #{limit} OFFSET #{offset}", "</script>"})
    List<Item> findPage(@Param("keyword") String keyword, @Param("reasonCode") String reasonCode,
            @Param("limit") int limit, @Param("offset") int offset);

    @Select({"<script>", "SELECT COUNT(*)", LIST_FROM, LIST_FILTER, "</script>"})
    long count(@Param("keyword") String keyword, @Param("reasonCode") String reasonCode);

    @Select("SELECT COUNT(*) FROM units WHERE rental_listing_status='off_market' AND rental_off_market_at >= DATE_FORMAT(CURRENT_DATE,'%Y-%m-01')")
    long countThisMonth();

    @Select("SELECT COUNT(DISTINCT u.id) FROM units u JOIN owner_units ou ON ou.unit_id=u.id WHERE u.rental_listing_status='off_market' AND (EXISTS (SELECT 1 FROM property_attachments pa WHERE pa.owner_unit_id=ou.id) OR EXISTS (SELECT 1 FROM property_photos pp WHERE pp.owner_unit_id=ou.id) OR EXISTS (SELECT 1 FROM property_contract_records pcr WHERE pcr.owner_unit_id=ou.id))")
    long countWithDocuments();

    @Select("SELECT COUNT(*) FROM units u WHERE u.rental_listing_status='off_market' AND NOT EXISTS (SELECT 1 FROM leases l WHERE l.unit_id=u.id AND l.status='active') AND EXISTS (SELECT 1 FROM owner_units ou JOIN owner_unit_services ous ON ous.owner_unit_id=ou.id AND ous.service_type='RENTAL' AND ous.status='active' WHERE ou.unit_id=u.id AND ou.status='active' AND ou.asset_stage='OPERATING')")
    long countCanRelist();

    @Select("SELECT id,rental_listing_status AS rentalListingStatus,listing_status AS listingStatus FROM units WHERE id=#{unitId} FOR UPDATE")
    UnitState lockUnit(@Param("unitId") Long unitId);

    @Select("SELECT id,rental_listing_status AS rentalListingStatus,listing_status AS listingStatus FROM units WHERE id=#{unitId}")
    UnitState findUnit(@Param("unitId") Long unitId);

    @Select("SELECT COUNT(*) FROM leases WHERE unit_id=#{unitId} AND status='active'")
    int countActiveLeases(@Param("unitId") Long unitId);

    @Select("SELECT COUNT(*) FROM owner_units ou JOIN owner_unit_services ous ON ous.owner_unit_id=ou.id AND ous.service_type='RENTAL' AND ous.status='active' WHERE ou.unit_id=#{unitId} AND ou.status='active' AND ou.asset_stage='OPERATING'")
    int countActiveRentalServices(@Param("unitId") Long unitId);

    @Select("SELECT COUNT(*) FROM owner_units WHERE unit_id=#{unitId} AND status='active' AND asset_stage<>'DISPOSED'")
    int countRelistableOwnerships(@Param("unitId") Long unitId);

    @Update("UPDATE units SET rental_listing_status='off_market',rental_off_market_reason_code=#{reasonCode},rental_off_market_note=#{note},rental_off_market_at=NOW(),rental_off_market_by=#{actorId} WHERE id=#{unitId} AND rental_listing_status='listed'")
    int markOffMarket(@Param("unitId") Long unitId, @Param("reasonCode") String reasonCode,
            @Param("note") String note, @Param("actorId") Long actorId);

    @Update("UPDATE units SET rental_listing_status='listed',rental_off_market_reason_code=NULL,rental_off_market_note=NULL,rental_off_market_at=NULL,rental_off_market_by=NULL WHERE id=#{unitId} AND rental_listing_status='off_market'")
    int relist(@Param("unitId") Long unitId);

    @Insert("INSERT INTO unit_rental_listing_status_history(unit_id,action,reason_code,note,changed_by) VALUES(#{unitId},#{action},#{reasonCode},#{note},#{actorId})")
    int insertHistory(@Param("unitId") Long unitId, @Param("action") String action,
            @Param("reasonCode") String reasonCode, @Param("note") String note, @Param("actorId") Long actorId);

    @Select("SELECT h.id,h.action,h.reason_code AS reasonCode,h.note,h.changed_by AS changedBy,COALESCE(u.display_name,'系统') AS changedByName,h.created_at AS createdAt FROM unit_rental_listing_status_history h LEFT JOIN users u ON u.id=h.changed_by WHERE h.unit_id=#{unitId} ORDER BY h.created_at DESC,h.id DESC")
    List<AdminPropertyManagementHistoryResponse> history(@Param("unitId") Long unitId);

    class UnitState {
        private Long id;
        private String rentalListingStatus;
        private String listingStatus;

        public UnitState() {}

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getRentalListingStatus() { return rentalListingStatus; }
        public void setRentalListingStatus(String rentalListingStatus) { this.rentalListingStatus = rentalListingStatus; }
        public String getListingStatus() { return listingStatus; }
        public void setListingStatus(String listingStatus) { this.listingStatus = listingStatus; }
    }
}
