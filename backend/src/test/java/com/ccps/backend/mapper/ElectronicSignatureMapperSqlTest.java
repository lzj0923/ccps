package com.ccps.backend.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class ElectronicSignatureMapperSqlTest {
    @Test void leaseSigningQueryUsesRealSqlNotEqualOperator() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/ccps/backend/mapper/ElectronicSignatureMapper.java"));
        assertThat(source).contains("d.status<>'superseded'");
        assertThat(source).doesNotContain("d.status&lt;&gt;'superseded'");
    }

    @Test void mandateSigningLinksSignedDocumentsWithTheirPackageRelation() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/ccps/backend/mapper/ElectronicSignatureMapper.java"));
        assertThat(source).contains("VALUES(#{documentId},'rental_mandate',#{mandateId},#{relationType})");
    }

    @Test void multiSignerRequestsAreScopedToTheOriginalDocument() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/ccps/backend/mapper/ElectronicSignatureMapper.java"));
        assertThat(source).contains("COALESCE(root_document_id,source_document_id)=#{rootDocumentId}")
                .contains("signer_role=#{signerRole}")
                .contains("signing_order=#{signingOrder}");
    }

    @Test void mandateDocumentListIncludesLatestSigningRequestState() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/ccps/backend/mapper/AdminRentalMandateDocumentMapper.java"));
        assertThat(source)
                .contains("signature_request.status AS signature_status")
                .contains("signature_request.signer_name AS signature_signer_name")
                .contains("signature_request.signer_email AS signature_signer_email")
                .contains("signature_request.requested_at AS signature_requested_at")
                .doesNotContain("signature_request.created_at AS signature_requested_at")
                .contains("SELECT MAX(latest_request.id)")
                .contains("latest_request.entity_type='rental_mandate'");
    }
}
