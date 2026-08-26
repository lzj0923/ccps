package com.ccps.backend.database;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;

class RentAccountingDateSchemaMigrationTest {
    @Test
    void addsAndBackfillsReceiptAndMonthlyAllocationColumns() throws Exception {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        DatabaseMetaData metadata = mock(DatabaseMetaData.class);
        Statement statement = mock(Statement.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getCatalog()).thenReturn("ccps_property_management");
        when(connection.getMetaData()).thenReturn(metadata);
        when(connection.createStatement()).thenReturn(statement);
        when(metadata.getColumns(anyString(), isNull(), anyString(), anyString())).thenAnswer(invocation -> result(false));

        new RentAccountingDateSchemaMigration(dataSource).run(null);

        verify(statement).executeUpdate("ALTER TABLE finance_records ADD COLUMN receipt_date DATE NULL "
                + "COMMENT 'Actual cash receipt date used for accounting reconciliation' AFTER transaction_date");
        verify(statement).executeUpdate("ALTER TABLE rent_payments ADD COLUMN allocated_amount DECIMAL(18,2) NULL "
                + "COMMENT 'Amount recognized for the linked rental month' AFTER finance_record_id");
        verify(statement).executeUpdate("UPDATE finance_records SET receipt_date=transaction_date WHERE receipt_date IS NULL");
        verify(statement).executeUpdate("UPDATE rent_payments rp JOIN finance_records fr ON fr.id=rp.finance_record_id "
                + "SET rp.allocated_amount=fr.amount WHERE rp.allocated_amount IS NULL");
    }

    private ResultSet result(boolean exists) throws Exception {
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.next()).thenReturn(exists);
        return resultSet;
    }
}
