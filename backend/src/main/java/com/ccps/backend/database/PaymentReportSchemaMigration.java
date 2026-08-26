package com.ccps.backend.database;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

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
public class PaymentReportSchemaMigration implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(PaymentReportSchemaMigration.class);
    private static final int MYSQL_DUPLICATE_COLUMN = 1060;
    private static final List<RequiredColumn> REQUIRED_COLUMNS = List.of(
            new RequiredColumn("payment_receipts", "fee_account_type",
                    "ALTER TABLE payment_receipts ADD COLUMN fee_account_type VARCHAR(40) NULL AFTER bank_reference"),
            new RequiredColumn("payment_receipts", "fee_account_no",
                    "ALTER TABLE payment_receipts ADD COLUMN fee_account_no VARCHAR(120) NULL AFTER fee_account_type"),
            new RequiredColumn("maintenance_work_orders", "payer_name",
                    "ALTER TABLE maintenance_work_orders ADD COLUMN payer_name VARCHAR(160) NULL AFTER actual_amount"),
            new RequiredColumn("maintenance_work_orders", "bank_name",
                    "ALTER TABLE maintenance_work_orders ADD COLUMN bank_name VARCHAR(120) NULL AFTER payer_name"),
            new RequiredColumn("maintenance_work_orders", "payment_account_no",
                    "ALTER TABLE maintenance_work_orders ADD COLUMN payment_account_no VARCHAR(120) NULL AFTER bank_name"),
            new RequiredColumn("maintenance_work_orders", "fee_account_type",
                    "ALTER TABLE maintenance_work_orders ADD COLUMN fee_account_type VARCHAR(40) NULL AFTER payment_account_no"),
            new RequiredColumn("maintenance_work_orders", "fee_account_no",
                    "ALTER TABLE maintenance_work_orders ADD COLUMN fee_account_no VARCHAR(120) NULL AFTER fee_account_type"));

    private final DataSource dataSource;

    public PaymentReportSchemaMigration(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            List<RequiredColumn> missing = findMissingColumns(connection);
            if (missing.isEmpty()) return;
            try (Statement statement = connection.createStatement()) {
                for (RequiredColumn column : missing) addColumn(statement, column);
            }
        }
    }

    private List<RequiredColumn> findMissingColumns(Connection connection) throws SQLException {
        String catalog = connection.getCatalog();
        DatabaseMetaData metadata = connection.getMetaData();
        List<RequiredColumn> missing = new ArrayList<>();
        for (RequiredColumn column : REQUIRED_COLUMNS) {
            try (ResultSet columns = metadata.getColumns(catalog, null, column.tableName(), column.columnName())) {
                if (!columns.next()) missing.add(column);
            }
        }
        return missing;
    }

    private void addColumn(Statement statement, RequiredColumn column) throws SQLException {
        try {
            statement.executeUpdate(column.ddl());
            LOGGER.info("Added missing payment report column {}.{}", column.tableName(), column.columnName());
        } catch (SQLException error) {
            if (error.getErrorCode() != MYSQL_DUPLICATE_COLUMN) throw error;
            LOGGER.info("Payment report column {}.{} was added concurrently", column.tableName(), column.columnName());
        }
    }

    private record RequiredColumn(String tableName, String columnName, String ddl) {
    }
}
