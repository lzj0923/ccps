package com.ccps.backend.mapper;

import java.math.BigDecimal;
import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ReserveTargetPolicyMapper {

    @Select("""
            SELECT ra.id
            FROM reserve_accounts ra
            JOIN owner_units ou ON ou.id = ra.owner_unit_id
              AND ou.status = 'active' AND ou.asset_stage = 'OPERATING'
            WHERE ra.status = 'active'
            ORDER BY ra.id
            """)
    List<Long> findActiveAccountIds();

    @Select("""
            SELECT ra.id AS account_id, ra.minimum_balance, ra.minimum_balance_mode,
                   ra.calculated_minimum_balance, ra.rent_buffer_amount,
                   ra.monthly_expense_average, ra.expense_buffer_months,
                   COALESCE(
                     (SELECT SUM(l.monthly_rent) FROM leases l
                       WHERE l.unit_id = ou.unit_id AND l.status = 'active'
                         AND CURRENT_DATE BETWEEN l.start_date AND l.end_date),
                     (SELECT l.monthly_rent FROM leases l
                       WHERE l.unit_id = ou.unit_id
                       ORDER BY l.end_date DESC, l.id DESC LIMIT 1), 0
                   ) AS monthly_rent,
                   COALESCE(
                     CAST(JSON_UNQUOTE(JSON_EXTRACT(pbp.profile_json, '$.buildingManagementFee')) AS DECIMAL(18, 2)),
                     0
                   ) AS building_management_fee,
                   COALESCE(expense.total_amount, 0) AS expense_total,
                   GREATEST(1, LEAST(12,
                     TIMESTAMPDIFF(MONTH, COALESCE(ou.actual_handover_date, CURRENT_DATE), CURRENT_DATE) + 1
                   )) AS tracked_months
            FROM reserve_accounts ra
            JOIN owner_units ou ON ou.id = ra.owner_unit_id
              AND ou.status = 'active' AND ou.asset_stage = 'OPERATING'
            LEFT JOIN property_basic_profiles pbp ON pbp.owner_unit_id = ou.id
            LEFT JOIN (
              SELECT ce.unit_id, ce.owner_id, SUM(fr.amount) AS total_amount
              FROM cashflow_entries ce
              JOIN finance_records fr ON fr.id = ce.finance_record_id
              WHERE ce.direction = 'expense'
                AND ce.category IN ('management','utilities','maintenance','cleaning','service_fee','insurance','tax','other')
                AND ce.occurred_on >= DATE_SUB(DATE_FORMAT(CURRENT_DATE, '%Y-%m-01'), INTERVAL 11 MONTH)
                AND fr.confirmation_status <> 'rejected'
                AND fr.payment_status <> 'voided'
              GROUP BY ce.unit_id, ce.owner_id
            ) expense ON expense.unit_id = ou.unit_id AND expense.owner_id = ou.owner_id
            WHERE ra.id = #{accountId} AND ra.status = 'active'
            FOR UPDATE
            """)
    TargetContext lockTarget(@Param("accountId") Long accountId);

    @Update("""
            UPDATE reserve_accounts
            SET calculated_minimum_balance = #{calculatedMinimum},
                rent_buffer_amount = #{rentBuffer},
                monthly_expense_average = #{monthlyExpenseAverage},
                expense_buffer_months = #{expenseBufferMonths},
                minimum_balance_calculated_at = NOW(),
                minimum_balance = CASE WHEN minimum_balance_mode = 'auto'
                                       THEN #{calculatedMinimum} ELSE minimum_balance END
            WHERE id = #{accountId} AND status = 'active'
            """)
    int updateCalculation(@Param("accountId") Long accountId,
            @Param("calculatedMinimum") BigDecimal calculatedMinimum,
            @Param("rentBuffer") BigDecimal rentBuffer,
            @Param("monthlyExpenseAverage") BigDecimal monthlyExpenseAverage,
            @Param("expenseBufferMonths") int expenseBufferMonths);

    @Insert("""
            INSERT INTO audit_logs (actor_user_id, action, entity_type, entity_id, before_data, after_data)
            VALUES (NULL, 'recalculate_reserve_target', 'reserve_account', #{accountId},
              JSON_OBJECT('minimumBalance', #{beforeMinimum}, 'mode', #{mode}),
              JSON_OBJECT('calculatedMinimum', #{calculatedMinimum}, 'rentBuffer', #{rentBuffer},
                          'monthlyExpenseAverage', #{monthlyExpenseAverage},
                          'expenseBufferMonths', #{expenseBufferMonths}))
            """)
    int insertCalculationAudit(@Param("accountId") Long accountId,
            @Param("beforeMinimum") BigDecimal beforeMinimum, @Param("mode") String mode,
            @Param("calculatedMinimum") BigDecimal calculatedMinimum,
            @Param("rentBuffer") BigDecimal rentBuffer,
            @Param("monthlyExpenseAverage") BigDecimal monthlyExpenseAverage,
            @Param("expenseBufferMonths") int expenseBufferMonths);

    class TargetContext {
        private Long accountId;
        private BigDecimal minimumBalance, calculatedMinimumBalance, rentBufferAmount,
                monthlyExpenseAverage, monthlyRent, buildingManagementFee, expenseTotal;
        private String minimumBalanceMode;
        private Integer trackedMonths, expenseBufferMonths;
        public Long getAccountId() { return accountId; } public void setAccountId(Long v) { accountId = v; }
        public BigDecimal getMinimumBalance() { return minimumBalance; } public void setMinimumBalance(BigDecimal v) { minimumBalance = v; }
        public BigDecimal getCalculatedMinimumBalance() { return calculatedMinimumBalance; } public void setCalculatedMinimumBalance(BigDecimal v) { calculatedMinimumBalance = v; }
        public BigDecimal getRentBufferAmount() { return rentBufferAmount; } public void setRentBufferAmount(BigDecimal v) { rentBufferAmount = v; }
        public BigDecimal getMonthlyExpenseAverage() { return monthlyExpenseAverage; } public void setMonthlyExpenseAverage(BigDecimal v) { monthlyExpenseAverage = v; }
        public BigDecimal getMonthlyRent() { return monthlyRent; } public void setMonthlyRent(BigDecimal v) { monthlyRent = v; }
        public BigDecimal getBuildingManagementFee() { return buildingManagementFee; } public void setBuildingManagementFee(BigDecimal v) { buildingManagementFee = v; }
        public BigDecimal getExpenseTotal() { return expenseTotal; } public void setExpenseTotal(BigDecimal v) { expenseTotal = v; }
        public String getMinimumBalanceMode() { return minimumBalanceMode; } public void setMinimumBalanceMode(String v) { minimumBalanceMode = v; }
        public Integer getTrackedMonths() { return trackedMonths; } public void setTrackedMonths(Integer v) { trackedMonths = v; }
        public Integer getExpenseBufferMonths() { return expenseBufferMonths; } public void setExpenseBufferMonths(Integer v) { expenseBufferMonths = v; }
    }
}
