package com.ccps.backend.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.ccps.backend.dto.AdminPropertyImportantMessageRequest;
import com.ccps.backend.dto.AdminPropertyImportantMessageResponse;
import com.ccps.backend.mapper.AdminPropertyImportantMessageMapper;
import com.ccps.backend.mapper.AdminPropertyImportantMessageMapper.MessageRow;

@Service
public class AdminPropertyImportantMessageService {
    private static final Set<String> IMPORTANCE=Set.of("normal","important","urgent");
    private final AdminPropertyImportantMessageMapper mapper;
    public AdminPropertyImportantMessageService(AdminPropertyImportantMessageMapper mapper){this.mapper=mapper;}

    @Transactional(readOnly=true)
    public List<AdminPropertyImportantMessageResponse> list(Long ownerId,Long ownerUnitId){requireProperty(ownerId,ownerUnitId);return mapper.list(ownerUnitId).stream().map(this::response).toList();}
    @Transactional
    public AdminPropertyImportantMessageResponse create(Long actorId,Long ownerId,Long ownerUnitId,AdminPropertyImportantMessageRequest request){requireProperty(ownerId,ownerUnitId);validate(request);MessageRow row=toRow(ownerUnitId,request);row.setCreatedBy(actorId);if(mapper.insert(row)!=1||row.getId()==null)throw conflict("Unable to create important message");return response(requireMessage(ownerUnitId,row.getId()));}
    @Transactional
    public AdminPropertyImportantMessageResponse update(Long ownerId,Long ownerUnitId,Long messageId,AdminPropertyImportantMessageRequest request){requireProperty(ownerId,ownerUnitId);validate(request);requireMessage(ownerUnitId,messageId);MessageRow row=toRow(ownerUnitId,request);row.setId(messageId);if(mapper.update(row)!=1)throw conflict("Important message was changed by another request");return response(requireMessage(ownerUnitId,messageId));}
    @Transactional
    public void delete(Long ownerId,Long ownerUnitId,Long messageId){requireProperty(ownerId,ownerUnitId);requireMessage(ownerUnitId,messageId);if(mapper.delete(ownerUnitId,messageId)!=1)throw conflict("Important message was changed by another request");}

    private MessageRow toRow(Long ownerUnitId,AdminPropertyImportantMessageRequest request){MessageRow row=new MessageRow();row.setOwnerUnitId(ownerUnitId);row.setSubject(request.subject().trim());row.setContent(blankToNull(request.content()));row.setAnnouncementStartDate(request.announcementStartDate());row.setAnnouncementEndDate(request.announcementEndDate());row.setImportance(request.importance());row.setReadFlag(request.read());return row;}
    private void validate(AdminPropertyImportantMessageRequest request){if(request==null)throw bad("Important message is required");if(request.subject()==null||request.subject().isBlank()||request.subject().trim().length()>200)throw bad("Message subject is required and must not exceed 200 characters");if(request.content()!=null&&request.content().length()>2000)throw bad("Message content must not exceed 2000 characters");if(request.announcementStartDate()==null)throw bad("Announcement start date is required");if(request.announcementEndDate()!=null&&request.announcementEndDate().isBefore(request.announcementStartDate()))throw bad("Announcement end date must not be before start date");if(!IMPORTANCE.contains(request.importance()))throw bad("Invalid message importance");}
    private void requireProperty(Long ownerId,Long ownerUnitId){if(mapper.ownsProperty(ownerId,ownerUnitId)!=1)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Property not found");}
    private MessageRow requireMessage(Long ownerUnitId,Long messageId){MessageRow row=mapper.find(ownerUnitId,messageId);if(row==null)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Important message not found");return row;}
    private AdminPropertyImportantMessageResponse response(MessageRow row){return new AdminPropertyImportantMessageResponse(row.getId(),row.getOwnerUnitId(),row.getSubject(),row.getContent(),row.getAnnouncementStartDate(),row.getAnnouncementEndDate(),row.getImportance(),row.isReadFlag(),row.getCreatedBy(),row.getCreatedByName(),row.getCreatedAt(),row.getUpdatedAt());}
    private String blankToNull(String value){return value==null||value.isBlank()?null:value.trim();}
    private ResponseStatusException bad(String message){return new ResponseStatusException(HttpStatus.BAD_REQUEST,message);}private ResponseStatusException conflict(String message){return new ResponseStatusException(HttpStatus.CONFLICT,message);}
}
