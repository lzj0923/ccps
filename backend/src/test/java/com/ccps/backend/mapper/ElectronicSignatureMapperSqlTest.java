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

    @Test void mandateSigningLinksSignedDocumentsAsSignedContracts() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/ccps/backend/mapper/ElectronicSignatureMapper.java"));
        assertThat(source).contains("VALUES(#{documentId},'rental_mandate',#{mandateId},'signed_contract')");
    }

    @Test void mandateReviewCanSeeSignedContractDocumentsWithoutRequestRow() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/ccps/backend/mapper/AdminRentalMandateMapper.java"));
        assertThat(source).contains("signed_document.document_type = 'signed_contract'");
    }
}
