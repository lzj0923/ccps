package com.ccps.backend.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;

import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.junit.jupiter.api.Test;

class AdminPropertyContractRecordMapperSqlTest {
    @Test
    void contractRecordsCanFindAnExistingGeneratedFileByNumber() throws Exception {
        Method method = AdminPropertyContractRecordMapper.class.getDeclaredMethod(
                "findByContractNo", Long.class, String.class);
        String sql = String.join(" ", method.getAnnotation(Select.class).value());

        assertThat(sql).contains("contract_no=#{contractNo}", "owner_unit_id=#{ownerUnitId}");
    }

    @Test
    void generatedFilesAreScopedToTheCurrentLeaseAndContractType() throws Exception {
        Method method = AdminPropertyContractRecordMapper.class.getDeclaredMethod(
                "findByLeaseAndType", Long.class, Long.class, String.class);
        String sql = String.join(" ", method.getAnnotation(Select.class).value());

        assertThat(sql).contains("lease_id=#{leaseId}", "contract_type=#{contractType}");
    }

    @Test
    void leaseContractRowsUseTheElectronicSignatureState() throws Exception {
        Method method = AdminPropertyContractRecordMapper.class.getDeclaredMethod(
                "listLeaseContracts", Long.class);
        String sql = String.join(" ", method.getAnnotation(Select.class).value());

        assertThat(sql).contains("electronic_signature_requests", "status='signed'", "THEN 'completed'", "ELSE 'pending'");
    }

    @Test
    void completedLeaseContractRowsExposeTheLatestApprovedSignedFile() throws Exception {
        Method method = AdminPropertyContractRecordMapper.class.getDeclaredMethod(
                "listLeaseContracts", Long.class);
        String sql = String.join(" ", method.getAnnotation(Select.class).value());

        assertThat(sql)
                .contains("final_sr.id = (", "SELECT completed.id", "ORDER BY completed.signed_at DESC",
                        "signed.status NOT IN ('voided','superseded')",
                        "COALESCE(signed.original_name,d.original_name) AS originalName",
                        "COALESCE(final_sr.signed_at,l.updated_at) AS updatedAt");
    }

    @Test
    void leaseContractRowsOnlyExposeDownloadableDocuments() throws Exception {
        Method method = AdminPropertyContractRecordMapper.class.getDeclaredMethod(
                "listLeaseContracts", Long.class);
        String sql = String.join(" ", method.getAnnotation(Select.class).value());

        assertThat(sql)
                .contains("JOIN documents d ON d.id=l.contract_document_id", "d.document_type='lease'",
                        "JOIN document_links dl ON dl.document_id=d.id", "dl.entity_type='lease'",
                        "dl.entity_id=l.id", "dl.relation_type='contract'")
                .doesNotContain("LEFT JOIN documents d");
    }

    @Test
    void leaseOptionsExposeTheRoomBoundToEachLease() throws Exception {
        Method method = AdminPropertyContractRecordMapper.class.getDeclaredMethod("leaseOptions", Long.class);
        String sql = String.join(" ", method.getAnnotation(Select.class).value());

        assertThat(sql).contains("l.rental_space_id", "LEFT JOIN rental_spaces", "rs.space_name AS rentalSpaceName")
                .doesNotContain("rs.name AS rentalSpaceName");
    }

    @Test
    void leaseOptionsExposeTenantDetailsNeededByTheAgreement() throws Exception {
        Method method = AdminPropertyContractRecordMapper.class.getDeclaredMethod("leaseOptions", Long.class);
        String sql = String.join(" ", method.getAnnotation(Select.class).value());

        assertThat(sql).contains("t.identity_no AS tenantIdentity", "t.phone AS tenantPhone",
                "t.email AS tenantEmail");
    }

    @Test
    void leaseOptionRowAcceptsTenantColumnsReturnedByMybatis() {
        var row = new AdminPropertyContractRecordMapper.LeaseOptionRow();
        var metaObject = SystemMetaObject.forObject(row);

        metaObject.setValue("tenantIdentity", "TENANT-001");
        metaObject.setValue("tenantPhone", "+60123456789");
        metaObject.setValue("tenantEmail", "tenant@example.com");

        assertThat(row.getTenantIdentity()).isEqualTo("TENANT-001");
        assertThat(row.getTenantPhone()).isEqualTo("+60123456789");
        assertThat(row.getTenantEmail()).isEqualTo("tenant@example.com");
    }
}
