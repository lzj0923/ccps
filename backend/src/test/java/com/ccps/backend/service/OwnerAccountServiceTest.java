package com.ccps.backend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import com.ccps.backend.dto.OwnerAccountResponse;
import com.ccps.backend.mapper.OwnerAccountMapper;

class OwnerAccountServiceTest {
    final OwnerAccountMapper mapper = mock(OwnerAccountMapper.class);
    final PortalSessionService sessions = mock(PortalSessionService.class);
    final OwnerAccountService service = new OwnerAccountService(mapper, sessions);
    final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    void owner() { when(mapper.find(7L)).thenReturn(new OwnerAccountResponse("Legal name", "", "", "", "")); }
    @Test void onlyCurrentOwnersContactFieldsCanBeWritten() {
        owner(); when(mapper.updateContact(eq(7L), any())).thenReturn(1);
        var result = service.update(7L, new OwnerAccountService.Contact(" +60123456789 ", "", "", " Kuala Lumpur "));
        assertEquals("Legal name", result.fullName()); assertEquals("+60123456789", result.mobilePhone());
        assertEquals("Kuala Lumpur", result.mailingAddress());
        verify(mapper).updateContact(7L, result); verifyNoInteractions(sessions);
    }
    @Test void inactiveOrUnlinkedOwnerCannotWrite() {
        assertThrows(ResponseStatusException.class, () -> service.update(8L, new OwnerAccountService.Contact("", "", "", "")));
        verify(mapper, never()).updateContact(any(), any());
    }
    @Test void rejectsInvalidContactAndOversizeAddress() {
        owner();
        assertThrows(ResponseStatusException.class, () -> service.update(7L, new OwnerAccountService.Contact("script", "", "", "")));
        assertThrows(ResponseStatusException.class, () -> service.update(7L, new OwnerAccountService.Contact("", "", "", "x".repeat(501))));
        verify(mapper, never()).updateContact(any(), any());
    }
    @Test void wrongCurrentPasswordDoesNotWriteOrRevoke() {
        owner(); when(mapper.passwordHash(7L)).thenReturn(encoder.encode("oldPassword123"));
        assertThrows(ResponseStatusException.class, () -> service.changePassword(7L, new OwnerAccountService.Password("incorrect", "newPassword456")));
        verify(mapper, never()).changePassword(any(), any(), any()); verifyNoInteractions(sessions);
    }
    @Test void rejectsWeakPasswordsBeforeWriting() {
        owner();
        for (String password : new String[]{"short1", "01234567890", "onlyletterslong", "密".repeat(25) + "Aa1"}) {
            assertThrows(ResponseStatusException.class, () -> service.changePassword(7L, new OwnerAccountService.Password("old", password)));
        }
        verify(mapper, never()).changePassword(any(), any(), any());
    }
    @Test void validChangeUsesHashAndRevokesSessionsOnlyAfterUpdate() {
        owner(); String old = encoder.encode("oldPassword123"); when(mapper.passwordHash(7L)).thenReturn(old);
        when(mapper.changePassword(eq(7L), eq(old), any())).thenReturn(1);
        service.changePassword(7L, new OwnerAccountService.Password("oldPassword123", "newPassword456"));
        ArgumentCaptor<String> hash = ArgumentCaptor.forClass(String.class);
        verify(mapper).changePassword(eq(7L), eq(old), hash.capture());
        assertTrue(encoder.matches("newPassword456", hash.getValue())); verify(sessions).revokeOwnerSessions(7L);
    }
    @Test void concurrentPasswordChangeDoesNotRevokeOrReportSuccess() {
        owner(); when(mapper.passwordHash(7L)).thenReturn(encoder.encode("oldPassword123"));
        assertThrows(ResponseStatusException.class, () -> service.changePassword(7L, new OwnerAccountService.Password("oldPassword123", "newPassword456")));
        verifyNoInteractions(sessions);
    }
}
