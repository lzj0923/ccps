package com.ccps.backend.mapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface AdminPropertyCashflowMapper {
    String ROW_COLUMNS = "ce.id,fr.id AS financeRecordId,fr.transaction_no AS transactionNo,ce.direction,ce.category,"
            + "ce.description,ce.allocation_note AS allocationNote,fr.amount,fr.currency,ce.occurred_on AS occurredOn,fr.payment_method AS paymentMethod,"
            + "fr.payment_status AS paymentStatus,fr.confirmation_status AS confirmationStatus,fr.sync_status AS syncStatus,"
            + "CASE WHEN rp.id IS NOT NULL THEN 'rent' WHEN mwo.id IS NOT NULL THEN 'maintenance' "
            + "WHEN fr.transaction_no LIKE 'MANUAL-CF-%' THEN 'manual' ELSE 'system' END AS source,"
            + "CASE WHEN fr.transaction_no LIKE 'MANUAL-CF-%' AND rp.id IS NULL AND mwo.id IS NULL AND fr.sync_status<>'exported' AND fr.payment_status<>'voided' THEN 1 ELSE 0 END AS editable,"
            + "mwo.id AS workOrderId,COALESCE(proof.documentId,financeProof.documentId) AS attachmentId,COALESCE(proof.originalName,financeProof.originalName) AS attachmentName,"
            + "COALESCE(proof.storageKey,financeProof.storageKey) AS attachmentStorageKey,COALESCE(proof.mimeType,financeProof.mimeType) AS attachmentMimeType,COALESCE(proof.fileSize,financeProof.fileSize) AS attachmentSize,"
            + "fr.receipt_date AS receiptDate,CASE WHEN fr.confirmation_status='confirmed' AND fr.payment_status='paid' THEN fr.transaction_date END AS paymentDate,COALESCE(cu.display_name,cu.username,'系統') AS createdByName,fr.created_at AS createdAt,fr.updated_at AS updatedAt";
    String ROW_JOINS = " FROM cashflow_entries ce JOIN finance_records fr ON fr.id=ce.finance_record_id "
            + "LEFT JOIN rent_payments rp ON rp.finance_record_id=fr.id LEFT JOIN maintenance_work_orders mwo ON mwo.cashflow_entry_id=ce.id "
            + "LEFT JOIN users cu ON cu.id=fr.created_by LEFT JOIN (SELECT dl.entity_id AS cashflowId,MAX(d.id) AS documentId,"
            + "MAX(d.original_name) AS originalName,MAX(d.storage_key) AS storageKey,MAX(d.mime_type) AS mimeType,MAX(d.file_size) AS fileSize "
            + "FROM document_links dl JOIN documents d ON d.id=dl.document_id WHERE dl.entity_type='cashflow' AND dl.relation_type='proof' GROUP BY dl.entity_id) proof ON proof.cashflowId=ce.id "
            + "LEFT JOIN (SELECT dl.entity_id AS financeId,MAX(d.id) AS documentId,MAX(d.original_name) AS originalName,MAX(d.storage_key) AS storageKey,MAX(d.mime_type) AS mimeType,MAX(d.file_size) AS fileSize FROM document_links dl JOIN documents d ON d.id=dl.document_id WHERE dl.entity_type='finance' AND dl.relation_type='payment_proof' AND d.status<>'superseded' GROUP BY dl.entity_id) financeProof ON financeProof.financeId=fr.id ";

    @Select("SELECT ou.id AS ownerUnitId,ou.unit_id AS unitId,ou.owner_id AS ownerId FROM owner_units ou WHERE ou.id=#{ownerUnitId} AND ou.owner_id=#{ownerId} AND ou.status='active'")
    PropertyContext findProperty(@Param("ownerId") Long ownerId,@Param("ownerUnitId") Long ownerUnitId);

    @Select("SELECT ou.id AS ownerUnitId,ou.unit_id AS unitId,ou.owner_id AS ownerId FROM owner_units ou WHERE ou.id=#{ownerUnitId} AND ou.owner_id=#{ownerId}")
    PropertyContext findPropertyForRead(@Param("ownerId") Long ownerId,@Param("ownerUnitId") Long ownerUnitId);

    @Select("SELECT "+ROW_COLUMNS+ROW_JOINS+"WHERE ce.unit_id=#{unitId} AND fr.payment_status<>'voided' AND ce.occurred_on >= #{startDate} AND ce.occurred_on < #{endDate} ORDER BY ce.occurred_on DESC,ce.id DESC")
    List<CashflowRow> listMonthly(@Param("unitId") Long unitId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Select("SELECT "+ROW_COLUMNS+ROW_JOINS+"WHERE ce.unit_id=#{unitId} AND fr.payment_status<>'voided' ORDER BY ce.occurred_on DESC,ce.id DESC")
    List<CashflowRow> list(@Param("unitId") Long unitId);

    @Select("SELECT "+ROW_COLUMNS+ROW_JOINS+"WHERE ce.unit_id=#{unitId} AND ce.id=#{cashflowId}")
    CashflowRow find(@Param("unitId") Long unitId,@Param("cashflowId") Long cashflowId);

    @Insert("INSERT INTO finance_records (transaction_no,record_type,unit_id,owner_id,amount,currency,transaction_date,payment_method,payment_status,confirmation_status,confirmed_by,confirmed_at,sync_status,created_by) "
            + "VALUES (#{transactionNo},'cashflow',#{unitId},#{ownerId},#{amount},'MYR',#{occurredOn},#{paymentMethod},#{paymentStatus},#{confirmationStatus},#{confirmedBy},#{confirmedAt},'not_synced',#{createdBy})")
    @Options(useGeneratedKeys=true,keyProperty="id")
    int insertFinance(FinanceRow row);

    @Insert("INSERT INTO cashflow_entries (finance_record_id,unit_id,owner_id,direction,category,description,allocation_note,occurred_on,attachment_status) "
            + "VALUES (#{financeRecordId},#{unitId},#{ownerId},#{direction},#{category},#{description},#{allocationNote},#{occurredOn},#{attachmentStatus})")
    @Options(useGeneratedKeys=true,keyProperty="id")
    int insertCashflow(CashflowWriteRow row);

    @Update("UPDATE finance_records SET amount=#{amount},transaction_date=#{occurredOn},payment_method=#{paymentMethod},payment_status=#{paymentStatus},confirmation_status=#{confirmationStatus},confirmed_by=#{confirmedBy},confirmed_at=#{confirmedAt} WHERE id=#{id}")
    int updateFinance(FinanceRow row);

    @Update("UPDATE cashflow_entries SET direction=#{direction},category=#{category},description=#{description},allocation_note=#{allocationNote},occurred_on=#{occurredOn},attachment_status=#{attachmentStatus} WHERE id=#{id}")
    int updateCashflow(CashflowWriteRow row);

    @Select("SELECT note FROM cashflow_note_defaults WHERE unit_id=#{unitId} AND direction=#{direction} AND category=#{category}")
    String findAllocationNoteDefault(@Param("unitId") Long unitId,@Param("direction") String direction,@Param("category") String category);
    @Insert("INSERT INTO cashflow_note_defaults (unit_id,direction,category,note,updated_by) VALUES (#{unitId},#{direction},#{category},#{note},#{actorId}) ON DUPLICATE KEY UPDATE note=VALUES(note),updated_by=VALUES(updated_by),updated_at=CURRENT_TIMESTAMP")
    int upsertAllocationNoteDefault(@Param("unitId") Long unitId,@Param("direction") String direction,@Param("category") String category,@Param("note") String note,@Param("actorId") Long actorId);
    @Delete("DELETE FROM cashflow_note_defaults WHERE unit_id=#{unitId} AND direction=#{direction} AND category=#{category}")
    int deleteAllocationNoteDefault(@Param("unitId") Long unitId,@Param("direction") String direction,@Param("category") String category);
    @Update("UPDATE cashflow_entries SET allocation_note=#{note} WHERE id=#{cashflowId}")
    int updateAllocationNote(@Param("cashflowId") Long cashflowId,@Param("note") String note);

    @Insert("INSERT INTO documents (document_no,original_name,storage_key,mime_type,file_size,document_type,status,uploaded_by) VALUES (#{documentNo},#{originalName},#{storageKey},#{mimeType},#{fileSize},'cashflow_attachment','approved',#{uploadedBy})")
    @Options(useGeneratedKeys=true,keyProperty="id")
    int insertDocument(DocumentRow row);

    @Insert("INSERT INTO document_links (document_id,entity_type,entity_id,relation_type) VALUES (#{documentId},'cashflow',#{cashflowId},'proof')")
    int insertDocumentLink(@Param("documentId") Long documentId,@Param("cashflowId") Long cashflowId);

    @Update("UPDATE documents SET original_name=#{originalName},storage_key=#{storageKey},mime_type=#{mimeType},file_size=#{fileSize} WHERE id=#{id}")
    int updateDocument(DocumentRow row);

    @Update("UPDATE cashflow_entries SET attachment_status=#{status} WHERE id=#{cashflowId}")
    int updateAttachmentStatus(@Param("cashflowId") Long cashflowId,@Param("status") String status);

    @Delete("DELETE FROM document_links WHERE entity_type='cashflow' AND entity_id=#{cashflowId} AND relation_type='proof'")
    int deleteDocumentLinks(@Param("cashflowId") Long cashflowId);
    @Delete("DELETE FROM documents WHERE id=#{documentId}") int deleteDocument(@Param("documentId") Long documentId);
    @Delete("DELETE FROM cashflow_entries WHERE id=#{cashflowId}") int deleteCashflow(@Param("cashflowId") Long cashflowId);
    @Delete("DELETE FROM finance_records WHERE id=#{financeRecordId}") int deleteFinance(@Param("financeRecordId") Long financeRecordId);

    @Select("SELECT rt.id,rt.reserve_account_id AS reserveAccountId,rt.amount FROM reserve_transactions rt WHERE rt.finance_record_id=#{financeRecordId} AND rt.transaction_type='debit' ORDER BY rt.id DESC LIMIT 1")
    ReserveDebit findReserveDebit(@Param("financeRecordId") Long financeRecordId);
    @Update("UPDATE reserve_accounts SET current_balance=current_balance+#{amount} WHERE id=#{reserveAccountId}")
    int restoreReserveBalance(@Param("reserveAccountId") Long reserveAccountId,@Param("amount") BigDecimal amount);
    @Select("SELECT current_balance FROM reserve_accounts WHERE id=#{reserveAccountId}")
    BigDecimal findReserveBalance(@Param("reserveAccountId") Long reserveAccountId);
    @Insert("INSERT INTO reserve_transactions (reserve_account_id,finance_record_id,transaction_type,amount,occurred_at,balance_after,note,created_by) VALUES (#{reserveAccountId},#{financeRecordId},'adjustment',#{amount},NOW(),#{balanceAfter},'人工收支作廢，預備金自動回沖',#{actorId})")
    int insertReserveReversal(@Param("reserveAccountId") Long reserveAccountId,@Param("financeRecordId") Long financeRecordId,@Param("amount") BigDecimal amount,@Param("balanceAfter") BigDecimal balanceAfter,@Param("actorId") Long actorId);
    @Update("UPDATE finance_records SET payment_status='voided',confirmation_status='rejected',confirmed_by=NULL,confirmed_at=NULL WHERE id=#{financeRecordId}")
    int voidFinance(@Param("financeRecordId") Long financeRecordId);

    @Insert("INSERT INTO audit_logs (actor_user_id,action,entity_type,entity_id,after_data) VALUES (#{actorId},#{action},'cashflow_entry',#{entityId},CAST(#{data} AS JSON))")
    int insertAudit(@Param("actorId") Long actorId,@Param("action") String action,@Param("entityId") Long entityId,@Param("data") String data);

    class PropertyContext {private Long ownerUnitId,unitId,ownerId;public Long getOwnerUnitId(){return ownerUnitId;}public void setOwnerUnitId(Long v){ownerUnitId=v;}public Long getUnitId(){return unitId;}public void setUnitId(Long v){unitId=v;}public Long getOwnerId(){return ownerId;}public void setOwnerId(Long v){ownerId=v;}}
    class FinanceRow {private Long id,unitId,ownerId,confirmedBy,createdBy;private String transactionNo,paymentMethod,paymentStatus,confirmationStatus;private BigDecimal amount;private LocalDate occurredOn;private LocalDateTime confirmedAt;public Long getId(){return id;}public void setId(Long v){id=v;}public Long getUnitId(){return unitId;}public void setUnitId(Long v){unitId=v;}public Long getOwnerId(){return ownerId;}public void setOwnerId(Long v){ownerId=v;}public Long getConfirmedBy(){return confirmedBy;}public void setConfirmedBy(Long v){confirmedBy=v;}public Long getCreatedBy(){return createdBy;}public void setCreatedBy(Long v){createdBy=v;}public String getTransactionNo(){return transactionNo;}public void setTransactionNo(String v){transactionNo=v;}public String getPaymentMethod(){return paymentMethod;}public void setPaymentMethod(String v){paymentMethod=v;}public String getPaymentStatus(){return paymentStatus;}public void setPaymentStatus(String v){paymentStatus=v;}public String getConfirmationStatus(){return confirmationStatus;}public void setConfirmationStatus(String v){confirmationStatus=v;}public BigDecimal getAmount(){return amount;}public void setAmount(BigDecimal v){amount=v;}public LocalDate getOccurredOn(){return occurredOn;}public void setOccurredOn(LocalDate v){occurredOn=v;}public LocalDateTime getConfirmedAt(){return confirmedAt;}public void setConfirmedAt(LocalDateTime v){confirmedAt=v;}}
    class CashflowWriteRow {private Long id,financeRecordId,unitId,ownerId;private String direction,category,description,allocationNote,attachmentStatus;private LocalDate occurredOn;public Long getId(){return id;}public void setId(Long v){id=v;}public Long getFinanceRecordId(){return financeRecordId;}public void setFinanceRecordId(Long v){financeRecordId=v;}public Long getUnitId(){return unitId;}public void setUnitId(Long v){unitId=v;}public Long getOwnerId(){return ownerId;}public void setOwnerId(Long v){ownerId=v;}public String getDirection(){return direction;}public void setDirection(String v){direction=v;}public String getCategory(){return category;}public void setCategory(String v){category=v;}public String getDescription(){return description;}public void setDescription(String v){description=v;}public String getAllocationNote(){return allocationNote;}public void setAllocationNote(String v){allocationNote=v;}public String getAttachmentStatus(){return attachmentStatus;}public void setAttachmentStatus(String v){attachmentStatus=v;}public LocalDate getOccurredOn(){return occurredOn;}public void setOccurredOn(LocalDate v){occurredOn=v;}}
    class DocumentRow {private Long id,uploadedBy,fileSize;private String documentNo,originalName,storageKey,mimeType;public Long getId(){return id;}public void setId(Long v){id=v;}public Long getUploadedBy(){return uploadedBy;}public void setUploadedBy(Long v){uploadedBy=v;}public Long getFileSize(){return fileSize;}public void setFileSize(Long v){fileSize=v;}public String getDocumentNo(){return documentNo;}public void setDocumentNo(String v){documentNo=v;}public String getOriginalName(){return originalName;}public void setOriginalName(String v){originalName=v;}public String getStorageKey(){return storageKey;}public void setStorageKey(String v){storageKey=v;}public String getMimeType(){return mimeType;}public void setMimeType(String v){mimeType=v;}}
    class ReserveDebit {private Long id,reserveAccountId;private BigDecimal amount;public Long getId(){return id;}public void setId(Long v){id=v;}public Long getReserveAccountId(){return reserveAccountId;}public void setReserveAccountId(Long v){reserveAccountId=v;}public BigDecimal getAmount(){return amount;}public void setAmount(BigDecimal v){amount=v;}}
    class CashflowRow {private Long id,financeRecordId,workOrderId,attachmentId,attachmentSize;private String transactionNo,direction,category,description,allocationNote,currency,paymentMethod,paymentStatus,confirmationStatus,syncStatus,source,attachmentName,attachmentStorageKey,attachmentMimeType,createdByName;private BigDecimal amount;private LocalDate occurredOn,receiptDate,paymentDate;public LocalDate getReceiptDate(){return receiptDate;}public void setReceiptDate(LocalDate v){receiptDate=v;}public LocalDate getPaymentDate(){return paymentDate;}public void setPaymentDate(LocalDate v){paymentDate=v;}private LocalDateTime createdAt,updatedAt;private boolean editable;public Long getId(){return id;}public void setId(Long v){id=v;}public Long getFinanceRecordId(){return financeRecordId;}public void setFinanceRecordId(Long v){financeRecordId=v;}public Long getWorkOrderId(){return workOrderId;}public void setWorkOrderId(Long v){workOrderId=v;}public Long getAttachmentId(){return attachmentId;}public void setAttachmentId(Long v){attachmentId=v;}public Long getAttachmentSize(){return attachmentSize;}public void setAttachmentSize(Long v){attachmentSize=v;}public String getTransactionNo(){return transactionNo;}public void setTransactionNo(String v){transactionNo=v;}public String getDirection(){return direction;}public void setDirection(String v){direction=v;}public String getCategory(){return category;}public void setCategory(String v){category=v;}public String getDescription(){return description;}public void setDescription(String v){description=v;}public String getAllocationNote(){return allocationNote;}public void setAllocationNote(String v){allocationNote=v;}public String getCurrency(){return currency;}public void setCurrency(String v){currency=v;}public String getPaymentMethod(){return paymentMethod;}public void setPaymentMethod(String v){paymentMethod=v;}public String getPaymentStatus(){return paymentStatus;}public void setPaymentStatus(String v){paymentStatus=v;}public String getConfirmationStatus(){return confirmationStatus;}public void setConfirmationStatus(String v){confirmationStatus=v;}public String getSyncStatus(){return syncStatus;}public void setSyncStatus(String v){syncStatus=v;}public String getSource(){return source;}public void setSource(String v){source=v;}public String getAttachmentName(){return attachmentName;}public void setAttachmentName(String v){attachmentName=v;}public String getAttachmentStorageKey(){return attachmentStorageKey;}public void setAttachmentStorageKey(String v){attachmentStorageKey=v;}public String getAttachmentMimeType(){return attachmentMimeType;}public void setAttachmentMimeType(String v){attachmentMimeType=v;}public String getCreatedByName(){return createdByName;}public void setCreatedByName(String v){createdByName=v;}public BigDecimal getAmount(){return amount;}public void setAmount(BigDecimal v){amount=v;}public LocalDate getOccurredOn(){return occurredOn;}public void setOccurredOn(LocalDate v){occurredOn=v;}public LocalDateTime getCreatedAt(){return createdAt;}public void setCreatedAt(LocalDateTime v){createdAt=v;}public LocalDateTime getUpdatedAt(){return updatedAt;}public void setUpdatedAt(LocalDateTime v){updatedAt=v;}public boolean isEditable(){return editable;}public void setEditable(boolean v){editable=v;}}
}
