package com.ccps.backend.generated.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * <p>
 * 
 * </p>
 *
 * @author CCPS
 * @since 2026-07-15
 */
@TableName("units")
public class Units implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("project_id")
    private Long projectId;

    @TableField("building")
    private String building;

    @TableField("floor_no")
    private String floorNo;

    @TableField("unit_no")
    private String unitNo;

    @TableField("unit_type")
    private String unitType;

    @TableField("area_sqm")
    private BigDecimal areaSqm;

    @TableField("bedroom_count")
    private Byte bedroomCount;

    @TableField("listing_status")
    private String listingStatus;

    @TableField("rental_listing_status")
    private String rentalListingStatus;

    @TableField("rental_off_market_reason_code")
    private String rentalOffMarketReasonCode;

    @TableField("rental_off_market_note")
    private String rentalOffMarketNote;

    @TableField("rental_off_market_at")
    private LocalDateTime rentalOffMarketAt;

    @TableField("rental_off_market_by")
    private Long rentalOffMarketBy;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public String getBuilding() {
        return building;
    }

    public void setBuilding(String building) {
        this.building = building;
    }

    public String getFloorNo() {
        return floorNo;
    }

    public void setFloorNo(String floorNo) {
        this.floorNo = floorNo;
    }

    public String getUnitNo() {
        return unitNo;
    }

    public void setUnitNo(String unitNo) {
        this.unitNo = unitNo;
    }

    public String getUnitType() {
        return unitType;
    }

    public void setUnitType(String unitType) {
        this.unitType = unitType;
    }

    public BigDecimal getAreaSqm() {
        return areaSqm;
    }

    public void setAreaSqm(BigDecimal areaSqm) {
        this.areaSqm = areaSqm;
    }

    public Byte getBedroomCount() {
        return bedroomCount;
    }

    public void setBedroomCount(Byte bedroomCount) {
        this.bedroomCount = bedroomCount;
    }

    public String getListingStatus() {
        return listingStatus;
    }

    public void setListingStatus(String listingStatus) {
        this.listingStatus = listingStatus;
    }

    public String getRentalListingStatus() { return rentalListingStatus; }
    public void setRentalListingStatus(String rentalListingStatus) { this.rentalListingStatus = rentalListingStatus; }
    public String getRentalOffMarketReasonCode() { return rentalOffMarketReasonCode; }
    public void setRentalOffMarketReasonCode(String rentalOffMarketReasonCode) { this.rentalOffMarketReasonCode = rentalOffMarketReasonCode; }
    public String getRentalOffMarketNote() { return rentalOffMarketNote; }
    public void setRentalOffMarketNote(String rentalOffMarketNote) { this.rentalOffMarketNote = rentalOffMarketNote; }
    public LocalDateTime getRentalOffMarketAt() { return rentalOffMarketAt; }
    public void setRentalOffMarketAt(LocalDateTime rentalOffMarketAt) { this.rentalOffMarketAt = rentalOffMarketAt; }
    public Long getRentalOffMarketBy() { return rentalOffMarketBy; }
    public void setRentalOffMarketBy(Long rentalOffMarketBy) { this.rentalOffMarketBy = rentalOffMarketBy; }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "Units{" +
            "id = " + id +
            ", projectId = " + projectId +
            ", building = " + building +
            ", floorNo = " + floorNo +
            ", unitNo = " + unitNo +
            ", unitType = " + unitType +
            ", areaSqm = " + areaSqm +
            ", bedroomCount = " + bedroomCount +
            ", listingStatus = " + listingStatus +
            ", createdAt = " + createdAt +
            ", updatedAt = " + updatedAt +
            "}";
    }
}
