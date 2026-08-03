package com.ccps.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import com.ccps.backend.dto.AdminVendorRequest;
import com.ccps.backend.dto.AdminVendorResponse;
import com.ccps.backend.mapper.AdminVendorMapper;
import com.ccps.backend.mapper.AdminVendorMapper.NewVendor;

@ExtendWith(MockitoExtension.class)
class AdminVendorServiceTest {
    @Mock private AdminVendorMapper mapper;
    @Mock private AdminAuditService auditService;
    private AdminVendorService service;

    @BeforeEach
    void setUp() { service = new AdminVendorService(mapper); }

    @Test
    void createsVendorWithGeneratedCode() {
        AdminVendorResponse created = vendor(7L, "VND-20260723-ABC123", "Bright Care", "Amy", "012", "a@example.com", "active");
        when(mapper.insert(any(NewVendor.class))).thenAnswer(invocation -> { invocation.<NewVendor>getArgument(0).setId(7L); return 1; });
        when(mapper.findById(7L)).thenReturn(created);

        AdminVendorResponse result = service.create(3L, request("Bright Care", "Amy", "012", "a@example.com"));

        assertThat(result.name()).isEqualTo("Bright Care");
        verify(mapper).insert(any(NewVendor.class));
    }

    @Test
    void rejectsBlankVendorName() {
        assertThatThrownBy(() -> service.create(3L, request("  ", "", "", "")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("name");
    }

    @Test
    void deactivatesInsteadOfDeleting() {
        when(mapper.findById(7L)).thenReturn(vendor(7L, "VND-7", "Bright Care", "", "", "", "active"));

        service.deactivate(3L, 7L);

        verify(mapper).setStatus(7L, "inactive");
    }

    @Test
    void recordsVendorCreationInAuditTrail() {
        service = new AdminVendorService(mapper, auditService);
        AdminVendorResponse created = vendor(7L, "VND-20260723-ABC123", "Bright Care", "Amy", "012", "a@example.com", "active");
        when(mapper.insert(any(NewVendor.class))).thenAnswer(invocation -> { invocation.<NewVendor>getArgument(0).setId(7L); return 1; });
        when(mapper.findById(7L)).thenReturn(created);

        service.create(3L, request("Bright Care", "Amy", "012", "a@example.com"));

        verify(auditService).record(eq(3L), eq("create_vendor"), eq("vendor"), eq(7L), eq(null), org.mockito.ArgumentMatchers.contains("Bright Care"));
    }

    private AdminVendorRequest request(String name, String contact, String phone, String email) {
        return new AdminVendorRequest(name, contact, phone, email, null);
    }

    private AdminVendorResponse vendor(Long id, String code, String name, String contact, String phone, String email, String status) {
        return new AdminVendorResponse(id, code, name, contact, phone, email, status);
    }
}
