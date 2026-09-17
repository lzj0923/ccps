package com.ccps.backend.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.ccps.backend.dto.AdminFundOperationsRequest.GenerateRemittanceBatch;
import com.ccps.backend.dto.AdminFundOperationsRequest.InternalTransfer;
import com.ccps.backend.dto.AdminFundOperationsRequest.RemittanceSetting;
import com.ccps.backend.dto.AdminFundOperationsRequest.Review;
import com.ccps.backend.dto.AdminFundOperationsResponse;
import com.ccps.backend.dto.AdminRecordCreateResponse;
import com.ccps.backend.dto.AdminReserveBatchRefundRequest;
import com.ccps.backend.mapper.AdminFundOperationsMapper;
import com.ccps.backend.mapper.AdminFundOperationsMapper.AccountOptionRow;
import com.ccps.backend.mapper.AdminFundOperationsMapper.InternalTransferRecord;
import com.ccps.backend.mapper.AdminFundOperationsMapper.InternalTransferRow;
import com.ccps.backend.mapper.AdminFundOperationsMapper.RemittanceBatchRecord;
import com.ccps.backend.mapper.AdminFundOperationsMapper.RemittanceBatchRow;
import com.ccps.backend.mapper.AdminFundOperationsMapper.RemittanceCandidateRow;
import com.ccps.backend.mapper.AdminFundOperationsMapper.RemittanceItemRow;
import com.ccps.backend.mapper.AdminFundOperationsMapper.RemittanceSettingRow;
import com.ccps.backend.mapper.AdminFundOperationsMapper.RemittanceSubmitRow;
import com.ccps.backend.mapper.AdminFundOperationsMapper.TransferAccountRow;

@Service
public class AdminFundOperationsService {
    private static final DateTimeFormatter NUMBER_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private final AdminFundOperationsMapper mapper;
    private final AdminReserveManagementService reserveService;

    public AdminFundOperationsService(AdminFundOperationsMapper mapper,
            AdminReserveManagementService reserveService) {
        this.mapper = mapper;
        this.reserveService = reserveService;
    }

    @Transactional(readOnly = true)
    public AdminFundOperationsResponse overview() {
        var accounts = mapper.findAccountOptions().stream().map(this::toAccount).toList();
        var transfers = mapper.findTransfers().stream().map(this::toTransfer).toList();
        var settings = mapper.findRemittanceSettings().stream().map(this::toSetting).toList();
        var batches = mapper.findRemittanceBatches().stream().map(this::toBatch).toList();
        return new AdminFundOperationsResponse(accounts, transfers, settings, batches);
    }

    @Transactional
    public AdminFundOperationsResponse createTransfer(Long actorId, InternalTransfer request) {
        if (request.sourceReserveAccountId().equals(request.targetReserveAccountId())) {
            badRequest("来源和目标备用金账户不能相同");
        }
        Map<Long, TransferAccountRow> accounts = transferAccounts(request.sourceReserveAccountId(),
                request.targetReserveAccountId());
        TransferAccountRow source = accounts.get(request.sourceReserveAccountId());
        TransferAccountRow target = accounts.get(request.targetReserveAccountId());
        validateTransferAccounts(source, target, request.amount());

        InternalTransferRecord record = new InternalTransferRecord();
        record.setTransferNo(number("RIT"));
        record.setSourceReserveAccountId(source.getId());
        record.setTargetReserveAccountId(target.getId());
        record.setAmount(request.amount());
        record.setRequestedDate(request.requestedDate());
        record.setReason(request.reason().trim());
        record.setCreatedBy(actorId);
        if (mapper.insertTransfer(record) != 1 || record.getId() == null) conflict("内部调拨申请建立失败");
        mapper.insertAudit(actorId, "create_reserve_internal_transfer", "reserve_internal_transfer",
                record.getId(), "pending", record.getReason());
        return overview();
    }

