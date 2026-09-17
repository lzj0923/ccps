package com.ccps.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record OwnerDashboardResponse(
        Summary summary,
        List<Property> properties,
        List<Notification> notifications,
        List<PendingItem> pendingItems) {

    public record Summary(
            int propertyCount,
            BigDecimal monthlyRentIncome,
            BigDecimal unpaidPropertyAmount,
            BigDecimal tenantDepositAmount,
            BigDecimal reserveBalance,
            int pendingMaintenanceCount) {
    }

    public record PendingItem(String type, String title, String detail, int count) {
    }

    public static class Property {
        private Long ownerUnitId;
        private Long coverDocumentId;
        private Long unitId;
        private String projectName;
        private String city;
        private String countryCode;
        private String unitNo;
        private String unitType;
        private BigDecimal areaSqm;
        private Integer bedroomCount;
        private String assetStage;
        private LocalDate expectedHandoverDate;
        private LocalDate actualHandoverDate;
        private String servicesCsv;
        private BigDecimal purchasePrice;
        private String currency;
        private LocalDate purchaseDate;
        private BigDecimal paidAmount;
        private BigDecimal remainingAmount;
        private Integer paidInstallmentCount;
        private Integer totalInstallmentCount;
        private LocalDate nextDueDate;
        private String paymentStatus;
        private String tenantName;
        private String leaseNo;
        private LocalDate leaseStartDate;
        private BigDecimal monthlyRent;
        private BigDecimal tenantDepositAmount;
        private LocalDate leaseEndDate;
        private BigDecimal currentMonthRentDue;
        private BigDecimal currentMonthRentPaid;
        private BigDecimal currentMonthRentOutstanding;
        private BigDecimal reserveMinimumBalance;
        private BigDecimal reserveBalance;
        private BigDecimal monthlyIncome;
        private BigDecimal monthlyExpense;
        private Integer pendingMaintenanceCount;

        public Long getOwnerUnitId() { return ownerUnitId; }
        public Long getCoverDocumentId() { return coverDocumentId; }
        public void setCoverDocumentId(Long coverDocumentId) { this.coverDocumentId = coverDocumentId; }
        public void setOwnerUnitId(Long ownerUnitId) { this.ownerUnitId = ownerUnitId; }
        public Long getUnitId() { return unitId; }
        public void setUnitId(Long unitId) { this.unitId = unitId; }
        public String getProjectName() { return projectName; }
        public void setProjectName(String projectName) { this.projectName = projectName; }
        public String getCity() { return city; }
        public void setCity(String city) { this.city = city; }
        public String getCountryCode() { return countryCode; }
        public void setCountryCode(String countryCode) { this.countryCode = countryCode; }
        public String getUnitNo() { return unitNo; }
        public void setUnitNo(String unitNo) { this.unitNo = unitNo; }
        public String getUnitType() { return unitType; }
        public void setUnitType(String unitType) { this.unitType = unitType; }
        public BigDecimal getAreaSqm() { return areaSqm; }
        public void setAreaSqm(BigDecimal areaSqm) { this.areaSqm = areaSqm; }
        public Integer getBedroomCount() { return bedroomCount; }
        public void setBedroomCount(Integer bedroomCount) { this.bedroomCount = bedroomCount; }
        public String getAssetStage() { return assetStage; }
        public void setAssetStage(String assetStage) { this.assetStage = assetStage; }
        public LocalDate getExpectedHandoverDate() { return expectedHandoverDate; }
        public void setExpectedHandoverDate(LocalDate expectedHandoverDate) { this.expectedHandoverDate = expectedHandoverDate; }
        public LocalDate getActualHandoverDate() { return actualHandoverDate; }
        public void setActualHandoverDate(LocalDate actualHandoverDate) { this.actualHandoverDate = actualHandoverDate; }
        public List<String> getServices() {
            return servicesCsv == null || servicesCsv.isBlank() ? List.of() : List.of(servicesCsv.split(","));
        }
        public void setServicesCsv(String servicesCsv) { this.servicesCsv = servicesCsv; }
        public BigDecimal getPurchasePrice() { return purchasePrice; }
        public void setPurchasePrice(BigDecimal purchasePrice) { this.purchasePrice = purchasePrice; }
        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }
        public LocalDate getPurchaseDate() { return purchaseDate; }
        public void setPurchaseDate(LocalDate purchaseDate) { this.purchaseDate = purchaseDate; }
        public BigDecimal getPaidAmount() { return paidAmount; }
        public void setPaidAmount(BigDecimal paidAmount) { this.paidAmount = paidAmount; }
        public BigDecimal getRemainingAmount() { return remainingAmount; }
        public void setRemainingAmount(BigDecimal remainingAmount) { this.remainingAmount = remainingAmount; }
        public Integer getPaidInstallmentCount() { return paidInstallmentCount; }
        public void setPaidInstallmentCount(Integer paidInstallmentCount) { this.paidInstallmentCount = paidInstallmentCount; }
        public Integer getTotalInstallmentCount() { return totalInstallmentCount; }
        public void setTotalInstallmentCount(Integer totalInstallmentCount) { this.totalInstallmentCount = totalInstallmentCount; }
        public LocalDate getNextDueDate() { return nextDueDate; }
        public void setNextDueDate(LocalDate nextDueDate) { this.nextDueDate = nextDueDate; }
        public String getPaymentStatus() { return paymentStatus; }
        public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }
        public String getTenantName() { return tenantName; }
        public void setTenantName(String tenantName) { this.tenantName = tenantName; }
        public String getLeaseNo() { return leaseNo; }
        public void setLeaseNo(String leaseNo) { this.leaseNo = leaseNo; }
        public LocalDate getLeaseStartDate() { return leaseStartDate; }
        public void setLeaseStartDate(LocalDate leaseStartDate) { this.leaseStartDate = leaseStartDate; }
        public BigDecimal getMonthlyRent() { return monthlyRent; }
        public void setMonthlyRent(BigDecimal monthlyRent) { this.monthlyRent = monthlyRent; }
        public BigDecimal getTenantDepositAmount() { return tenantDepositAmount; }
        public void setTenantDepositAmount(BigDecimal tenantDepositAmount) { this.tenantDepositAmount = tenantDepositAmount; }
        public LocalDate getLeaseEndDate() { return leaseEndDate; }
        public void setLeaseEndDate(LocalDate leaseEndDate) { this.leaseEndDate = leaseEndDate; }
        public BigDecimal getCurrentMonthRentDue() { return currentMonthRentDue; }
        public void setCurrentMonthRentDue(BigDecimal currentMonthRentDue) { this.currentMonthRentDue = currentMonthRentDue; }
        public BigDecimal getCurrentMonthRentPaid() { return currentMonthRentPaid; }
        public void setCurrentMonthRentPaid(BigDecimal currentMonthRentPaid) { this.currentMonthRentPaid = currentMonthRentPaid; }
        public BigDecimal getCurrentMonthRentOutstanding() { return currentMonthRentOutstanding; }
        public void setCurrentMonthRentOutstanding(BigDecimal currentMonthRentOutstanding) { this.currentMonthRentOutstanding = currentMonthRentOutstanding; }
        public BigDecimal getReserveMinimumBalance() { return reserveMinimumBalance; }
        public void setReserveMinimumBalance(BigDecimal reserveMinimumBalance) { this.reserveMinimumBalance = reserveMinimumBalance; }
        public BigDecimal getReserveBalance() { return reserveBalance; }
        public void setReserveBalance(BigDecimal reserveBalance) { this.reserveBalance = reserveBalance; }
        public BigDecimal getMonthlyIncome() { return monthlyIncome; }
        public void setMonthlyIncome(BigDecimal monthlyIncome) { this.monthlyIncome = monthlyIncome; }
        public BigDecimal getMonthlyExpense() { return monthlyExpense; }
        public void setMonthlyExpense(BigDecimal monthlyExpense) { this.monthlyExpense = monthlyExpense; }
        public Integer getPendingMaintenanceCount() { return pendingMaintenanceCount; }
        public void setPendingMaintenanceCount(Integer pendingMaintenanceCount) { this.pendingMaintenanceCount = pendingMaintenanceCount; }
    }

    public static class Notification {
        private Long id;
        private String title;
        private String body;
        private String priority;
        private String status;
        private LocalDate createdDate;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getBody() { return body; }
        public void setBody(String body) { this.body = body; }
        public String getPriority() { return priority; }
        public void setPriority(String priority) { this.priority = priority; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public LocalDate getCreatedDate() { return createdDate; }
        public void setCreatedDate(LocalDate createdDate) { this.createdDate = createdDate; }
    }
}
