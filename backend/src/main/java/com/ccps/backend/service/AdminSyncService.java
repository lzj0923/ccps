package com.ccps.backend.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.ccps.backend.dto.AdminSyncPreviewResponse;
import com.ccps.backend.dto.AdminSyncRequest;
import com.ccps.backend.dto.AdminSyncResponse;
import com.ccps.backend.mapper.AdminSyncMapper;
import com.ccps.backend.mapper.AdminSyncMapper.BatchRow;
import com.ccps.backend.mapper.AdminSyncMapper.FinanceSyncRow;
import com.ccps.backend.mapper.AdminSyncMapper.ItemRow;
import com.ccps.backend.mapper.AdminSyncMapper.NewBatch;
import com.ccps.backend.mapper.AdminSyncMapper.NewItem;
import com.ccps.backend.mapper.AdminSyncMapper.SummaryRow;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class AdminSyncService {
    private final AdminSyncMapper mapper;
    private final AccountingSyncGateway gateway;
    private final FileAccountingSyncGateway fileGateway;
    private final ObjectMapper objectMapper;

    public AdminSyncService(AdminSyncMapper mapper, AccountingSyncGateway gateway,
                            FileAccountingSyncGateway fileGateway, ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.gateway = gateway;
        this.fileGateway = fileGateway;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public AdminSyncResponse overview() {
        SummaryRow summary = mapper.findSummary();
        return new AdminSyncResponse(new AdminSyncResponse.Summary(
                count(summary == null ? null : summary.getEligibleCount()),
                count(summary == null ? null : summary.getExportedCount()),
                count(summary == null ? null : summary.getFailedCount()),
                count(summary == null ? null : summary.getBatchCount()),
                count(summary == null ? null : summary.getItemCount())),
                mapper.findBatches().stream().map(this::toBatch).toList(),
                mapper.findItems().stream().map(this::toItem).toList());
    }

    @Transactional(readOnly = true)
    public AdminSyncPreviewResponse preview(AdminSyncRequest request) {
        List<FinanceSyncRow> rows = eligible(request);
        List<String> warnings = new ArrayList<>();
        long missingParty = rows.stream().filter(row -> blank(row.getPartyName())).count();
        long missingProject = rows.stream().filter(row -> blank(row.getProjectCode())).count();
        if (missingParty > 0) warnings.add(missingParty + " 筆交易沒有往來單位，將使用 GENERAL 代碼");
        if (missingProject > 0) warnings.add(missingProject + " 筆交易沒有建案代碼");
        BigDecimal total = rows.stream().map(FinanceSyncRow::getAmount).map(this::zero).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new AdminSyncPreviewResponse(rows.size(), total, warnings, rows.stream().map(this::toPreview).toList());
    }

    @Transactional
    public AdminSyncResponse.Batch createBatch(Long actorId, AdminSyncRequest request) {
        List<FinanceSyncRow> rows = eligible(request);
        if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.CONFLICT, "No confirmed unsynchronized records match the selection");
        String batchNo = "SQL-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase(Locale.ROOT);
        NewBatch batch = new NewBatch(); batch.setBatchNo(batchNo); batch.setSourceModule(request.sourceModule());
        batch.setTotalCount(rows.size()); batch.setCreatedBy(actorId);
        if (mapper.insertBatch(batch) != 1 || batch.getId() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Accounting export batch could not be created");
        }
        List<NewItem> items = new ArrayList<>();
        for (FinanceSyncRow row : rows) {
            NewItem item = new NewItem(); item.setBatchId(batch.getId()); item.setEntityId(row.getId()); item.setPayloadJson(json(row));
            if (mapper.insertItem(item) != 1 || item.getId() == null) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Accounting export item could not be created");
            }
            items.add(item);
        }
        exportBatch(batch, rows, items);
        mapper.insertAudit(actorId, "create_accounting_export", batch.getId(), batchNo);
        return toBatch(mapper.findBatch(batch.getId()));
    }

    @Transactional
    public AdminSyncResponse.Batch retryBatch(Long actorId, Long batchId) {
        BatchRow batch = requireBatch(batchId);
        List<FinanceSyncRow> rows = mapper.findFailedBatchRecords(batchId);
        if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.CONFLICT, "This batch has no failed items to retry");
        mapper.restartBatch(batchId);
        List<NewItem> items = rows.stream().map(row -> {
            NewItem item = new NewItem(); item.setId(mapper.findBatchItemId(batchId, row.getId()));
            item.setBatchId(batchId); item.setEntityId(row.getId()); return item;
        }).toList();
        NewBatch retry = new NewBatch(); retry.setId(batchId); retry.setBatchNo(batch.getBatchNo());
        retry.setSourceModule(batch.getSourceModule()); retry.setTotalCount(batch.getTotalCount() == null ? rows.size() : batch.getTotalCount());
        exportBatch(retry, rows, items);
        mapper.insertAudit(actorId, "retry_accounting_export", batchId, batch.getBatchNo());
        return toBatch(mapper.findBatch(batchId));
    }

    public Download download(Long batchId) {
        BatchRow batch = requireBatch(batchId);
        if (!"exported".equals(batch.getStatus()) && !"partial".equals(batch.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Accounting export file is not available");
        }
        Path path = fileGateway.locate(batch.getBatchNo());
        if (!Files.isRegularFile(path)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Accounting export file not found");
        try { return new Download(path, batch.getBatchNo() + ".csv", Files.size(path)); }
        catch (IOException exception) { throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to read accounting export", exception); }
    }

    private void exportBatch(NewBatch batch, List<FinanceSyncRow> rows, List<NewItem> items) {
        try {
            Path file = gateway.export(batch.getBatchNo(), rows.stream().map(this::toGatewayRecord).toList());
            for (int index = 0; index < rows.size(); index++) {
                FinanceSyncRow row = rows.get(index); NewItem item = items.get(index);
                mapper.markItemSuccess(item.getId(), file.getFileName() + "#" + row.getTransactionNo());
                mapper.updateFinanceSync(row.getId(), batch.getId(), "exported");
            }
            mapper.completeBatch(batch.getId(), "exported", rows.size(), 0);
        } catch (IOException | RuntimeException exception) {
            String message = exception.getMessage() == null ? "Accounting export failed" : exception.getMessage();
            for (int index = 0; index < rows.size(); index++) {
                mapper.markItemFailed(items.get(index).getId(), message);
                mapper.updateFinanceSync(rows.get(index).getId(), batch.getId(), "failed");
            }
            mapper.completeBatch(batch.getId(), "failed", 0, rows.size());
        }
    }

    private List<FinanceSyncRow> eligible(AdminSyncRequest request) {
        List<Long> ids = request.recordIds() == null ? List.of() : request.recordIds().stream().filter(java.util.Objects::nonNull).distinct().toList();
        return mapper.findEligibleRecords(request.sourceModule(), ids);
    }

    private AccountingSyncGateway.Record toGatewayRecord(FinanceSyncRow row) {
        return new AccountingSyncGateway.Record(row.getId(), row.getTransactionNo(), documentType(row.getRecordType()),
                row.getTransactionDate(), row.getPartyCode(), row.getPartyName(), row.getProjectCode(),
                row.getProjectName(), row.getUnitNo(), description(row), zero(row.getAmount()),
                row.getCurrency(), row.getPaymentMethod());
    }

    private String documentType(String type) {
        return switch (type == null ? "" : type) {
            case "property_payment" -> "PROPERTY_RECEIPT"; case "rent_payment" -> "RENT_RECEIPT";
            case "reserve_topup" -> "RESERVE_RECEIPT"; case "reserve_debit" -> "RESERVE_PAYMENT";
            case "cashflow" -> "CASHFLOW"; default -> "GENERAL_JOURNAL";
        };
    }
    private String description(FinanceSyncRow row) {
        return String.join(" · ", List.of(text(row.getProjectName(), "CCPS"), text(row.getUnitNo(), "GENERAL"), documentType(row.getRecordType())));
    }
    private String json(FinanceSyncRow row) {
        AccountingSyncGateway.Record record = toGatewayRecord(row);
        java.util.Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sourceId", record.sourceId()); payload.put("transactionNo", record.transactionNo());
        payload.put("documentType", record.documentType()); payload.put("documentDate", text(record.documentDate(), ""));
        payload.put("partyCode", record.partyCode()); payload.put("partyName", record.partyName());
        payload.put("projectCode", record.projectCode()); payload.put("projectName", record.projectName());
        payload.put("unitNo", record.unitNo()); payload.put("description", record.description());
        payload.put("amount", record.amount()); payload.put("currency", record.currency());
        payload.put("paymentMethod", record.paymentMethod());
        try { return objectMapper.writeValueAsString(payload); }
        catch (JsonProcessingException exception) { throw new IllegalArgumentException("Unable to serialize accounting record", exception); }
    }
    private AdminSyncPreviewResponse.Item toPreview(FinanceSyncRow row) { return new AdminSyncPreviewResponse.Item(row.getId(), row.getTransactionNo(), row.getRecordType(), row.getTransactionDate(), row.getPartyName(), row.getProjectName(), row.getUnitNo(), zero(row.getAmount()), row.getCurrency(), row.getSyncStatus()); }
    private AdminSyncResponse.Batch toBatch(BatchRow row) { return new AdminSyncResponse.Batch(row.getId(), row.getBatchNo(), row.getSourceModule(), row.getTriggerMode(), row.getStatus(), value(row.getTotalCount()), value(row.getSuccessCount()), value(row.getFailureCount()), row.getStartedAt(), row.getCompletedAt(), row.getCreatedByName(), row.getCreatedAt()); }
    private AdminSyncResponse.Item toItem(ItemRow row) { return new AdminSyncResponse.Item(row.getId(), row.getBatchId(), row.getBatchNo(), row.getEntityType(), row.getEntityId(), row.getOperation(), row.getStatus(), row.getExternalId(), row.getErrorMessage(), row.getTransactionNo(), row.getSyncedAt()); }
    private BatchRow requireBatch(Long id) { BatchRow row = mapper.findBatch(id); if (row == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Accounting batch not found"); return row; }
    private long count(Long value) { return value == null ? 0 : value; } private int value(Integer value) { return value == null ? 0 : value; }
    private BigDecimal zero(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }
    private boolean blank(String value) { return value == null || value.isBlank(); }
    private String text(String value, String fallback) { return blank(value) ? fallback : value; }
    private String text(Object value, String fallback) { return value == null || String.valueOf(value).isBlank() ? fallback : String.valueOf(value); }
    public record Download(Path path, String filename, long size) { }
}
