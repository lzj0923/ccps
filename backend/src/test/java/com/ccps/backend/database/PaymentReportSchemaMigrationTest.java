package com.ccps.backend.database;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;

class PaymentReportSchemaMigrationTest {

    @Test
    void missingPaymentReportColumnsAreAddedBeforeRequestsCanRun() throws Exception {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        DatabaseMetaData metadata = mock(DatabaseMetaData.class);
        Statement statement = mock(Statement.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getCatalog()).thenReturn("ccps_property_management");
        when(connection.getMetaData()).thenReturn(metadata);
        when(connection.createStatement()).thenReturn(statement);
        when(metadata.getColumns(anyString(), isNull(), anyString(), anyString())).thenAnswer(invocation -> result(false));

        new PaymentReportSchemaMigration(dataSource).run(null);

        verify(statement).executeUpdate("ALTER TABLE payment_receipts ADD COLUMN fee_account_type VARCHAR(40) NULL AFTER bank_reference");
        verify(statement).executeUpdate("ALTER TABLE payment_receipts ADD COLUMN fee_account_no VARCHAR(120) NULL AFTER fee_account_type");
        verify(statement).executeUpdate("ALTER TABLE maintenance_work_orders ADD COLUMN payer_name VARCHAR(160) NULL AFTER actual_amount");
        verify(statement, times(7)).executeUpdate(anyString());
    }

    @Test
    void existingPaymentReportColumnsAreNotAddedAgain() throws Exception {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        DatabaseMetaData metadata = mock(DatabaseMetaData.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getCatalog()).thenReturn("ccps_property_management");
        when(connection.getMetaData()).thenReturn(metadata);
        when(metadata.getColumns(anyString(), isNull(), anyString(), anyString())).thenAnswer(invocation -> result(true));

        new PaymentReportSchemaMigration(dataSource).run(null);

        verify(connection, never()).createStatement();
    }

    private ResultSet result(boolean exists) throws Exception {
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.next()).thenReturn(exists);
        return resultSet;
    }
}
