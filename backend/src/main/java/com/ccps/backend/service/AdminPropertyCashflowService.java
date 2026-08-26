package com.ccps.backend.service;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.ccps.backend.dto.AdminPropertyCashflowResponse;
import com.ccps.backend.mapper.AdminPropertyCashflowMapper;
import com.ccps.backend.mapper.AdminPropertyCashflowMapper.CashflowRow;
import com.ccps.backend.mapper.AdminPropertyCashflowMapper.CashflowWriteRow;
import com.ccps.backend.mapper.AdminPropertyCashflowMapper.DocumentRow;
import com.ccps.backend.mapper.AdminPropertyCashflowMapper.FinanceRow;
import com.ccps.backend.mapper.AdminPropertyCashflowMapper.PropertyContext;
import com.ccps.backend.mapper.AdminPropertyCashflowMapper.ReserveDebit;

@Service
public class AdminPropertyCashflowService {
    private static final long MAX_SIZE=15L*1024L*1024L;
    private static final Set<String> DIRECTIONS=Set.of("income","expense");
    private static final Set<String> CATEGORIES=Set.of("rent","maintenance","utilities","management","management_service_fee","land_tax","assessment_tax","fire_insurance","agency_commission","cleaning","deposit","service_fee","insurance","tax","other");
    private static final Set<String> METHODS=Set.of("unpaid","bank_transfer","online_payment","cash","reserve_account","other");
    private static final Set<String> CONFIRMATIONS=Set.of("pending");
    private static final Set<String> EXTENSIONS=Set.of("pdf","doc","docx","xls","xlsx","txt","csv","jpg","jpeg","png");

    private final AdminPropertyCashflowMapper mapper; private final Path root;
    public AdminPropertyCashflowService(AdminPropertyCashflowMapper mapper,@Value("${ccps.storage.property-cashflow:uploads/property-cashflow}")String root){this.mapper=mapper;this.root=Path.of(root).toAbsolutePath().normalize();}

    @Transactional(readOnly=true)
    public List<AdminPropertyCashflowResponse> list(Long ownerId,Long ownerUnitId){PropertyContext context=requireProperty(ownerId,ownerUnitId);return mapper.list(context.getUnitId()).stream().map(this::response).toList();}

