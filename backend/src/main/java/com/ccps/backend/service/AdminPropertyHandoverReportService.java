package com.ccps.backend.service;

import java.io.IOException;
import java.io.InputStream;
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
import java.util.ArrayList;
import java.util.HashSet;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.ccps.backend.dto.AdminPropertyHandoverReportResponse;
import com.ccps.backend.mapper.AdminPropertyHandoverReportMapper;
import com.ccps.backend.mapper.AdminPropertyHandoverReportMapper.DocumentRow;
import com.ccps.backend.mapper.AdminPropertyHandoverReportMapper.PropertyInfo;
import com.ccps.backend.mapper.AdminPropertyHandoverReportMapper.ReportRow;
import com.ccps.backend.service.AdminPropertyHandoverChecklistService.SyncItem;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Service
public class AdminPropertyHandoverReportService {
    private static final long MAX_SIZE = 15L * 1024L * 1024L;
    private static final Set<String> EXTENSIONS = Set.of("pdf","doc","docx","xls","xlsx","txt","csv","jpg","jpeg","png");

    private final AdminPropertyHandoverReportMapper mapper;
    private final ObjectMapper json;
    private final HandoverReportPdfService pdfService;
    private final AdminPropertyHandoverChecklistService checklistService;
    private final Path root;

    @Autowired
    public AdminPropertyHandoverReportService(AdminPropertyHandoverReportMapper mapper,
            ObjectMapper json,
            @Value("${ccps.storage.property-handover-reports:uploads/property-handover-reports}") String root,
            AdminPropertyHandoverChecklistService checklistService) {
        this.mapper=mapper; this.json=json; this.pdfService=new HandoverReportPdfService(); this.checklistService=checklistService; this.root=Path.of(root).toAbsolutePath().normalize();
    }

    public AdminPropertyHandoverReportService(AdminPropertyHandoverReportMapper mapper,ObjectMapper json,String root) {
        this(mapper,json,root,null);
    }

    @Transactional(readOnly=true)
    public List<AdminPropertyHandoverReportResponse> list(Long ownerId,Long ownerUnitId){
        requireProperty(ownerId,ownerUnitId); return mapper.list(ownerUnitId).stream().map(this::response).toList();
    }

    @Transactional(readOnly=true)
    public List<AdminPropertyHandoverReportResponse> listForOwnerUser(Long userId,Long ownerUnitId){
        requirePropertyForUser(userId,ownerUnitId); return mapper.list(ownerUnitId).stream().map(this::response).toList();
    }

    @Transactional
    public AdminPropertyHandoverReportResponse create(Long actorId,Long ownerId,Long ownerUnitId,String title,
            LocalDate reportDate,LocalDate trackingStartDate,LocalDate trackingEndDate,String remarks,
            String contentJson,boolean completed,MultipartFile file,List<MultipartFile> photos,String photoMeta){
        requireProperty(ownerId,ownerUnitId); validate(title,reportDate,trackingStartDate,trackingEndDate,remarks); validateContent(contentJson);
        StoredFile stored=file==null||file.isEmpty()?null:store(ownerUnitId,file);
        StoredContent content=attachPhotos(ownerUnitId,contentJson,photos,photoMeta);
        try{
            if(completed)validateCompletedContent(content.json());
            Long documentId=null;
            if(stored!=null){DocumentRow document=document(actorId,stored);if(mapper.insertDocument(document)!=1||document.getId()==null)throw conflict("Unable to create handover attachment");documentId=document.getId();}
            ReportRow row=new ReportRow();row.setOwnerUnitId(ownerUnitId);row.setDocumentId(documentId);row.setTitle(title.trim());
            row.setReportDate(reportDate);row.setTrackingStartDate(trackingStartDate);row.setTrackingEndDate(trackingEndDate);
            row.setRemarks(blankToNull(remarks));row.setContentJson(content.json());row.setCompleted(completed);row.setCreatedBy(actorId);
            if(mapper.insertReport(row)!=1||row.getId()==null)throw conflict("Unable to create handover report");
            syncCompletedChecklist(ownerUnitId, content.json(), completed);
            return response(requireReport(ownerUnitId,row.getId()));
        }catch(RuntimeException error){if(stored!=null)deleteQuietly(stored.path());content.paths().forEach(this::deleteQuietly);throw error;}
    }

