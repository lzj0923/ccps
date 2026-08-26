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

    @Test void multiSignerPackagesPersistProfilesAndSupersedeEveryPreviousRun() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/ccps/backend/mapper/ElectronicSignatureMapper.java"));
        assertThat(source)
                .contains("INSERT INTO electronic_signature_participants")
                .contains("ON DUPLICATE KEY UPDATE")
                .contains("status='superseded'")
                .contains("ORDER BY signing_order");
        String schema = Files.readString(Path.of("src/main/resources/schema.sql"));
        assertThat(schema).contains("CREATE TABLE IF NOT EXISTS electronic_signature_participants")
                .contains("PRIMARY KEY (root_document_id, signer_role)")
                .contains("UNIQUE KEY uk_e_signature_participant_order (root_document_id, signing_order)");
    }

    @Test void mandateDocumentListIncludesLatestSigningRequestState() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/ccps/backend/mapper/AdminRentalMandateDocumentMapper.java"));
        assertThat(source)
                .contains("signature_request.status AS signature_status")
                .contains("signature_request.signer_name AS signature_signer_name")
                .contains("signature_request.signer_email AS signature_signer_email")
                .contains("signature_request.requested_at AS signature_requested_at")
                .doesNotContain("signature_request.created_at AS signature_requested_at")
                .contains("ORDER BY CASE latest_request.status WHEN 'pending' THEN 0 WHEN 'signed' THEN 1 ELSE 2 END")
                .contains("latest_request.entity_type='rental_mandate'");
    }
}
