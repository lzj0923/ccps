package com.ccps.backend.mapper;

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
public interface AdminPropertyPhotoMapper {
    String COLUMNS = "pp.id, pp.owner_unit_id AS ownerUnitId, pp.lease_id AS leaseId,pp.rental_stage AS rentalStage,pp.version_month AS versionMonth,pp.document_id AS documentId, "
            + "pp.title, pp.category, pp.description, pp.sort_order AS sortOrder, pp.is_cover AS coverFlag, "
            + "d.original_name AS originalName, d.storage_key AS storageKey, d.mime_type AS mimeType, "
            + "d.file_size AS fileSize, pp.created_at AS createdAt, pp.updated_at AS updatedAt";
    String JOINS = " FROM property_photos pp JOIN documents d ON d.id=pp.document_id ";

    @Select("SELECT COUNT(*) FROM owner_units WHERE id=#{ownerUnitId} AND owner_id=#{ownerId} AND status='active'")
    int ownsProperty(@Param("ownerId") Long ownerId, @Param("ownerUnitId") Long ownerUnitId);

    @Select("SELECT COUNT(*) FROM leases l JOIN owner_units ou ON ou.unit_id=l.unit_id WHERE ou.id=#{ownerUnitId} AND l.id=#{leaseId}")
    int leaseBelongs(@Param("ownerUnitId") Long ownerUnitId,@Param("leaseId") Long leaseId);

    @Select("<script>SELECT " + COLUMNS + JOINS + "WHERE pp.owner_unit_id=#{ownerUnitId} AND pp.lease_id IS NULL "
            + "<if test='versionMonth != null and versionMonth != \"\"'>AND pp.version_month=#{versionMonth} </if>"
            + "ORDER BY pp.is_cover DESC, pp.sort_order, pp.id</script>")
    List<PhotoRow> listRegular(@Param("ownerUnitId") Long ownerUnitId, @Param("versionMonth") String versionMonth);

    @Select("SELECT pp.version_month AS versionMonth,COUNT(*) AS photoCount,"
            + "COALESCE(MAX(CASE WHEN pp.is_cover=1 THEN pp.id END),MIN(pp.id)) AS coverPhotoId,MAX(pp.updated_at) AS updatedAt "
            + "FROM property_photos pp WHERE pp.owner_unit_id=#{ownerUnitId} AND pp.lease_id IS NULL AND pp.version_month IS NOT NULL "
            + "GROUP BY pp.version_month ORDER BY pp.version_month DESC")
    List<PhotoVersionRow> listVersions(@Param("ownerUnitId") Long ownerUnitId);

    @Select("SELECT " + COLUMNS + JOINS + "WHERE pp.owner_unit_id=#{ownerUnitId} AND pp.lease_id=#{leaseId} ORDER BY pp.rental_stage, pp.sort_order, pp.id")
    List<PhotoRow> listRental(@Param("ownerUnitId") Long ownerUnitId, @Param("leaseId") Long leaseId);

    @Select("SELECT " + COLUMNS + JOINS
            + "JOIN leases l ON l.id=#{leaseId} JOIN owner_units ou ON ou.unit_id=l.unit_id AND ou.status='active' "
            + "WHERE pp.owner_unit_id=ou.id AND pp.lease_id IS NULL "
            + "AND ou.id=(SELECT ou2.id FROM owner_units ou2 WHERE ou2.unit_id=l.unit_id AND ou2.status='active' "
            + "ORDER BY ou2.is_primary DESC, ou2.id DESC LIMIT 1) "
            + "ORDER BY pp.id")
    List<PhotoRow> listRegularByLease(@Param("leaseId") Long leaseId);

    @Select("SELECT " + COLUMNS + JOINS + "WHERE pp.owner_unit_id=#{ownerUnitId} AND pp.id=#{photoId}")
    PhotoRow find(@Param("ownerUnitId") Long ownerUnitId, @Param("photoId") Long photoId);

    @Select("SELECT COUNT(*) FROM property_photos WHERE owner_unit_id=#{ownerUnitId} AND lease_id IS NULL AND version_month=#{versionMonth}")
    int countRegular(@Param("ownerUnitId") Long ownerUnitId, @Param("versionMonth") String versionMonth);

    @Select("SELECT COUNT(*) FROM property_photos WHERE owner_unit_id=#{ownerUnitId} AND lease_id IS NULL AND version_month=#{versionMonth} AND is_cover=1")
    int regularCoverCount(@Param("ownerUnitId") Long ownerUnitId, @Param("versionMonth") String versionMonth);