    @Transactional
    public AdminPropertyHandoverReportResponse update(Long ownerId,Long ownerUnitId,Long reportId,String title,
            LocalDate reportDate,LocalDate trackingStartDate,LocalDate trackingEndDate,String remarks,
            String contentJson,boolean completed,MultipartFile file,List<MultipartFile> photos,String photoMeta){
        requireProperty(ownerId,ownerUnitId);validate(title,reportDate,trackingStartDate,trackingEndDate,remarks);validateContent(contentJson);
        ReportRow current=requireReport(ownerUnitId,reportId);
        StoredFile replacement=file==null||file.isEmpty()?null:store(ownerUnitId,file);
        StoredContent content=attachPhotos(ownerUnitId,contentJson,photos,photoMeta);
        Path previous=current.getStorageKey()==null?null:resolve(current.getStorageKey());
        Set<Path> oldPhotos=photoPaths(current.getContentJson());
        try{
            if(completed)validateCompletedContent(content.json());
            current.setTitle(title.trim());current.setReportDate(reportDate);current.setTrackingStartDate(trackingStartDate);
            current.setTrackingEndDate(trackingEndDate);current.setRemarks(blankToNull(remarks));current.setContentJson(content.json());current.setCompleted(completed);
            if(replacement!=null){
                if(current.getDocumentId()==null){DocumentRow document=document(null,replacement);if(mapper.insertDocument(document)!=1||document.getId()==null)throw conflict("Unable to create handover attachment");current.setDocumentId(document.getId());}
                else{DocumentRow document=document(null,replacement);document.setId(current.getDocumentId());if(mapper.updateDocument(document)!=1)throw conflict("Unable to replace handover attachment");}
            }
            if(mapper.updateReport(current)!=1)throw conflict("Handover report was changed by another request");
            syncCompletedChecklist(ownerUnitId, content.json(), completed);
            if(replacement!=null&&previous!=null)deleteQuietly(previous);
            oldPhotos.removeAll(photoPaths(content.json()));oldPhotos.forEach(this::deleteQuietly);
            return response(requireReport(ownerUnitId,reportId));
        }catch(RuntimeException error){if(replacement!=null)deleteQuietly(replacement.path());content.paths().forEach(this::deleteQuietly);throw error;}
    }

    @Transactional
    public void delete(Long ownerId,Long ownerUnitId,Long reportId){
        requireProperty(ownerId,ownerUnitId);ReportRow current=requireReport(ownerUnitId,reportId);
        if(mapper.deleteReport(ownerUnitId,reportId)!=1)throw conflict("Handover report was changed by another request");
        if(current.getDocumentId()!=null)mapper.deleteDocument(current.getDocumentId());
        if(current.getStorageKey()!=null)deleteQuietly(resolve(current.getStorageKey()));photoPaths(current.getContentJson()).forEach(this::deleteQuietly);
    }

    @Transactional(readOnly=true)
    public Download download(Long ownerId,Long ownerUnitId,Long reportId){
        requireProperty(ownerId,ownerUnitId);ReportRow row=requireReport(ownerUnitId,reportId);
        return download(ownerUnitId,row);
    }

    @Transactional(readOnly=true)
    public Download downloadForOwnerUser(Long userId,Long ownerUnitId,Long reportId){
        requirePropertyForUser(userId,ownerUnitId);return download(ownerUnitId,requireReport(ownerUnitId,reportId));
    }

