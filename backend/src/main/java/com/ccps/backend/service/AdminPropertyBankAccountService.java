package com.ccps.backend.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.ccps.backend.dto.AdminPropertyBankAccountRequest;
import com.ccps.backend.dto.AdminPropertyBankAccountResponse;
import com.ccps.backend.mapper.AdminPropertyBankAccountMapper;
import com.ccps.backend.mapper.AdminPropertyBankAccountMapper.AccountRow;

@Service
public class AdminPropertyBankAccountService {
    private final AdminPropertyBankAccountMapper mapper;
    public AdminPropertyBankAccountService(AdminPropertyBankAccountMapper mapper){this.mapper=mapper;}

    @Transactional(readOnly=true)
    public List<AdminPropertyBankAccountResponse> list(Long ownerId,Long ownerUnitId){requireProperty(ownerId,ownerUnitId);return mapper.list(ownerUnitId).stream().map(this::response).toList();}
    @Transactional
    public AdminPropertyBankAccountResponse create(Long actorId,Long ownerId,Long ownerUnitId,AdminPropertyBankAccountRequest request){requireProperty(ownerId,ownerUnitId);validate(request);AccountRow row=toRow(ownerUnitId,request);row.setCreatedBy(actorId);if(mapper.insert(row)!=1||row.getId()==null)throw conflict("Unable to create bank account");return response(requireAccount(ownerUnitId,row.getId()));}
    @Transactional
    public AdminPropertyBankAccountResponse update(Long ownerId,Long ownerUnitId,Long accountId,AdminPropertyBankAccountRequest request){requireProperty(ownerId,ownerUnitId);validate(request);requireAccount(ownerUnitId,accountId);AccountRow row=toRow(ownerUnitId,request);row.setId(accountId);if(mapper.update(row)!=1)throw conflict("Bank account was changed by another request");return response(requireAccount(ownerUnitId,accountId));}
    @Transactional
    public void delete(Long ownerId,Long ownerUnitId,Long accountId){requireProperty(ownerId,ownerUnitId);requireAccount(ownerUnitId,accountId);if(mapper.delete(ownerUnitId,accountId)!=1)throw conflict("Bank account was changed by another request");}

    private AccountRow toRow(Long ownerUnitId,AdminPropertyBankAccountRequest request){AccountRow row=new AccountRow();row.setOwnerUnitId(ownerUnitId);row.setItemName(request.itemName().trim());row.setPaymentName(request.paymentName().trim());row.setAccountNo(request.accountNo().trim());row.setBankAddress(blankToNull(request.bankAddress()));row.setBranchCode(blankToNull(request.branchCode()));row.setSwiftCode(blankToNull(request.swiftCode()));row.setTransferLimit(positiveOrNull(request.transferLimit()));row.setOverseasBank(Boolean.TRUE.equals(request.overseasBank()));row.setOverseasTransferFee(Boolean.TRUE.equals(request.overseasBank())?zero(request.overseasTransferFee()):java.math.BigDecimal.ZERO);row.setRemarks(blankToNull(request.remarks()));return row;}
    private void validate(AdminPropertyBankAccountRequest request){if(request==null)throw bad("Bank account is required");required(request.itemName(),120,"Bank item");required(request.paymentName(),160,"Payment name");required(request.accountNo(),120,"Account number");optional(request.bankAddress(),500,"Bank address");optional(request.branchCode(),80,"Branch code");optional(request.swiftCode(),80,"SWIFT code");nonNegative(request.transferLimit(),"Transfer limit");nonNegative(request.overseasTransferFee(),"Overseas transfer fee");optional(request.remarks(),1000,"Remarks");}
    private void required(String value,int max,String label){if(value==null||value.isBlank()||value.trim().length()>max)throw bad(label+" is required and must not exceed "+max+" characters");}
    private void requireProperty(Long ownerId,Long ownerUnitId){if(mapper.ownsProperty(ownerId,ownerUnitId)!=1)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Property not found");}
    private AccountRow requireAccount(Long ownerUnitId,Long accountId){AccountRow row=mapper.find(ownerUnitId,accountId);if(row==null)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Bank account not found");return row;}
    private void optional(String value,int max,String label){if(value!=null&&value.trim().length()>max)throw bad(label+" must not exceed "+max+" characters");}
    private AdminPropertyBankAccountResponse response(AccountRow row){return new AdminPropertyBankAccountResponse(row.getId(),row.getOwnerUnitId(),row.getItemName(),row.getPaymentName(),row.getAccountNo(),row.getBankAddress(),row.getBranchCode(),row.getSwiftCode(),row.getTransferLimit(),Boolean.TRUE.equals(row.getOverseasBank()),zero(row.getOverseasTransferFee()),row.getRemarks(),row.getCreatedBy(),row.getCreatedByName(),row.getCreatedAt(),row.getUpdatedAt());}
    private java.math.BigDecimal positiveOrNull(java.math.BigDecimal value){return value==null||value.signum()<=0?null:value;}
    private java.math.BigDecimal zero(java.math.BigDecimal value){return value==null?java.math.BigDecimal.ZERO:value;}
    private void nonNegative(java.math.BigDecimal value,String label){if(value!=null&&(value.signum()<0||value.scale()>2))throw bad(label+" must be zero or greater with at most two decimals");}
    private String blankToNull(String value){return value==null||value.isBlank()?null:value.trim();}
    private ResponseStatusException bad(String message){return new ResponseStatusException(HttpStatus.BAD_REQUEST,message);} private ResponseStatusException conflict(String message){return new ResponseStatusException(HttpStatus.CONFLICT,message);}
}