    @Transactional
    public AdminFundOperationsResponse approveTransfer(Long actorId, Long transferId, Review review) {
        InternalTransferRecord transfer = requireTransfer(transferId, "pending");
        Map<Long, TransferAccountRow> accounts = transferAccounts(transfer.getSourceReserveAccountId(),
                transfer.getTargetReserveAccountId());
        TransferAccountRow source = accounts.get(transfer.getSourceReserveAccountId());
        TransferAccountRow target = accounts.get(transfer.getTargetReserveAccountId());
        validateTransferAccounts(source, target, transfer.getAmount());

        BigDecimal sourceAfter = source.getCurrentBalance().subtract(transfer.getAmount());
        BigDecimal targetAfter = target.getCurrentBalance().add(transfer.getAmount());
        if (mapper.debitTransferSource(source.getId(), transfer.getAmount()) != 1
                || mapper.creditTransferTarget(target.getId(), transfer.getAmount()) != 1) {
            conflict("内部调拨余额已变化，请重新检查");
        }
        String note = "内部调拨 " + transferId + "：" + text(review.note(), "审批通过");
        mapper.insertReserveTransaction(source.getId(), transferId, "transfer_out", transfer.getAmount(), sourceAfter, note, actorId);
        mapper.insertReserveTransaction(target.getId(), transferId, "transfer_in", transfer.getAmount(), targetAfter, note, actorId);
        if (mapper.reviewTransfer(transferId, "approved", trim(review.note()), actorId) != 1) {
            conflict("内部调拨已被其他请求处理");
        }
        mapper.insertAudit(actorId, "approve_reserve_internal_transfer", "reserve_internal_transfer",
                transferId, "approved", trim(review.note()));
        return overview();
    }

    @Transactional
    public AdminFundOperationsResponse rejectTransfer(Long actorId, Long transferId, Review review) {
        requireTransfer(transferId, "pending");
        if (mapper.reviewTransfer(transferId, "rejected", trim(review.note()), actorId) != 1) {
            conflict("内部调拨已被其他请求处理");
        }
        mapper.insertAudit(actorId, "reject_reserve_internal_transfer", "reserve_internal_transfer",
                transferId, "rejected", trim(review.note()));
        return overview();
    }

    @Transactional
    public AdminFundOperationsResponse reverseTransfer(Long actorId, Long transferId, Review review) {
        InternalTransferRecord transfer = requireTransfer(transferId, "approved");
        Map<Long, TransferAccountRow> accounts = transferAccounts(transfer.getSourceReserveAccountId(),
                transfer.getTargetReserveAccountId());
        TransferAccountRow originalSource = accounts.get(transfer.getSourceReserveAccountId());
        TransferAccountRow originalTarget = accounts.get(transfer.getTargetReserveAccountId());
        validateTransferAccounts(originalTarget, originalSource, transfer.getAmount());

        BigDecimal targetAfter = originalTarget.getCurrentBalance().subtract(transfer.getAmount());
        BigDecimal sourceAfter = originalSource.getCurrentBalance().add(transfer.getAmount());
        if (mapper.debitTransferSource(originalTarget.getId(), transfer.getAmount()) != 1
                || mapper.creditTransferTarget(originalSource.getId(), transfer.getAmount()) != 1) {
            conflict("冲正时账户余额已变化，请重新检查");
        }
        String note = "内部调拨冲正 " + transferId + "：" + text(review.note(), "冲正");
        mapper.insertReserveTransaction(originalTarget.getId(), transferId, "transfer_reverse_out", transfer.getAmount(), targetAfter, note, actorId);
        mapper.insertReserveTransaction(originalSource.getId(), transferId, "transfer_reverse_in", transfer.getAmount(), sourceAfter, note, actorId);
        if (mapper.reverseTransfer(transferId, trim(review.note()), actorId) != 1) {
            conflict("内部调拨已被其他请求处理");
        }
        mapper.insertAudit(actorId, "reverse_reserve_internal_transfer", "reserve_internal_transfer",
                transferId, "reversed", trim(review.note()));
        return overview();
    }