    @Insert("INSERT INTO documents (document_no,original_name,storage_key,mime_type,file_size,document_type,status,uploaded_by) "
            + "VALUES (#{documentNo},#{originalName},#{storageKey},#{mimeType},#{fileSize},'property_photo','approved',#{uploadedBy})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertDocument(DocumentRow row);

    @Insert("INSERT INTO property_photos (owner_unit_id,lease_id,rental_stage,version_month,document_id,title,category,description,sort_order,is_cover) "
            + "VALUES (#{ownerUnitId},#{leaseId},#{rentalStage},#{versionMonth},#{documentId},#{title},#{category},#{description},#{sortOrder},#{coverFlag})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertPhoto(PhotoRow row);

    @Update("UPDATE property_photos SET lease_id=#{leaseId},rental_stage=#{rentalStage},version_month=#{versionMonth},title=#{title},category=#{category},description=#{description},sort_order=#{sortOrder},is_cover=#{coverFlag} "
            + "WHERE id=#{id} AND owner_unit_id=#{ownerUnitId}")
    int updatePhoto(PhotoRow row);

    @Update("UPDATE documents SET original_name=#{originalName},storage_key=#{storageKey},mime_type=#{mimeType},file_size=#{fileSize} WHERE id=#{id}")
    int updateDocument(DocumentRow row);

    @Update("UPDATE property_photos SET is_cover=0 WHERE owner_unit_id=#{ownerUnitId} AND lease_id IS NULL AND version_month=#{versionMonth}")
    int clearRegularCover(@Param("ownerUnitId") Long ownerUnitId, @Param("versionMonth") String versionMonth);

    @Update("UPDATE property_photos SET is_cover=1 WHERE id=(SELECT id FROM (SELECT id FROM property_photos WHERE owner_unit_id=#{ownerUnitId} AND lease_id IS NULL AND version_month=#{versionMonth} ORDER BY sort_order,id LIMIT 1) first_photo)")
    int assignFirstRegularCover(@Param("ownerUnitId") Long ownerUnitId, @Param("versionMonth") String versionMonth);

    @Delete("DELETE FROM property_photos WHERE id=#{photoId} AND owner_unit_id=#{ownerUnitId}")
    int deletePhoto(@Param("ownerUnitId") Long ownerUnitId, @Param("photoId") Long photoId);

    @Delete("DELETE FROM documents WHERE id=#{documentId}")
    int deleteDocument(@Param("documentId") Long documentId);

    class DocumentRow {
        private Long id;
        private String documentNo;
        private String originalName;
        private String storageKey;
        private String mimeType;
        private Long fileSize;
        private Long uploadedBy;
        public Long getId(){return id;} public void setId(Long value){id=value;}
        public String getDocumentNo(){return documentNo;} public void setDocumentNo(String value){documentNo=value;}
        public String getOriginalName(){return originalName;} public void setOriginalName(String value){originalName=value;}
        public String getStorageKey(){return storageKey;} public void setStorageKey(String value){storageKey=value;}
        public String getMimeType(){return mimeType;} public void setMimeType(String value){mimeType=value;}
        public Long getFileSize(){return fileSize;} public void setFileSize(Long value){fileSize=value;}
        public Long getUploadedBy(){return uploadedBy;} public void setUploadedBy(Long value){uploadedBy=value;}
    }

    class PhotoRow {
        private Long id;
        private Long ownerUnitId;
        private Long documentId;
        private Long leaseId;
        private String rentalStage;
        private String versionMonth;
        private String title;
        private String category;
        private String description;
        private Integer sortOrder;
        private boolean coverFlag;
        private String originalName;
        private String storageKey;
        private String mimeType;
        private Long fileSize;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        public Long getId(){return id;} public void setId(Long value){id=value;}
        public Long getOwnerUnitId(){return ownerUnitId;} public void setOwnerUnitId(Long value){ownerUnitId=value;}
        public Long getDocumentId(){return documentId;} public void setDocumentId(Long value){documentId=value;}
        public Long getLeaseId(){return leaseId;} public void setLeaseId(Long value){leaseId=value;}
        public String getRentalStage(){return rentalStage;} public void setRentalStage(String value){rentalStage=value;}
        public String getVersionMonth(){return versionMonth;} public void setVersionMonth(String value){versionMonth=value;}
        public String getTitle(){return title;} public void setTitle(String value){title=value;}
        public String getCategory(){return category;} public void setCategory(String value){category=value;}
        public String getDescription(){return description;} public void setDescription(String value){description=value;}
        public Integer getSortOrder(){return sortOrder;} public void setSortOrder(Integer value){sortOrder=value;}
        public boolean isCoverFlag(){return coverFlag;} public void setCoverFlag(boolean value){coverFlag=value;}
        public String getOriginalName(){return originalName;} public void setOriginalName(String value){originalName=value;}
        public String getStorageKey(){return storageKey;} public void setStorageKey(String value){storageKey=value;}
        public String getMimeType(){return mimeType;} public void setMimeType(String value){mimeType=value;}
        public Long getFileSize(){return fileSize;} public void setFileSize(Long value){fileSize=value;}
        public LocalDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(LocalDateTime value){createdAt=value;}
        public LocalDateTime getUpdatedAt(){return updatedAt;} public void setUpdatedAt(LocalDateTime value){updatedAt=value;}
    }

    class PhotoVersionRow {
        private String versionMonth;
        private Integer photoCount;
        private Long coverPhotoId;
        private LocalDateTime updatedAt;
        public String getVersionMonth(){return versionMonth;} public void setVersionMonth(String value){versionMonth=value;}
        public Integer getPhotoCount(){return photoCount;} public void setPhotoCount(Integer value){photoCount=value;}
        public Long getCoverPhotoId(){return coverPhotoId;} public void setCoverPhotoId(Long value){coverPhotoId=value;}
        public LocalDateTime getUpdatedAt(){return updatedAt;} public void setUpdatedAt(LocalDateTime value){updatedAt=value;}
    }
}
