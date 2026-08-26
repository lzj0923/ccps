package com.ccps.backend.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import com.ccps.backend.mapper.AdminPropertyImportantMessageMapper;

@ExtendWith(MockitoExtension.class)
class AdminPropertyImportantMessageServiceOwnerAccessTest {
    @Mock private AdminPropertyImportantMessageMapper mapper;
    @InjectMocks private AdminPropertyImportantMessageService service;

    @Test
    void rejectsInformationForPropertyOutsideAuthenticatedOwner() {
        when(mapper.ownsPropertyForUser(42L, 7L)).thenReturn(0);

        assertThatThrownBy(() -> service.listForOwnerUser(42L, 7L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Property not found");
        verify(mapper, never()).list(7L);
    }

    @Test
    void listsInformationAfterOwnerAccessCheck() {
        when(mapper.ownsPropertyForUser(42L, 7L)).thenReturn(1);
        when(mapper.list(7L)).thenReturn(List.of());

        service.listForOwnerUser(42L, 7L);

        verify(mapper).list(7L);
    }
}
