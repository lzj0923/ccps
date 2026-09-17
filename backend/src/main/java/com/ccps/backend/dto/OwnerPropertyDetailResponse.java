package com.ccps.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record OwnerPropertyDetailResponse(
        Property property,
        BankAccount bankAccount,
        List<Mandate> mandates,
        List<Lease> leases,
        List<Photo> photos) {

    public record Property(Long ownerUnitId, String projectName, String address, String countryCode,
            String building, String floorNo, String unitNo, BigDecimal areaSqm) { }

    public record BankAccount(Long id, String bankName, String accountName, String maskedAccountNo,
            String bankAddress, String branchCode, String swiftCode, boolean overseasBank) { }

    public record Mandate(Long id, String mandateNo, String mandateType, LocalDate startDate,
            LocalDate endDate, String status) { }

    public record Lease(Long id, String leaseNo, String tenantName, BigDecimal monthlyRent,
            BigDecimal depositAmount, LocalDate startDate, LocalDate endDate, String status,
            Long contractDocumentId) { }

    public record Photo(Long id, Long leaseId, String rentalStage, String versionMonth,
            Long documentId, String title, String category, String description, boolean cover,
            String fileName, String mimeType, LocalDateTime createdAt) { }
}