    @Transactional
    public AdminFundOperationsResponse saveRemittanceSetting(Long actorId, Long accountId,
            RemittanceSetting request) {
        AccountOptionRow account = mapper.findAccountOptions().stream()
                .filter(item -> item.getId().equals(accountId)).findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "备用金账户不存在"));
        if (Boolean.TRUE.equals(request.enabled()) && !"manual".equals(request.cycle())) {
            if (request.nextRemittanceDate() == null) badRequest("启用定期汇款时必须设置下次汇款日期");
            if (request.defaultBankAccountId() == null) badRequest("启用定期汇款时必须选择收款银行账户");
        }
        if (request.defaultBankAccountId() != null
                && mapper.countBankForAccount(accountId, request.defaultBankAccountId()) != 1) {
            badRequest("收款银行账户不属于该房产");
        }
        if (Boolean.TRUE.equals(request.holdEnabled()) && trim(request.holdReason()) == null) {
            badRequest("暂缓汇款时必须填写原因");
        }
        mapper.upsertRemittanceSetting(actorId, accountId, request.cycle(), request.nextRemittanceDate(),
                request.enabled(), request.holdEnabled(), trim(request.holdReason()), request.holdUntil(),
                request.retainedAmount(), request.taxRetainedAmount(), request.defaultBankAccountId());
        mapper.insertAudit(actorId, "update_owner_remittance_setting", "reserve_account", accountId,
                request.enabled() ? "enabled" : "disabled", trim(request.holdReason()));
        return overview();
    }

    @Transactional
    public AdminFundOperationsResponse generateRemittanceBatch(Long actorId, GenerateRemittanceBatch request) {
        List<RemittanceCandidateRow> candidates = mapper.lockDueRemittanceCandidates(request.scheduledDate());
        record Due(RemittanceCandidateRow row, BigDecimal amount) { }
        List<Due> due = candidates.stream().map(row -> {
            BigDecimal retained = max(zero(row.getMinimumBalance()), zero(row.getRetainedAmount()))
                    .add(zero(row.getTaxRetainedAmount()));
            return new Due(row, zero(row.getCurrentBalance()).subtract(retained));
        }).filter(item -> item.amount().signum() > 0).toList();
        if (due.isEmpty()) badRequest("没有到期、未暂缓且有可汇余额的房产");

        RemittanceBatchRecord batch = new RemittanceBatchRecord();
        batch.setBatchNo(number("ORB"));
        batch.setScheduledDate(request.scheduledDate());
        batch.setTotalAmount(due.stream().map(Due::amount).reduce(BigDecimal.ZERO, BigDecimal::add));
        batch.setItemCount(due.size());
        batch.setNote(trim(request.note()));
        batch.setCreatedBy(actorId);
        if (mapper.insertRemittanceBatch(batch) != 1 || batch.getId() == null) conflict("汇款批次建立失败");
        for (Due item : due) {
            RemittanceCandidateRow row = item.row();
            if (mapper.insertRemittanceItem(batch.getId(), row.getReserveAccountId(), row.getBankAccountId(),
                    item.amount(), max(zero(row.getMinimumBalance()), zero(row.getRetainedAmount())),
                    zero(row.getTaxRetainedAmount())) != 1) conflict("汇款批次明细建立失败");
        }
        mapper.insertAudit(actorId, "generate_owner_remittance_batch", "owner_remittance_batch",
                batch.getId(), "draft", trim(request.note()));
        return overview();
    }

    @Transactional
    public AdminFundOperationsResponse submitRemittanceBatch(Long actorId, Long batchId, Review review) {
        RemittanceBatchRecord batch = requireBatch(batchId, "draft");
        List<RemittanceSubmitRow> items = mapper.lockRemittanceItems(batchId);
        if (items.isEmpty()) conflict("汇款批次没有明细");
        for (RemittanceSubmitRow item : items) {
            String note = "定期业主汇款 " + batch.getBatchNo() + "；" + text(review.note(), "提交财务确认");
            List<AdminRecordCreateResponse> finances = reserveService.createRefunds(actorId,
                    new AdminReserveBatchRefundRequest(List.of(new AdminReserveBatchRefundRequest.Item(
                            item.getReserveAccountId(), item.getBankAccountId(), item.getAmount(), "bank_transfer")),
                            batch.getScheduledDate(), "bank_transfer", note));
            if (finances.isEmpty() || mapper.linkRemittanceFinance(item.getId(), finances.get(0).id()) != 1) {
                conflict("汇款明细已被其他请求处理");
            }
            for (AdminRecordCreateResponse finance : finances) {
                if (mapper.insertRemittanceFinanceLink(item.getId(), finance.id()) != 1) {
                    conflict("汇款拆分财务单关联失败");
                }
            }
            mapper.advanceRemittanceDate(item.getReserveAccountId(), batch.getScheduledDate(), actorId);
        }
        if (mapper.reviewRemittanceBatch(batchId, "submitted", actorId) != 1) conflict("汇款批次已被其他请求处理");
        mapper.insertAudit(actorId, "submit_owner_remittance_batch", "owner_remittance_batch",
                batchId, "submitted", trim(review.note()));
        return overview();
    }

    @Transactional
    public AdminFundOperationsResponse rejectRemittanceBatch(Long actorId, Long batchId, Review review) {
        requireBatch(batchId, "draft");
        if (mapper.reviewRemittanceBatch(batchId, "rejected", actorId) != 1) conflict("汇款批次已被其他请求处理");
        mapper.insertAudit(actorId, "reject_owner_remittance_batch", "owner_remittance_batch",
                batchId, "rejected", trim(review.note()));
        return overview();
    }

    private Map<Long, TransferAccountRow> transferAccounts(Long sourceId, Long targetId) {
        Map<Long, TransferAccountRow> rows = mapper.lockTransferAccounts(sourceId, targetId).stream()
                .collect(Collectors.toMap(TransferAccountRow::getId, Function.identity()));
        if (rows.size() != 2) badRequest("调拨账户不存在或未启用");
        return rows;
    }

    private void validateTransferAccounts(TransferAccountRow source, TransferAccountRow target, BigDecimal amount) {
        if (source == null || target == null || !source.getOwnerId().equals(target.getOwnerId())) {
            badRequest("只允许同一业主名下的房产内部调拨");
        }
        if (zero(source.getCurrentBalance()).subtract(amount).compareTo(zero(source.getMinimumBalance())) < 0) {
            badRequest("调拨后来源账户余额不能低于最低留存标准");
        }
    }

    private InternalTransferRecord requireTransfer(Long id, String status) {
        InternalTransferRecord row = mapper.lockTransfer(id);
        if (row == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "内部调拨不存在");
        if (!status.equals(row.getStatus())) conflict("内部调拨当前状态不允许该操作");
        return row;
    }

    private RemittanceBatchRecord requireBatch(Long id, String status) {
        RemittanceBatchRecord row = mapper.lockRemittanceBatch(id);
        if (row == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "汇款批次不存在");
        if (!status.equals(row.getStatus())) conflict("汇款批次当前状态不允许该操作");
        return row;
    }

    private AdminFundOperationsResponse.ReserveAccountOption toAccount(AccountOptionRow r) {
        return new AdminFundOperationsResponse.ReserveAccountOption(r.getId(), r.getOwnerId(), r.getOwnerUnitId(),
                r.getOwnerName(), r.getProjectName(), r.getUnitNo(), r.getCurrentBalance(), r.getMinimumBalance(),
                r.getDefaultBankAccountId(), r.getDefaultBankName(), r.getDefaultBankAccountNo());
    }

    private AdminFundOperationsResponse.InternalTransfer toTransfer(InternalTransferRow r) {
        return new AdminFundOperationsResponse.InternalTransfer(r.getId(), r.getTransferNo(),
                r.getSourceReserveAccountId(), r.getTargetReserveAccountId(), r.getOwnerName(),
                r.getSourceProperty(), r.getTargetProperty(), r.getAmount(), r.getRequestedDate(), r.getReason(),
                r.getStatus(), r.getReviewNote(), r.getCreatedByName(), r.getReviewedByName(), r.getReviewedAt(),
                r.getReversedAt(), r.getCreatedAt());
    }

    private AdminFundOperationsResponse.RemittanceSetting toSetting(RemittanceSettingRow r) {
        return new AdminFundOperationsResponse.RemittanceSetting(r.getId(), r.getReserveAccountId(), r.getOwnerName(),
                r.getPropertyName(), r.getCycle(), r.getNextRemittanceDate(), r.getEnabled(), r.getHoldEnabled(),
                r.getHoldReason(), r.getHoldUntil(), r.getRetainedAmount(), r.getTaxRetainedAmount(),
                r.getDefaultBankAccountId(), r.getBankName(), r.getBankAccountNo());
    }

    private AdminFundOperationsResponse.RemittanceBatch toBatch(RemittanceBatchRow r) {
        List<AdminFundOperationsResponse.RemittanceItem> items = mapper.findRemittanceItems(r.getId()).stream()
                .map(this::toItem).toList();
        return new AdminFundOperationsResponse.RemittanceBatch(r.getId(), r.getBatchNo(), r.getScheduledDate(),
                r.getStatus(), r.getTotalAmount(), r.getItemCount(), r.getNote(), r.getCreatedByName(),
                r.getReviewedByName(), r.getReviewedAt(), r.getCreatedAt(), items);
    }

    private AdminFundOperationsResponse.RemittanceItem toItem(RemittanceItemRow r) {
        return new AdminFundOperationsResponse.RemittanceItem(r.getId(), r.getReserveAccountId(), r.getOwnerName(),
                r.getPropertyName(), r.getBankName(), r.getBankAccountNo(), r.getAmount(), r.getRetainedAmount(),
                r.getTaxRetainedAmount(), r.getFinanceRecordId(), r.getTransactionNo(), r.getPaymentStatus(),
                r.getConfirmationStatus());
    }

    private String number(String prefix) {
        return prefix + "-" + LocalDateTime.now().format(NUMBER_TIME) + "-"
                + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }
    private BigDecimal zero(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }
    private BigDecimal max(BigDecimal left, BigDecimal right) { return left.max(right); }
    private String trim(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private String text(String value, String fallback) { return trim(value) == null ? fallback : value.trim(); }
    private void badRequest(String message) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
    private void conflict(String message) { throw new ResponseStatusException(HttpStatus.CONFLICT, message); }
}
