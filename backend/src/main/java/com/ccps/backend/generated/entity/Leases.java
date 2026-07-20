package com.ccps.backend.generated.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * <p>
 * 
 * </p>
 *
 * @author CCPS
 * @since 2026-07-15
 */
@TableName("leases")
public class Leases implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("unit_id")
    private Long unitId;

    @TableField("tenant_id")
    private Long tenantId;

    @TableField("lease_no")
    private String leaseNo;

    @TableField("start_date")
    private LocalDate startDate;

    @TableField("end_date")
    private LocalDate endDate;

    @TableField("monthly_rent")
    private BigDecimal monthlyRent;

    @TableField("deposit_amount")
    private BigDecimal depositAmount;

    @TableField("payment_day")
    private Byte paymentDay;

    @TableField("status")
    private String status;

    @TableField("contract_document_id")
    private Long contractDocumentId;

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

    public Long getUnitId() {
        return unitId;
    }

    public void setUnitId(Long unitId) {
        this.unitId = unitId;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public String getLeaseNo() {
        return leaseNo;
    }

    public void setLeaseNo(String leaseNo) {
        this.leaseNo = leaseNo;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public BigDecimal getMonthlyRent() {
        return monthlyRent;
    }

    public void setMonthlyRent(BigDecimal monthlyRent) {
        this.monthlyRent = monthlyRent;
    }

    public BigDecimal getDepositAmount() {
        return depositAmount;
    }

    public void setDepositAmount(BigDecimal depositAmount) {
        this.depositAmount = depositAmount;
    }

    public Byte getPaymentDay() {
        return paymentDay;
    }

    public void setPaymentDay(Byte paymentDay) {
        this.paymentDay = paymentDay;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getContractDocumentId() {
        return contractDocumentId;
    }

    public void setContractDocumentId(Long contractDocumentId) {
        this.contractDocumentId = contractDocumentId;
    }

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
        return "Leases{" +
            "id = " + id +
            ", unitId = " + unitId +
            ", tenantId = " + tenantId +
            ", leaseNo = " + leaseNo +
            ", startDate = " + startDate +
            ", endDate = " + endDate +
            ", monthlyRent = " + monthlyRent +
            ", depositAmount = " + depositAmount +
            ", paymentDay = " + paymentDay +
            ", status = " + status +
            ", contractDocumentId = " + contractDocumentId +
            ", createdAt = " + createdAt +
            ", updatedAt = " + updatedAt +
            "}";
    }
}
