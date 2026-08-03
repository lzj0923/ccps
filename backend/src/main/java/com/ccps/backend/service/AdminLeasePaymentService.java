package com.ccps.backend.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.ccps.backend.dto.AdminLeasePaymentResponse;
import com.ccps.backend.dto.AdminLeasePaymentUpdateRequest;
import com.ccps.backend.mapper.AdminLeasePaymentMapper;
import com.ccps.backend.mapper.AdminLeasePaymentMapper.PaymentRow;

@Service
public class AdminLeasePaymentService {
    private static final Set<String> METHODS=Set.of("cash","bank_transfer","online_payment");
    private final AdminLeasePaymentMapper mapper;private final Clock clock;
    @Autowired
    public AdminLeasePaymentService(AdminLeasePaymentMapper mapper){this(mapper,Clock.systemDefaultZone());}
    AdminLeasePaymentService(AdminLeasePaymentMapper mapper,Clock clock){this.mapper=mapper;this.clock=clock;}

    @Transactional(readOnly=true)
    public List<AdminLeasePaymentResponse> list(Long leaseId){return mapper.list(leaseId).stream().map(this::response).toList();}

    @Transactional
    public AdminLeasePaymentResponse update(Long actorId,Long leaseId,Long paymentId,AdminLeasePaymentUpdateRequest request){
        validate(request);PaymentRow row=require(leaseId,paymentId);if("voided".equals(row.getPaymentStatus()))throw conflict("Rent payment is already voided");
        BigDecimal delta=request.amount().subtract(row.getAmount());
        if(delta.signum()!=0&&mapper.adjustInvoice(row.getInvoiceId(),delta)!=1)throw bad("Payment amount exceeds invoice balance");
        String payer=blank(request.payerName())==null?row.getPayerName():request.payerName().trim();String reference=blank(request.paymentReference());String note=blank(request.note());
        if(mapper.updateFinance(row.getFinanceRecordId(),request.amount(),request.paymentDate(),request.paymentMethod(),actorId)!=1)throw conflict("Rent payment state changed; reload and try again");
        mapper.updateReceipt(row.getFinanceRecordId(),payer,reference,note);mapper.updateCashflow(row.getFinanceRecordId(),request.paymentDate(),"租金收款 · "+row.getLeaseNo());
        mapper.audit(actorId,"update_rent_payment",row.getFinanceRecordId(),json(row.getAmount()),json(request.amount()));
        return mapper.list(leaseId).stream().filter(item->item.getPaymentId().equals(paymentId)).findFirst().map(this::response).orElseThrow(()->conflict("Unable to reload rent payment"));
    }

    @Transactional
    public void delete(Long actorId,Long leaseId,Long paymentId){
        PaymentRow row=require(leaseId,paymentId);if("voided".equals(row.getPaymentStatus()))return;
        if(mapper.adjustInvoice(row.getInvoiceId(),row.getAmount().negate())!=1)throw conflict("Unable to reverse invoice payment");
        if(mapper.voidFinance(row.getFinanceRecordId(),actorId)!=1)throw conflict("Rent payment state changed; reload and try again");
        mapper.audit(actorId,"void_rent_payment",row.getFinanceRecordId(),json(row.getAmount()),"{\"status\":\"voided\"}");
    }

    private void validate(AdminLeasePaymentUpdateRequest r){if(r==null)throw bad("Rent payment is required");if(!METHODS.contains(r.paymentMethod()))throw bad("Invalid rent payment method");if(r.paymentDate().isAfter(LocalDate.now(clock)))throw bad("Rent payment date cannot be in the future");if(!"cash".equals(r.paymentMethod())&&blank(r.paymentReference())==null)throw bad("Payment reference is required for non-cash rent collection");}
    private PaymentRow require(Long leaseId,Long paymentId){PaymentRow row=mapper.lock(leaseId,paymentId);if(row==null)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Rent payment not found");return row;}
    private AdminLeasePaymentResponse response(PaymentRow r){return new AdminLeasePaymentResponse(r.getPaymentId(),r.getFinanceRecordId(),r.getInvoiceId(),r.getLeaseId(),r.getLeaseNo(),r.getBillingMonth(),r.getTransactionNo(),r.getAmount(),r.getPaymentDate(),r.getPaymentMethod(),r.getPayerName(),r.getPaymentReference(),r.getNote(),r.getProofDocumentId(),r.getProofName(),r.getSyncStatus(),r.getCreatedAt());}
    private String blank(String v){return v==null||v.isBlank()?null:v.trim();}private String json(BigDecimal amount){return "{\"amount\":"+amount.toPlainString()+"}";}
    private ResponseStatusException bad(String m){return new ResponseStatusException(HttpStatus.BAD_REQUEST,m);}private ResponseStatusException conflict(String m){return new ResponseStatusException(HttpStatus.CONFLICT,m);}
}
