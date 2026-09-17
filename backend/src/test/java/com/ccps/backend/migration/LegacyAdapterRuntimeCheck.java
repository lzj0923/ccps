package com.ccps.backend.migration;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.sql.DriverManager;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import com.ccps.backend.CcpsBackendApplication;
import com.ccps.backend.dto.AdminTenantDepositTransactionRequest;
import com.ccps.backend.dto.AdminMaintenanceCreateRequest;
import com.ccps.backend.dto.AdminMaintenanceCompleteRequest;
import com.ccps.backend.service.AdminTenancyService;
import com.ccps.backend.service.AdminFinanceReviewService;
import com.ccps.backend.service.AdminMaintenanceService;

/** Executable local integration check. Every business-operation test is rolled back. */
public final class LegacyAdapterRuntimeCheck {
    private static long id(JdbcTemplate jdbc,String table) {
        return jdbc.queryForObject("SELECT target_id FROM legacy_import_links WHERE source_system='ccps-adapter-demo-v1' AND target_table=? LIMIT 1",Long.class,table);
    }
    private static void money(JdbcTemplate jdbc,String sql,long id,String expected) {
        BigDecimal actual=jdbc.queryForObject(sql,BigDecimal.class,id);
        if (actual==null || actual.compareTo(new BigDecimal(expected))!=0)
            throw new AssertionError(sql+" expected="+expected+" actual="+actual);
    }
    public static void main(String[] args) throws Exception {
        String url=System.getenv("DB_URL");
        if(url==null || !url.startsWith("jdbc:mysql://127.0.0.1:3338/ccps_property_management?"))
            throw new IllegalStateException("Only the isolated trial database is allowed");
        try(var conn=DriverManager.getConnection(url,"root","");var stmt=conn.createStatement();var rs=stmt.executeQuery("SELECT @@datadir")) {
            rs.next();
            if(!rs.getString(1).replace('\\','/').contains("/legacy-migration-trial/mysql-data/"))
                throw new IllegalStateException("Unexpected MySQL data directory");
        }
        SpringApplication app=new SpringApplication(CcpsBackendApplication.class);
        app.setWebApplicationType(WebApplicationType.NONE);
        try(var ctx=app.run(args)) {
            JdbcTemplate jdbc=ctx.getBean(JdbcTemplate.class);
            TransactionTemplate tx=new TransactionTemplate(ctx.getBean(PlatformTransactionManager.class));
            AdminTenancyService tenancy=ctx.getBean(AdminTenancyService.class);
            AdminFinanceReviewService finance=ctx.getBean(AdminFinanceReviewService.class);
            AdminMaintenanceService maintenance=ctx.getBean(AdminMaintenanceService.class);
            long lease=id(jdbc,"leases"),reserve=id(jdbc,"reserve_accounts"),owner=id(jdbc,"owners"),
                holding=id(jdbc,"owner_units"),unit=id(jdbc,"units");
            long actor=jdbc.queryForObject("SELECT id FROM users WHERE username='admin'",Long.class);
            tx.executeWithoutResult(status->{
                long before=jdbc.queryForObject("SELECT COUNT(*) FROM rent_invoices",Long.class);
                tenancy.generateCurrentMonthInvoices(); tenancy.generateCurrentMonthInvoices();
                long after=jdbc.queryForObject("SELECT COUNT(*) FROM rent_invoices",Long.class);
                if(before!=after) throw new AssertionError("Repeated billing changed existing invoices");
                status.setRollbackOnly();
            });
            System.out.println("CHECK: repeated monthly billing is idempotent");
            tx.executeWithoutResult(status->{
                var refund=tenancy.createTenantDepositTransaction(actor,lease,
                    new AdminTenantDepositTransactionRequest("refund",new BigDecimal("500"),LocalDate.now(),"迁移适配回归测试"));
                money(jdbc,"SELECT SUM(CASE direction WHEN 'credit' THEN amount ELSE -amount END) FROM tenant_deposit_transactions WHERE lease_id=? AND status IN ('posted','pending')",lease,"3000");
                long fr=jdbc.queryForObject("SELECT finance_record_id FROM tenant_deposit_transactions WHERE id=?",Long.class,refund.id());
                finance.confirm(actor,fr,LocalDate.now(),LocalDate.now(),"迁移退款测试");
                money(jdbc,"SELECT SUM(CASE direction WHEN 'credit' THEN amount ELSE -amount END) FROM tenant_deposit_transactions WHERE lease_id=? AND status='posted'",lease,"3000");
                money(jdbc,"SELECT current_balance FROM reserve_accounts WHERE id=?",reserve,"4200");
                status.setRollbackOnly();
            });
            System.out.println("CHECK: deposit refund and finance confirmation debit exactly once");
            tx.executeWithoutResult(status->{
                var work=maintenance.createForProperty(actor,owner,holding,
                    new AdminMaintenanceCreateRequest(unit,null,"maintenance","迁移后维修测试","rollback test",LocalDateTime.now(),new BigDecimal("100")));
                maintenance.complete(actor,work.id(),new AdminMaintenanceCompleteRequest(new BigDecimal("100"),"reserve","迁移后维修完成测试"));
                Long fr=jdbc.queryForObject("SELECT ce.finance_record_id FROM maintenance_work_orders m JOIN cashflow_entries ce ON ce.id=m.cashflow_entry_id WHERE m.id=?",Long.class,work.id());
                finance.confirm(actor,fr,LocalDate.now(),LocalDate.now(),"迁移维修财务确认测试");
                money(jdbc,"SELECT current_balance FROM reserve_accounts WHERE id=?",reserve,"4600");
                status.setRollbackOnly();
            });
            System.out.println("CHECK: maintenance workflow and reserve settlement work after migration");
            money(jdbc,"SELECT current_balance FROM reserve_accounts WHERE id=?",reserve,"4700");
            System.out.println("CHECK: all operation tests rolled back; baseline preserved");
        }
    }
}