    private Download download(Long ownerUnitId,ReportRow row){
        if(row.getContentJson()!=null&&!row.getContentJson().isBlank()){
            byte[] bytes=pdfService.create(toPdfReport(ownerUnitId,row));
            return new Download(null,bytes,downloadName(row),"application/pdf",bytes.length);
        }
        if(row.getDocumentId()==null||row.getStorageKey()==null)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Handover attachment not found");
        Path path=resolve(row.getStorageKey());if(!Files.isRegularFile(path))throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Handover attachment file not found");
        return new Download(path,null,row.getOriginalName(),row.getMimeType()==null?"application/octet-stream":row.getMimeType(),row.getFileSize()==null?0:row.getFileSize());
    }

    private StoredFile store(Long ownerUnitId,MultipartFile file){
        if(file.getSize()>MAX_SIZE)throw bad("Attachment exceeds 15 MB");String originalName=safeName(file.getOriginalFilename());
        String extension=extension(originalName);if(!EXTENSIONS.contains(extension))throw bad("Unsupported attachment type");
        Path directory=root.resolve(String.valueOf(ownerUnitId)).normalize();Path path=directory.resolve(UUID.randomUUID().toString().replace("-","")+"."+extension).normalize();
        if(!directory.startsWith(root)||!path.startsWith(directory))throw bad("Invalid handover attachment path");
        try{Files.createDirectories(directory);try(InputStream input=file.getInputStream()){Files.copy(input,path,StandardCopyOption.REPLACE_EXISTING);}}
        catch(IOException error){throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,"Unable to store handover attachment");}
        String mime=file.getContentType()==null||file.getContentType().isBlank()?"application/octet-stream":file.getContentType().toLowerCase(Locale.ROOT);
        return new StoredFile(path,root.relativize(path).toString().replace('\\','/'),originalName,mime,file.getSize());
    }

    private StoredContent attachPhotos(Long ownerUnitId,String contentJson,List<MultipartFile> photos,String photoMeta){
        if((photos==null||photos.stream().allMatch(file->file==null||file.isEmpty()))&&(contentJson==null||contentJson.isBlank()))return new StoredContent(null,List.of());
        ObjectNode content=content(contentJson);ArrayNode photoEntries=content.withArray("photos");List<PhotoMeta> metadata=photoMeta(photoMeta);
        List<Path> paths=new ArrayList<>();int index=0;
        try{
            if(photos!=null)for(MultipartFile photo:photos){
                if(photo==null||photo.isEmpty())continue;
                StoredFile stored=storePhoto(ownerUnitId,photo);paths.add(stored.path());PhotoMeta meta=index<metadata.size()?metadata.get(index):new PhotoMeta("現場照片","");index++;
                ObjectNode entry=photoEntries.addObject();entry.put("section",meta.section());entry.put("caption",meta.caption());entry.put("storageKey",stored.storageKey());entry.put("originalName",stored.originalName());
            }
            return new StoredContent(json.writeValueAsString(content),paths);
        }catch(JsonProcessingException error){paths.forEach(this::deleteQuietly);throw bad("Invalid handover report content");}
    }

    private StoredFile storePhoto(Long ownerUnitId,MultipartFile file){
        String name=safeName(file.getOriginalFilename());String ext=extension(name);
        if(!Set.of("jpg","jpeg","png").contains(ext))throw bad("Handover photos must be JPG or PNG");
        return store(ownerUnitId,file);
    }

    private ObjectNode content(String contentJson){
        try{JsonNode parsed=contentJson==null||contentJson.isBlank()?json.createObjectNode():json.readTree(contentJson);if(parsed==null||!parsed.isObject())throw bad("Invalid handover report content");return (ObjectNode)parsed;}
        catch(JsonProcessingException error){throw bad("Invalid handover report content");}
    }

    private List<PhotoMeta> photoMeta(String value){
        if(value==null||value.isBlank())return List.of();
        try{JsonNode entries=json.readTree(value);if(entries==null||!entries.isArray())throw bad("Invalid handover photo data");List<PhotoMeta> result=new ArrayList<>();for(JsonNode item:entries)result.add(new PhotoMeta(text(item,"section","現場照片"),text(item,"caption","")));return result;}
        catch(JsonProcessingException error){throw bad("Invalid handover photo data");}
    }

    private Set<Path> photoPaths(String contentJson){
        if(contentJson==null||contentJson.isBlank())return new HashSet<>();Set<Path> result=new HashSet<>();
        for(JsonNode photo:content(contentJson).withArray("photos")){String key=text(photo,"storageKey","");if(!key.isBlank())result.add(resolve(key));}return result;
    }

    private HandoverReportPdfService.Report toPdfReport(Long ownerUnitId,ReportRow row){
        ObjectNode content=content(row.getContentJson());PropertyInfo property=mapper.propertyInfo(ownerUnitId);
        List<HandoverReportPdfService.Section> sections=new ArrayList<>();
        for(JsonNode section:content.withArray("sections")){
            List<HandoverReportPdfService.Item> items=new ArrayList<>();
            for(JsonNode item:section.withArray("items"))items.add(new HandoverReportPdfService.Item(text(item,"name",""),text(item,"quantity",""),text(item,"condition",""),text(item,"remarks","")));
            sections.add(new HandoverReportPdfService.Section(text(section,"title","物品清單"),items));
        }
        return new HandoverReportPdfService.Report(property==null?"":property.getProjectName(),property==null?"":property.getUnitNo(),property==null?"":property.getOwnerName(),text(content,"unitType",""),
                text(content,"handoverFrom",""),text(content,"handoverTo",""),row.getReportDate(),sections,issues(content.withArray("tenantIssues")),issues(content.withArray("ownerIssues")),
                text(content,"remarks",row.getRemarks()),photos(content.withArray("photos")));
    }

    private List<HandoverReportPdfService.Issue> issues(ArrayNode entries){List<HandoverReportPdfService.Issue> result=new ArrayList<>();for(JsonNode item:entries)result.add(new HandoverReportPdfService.Issue(text(item,"name",""),text(item,"condition",""),text(item,"recommendation",""),text(item,"remarks","")));return result;}
    private List<HandoverReportPdfService.Photo> photos(ArrayNode entries){List<HandoverReportPdfService.Photo> result=new ArrayList<>();for(JsonNode item:entries){String key=text(item,"storageKey","");if(!key.isBlank())result.add(new HandoverReportPdfService.Photo(text(item,"section","現場照片"),text(item,"caption",""),resolve(key)));}return result;}
    private void syncCompletedChecklist(Long ownerUnitId,String contentJson,boolean completed){
        if(!completed||checklistService==null)return;
        ObjectNode content=content(contentJson);List<SyncItem> items=new ArrayList<>();
        for(JsonNode section:content.withArray("sections"))for(JsonNode item:section.withArray("items"))items.add(new SyncItem(text(section,"title","其他"),text(item,"name",""),text(item,"quantity",null)));
        checklistService.syncCompletedReport(ownerUnitId,items);
    }
    private String text(JsonNode node,String field,String fallback){String value=node.path(field).asText("").trim();return value.isBlank()?fallback:value;}
    private String downloadName(ReportRow row){return safeName("交屋報告_"+row.getTitle()+"_"+row.getReportDate()+".pdf");}

    private DocumentRow document(Long actorId,StoredFile stored){DocumentRow row=new DocumentRow();row.setDocumentNo("HANDOVER-REPORT-"+LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))+"-"+UUID.randomUUID().toString().substring(0,8).toUpperCase());row.setOriginalName(stored.originalName());row.setStorageKey(stored.storageKey());row.setMimeType(stored.mimeType());row.setFileSize(stored.size());row.setUploadedBy(actorId);return row;}
    private AdminPropertyHandoverReportResponse response(ReportRow row){String path=row.getOriginalName()==null?null:"/交屋報告/"+row.getOriginalName();return new AdminPropertyHandoverReportResponse(row.getId(),row.getOwnerUnitId(),row.getDocumentId(),row.getTitle(),row.getReportDate(),row.getTrackingStartDate(),row.getTrackingEndDate(),path,row.getOriginalName(),row.getMimeType(),row.getFileSize(),row.getRemarks(),row.getContentJson(),row.isCompleted(),row.getCreatedBy(),row.getCreatedByName(),row.getCreatedAt(),row.getUpdatedAt());}
    private void validate(String title,LocalDate reportDate,LocalDate start,LocalDate end,String remarks){if(title==null||title.isBlank()||title.trim().length()>160)throw bad("Handover report title is required and must not exceed 160 characters");if(reportDate==null)throw bad("Report date is required");if(start!=null&&end!=null&&end.isBefore(start))throw bad("Tracking end date must not be before start date");if(remarks!=null&&remarks.length()>1000)throw bad("Handover report remarks must not exceed 1000 characters");}
    private void validateContent(String contentJson){if(contentJson!=null&&contentJson.length()>200000)throw bad("Handover report content is too large");if(contentJson!=null&&!contentJson.isBlank())content(contentJson);}
    private void validateCompletedContent(String contentJson){
        ObjectNode value=content(contentJson);List<String> missing=new ArrayList<>();
        if(text(value,"unitType","").isBlank())missing.add("房型");
        if(text(value,"handoverFrom","").isBlank())missing.add("交接人");
        if(text(value,"handoverTo","").isBlank())missing.add("接收人");
        boolean hasChecklist=false;
        for(JsonNode section:value.withArray("sections")){if(section.withArray("items").size()>0){hasChecklist=true;break;}}
        if(!hasChecklist)missing.add("交接清单");
        boolean hasPhoto=false;
        for(JsonNode photo:value.withArray("photos")){if(!text(photo,"storageKey","").isBlank()){hasPhoto=true;break;}}
        if(!hasPhoto)missing.add("照片");
        if(!missing.isEmpty())throw bad("交接报告资料不完整："+String.join("、",missing));
    }
    private void requireProperty(Long ownerId,Long ownerUnitId){if(mapper.ownsProperty(ownerId,ownerUnitId)!=1)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Property not found");}
    private void requirePropertyForUser(Long userId,Long ownerUnitId){if(mapper.ownsPropertyForUser(userId,ownerUnitId)!=1)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Property not found");}
    private ReportRow requireReport(Long ownerUnitId,Long reportId){ReportRow row=mapper.find(ownerUnitId,reportId);if(row==null)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Handover report not found");return row;}
    private Path resolve(String storageKey){Path path=root.resolve(storageKey).normalize();if(!path.startsWith(root))throw bad("Invalid handover attachment path");return path;}
    private String safeName(String value){String normalized=value==null?"attachment":value.replace('\\','/');int index=normalized.lastIndexOf('/');return(index>=0?normalized.substring(index+1):normalized).replaceAll("[\\r\\n]","_");}
    private String extension(String name){int index=name.lastIndexOf('.');return index<0?"":name.substring(index+1).toLowerCase(Locale.ROOT);}
    private String blankToNull(String value){return value==null||value.isBlank()?null:value.trim();}
    private void deleteQuietly(Path path){try{Files.deleteIfExists(path);}catch(IOException ignored){}}
    private ResponseStatusException bad(String message){return new ResponseStatusException(HttpStatus.BAD_REQUEST,message);}
    private ResponseStatusException conflict(String message){return new ResponseStatusException(HttpStatus.CONFLICT,message);}
    private record StoredFile(Path path,String storageKey,String originalName,String mimeType,long size){}
    private record StoredContent(String json,List<Path> paths){}
    private record PhotoMeta(String section,String caption){}
    public record Download(Path path,byte[] bytes,String originalName,String mimeType,long size){}
}
