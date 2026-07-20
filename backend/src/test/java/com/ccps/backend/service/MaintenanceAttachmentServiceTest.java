package com.ccps.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.ccps.backend.dto.MaintenanceDetailResponse.Attachment;
import com.ccps.backend.mapper.OwnerExpenseMaintenanceMapper;
import com.ccps.backend.mapper.OwnerExpenseMaintenanceMapper.AttachmentFile;
import com.ccps.backend.mapper.OwnerExpenseMaintenanceMapper.NewAttachment;
import com.ccps.backend.mapper.OwnerExpenseMaintenanceMapper.WorkOrderAccess;

@ExtendWith(MockitoExtension.class)
class MaintenanceAttachmentServiceTest {
    @Mock
    private OwnerExpenseMaintenanceMapper mapper;

    @TempDir
    Path tempDir;

    @Test
    void storesOwnedMaintenancePhotoAndReturnsAttachmentList() throws Exception {
        WorkOrderAccess access = new WorkOrderAccess();
        access.setId(8L);
        when(mapper.findWorkOrderAccess(42L, 8L)).thenReturn(access);
        doAnswer(invocation -> {
            NewAttachment value = invocation.getArgument(0);
            value.setId(91L);
            return 1;
        }).when(mapper).insertAttachment(any(NewAttachment.class));
        Attachment result = new Attachment(91L, "before.png", "image/png", 4L,
                "before_photo", LocalDateTime.parse("2026-07-16T10:00:00"));
        when(mapper.findAttachments(8L)).thenReturn(List.of(result));
        MaintenanceAttachmentService service = new MaintenanceAttachmentService(mapper, tempDir.toString());
        MockMultipartFile file = new MockMultipartFile("files", "before.png", "image/png", new byte[] {1, 2, 3, 4});

        List<Attachment> attachments = service.upload(42L, 8L, "before_photo", List.of(file));

        assertThat(attachments).containsExactly(result);
        assertThat(Files.list(tempDir.resolve("8"))).hasSize(1);
        verify(mapper).insertAttachmentLink(91L, 8L, "before_photo");
    }

    @Test
    void rejectsPdfPhotoAndForeignWorkOrder() {
        WorkOrderAccess access = new WorkOrderAccess();
        access.setId(8L);
        when(mapper.findWorkOrderAccess(42L, 8L)).thenReturn(access);
        MaintenanceAttachmentService service = new MaintenanceAttachmentService(mapper, tempDir.toString());
        MockMultipartFile pdf = new MockMultipartFile("files", "photo.pdf", "application/pdf", new byte[] {1});

        assertThatThrownBy(() -> service.upload(42L, 8L, "before_photo", List.of(pdf)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("JPG or PNG");
        assertThatThrownBy(() -> service.upload(42L, 99L, "invoice", List.of(pdf)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void rejectsReceiptUploadAfterPaymentIsConfirmed() {
        WorkOrderAccess access = new WorkOrderAccess();
        access.setId(8L);
        access.setPaymentStatus("paid");
        access.setConfirmationStatus("confirmed");
        when(mapper.findWorkOrderAccess(42L, 8L)).thenReturn(access);
        MaintenanceAttachmentService service = new MaintenanceAttachmentService(mapper, tempDir.toString());
        MockMultipartFile receipt = new MockMultipartFile("files", "receipt.pdf", "application/pdf", new byte[] {1});

        assertThatThrownBy(() -> service.upload(42L, 8L, "invoice", List.of(receipt)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("already confirmed");
    }

    @Test
    void resolvesOwnedAttachmentInsideStorageRoot() throws Exception {
        Path stored = Files.createDirectories(tempDir.resolve("8")).resolve("invoice.pdf");
        Files.write(stored, new byte[] {1, 2});
        AttachmentFile file = new AttachmentFile();
        file.setDocumentId(91L);
        file.setOriginalName("invoice.pdf");
        file.setStorageKey("8/invoice.pdf");
        file.setMimeType("application/pdf");
        file.setFileSize(2L);
        when(mapper.findAttachmentFile(42L, 91L)).thenReturn(file);
        MaintenanceAttachmentService service = new MaintenanceAttachmentService(mapper, tempDir.toString());

        MaintenanceAttachmentService.Download result = service.download(42L, 91L);

        assertThat(result.path()).isEqualTo(stored);
        assertThat(result.originalName()).isEqualTo("invoice.pdf");
    }
}
