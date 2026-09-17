package com.ccps.backend.migration;

import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.builder.annotation.MapperAnnotationBuilder;
import com.ccps.backend.mapper.AdminTenancyMapper;
import java.time.LocalDate;
import java.util.Map;

/** Unit-level check of the actual MyBatis bound SQL, without a database. */
public final class LegacyBillingSqlCheck {
    public static void main(String[] args) {
        Configuration cfg=new Configuration();
        new MapperAnnotationBuilder(cfg,AdminTenancyMapper.class).parse();
        for(String method:new String[]{"generateMonthlyInvoices","findLeasePeriodForBillingMonth","findLeasePeriodContractFile"}) {
            String sql=cfg.getMappedStatement(AdminTenancyMapper.class.getName()+"."+method)
                .getBoundSql(Map.of("billingMonth",LocalDate.of(2026,9,1),"leaseId",1L,"periodId",1L)).getSql();
            if(sql.contains("&lt;")||sql.contains("&gt;"))
                throw new AssertionError(method+" sends XML escapes as SQL operators: "+sql);
        }
        System.out.println("CHECK: billing bound SQL contains executable comparison operators");
    }
}
