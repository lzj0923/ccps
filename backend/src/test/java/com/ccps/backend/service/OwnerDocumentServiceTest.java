package com.ccps.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import com.ccps.backend.dto.OwnerDocumentResponse;
import com.ccps.backend.mapper.OwnerDocumentMapper;
import com.ccps.backend.mapper.OwnerDocumentMapper.DocumentFile;
import com.ccps.backend.mapper.OwnerDocumentMapper.DocumentRow;

@ExtendWith(MockitoExtension.class)
class OwnerDocumentServiceTest {
    @Mock private OwnerDocumentMapper mapper;

    @Test
    void mapsDocumentsIntoCategoriesAndSummary(@TempDir Path tempDir) {
        OwnerDocumentService service = new OwnerDocumentService(mapper, tempDir);
        DocumentRow row = row(1L, "purchase_contract", "pending_signature", LocalDate.now().plusDays(10));
        when(mapper.findDocuments(42L)).thenReturn(List.of(row));

        OwnerDocumentResponse result = service.getDocuments(42L);

        assertThat(result.summary().totalCount()).isEqualTo(1);
        assertThat(result.summary().pendingSignatureCount()).isEqualTo(0);
        assertThat(result.summary().monthNewCount()).isEqualTo(1);
        assertThat(result.summary().expiringCount()).isEqualTo(1);
        assertThat(result.documents().get(0).category()).isEqualTo("sale");
        assertThat(result.documents().get(0).status()).isEqualTo("即將到期");
    }

    @Test
    void onlyAllowsDownloadWhenMapperScopesTheDocument(@TempDir Path tempDir) throws Exception {
        OwnerDocumentService service = new OwnerDocumentService(mapper, tempDir);
        Path file = tempDir.resolve("contract.pdf");
        Files.writeString(file, "test");
        DocumentFile document = new DocumentFile();
        document.setOriginalName("contract:2026.pdf");
        document.setStorageKey("contract.pdf");
        document.setMimeType("application/pdf");
        document.setFileSize(4L);
        when(mapper.findDocumentFile(42L, 1L)).thenReturn(document);

        OwnerDocumentService.Download download = service.download(42L, 1L);
        assertThat(download.path()).isEqualTo(file);
        assertThat(download.originalName()).isEqualTo("contract_2026.pdf");

        when(mapper.findDocumentFile(42L, 2L)).thenReturn(null);
        assertThatThrownBy(() -> service.download(42L, 2L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Document not found");
    }

    @Test
    void signedDocumentsRemainPreviewableFromElectronicSignatureStorage(@TempDir Path tempDir) throws Exception {
        OwnerDocumentService service = new OwnerDocumentService(mapper, tempDir);
        Path file = tempDir.resolve("electronic-signatures/43/signed-contract.pdf");
        Files.createDirectories(file.getParent());
        Files.writeString(file, "signed");

        DocumentRow row = row(93L, "signed_contract", "approved", null);
        row.setOriginalName("rental-remittance-已签署.pdf");
        row.setStorageKey("43/signed-contract.pdf");
        when(mapper.findDocuments(42L)).thenReturn(List.of(row));

        DocumentFile document = new DocumentFile();
        document.setOriginalName(row.getOriginalName());
        document.setStorageKey(row.getStorageKey());
        document.setMimeType("application/pdf");
        document.setFileSize(6L);
        when(mapper.findDocumentFile(42L, 93L)).thenReturn(document);

        OwnerDocumentResponse result = service.getDocuments(42L);
        assertThat(result.documents().get(0).downloadable()).isTrue();
        assertThat(result.documents().get(0).status()).isEqualTo("已確認");
        assertThat(service.download(42L, 93L).path()).isEqualTo(file);
    }

    @Test
    void propertyPhotoStorageIsAvailableToOwnerPreview(@TempDir Path tempDir) throws Exception {
        OwnerDocumentService service = new OwnerDocumentService(mapper, tempDir);
        Path file = tempDir.resolve("property-photos/11/photo.jpg");
        Files.createDirectories(file.getParent());
        Files.write(file, new byte[] {1, 2, 3});
        DocumentRow row = row(101L, "property_photo", "approved", null);
        row.setStorageKey("11/photo.jpg");
        when(mapper.findDocuments(42L)).thenReturn(List.of(row));
        DocumentFile document = new DocumentFile();
        document.setStorageKey(row.getStorageKey());
        document.setMimeType("image/jpeg");
        when(mapper.findDocumentFile(42L, 101L)).thenReturn(document);
        assertThat(service.getDocuments(42L).documents().get(0).downloadable()).isTrue();
        assertThat(service.download(42L, 101L).path()).isEqualTo(file);
    }

    @Test
    void classifiesCashflowAndPhotosInsteadOfHidingThemUnderOther() {
        OwnerDocumentService service = new OwnerDocumentService(mapper, Path.of("unused-test-root"));
        when(mapper.findDocuments(42L)).thenReturn(List.of(
                row(1L, "cashflow_attachment", "approved", null),
                row(2L, "property_photo", "approved", null)));
        assertThat(service.getDocuments(42L).documents()).extracting(OwnerDocumentResponse.DocumentItem::category)
                .containsExactly("cashflow", "photo");
    }

    @Test
    void independentContractUsesDedicatedSourceAndCannotEscapeStorage(@TempDir Path tempDir) throws Exception {
        OwnerDocumentService service = new OwnerDocumentService(mapper, tempDir);
        Path file = tempDir.resolve("property-contracts/11/signed.pdf");
        Files.createDirectories(file.getParent()); Files.writeString(file, "contract");
        DocumentRow contract = row(1L, "management", "signed", null);
        contract.setSource("property_contract"); contract.setStorageKey("11/signed.pdf");
        DocumentRow regular = row(1L, "receipt", "approved", null);
        regular.setCreatedAt(LocalDateTime.now().minusDays(1));
        when(mapper.findDocuments(42L)).thenReturn(List.of(regular));
        when(mapper.findContractDocuments(42L)).thenReturn(List.of(contract));
        var documents = service.getDocuments(42L).documents();
        assertThat(documents).extracting(OwnerDocumentResponse.DocumentItem::source).containsExactly("property_contract", "document");
        assertThat(documents.get(0).downloadable()).isTrue();
        DocumentFile archived = new DocumentFile(); archived.setStorageKey("11/signed.pdf");
        when(mapper.findContractFile(42L, 1L)).thenReturn(archived);
        assertThat(service.download(42L, 1L, "property_contract").path()).isEqualTo(file);
        assertThatThrownBy(() -> service.download(42L, 1L, "document")).isInstanceOf(ResponseStatusException.class);
        archived.setStorageKey("../../outside.pdf");
        assertThatThrownBy(() -> service.download(42L, 1L, "property_contract")).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.download(42L, 1L, "unknown")).isInstanceOf(ResponseStatusException.class);
    }

    private DocumentRow row(Long id, String type, String status, LocalDate expiresAt) {
        DocumentRow row = new DocumentRow();
        row.setId(id);
        row.setDocumentNo("DOC-" + id);
        row.setOriginalName("purchase-contract.pdf");
        row.setStorageKey("missing.pdf");
        row.setDocumentType(type);
        row.setStatus(status);
        row.setFileSize(1024L);
        row.setCreatedAt(LocalDateTime.now());
        row.setUpdatedAt(LocalDateTime.now());
        row.setExpiresAt(expiresAt);
        return row;
    }
}
