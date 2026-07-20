package com.ccps.backend.service;

import com.ccps.backend.dto.AdminRentalMandateDocumentResponse;
import com.ccps.backend.mapper.AdminRentalMandateDocumentMapper;
import com.ccps.backend.mapper.AdminRentalMandateDocumentMapper.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import java.io.*; import java.nio.file.*; import java.security.*; import java.util.*;

@Service
public class AdminRentalMandateDocumentService {
  private static final long MAX=10L*1024*1024; private static final Set<String> TYPES=Set.of("mandate_document","authorization","handover_photo","inventory");
  private final AdminRentalMandateDocumentMapper mapper; private final Path root;
  public AdminRentalMandateDocumentService(AdminRentalMandateDocumentMapper mapper,@Value("${ccps.storage.rental-mandates:uploads/rental-mandates}") String root){this.mapper=mapper;this.root=Path.of(root).toAbsolutePath().normalize();}
  public List<AdminRentalMandateDocumentResponse> list(Long id){return mapper.findDocuments(id);}
  @Transactional public List<AdminRentalMandateDocumentResponse> upload(Long actor,Long mandateId,String relation,MultipartFile file){
    if(mapper.findMandate(mandateId)==null) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Rental mandate not found");
    if(!TYPES.contains(relation)||file==null||file.isEmpty()||file.getSize()>MAX||!Set.of("image/jpeg","image/png","application/pdf").contains(file.getContentType())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Only JPG, PNG or PDF files up to 10MB are supported");
    String original=safe(file.getOriginalFilename()); String token=UUID.randomUUID().toString().replace("-",""); String ext=file.getContentType().equals("image/jpeg")?".jpg":file.getContentType().equals("image/png")?".png":".pdf"; Path dir=root.resolve(String.valueOf(mandateId)).normalize(), target=dir.resolve(token+ext).normalize();
    try { Files.createDirectories(dir); Files.copy(file.getInputStream(),target,StandardCopyOption.REPLACE_EXISTING); NewDocument doc=new NewDocument(); doc.setDocumentNo("RM-DOC-"+token.substring(0,10).toUpperCase()); doc.setOriginalName(original); doc.setStorageKey(root.relativize(target).toString().replace('\\','/')); doc.setMimeType(file.getContentType()); doc.setFileSize(file.getSize()); doc.setChecksumSha256(sha256(target)); doc.setDocumentType(relation); doc.setUploadedBy(actor); mapper.insertDocument(doc); mapper.insertLink(doc.getId(),mandateId,relation); return mapper.findDocuments(mandateId); } catch(IOException e){try{Files.deleteIfExists(target);}catch(IOException ignored){} throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,"Unable to store rental mandate document",e);}
  }
  public Download download(Long mandateId,Long documentId){AttachmentFile f=mapper.findFile(mandateId,documentId);if(f==null)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Document not found");Path p=root.resolve(f.getStorageKey()).normalize();if(!p.startsWith(root)||!Files.isRegularFile(p))throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Document file unavailable");return new Download(p,safe(f.getOriginalName()),f.getMimeType()==null?"application/octet-stream":f.getMimeType(),f.getFileSize()==null?0:f.getFileSize());}
  private String safe(String n){String s=n==null?"attachment":n.replace('\\','/');s=s.substring(s.lastIndexOf('/')+1).replaceAll("[\\r\\n\\t]","_");return s.isBlank()?"attachment":s.length()>255?s.substring(s.length()-255):s;}
  private String sha256(Path p)throws IOException{try{MessageDigest d=MessageDigest.getInstance("SHA-256");try(InputStream in=Files.newInputStream(p)){in.transferTo(OutputStream.nullOutputStream());}byte[] b=Files.readAllBytes(p);return HexFormat.of().formatHex(d.digest(b));}catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
  public record Download(Path path,String originalName,String mimeType,long size){}
}
