package com.ccps.backend.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import com.ccps.backend.dto.ElectronicSignaturePublicResponse;
import com.ccps.backend.dto.ElectronicSignatureSignRequest;
import com.ccps.backend.mapper.ElectronicSignatureMapper;
import com.ccps.backend.mapper.OwnerSignatureMapper;
import com.ccps.backend.mapper.OwnerSignatureMapper.Recipient;

class OwnerSignatureServiceTest {
    final OwnerSignatureMapper mapper=mock(OwnerSignatureMapper.class);
    final ElectronicSignatureMapper audit=mock(ElectronicSignatureMapper.class);
    final ElectronicSignatureService signing=mock(ElectronicSignatureService.class);
    final OwnerSignatureService service=new OwnerSignatureService(mapper,audit,signing);
    ElectronicSignaturePublicResponse response(boolean pending) {
        return new ElectronicSignaturePublicResponse(1L,"TEST ONLY.pdf","Test Owner","",pending ? "pending" : "signed",
            LocalDateTime.now().plusDays(1),null,pending,!pending,"authorization","owner",1);
    }
    void dispatchReady() {
        when(mapper.lockRequest(1L)).thenReturn(1L);
        when(mapper.assignedOwner(1L)).thenReturn(null);
        when(signing.viewById(1L)).thenReturn(response(true));
        when(mapper.activeDocument(1L)).thenReturn(1);
        when(mapper.recipients(1L)).thenReturn(List.of(new Recipient(10L,20L,"Test Owner")));
        when(mapper.assign(1L,10L,20L,7L)).thenReturn(1);
    }
    @Test void dispatchBindsExplicitPropertyOwnerAndAudits() {
        dispatchReady(); service.dispatch(7L,1L,10L);
        verify(mapper).assign(1L,10L,20L,7L);
        verify(audit).insertEvent(eq(1L),eq("owner_app_dispatched"),anyString(),isNull(),isNull());
    }
    @Test void dispatchCannotSendToUnrelatedOwner() {
        dispatchReady(); assertThatThrownBy(()->service.dispatch(7L,1L,99L)).isInstanceOf(ResponseStatusException.class);
        verify(mapper,never()).assign(any(),any(),any(),any());
    }
    @Test void repeatDispatchIsIdempotentButCannotChangeRecipient() {
        dispatchReady(); when(mapper.assignedOwner(1L)).thenReturn(10L);
        service.dispatch(7L,1L,10L); verify(mapper,never()).assign(any(),any(),any(),any()); verifyNoInteractions(audit);
        when(mapper.assignedOwner(1L)).thenReturn(11L);
        assertThatThrownBy(()->service.dispatch(7L,1L,10L)).isInstanceOf(ResponseStatusException.class);
    }
    @Test void completedExpiredOrVoidedRequestsCannotBeDispatched() {
        dispatchReady(); when(signing.viewById(1L)).thenReturn(response(false));
        assertThatThrownBy(()->service.dispatch(7L,1L,10L)).isInstanceOf(ResponseStatusException.class);
        when(signing.viewById(1L)).thenReturn(response(true)); when(mapper.activeDocument(1L)).thenReturn(0);
        assertThatThrownBy(()->service.dispatch(7L,1L,10L)).isInstanceOf(ResponseStatusException.class);
        verify(mapper,never()).assign(any(),any(),any(),any());
    }
    @Test void otherOwnerCannotViewDownloadOrSignEvenKnowingTheRequestId() {
        when(mapper.lockRequest(1L)).thenReturn(1L);
        assertThatThrownBy(()->service.view(99L,1L)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(()->service.file(99L,1L,false)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(()->service.file(99L,1L,true)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(()->service.sign(99L,1L,null,"127.0.0.1","test")).isInstanceOf(ResponseStatusException.class);
        verifyNoInteractions(signing);
    }
    @Test void authorizedSignReusesPdfPipelineAndAuditHasAuthenticatedActor() {
        when(mapper.lockRequest(1L)).thenReturn(1L); when(mapper.authorized(20L,1L)).thenReturn(1);
        when(signing.signById(eq(1L),any(),eq("127.0.0.1"),eq("test"))).thenReturn(response(false));
        var payload=mock(ElectronicSignatureSignRequest.class);
        assertThat(service.sign(20L,1L,payload,"127.0.0.1","test").canDownloadSigned()).isTrue();
        verify(audit).insertAudit(20L,"complete_owner_app_signature","electronic_signature",1L,1L,"Test Owner");
    }
    @Test void listUsesSessionUserRatherThanClientSuppliedIdentity() {
        when(mapper.tasks(20L)).thenReturn(List.of()); assertThat(service.tasks(20L)).isEmpty(); verify(mapper).tasks(20L);
    }
}
