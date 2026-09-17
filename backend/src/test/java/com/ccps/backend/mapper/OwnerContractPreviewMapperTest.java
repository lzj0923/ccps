package com.ccps.backend.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import org.apache.ibatis.annotations.Select;
import org.junit.jupiter.api.Test;

class OwnerContractPreviewMapperTest {
    @Test void selectsOnlySignedVersionOfTheSameSourceAndExcludesVoidedReplacements() throws Exception {
        String sql = String.join(" ", OwnerPropertyDetailMapper.class.getMethod("findPreviewContractId", Long.class)
                .getAnnotation(Select.class).value());
        assertThat(sql).contains("signature_request.source_document_id=source_document.id",
                "signature_request.status='signed'", "signed_document.id=signature_request.signed_document_id",
                "COALESCE(signed_document.status,'') NOT IN ('voided','superseded')",
                "ORDER BY signature_request.id DESC LIMIT 1",
                "source_document.id=#{sourceDocumentId}");
    }
}
