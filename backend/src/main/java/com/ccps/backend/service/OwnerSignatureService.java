package com.ccps.backend.service;

import java.util.List;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.ccps.backend.dto.ElectronicSignaturePublicResponse;
import com.ccps.backend.dto.ElectronicSignatureSignRequest;
import com.ccps.backend.mapper.ElectronicSignatureMapper;
import com.ccps.backend.mapper.OwnerSignatureMapper;
import com.ccps.backend.mapper.OwnerSignatureMapper.Recipient;
import com.ccps.backend.mapper.OwnerSignatureMapper.Task;

@Service
public class OwnerSignatureService {
    private final OwnerSignatureMapper mapper;
    private final ElectronicSignatureMapper signatures;
    private final ElectronicSignatureService signing;

    public OwnerSignatureService(OwnerSignatureMapper mapper, ElectronicSignatureMapper signatures,
                                 ElectronicSignatureService signing) {
        this.mapper = mapper; this.signatures = signatures; this.signing = signing;
    }

    @Transactional(readOnly = true)
    public List<Recipient> recipients(Long requestId) {
        signing.viewById(requestId);
        return mapper.recipients(requestId);
    }

    @Transactional
    public void dispatch(Long actorId, Long requestId, Long ownerId) {
        if (mapper.lockRequest(requestId) == null) throw missing();
        var request = signing.viewById(requestId);
        if (!request.canSign() || mapper.activeDocument(requestId) != 1)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "签署任务已失效，请重新发起");
        Recipient recipient = mapper.recipients(requestId).stream()
            .filter(value -> Objects.equals(value.ownerId(), ownerId)).findFirst().orElseThrow(OwnerSignatureService::missing);
        Long assigned = mapper.assignedOwner(requestId);
        if (assigned != null) {
            if (!assigned.equals(ownerId)) throw new ResponseStatusException(HttpStatus.CONFLICT, "任务已发送给其他业主，请重新发起签署");
            return; // Same dispatch is idempotent; no duplicate task or notification.
        }
        if (mapper.assign(requestId, ownerId, recipient.userId(), actorId) != 1)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "签署任务发送失败，请重试");
        signatures.insertEvent(requestId, "owner_app_dispatched", "Assigned to owner account " + recipient.userId(), null, null);
        signatures.insertAudit(actorId, "dispatch_owner_signature", "electronic_signature", requestId, requestId, request.signerName());
    }

    @Transactional(readOnly = true)
    public List<Task> tasks(Long userId) { return mapper.tasks(userId); }

    @Transactional(readOnly = true)
    public ElectronicSignaturePublicResponse view(Long userId, Long requestId) {
        authorize(userId, requestId);
        return signing.viewById(requestId);
    }

    @Transactional
    public ElectronicSignaturePublicResponse sign(Long userId, Long requestId,
            ElectronicSignatureSignRequest payload, String remoteIp, String userAgent) {
        if (mapper.lockRequest(requestId) == null) throw missing();
        authorize(userId, requestId);
        var result = signing.signById(requestId, payload, remoteIp, userAgent);
        signatures.insertAudit(userId, "complete_owner_app_signature", "electronic_signature", requestId, requestId, result.signerName());
        return result;
    }

    @Transactional(readOnly = true)
    public ElectronicSignatureService.Download file(Long userId, Long requestId, boolean signed) {
        authorize(userId, requestId);
        return signing.downloadById(requestId, signed);
    }

    private void authorize(Long userId, Long requestId) {
        if (userId == null || mapper.authorized(userId, requestId) != 1) throw missing();
    }
    private static ResponseStatusException missing() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "签署任务不存在或无权访问");
    }
}
