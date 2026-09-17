package com.ccps.backend.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import java.sql.DriverManager;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.*;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/** Real MySQL joins in a newly created, randomly named test database. Never uses business tables. */
@EnabledIfEnvironmentVariable(named="CCPS_SIGNATURE_DB_TEST",matches="true")
class OwnerSignatureDatabaseTest {
    @Test void realSqlScopesAssignmentsAndRevokesClosedDocumentsAndOwnership() throws Exception {
        try(var connection=DriverManager.getConnection(System.getenv("CCPS_TEST_DB_URL"),System.getenv("CCPS_TEST_DB_USER"),System.getenv("CCPS_TEST_DB_PASSWORD"))) {
            // Never select a business database, even if supplied accidentally.
            if (connection.getCatalog() != null && !connection.getCatalog().isBlank())
                throw new IllegalArgumentException("The test connection must not select an existing database");
            String testDatabase="ccps_sign_test_"+java.util.UUID.randomUUID().toString().replace("-", "");
            try(var create=connection.createStatement()) { create.execute("CREATE DATABASE "+testDatabase); }
            connection.setCatalog(testDatabase);
            System.out.println("Isolated signature test database: " + testDatabase);
            try(var statement=connection.createStatement()) {
                for(String ddl:new String[]{
                    "users(id BIGINT,status VARCHAR(20))",
                    "owners(id BIGINT,user_id BIGINT,full_name VARCHAR(100),status VARCHAR(20))",
                    "owner_units(id BIGINT,owner_id BIGINT,unit_id BIGINT,status VARCHAR(20))",
                    "rental_mandates(id BIGINT,owner_unit_id BIGINT)","leases(id BIGINT,unit_id BIGINT)",
                    "documents(id BIGINT,original_name VARCHAR(100),status VARCHAR(20))",
                    "electronic_signature_requests(id BIGINT,source_document_id BIGINT,root_document_id BIGINT,entity_type VARCHAR(40),entity_id BIGINT,signer_role VARCHAR(30),signer_name VARCHAR(100),status VARCHAR(20),expires_at DATETIME,signed_at DATETIME)",
                    "owner_signature_tasks(request_id BIGINT PRIMARY KEY,owner_id BIGINT,recipient_user_id BIGINT,assigned_by BIGINT,created_at DATETIME DEFAULT CURRENT_TIMESTAMP)"
                }) statement.execute("CREATE TABLE "+ddl);
                statement.execute("INSERT INTO users VALUES(20,'active'),(21,'active')");
                statement.execute("INSERT INTO owners VALUES(10,20,'TEST OWNER','active'),(11,21,'OTHER OWNER','active')");
                statement.execute("INSERT INTO owner_units VALUES(30,10,40,'active'),(31,11,41,'active')");
                statement.execute("INSERT INTO rental_mandates VALUES(50,30)");
                statement.execute("INSERT INTO leases VALUES(60,40)");
                statement.execute("INSERT INTO documents VALUES(70,'TEST ONLY.pdf','approved')");
                statement.execute("INSERT INTO electronic_signature_requests VALUES(1,70,70,'rental_mandate',50,'owner','TEST OWNER','pending',DATE_ADD(NOW(),INTERVAL 1 DAY),NULL)");
            }
            Configuration config=new Configuration(new Environment("test",new JdbcTransactionFactory(),new UnpooledDataSource()));
            config.setMapUnderscoreToCamelCase(true); config.addMapper(OwnerSignatureMapper.class);
            try(SqlSession session=new SqlSessionFactoryBuilder().build(config).openSession(connection)) {
                var mapper=session.getMapper(OwnerSignatureMapper.class);
                assertThat(mapper.recipients(1L)).extracting(OwnerSignatureMapper.Recipient::ownerId).containsExactly(10L);
                assertThat(mapper.authorized(20L,1L)).isZero();
                assertThat(mapper.assign(1L,10L,20L,7L)).isEqualTo(1);
                assertThat(mapper.tasks(20L)).hasSize(1);
                assertThat(mapper.tasks(20L).get(0).canSign()).isTrue();
                assertThat(mapper.tasks(21L)).isEmpty(); assertThat(mapper.authorized(21L,1L)).isZero();
                assertThat(mapper.authorized(20L,1L)).isEqualTo(1);
                try(var s=connection.createStatement()) {
                    s.execute("UPDATE electronic_signature_requests SET expires_at=DATE_SUB(NOW(),INTERVAL 1 DAY)"); session.clearCache();
                    assertThat(mapper.tasks(20L).get(0).canSign()).isFalse();
                    s.execute("UPDATE electronic_signature_requests SET entity_type='lease',entity_id=60,status='signed'"); session.clearCache();
                    assertThat(mapper.recipients(1L)).hasSize(1); assertThat(mapper.tasks(20L).get(0).status()).isEqualTo("signed");
                    s.execute("UPDATE documents SET status='superseded'"); session.clearCache();
                    assertThat(mapper.authorized(20L,1L)).isZero();
                    s.execute("UPDATE documents SET status='approved'");
                    s.execute("UPDATE owner_units SET status='inactive' WHERE owner_id=10"); session.clearCache();
                    assertThat(mapper.tasks(20L)).isEmpty(); assertThat(mapper.authorized(20L,1L)).isZero();
                }
            }
        }
    }
}
