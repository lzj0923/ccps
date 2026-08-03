package com.ccps.backend.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;

import org.apache.ibatis.annotations.Select;
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
}
