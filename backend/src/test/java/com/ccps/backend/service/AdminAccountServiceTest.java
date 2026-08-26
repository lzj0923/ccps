package com.ccps.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import com.ccps.backend.mapper.AdminAccountMapper;
import com.ccps.backend.mapper.AdminAccountMapper.AccountRecord;

@ExtendWith(MockitoExtension.class)
class AdminAccountServiceTest {
    @Mock private AdminAccountMapper mapper;

    @Test
    void rejectsAnOwnerPhoneAlreadyUsedAsALoginWithAnExplicitMessage() {
        when(mapper.countUsername("18981712596", null)).thenReturn(1);

        assertThatThrownBy(() -> new AdminAccountService(mapper)
                .createOwnerAccount("18981712596", "Another Owner", "shared@example.com"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Owner phone already exists");
    }

    @Test
    void sharedOwnerEmailDoesNotBlockCreatingAUniquePhoneLogin() {
        when(mapper.countUsername("18981712597", null)).thenReturn(0);
        when(mapper.countEmail("shared@example.com", null)).thenReturn(1);
        when(mapper.insert(any(AccountRecord.class))).thenAnswer(invocation -> {
            AccountRecord account = invocation.getArgument(0);
            assertThat(account.getEmail()).isNull();
            account.setId(22L);
            return 1;
        });
        when(mapper.insertRole(22L, AuthService.OWNER_ROLE)).thenReturn(1);

        Long accountId = new AdminAccountService(mapper)
                .createOwnerAccount("18981712597", "Another Owner", "shared@example.com");

        assertThat(accountId).isEqualTo(22L);
    }

    @Test
    void synchronizesAnUpdatedOwnerPhoneWithThePortalLogin() {
        when(mapper.findOwnerAccountId(7L)).thenReturn(22L);
        when(mapper.countUsername("+60123456789", 22L)).thenReturn(0);
        when(mapper.updateOwnerLogin(22L, "+60123456789", "Test Owner", "+60123456789", "active"))
                .thenReturn(1);

        new AdminAccountService(mapper)
                .synchronizeOwnerAccount(7L, "+60123456789", "Test Owner", "active");

        verify(mapper).updateOwnerLogin(22L, "+60123456789", "Test Owner", "+60123456789", "active");
    }

    @Test
    void rejectsSynchronizingAnOwnerPhoneUsedByAnotherLogin() {
        when(mapper.findOwnerAccountId(7L)).thenReturn(22L);
        when(mapper.countUsername("+60123456789", 22L)).thenReturn(1);

        assertThatThrownBy(() -> new AdminAccountService(mapper)
                .synchronizeOwnerAccount(7L, "+60123456789", "Test Owner", "active"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Owner phone already exists");
        verify(mapper, never()).updateOwnerLogin(any(), any(), any(), any(), any());
    }
}
