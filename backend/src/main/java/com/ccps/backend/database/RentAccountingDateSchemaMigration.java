package com.ccps.backend.database;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RentAccountingDateSchemaMigration implements ApplicationRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(RentAccountingDateSchemaMigration.class);
    private static final int MYSQL_DUPLICATE_COLUMN = 1060;
    private final DataSource dataSource;

    public RentAccountingDateSchemaMigration(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            addIfMissing(connection, statement, "finance_records", "receipt_date",
                    "ALTER TABLE finance_records ADD COLUMN receipt_date DATE NULL "
                            + "COMMENT 'Actual cash receipt date used for accounting reconciliation' AFTER transaction_date");
            addIfMissing(connection, statement, "rent_payments", "allocated_amount",
                    "ALTER TABLE rent_payments ADD COLUMN allocated_amount DECIMAL(18,2) NULL "
                            + "COMMENT 'Amount recognized for the linked rental month' AFTER finance_record_id");
            statement.executeUpdate("UPDATE finance_records SET receipt_date=transaction_date WHERE receipt_date IS NULL");
            statement.executeUpdate("UPDATE rent_payments rp JOIN finance_records fr ON fr.id=rp.finance_record_id "
                    + "SET rp.allocated_amount=fr.amount WHERE rp.allocated_amount IS NULL");
        }
    }

    private void addIfMissing(Connection connection, Statement statement, String table, String column, String ddl)
            throws SQLException {
        DatabaseMetaData metadata = connection.getMetaData();
        try (ResultSet columns = metadata.getColumns(connection.getCatalog(), null, table, column)) {
            if (columns.next()) return;
        }
        try {
            statement.executeUpdate(ddl);
            LOGGER.info("Added missing rent accounting column {}.{}", table, column);
        } catch (SQLException error) {
            if (error.getErrorCode() != MYSQL_DUPLICATE_COLUMN) throw error;
            LOGGER.info("Rent accounting column {}.{} was added concurrently", table, column);
        }
    }
}