    @Transactional
    public AdminPropertyCashflowResponse create(Long actorId,Long ownerId,Long ownerUnitId,String direction,String category,
            String description,BigDecimal amount,LocalDate occurredOn,String paymentMethod,String confirmationStatus,String allocationNote,boolean reuseAllocationNote,MultipartFile file){
        PropertyContext context=requireProperty(ownerId,ownerUnitId);validate(direction,category,description,amount,occurredOn,paymentMethod,confirmationStatus);
        String resolvedNote=resolveAllocationNote(context.getUnitId(),direction,category,allocationNote);validateAllocationNote(resolvedNote);
        StoredFile stored=file==null||file.isEmpty()?null:store(ownerUnitId,file);
        try{
            FinanceRow finance=new FinanceRow();finance.setTransactionNo("MANUAL-CF-"+LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))+"-"+UUID.randomUUID().toString().substring(0,8).toUpperCase());finance.setUnitId(context.getUnitId());finance.setOwnerId(context.getOwnerId());finance.setAmount(amount);finance.setOccurredOn(occurredOn);finance.setPaymentMethod(paymentMethod);finance.setPaymentStatus("unpaid");finance.setConfirmationStatus("pending");finance.setConfirmedBy(null);finance.setConfirmedAt(null);finance.setCreatedBy(actorId);
            if(mapper.insertFinance(finance)!=1||finance.getId()==null)throw conflict("Unable to create finance record");
            CashflowWriteRow cashflow=new CashflowWriteRow();cashflow.setFinanceRecordId(finance.getId());cashflow.setUnitId(context.getUnitId());cashflow.setOwnerId(context.getOwnerId());cashflow.setDirection(direction);cashflow.setCategory(category);cashflow.setDescription(description.trim());cashflow.setAllocationNote(resolvedNote);cashflow.setOccurredOn(occurredOn);cashflow.setAttachmentStatus(stored==null?"missing":"provided");
            if(mapper.insertCashflow(cashflow)!=1||cashflow.getId()==null)throw conflict("Unable to create cashflow entry");
            saveAllocationNoteDefault(actorId,context.getUnitId(),direction,category,resolvedNote,reuseAllocationNote);
            if(stored!=null)saveNewDocument(actorId,cashflow.getId(),stored);
            mapper.insertAudit(actorId,"create",cashflow.getId(),"{}");return response(requireCashflow(context.getUnitId(),cashflow.getId()));
        }catch(RuntimeException error){if(stored!=null)deleteQuietly(stored.path());throw error;}
    }

    @Transactional
    public AdminPropertyCashflowResponse update(Long actorId,Long ownerId,Long ownerUnitId,Long cashflowId,String direction,
            String category,String description,BigDecimal amount,LocalDate occurredOn,String paymentMethod,String confirmationStatus,String allocationNote,boolean reuseAllocationNote,MultipartFile file){
        PropertyContext context=requireProperty(ownerId,ownerUnitId);validate(direction,category,description,amount,occurredOn,paymentMethod,confirmationStatus);
        String resolvedNote=normalizeAllocationNote(allocationNote);validateAllocationNote(resolvedNote);
        CashflowRow current=requireCashflow(context.getUnitId(),cashflowId);requireEditable(current);
        StoredFile replacement=file==null||file.isEmpty()?null:store(ownerUnitId,file);Path previous=current.getAttachmentStorageKey()==null?null:resolve(current.getAttachmentStorageKey());
        try{
            FinanceRow finance=new FinanceRow();finance.setId(current.getFinanceRecordId());finance.setAmount(amount);finance.setOccurredOn(occurredOn);finance.setPaymentMethod(paymentMethod);finance.setPaymentStatus("unpaid");finance.setConfirmationStatus("pending");finance.setConfirmedBy(null);finance.setConfirmedAt(null);if(mapper.updateFinance(finance)!=1)throw conflict("Unable to update finance record");
            CashflowWriteRow cashflow=new CashflowWriteRow();cashflow.setId(cashflowId);cashflow.setDirection(direction);cashflow.setCategory(category);cashflow.setDescription(description.trim());cashflow.setAllocationNote(resolvedNote);cashflow.setOccurredOn(occurredOn);cashflow.setAttachmentStatus(replacement!=null||current.getAttachmentId()!=null?"provided":"missing");if(mapper.updateCashflow(cashflow)!=1)throw conflict("Unable to update cashflow entry");
            saveAllocationNoteDefault(actorId,context.getUnitId(),direction,category,resolvedNote,reuseAllocationNote);
            if(replacement!=null){if(current.getAttachmentId()==null)saveNewDocument(actorId,cashflowId,replacement);else{DocumentRow document=document(actorId,replacement);document.setId(current.getAttachmentId());if(mapper.updateDocument(document)!=1)throw conflict("Unable to replace cashflow proof");}if(previous!=null)deleteQuietly(previous);}
            mapper.insertAudit(actorId,"update",cashflowId,"{}");return response(requireCashflow(context.getUnitId(),cashflowId));
        }catch(RuntimeException error){if(replacement!=null)deleteQuietly(replacement.path());throw error;}
    }

    @Transactional
    public AdminPropertyCashflowResponse updateAllocationNote(Long actorId,Long ownerId,Long ownerUnitId,Long cashflowId,String note,boolean reuse){
        PropertyContext context=requireProperty(ownerId,ownerUnitId);CashflowRow current=requireCashflow(context.getUnitId(),cashflowId);
        String normalized=normalizeAllocationNote(note);validateAllocationNote(normalized);
        if(mapper.updateAllocationNote(cashflowId,normalized)!=1)throw conflict("Unable to update allocation note");
        saveAllocationNoteDefault(actorId,context.getUnitId(),current.getDirection(),current.getCategory(),normalized,reuse);
        mapper.insertAudit(actorId,"update_allocation_note",cashflowId,"{}");
        return response(requireCashflow(context.getUnitId(),cashflowId));
    }

    @Transactional
    public void delete(Long actorId,Long ownerId,Long ownerUnitId,Long cashflowId){
        PropertyContext context=requireProperty(ownerId,ownerUnitId);CashflowRow current=requireCashflow(context.getUnitId(),cashflowId);requireEditable(current);
        ReserveDebit reserveDebit=mapper.findReserveDebit(current.getFinanceRecordId());
        boolean posted=reserveDebit!=null||"paid".equals(current.getPaymentStatus())||"confirmed".equals(current.getConfirmationStatus());
        if(posted){
            if(reserveDebit!=null){
                if(mapper.restoreReserveBalance(reserveDebit.getReserveAccountId(),reserveDebit.getAmount())!=1)throw conflict("Unable to restore reserve balance");
                BigDecimal balanceAfter=mapper.findReserveBalance(reserveDebit.getReserveAccountId());
                if(balanceAfter==null||mapper.insertReserveReversal(reserveDebit.getReserveAccountId(),current.getFinanceRecordId(),reserveDebit.getAmount(),balanceAfter,actorId)!=1)throw conflict("Unable to record reserve reversal");
            }
            if(mapper.voidFinance(current.getFinanceRecordId())!=1)throw conflict("Unable to void finance record");
            mapper.insertAudit(actorId,"void",cashflowId,"{}");return;
        }
        Path proof=current.getAttachmentStorageKey()==null?null:resolve(current.getAttachmentStorageKey());
        if(current.getAttachmentId()!=null){mapper.deleteDocumentLinks(cashflowId);mapper.deleteDocument(current.getAttachmentId());}
        if(mapper.deleteCashflow(cashflowId)!=1)throw conflict("Unable to delete cashflow entry");
        if(mapper.deleteFinance(current.getFinanceRecordId())!=1)throw conflict("Unable to delete finance record");
        mapper.insertAudit(actorId,"delete",cashflowId,"{}");if(proof!=null)deleteQuietly(proof);
    }

    @Transactional(readOnly=true)
    public Download download(Long ownerId,Long ownerUnitId,Long cashflowId){PropertyContext context=requireProperty(ownerId,ownerUnitId);CashflowRow row=requireCashflow(context.getUnitId(),cashflowId);if(row.getAttachmentId()==null||row.getAttachmentStorageKey()==null)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Cashflow proof not found");Path path=resolve(row.getAttachmentStorageKey());if(!Files.isRegularFile(path))throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Cashflow proof file not found");return new Download(path,row.getAttachmentName(),row.getAttachmentMimeType()==null?"application/octet-stream":row.getAttachmentMimeType(),row.getAttachmentSize()==null?0:row.getAttachmentSize());}

    private void saveNewDocument(Long actorId,Long cashflowId,StoredFile stored){DocumentRow document=document(actorId,stored);if(mapper.insertDocument(document)!=1||document.getId()==null)throw conflict("Unable to create cashflow proof");if(mapper.insertDocumentLink(document.getId(),cashflowId)!=1)throw conflict("Unable to link cashflow proof");mapper.updateAttachmentStatus(cashflowId,"provided");}
    private StoredFile store(Long ownerUnitId,MultipartFile file){if(file.getSize()>MAX_SIZE)throw bad("Proof file exceeds 15 MB");String name=safeName(file.getOriginalFilename()),extension=extension(name);if(!EXTENSIONS.contains(extension))throw bad("Unsupported proof file type");Path directory=root.resolve(String.valueOf(ownerUnitId)).normalize(),path=directory.resolve(UUID.randomUUID().toString().replace("-","")+"."+extension).normalize();if(!directory.startsWith(root)||!path.startsWith(directory))throw bad("Invalid cashflow proof path");try{Files.createDirectories(directory);try(InputStream input=file.getInputStream()){Files.copy(input,path,StandardCopyOption.REPLACE_EXISTING);}}catch(IOException error){throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,"Unable to store cashflow proof");}String mime=file.getContentType()==null||file.getContentType().isBlank()?"application/octet-stream":file.getContentType().toLowerCase(Locale.ROOT);return new StoredFile(path,root.relativize(path).toString().replace('\\','/'),name,mime,file.getSize());}
    private DocumentRow document(Long actorId,StoredFile stored){DocumentRow row=new DocumentRow();row.setDocumentNo("CASHFLOW-PROOF-"+LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))+"-"+UUID.randomUUID().toString().substring(0,8).toUpperCase());row.setOriginalName(stored.originalName());row.setStorageKey(stored.storageKey());row.setMimeType(stored.mimeType());row.setFileSize(stored.size());row.setUploadedBy(actorId);return row;}
    private AdminPropertyCashflowResponse response(CashflowRow row){String path=row.getAttachmentName()==null?null:"/收支憑證/"+row.getAttachmentName();return new AdminPropertyCashflowResponse(row.getId(),row.getFinanceRecordId(),row.getTransactionNo(),row.getDirection(),row.getCategory(),row.getDescription(),row.getAllocationNote(),row.getAmount(),row.getCurrency(),row.getOccurredOn(),row.getPaymentMethod(),row.getPaymentStatus(),row.getConfirmationStatus(),row.getSyncStatus(),row.getSource(),row.isEditable(),row.getWorkOrderId(),row.getAttachmentId(),path,row.getAttachmentName(),row.getAttachmentSize(),row.getCreatedByName(),row.getCreatedAt(),row.getUpdatedAt());}
    private String resolveAllocationNote(Long unitId,String direction,String category,String note){String normalized=normalizeAllocationNote(note);return normalized!=null?normalized:normalizeAllocationNote(mapper.findAllocationNoteDefault(unitId,direction,category));}
    private String normalizeAllocationNote(String note){if(note==null)return null;String value=note.trim();return value.isEmpty()?null:value;}
    private void validateAllocationNote(String note){if(note!=null&&note.length()>500)throw bad("Allocation note must not exceed 500 characters");}
    private void saveAllocationNoteDefault(Long actorId,Long unitId,String direction,String category,String note,boolean reuse){if(!reuse)return;if(note==null){mapper.deleteAllocationNoteDefault(unitId,direction,category);return;}mapper.upsertAllocationNoteDefault(unitId,direction,category,note,actorId);}
    private void validate(String direction,String category,String description,BigDecimal amount,LocalDate date,String method,String confirmation){if(!DIRECTIONS.contains(direction))throw bad("Invalid cashflow direction");if(!CATEGORIES.contains(category))throw bad("Invalid cashflow category");if(description==null||description.isBlank()||description.trim().length()>500)throw bad("Description is required and must not exceed 500 characters");if(amount==null||amount.signum()<=0||amount.scale()>2)throw bad("Amount must be greater than zero with at most two decimals");if(date==null)throw bad("Cashflow date is required");if(method==null||!METHODS.contains(method))throw bad("Invalid payment method");if(!CONFIRMATIONS.contains(confirmation))throw bad("Invalid confirmation status");}
    private PropertyContext requireProperty(Long ownerId,Long ownerUnitId){PropertyContext context=mapper.findProperty(ownerId,ownerUnitId);if(context==null)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Property not found");return context;}
    private CashflowRow requireCashflow(Long unitId,Long cashflowId){CashflowRow row=mapper.find(unitId,cashflowId);if(row==null)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Cashflow entry not found");return row;}
    private void requireEditable(CashflowRow row){if(!row.isEditable())throw new ResponseStatusException(HttpStatus.CONFLICT,"System-generated or exported cashflow entries cannot be modified here");}
    private Path resolve(String storageKey){Path path=root.resolve(storageKey).normalize();if(!path.startsWith(root))throw bad("Invalid cashflow proof path");return path;}
    private String safeName(String value){String normalized=value==null?"proof":value.replace('\\','/');int index=normalized.lastIndexOf('/');return(index>=0?normalized.substring(index+1):normalized).replaceAll("[\\r\\n]","_");}
    private String extension(String name){int index=name.lastIndexOf('.');return index<0?"":name.substring(index+1).toLowerCase(Locale.ROOT);}
    private void deleteQuietly(Path path){try{Files.deleteIfExists(path);}catch(IOException ignored){}}
    private ResponseStatusException bad(String message){return new ResponseStatusException(HttpStatus.BAD_REQUEST,message);}private ResponseStatusException conflict(String message){return new ResponseStatusException(HttpStatus.CONFLICT,message);}
    private record StoredFile(Path path,String storageKey,String originalName,String mimeType,long size){}public record Download(Path path,String originalName,String mimeType,long size){}
}
