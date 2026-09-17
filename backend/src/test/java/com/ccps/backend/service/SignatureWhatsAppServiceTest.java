package com.ccps.backend.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import java.util.*;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import com.ccps.backend.mapper.WhatsAppNotificationMapper;

class SignatureWhatsAppServiceTest {
    JdbcTemplate db=mock(JdbcTemplate.class);
    TransactionTemplate tx=mock(TransactionTemplate.class);
    WhatsAppNotificationMapper attempts=mock(WhatsAppNotificationMapper.class);
    WhatsAppGraphClient graph=mock(WhatsAppGraphClient.class);
    SignatureWhatsAppService service=new SignatureWhatsAppService(db,tx,attempts,graph,"https://sign.example");
    Map<String,Object> signer(String role){return Map.of("signer_role",role,"entity_type","lease","entity_id",1L,"signer_name","Alice");}

    @Test void neverGuessesCountryCode(){
        assertThat(SignatureWhatsAppService.strictPhone("0123456789")).isEmpty();
        assertThat(SignatureWhatsAppService.strictPhone("8618981712596")).isEmpty();
        assertThat(SignatureWhatsAppService.strictPhone("+60 (12) 345-6789")).isEqualTo("+60123456789");
    }
    @Test void witnessNeverFallsBackToOwner(){assertThat(service.recipient(signer("owner_witness"))).isEmpty();verifyNoInteractions(db);}
    @Test void ownerMustMatchSignerNameAndBeUnique(){
        when(db.queryForList(anyString(),any(Object[].class))).thenReturn(List.of(Map.of("full_name","Bob","phone","+60123456789")));
        assertThat(service.recipient(signer("owner"))).isEmpty();
        when(db.queryForList(anyString(),any(Object[].class))).thenReturn(List.of(Map.of("full_name","Alice","phone","+60123456789")));
        assertThat(service.recipient(signer("owner"))).isEqualTo("+60123456789");
        when(db.queryForList(anyString(),any(Object[].class))).thenReturn(List.of(Map.of("full_name","Alice","phone","+60123456789"),Map.of("full_name","Alice","phone","+60123456780")));
        assertThat(service.recipient(signer("second_owner"))).isEmpty();
    }
    @Test void tenantOptOutCannotBeBypassed(){
        when(db.queryForList(anyString(),any(Object[].class))).thenReturn(List.of(Map.of("full_name","Alice","phone","+60123456789","opted_out_at",LocalDateTime.now())));
        assertThatThrownBy(()->service.recipient(signer("tenant"))).isInstanceOf(ResponseStatusException.class).hasMessageContaining("退订");
    }
    @Test void mandateTenantUsesLinkedLeaseAndDeduplicatesTenant(){
        when(db.queryForList(anyString(),any(Object[].class))).thenReturn(List.of(Map.of("full_name","Alice","phone","+60123456789")));
        assertThat(service.recipient(Map.of("signer_role","tenant","entity_type","rental_mandate","entity_id",25L,"signer_name","Alice"))).isEqualTo("+60123456789");
        verify(db).queryForList(argThat((String s)->s.contains("SELECT DISTINCT")&&s.contains("l.rental_mandate_id=?")),eq("rental_mandate"),eq(25L),eq("rental_mandate"),eq(25L));
    }
    @Test void existingLocalPhoneIsRetainedWithoutGuessingCountry(){
        when(db.queryForList(anyString(),any(Object[].class))).thenReturn(List.of(Map.of("full_name","Alice","phone","18981712596")));
        assertThat(service.recipient(signer("owner"))).isEqualTo("18981712596");
    }
    @Test void invalidTokenRejectedBeforeDatabase(){
        assertThatThrownBy(()->service.request(1L,"https://evil.example/token",true)).isInstanceOf(ResponseStatusException.class);
        verifyNoInteractions(db);
    }
    @Test void expiredOrWrongTokenDoesNotSend(){
        when(db.queryForList(anyString(),any(Object[].class))).thenReturn(List.of());
        assertThatThrownBy(()->service.prepare(1L,2L,"abcdefghijklmnopqrstuvwx","+60123456789")).isInstanceOf(ResponseStatusException.class);
        verifyNoInteractions(graph,attempts);
    }
    @Test void sqlChecksTokenRequestAndLocksBeforeSending(){
        when(db.queryForList(anyString(),any(Object[].class))).thenReturn(List.of(signer("owner")));
        service.request(9L,"abcdefghijklmnopqrstuvwx",true);
        verify(db).queryForList(argThat((String s)->s.contains("sr.id=?")&&s.contains("sr.access_token_hash=? FOR UPDATE")&&s.contains("expires_at>NOW()")&&s.contains("root.status NOT IN")),eq(9L),matches("[0-9a-f]{64}"));
    }
    @Test void existingAttemptNeverResendsEvenIfUnknown(){
        when(graph.configured()).thenReturn(true);
        when(tx.execute(any())).thenReturn(new SignatureWhatsAppService.Prepared(null,null,null,null,new SignatureWhatsAppService.State("+60123456789","unknown",true)));
        assertThat(service.send(1L,2L,"abcdefghijklmnopqrstuvwx",null).status()).isEqualTo("unknown");
        verify(graph,never()).sendTemplate(any(),any(),any(),any());
    }
    @Test void acceptedMessageUsesFourParametersAndExistingCallbackTrace(){
        when(graph.configured()).thenReturn(true);
        var params=List.of("Alice","Agreement","2026-10-01","https://sign.example/sign/token");
        when(tx.execute(any())).thenReturn(new SignatureWhatsAppService.Prepared(7L,8L,"60123456789",params,null));
        when(graph.sendTemplate("60123456789","ccps_signature_invitation","zh_CN",params)).thenReturn(new WhatsAppGraphClient.SendResult("wamid.test","60123456789"));
        when(db.queryForList(anyString(),any(Object[].class))).thenReturn(List.of(Map.of("destination","60123456789","status","sent")));
        assertThat(service.send(1L,2L,"abcdefghijklmnopqrstuvwx",null).status()).isEqualTo("sent");
        verify(attempts).markAttemptAccepted(8L,"wamid.test","60123456789");verify(attempts).markDeliveryAccepted(7L);
    }
    @Test void transportUnknownIsRecordedAndNeverRetried(){
        when(graph.configured()).thenReturn(true);
        when(tx.execute(any())).thenReturn(new SignatureWhatsAppService.Prepared(7L,8L,"60123456789",List.of("a","b","c","d"),null));
        when(graph.sendTemplate(any(),any(),any(),any())).thenThrow(new WhatsAppGraphClient.WhatsAppGraphException("timeout",null,null,null,true));
        when(db.queryForList(anyString(),any(Object[].class))).thenReturn(List.of(Map.of("destination","60123456789","status","unknown")));
        assertThat(service.send(1L,2L,"abcdefghijklmnopqrstuvwx",null).status()).isEqualTo("unknown");
        verify(attempts).markDeliveryFailed(eq(7L),eq("unknown"),any());verify(graph,times(1)).sendTemplate(any(),any(),any(),any());
    }
    @Test void acceptsBothMySqlDatetimeRepresentations(){
        LocalDateTime date=LocalDateTime.of(2026,10,1,12,0);
        assertThat(SignatureWhatsAppService.expiry(date)).isEqualTo("2026-10-01");
        assertThat(SignatureWhatsAppService.expiry(java.sql.Timestamp.valueOf(date))).isEqualTo("2026-10-01");
    }
}
