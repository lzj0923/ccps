package com.ccps.backend.mapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.ccps.backend.dto.OwnerRentIncomeResponse.PropertyOption;
import com.ccps.backend.dto.OwnerRentIncomeResponse.RecentReceipt;
import com.ccps.backend.dto.OwnerRentIncomeResponse.RentRecord;
import com.ccps.backend.dto.OwnerRentIncomeResponse.YearlyTrend;

@Mapper
public interface OwnerRentIncomeMapper {

    @Update("""
            UPDATE finance_records fr
            JOIN rent_payments rp ON rp.finance_record_id = fr.id
            JOIN rent_invoices ri ON ri.id = rp.rent_invoice_id
            JOIN leases l ON l.id = ri.lease_id
            JOIN owner_units ou ON ou.unit_id = l.unit_id
              AND ou.status = 'active' AND ou.asset_stage = 'OPERATING'
            JOIN owners o ON o.id = ou.owner_id AND o.status = 'active'
            SET fr.confirmation_status = 'confirmed'
            WHERE rp.rent_invoice_id = #{invoiceId}
              AND o.user_id = #{userId}
              AND fr.record_type = 'rent_payment'
              AND fr.confirmation_status = 'pending'
            """)
    int confirmPendingReceipt(@Param("userId") Long userId,
                              @Param("invoiceId") Long invoiceId);

    @Select("""
            SELECT
              COALESCE(SUM(rcs.amount_due), 0) AS amount_due,
              COALESCE(SUM(rcs.amount_paid), 0) AS amount_paid,
              COALESCE(SUM(rcs.unpaid_amount), 0) AS unpaid_amount
            FROM v_rent_collection_status rcs
            JOIN units u ON u.id = rcs.unit_id
            WHERE rcs.billing_month >= #{startDate}
              AND rcs.billing_month < #{endDate}
              AND (#{projectId} IS NULL OR u.project_id = #{projectId})
              AND EXISTS (
                SELECT 1
                FROM owner_units ou
                JOIN owners o ON o.id = ou.owner_id
                WHERE ou.unit_id = rcs.unit_id
                  AND ou.status = 'active'
                  AND ou.asset_stage = 'OPERATING'
                  AND o.status = 'active'
                  AND o.user_id = #{userId}
              )
            """)
    PeriodTotals findPeriodTotals(
            @Param("userId") Long userId,
            @Param("projectId") Long projectId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Select("""
            SELECT DISTINCT p.id, p.name
            FROM owners o
            JOIN owner_units ou ON ou.owner_id = o.id AND ou.status = 'active' AND ou.asset_stage = 'OPERATING'
            JOIN units u ON u.id = ou.unit_id
            JOIN projects p ON p.id = u.project_id
            WHERE o.user_id = #{userId}
              AND o.status = 'active'
              AND p.status = 'active'
            ORDER BY p.name
            """)
    List<PropertyOption> findProperties(@Param("userId") Long userId);

    @Select("""
            SELECT
              YEAR(rcs.billing_month) AS year,
              COALESCE(SUM(rcs.amount_paid), 0) AS amount_paid
            FROM v_rent_collection_status rcs
            JOIN units u ON u.id = rcs.unit_id
            WHERE (#{projectId} IS NULL OR u.project_id = #{projectId})
              AND EXISTS (
                SELECT 1
                FROM owner_units ou
                JOIN owners o ON o.id = ou.owner_id
                WHERE ou.unit_id = rcs.unit_id
                  AND ou.status = 'active'
                  AND ou.asset_stage = 'OPERATING'
                  AND o.status = 'active'
                  AND o.user_id = #{userId}
              )
            GROUP BY YEAR(rcs.billing_month)
            ORDER BY year DESC
            LIMIT 5
            """)
    List<YearlyTrend> findYearlyTrend(
            @Param("userId") Long userId,
            @Param("projectId") Long projectId);

    @Select("""
            <script>
            SELECT
              rcs.rent_invoice_id AS invoice_id,
              p.id AS project_id,
              p.name AS project_name,
              u.unit_no,
              t.full_name AS tenant_name,
              rcs.billing_month,
              rcs.due_date,
              rcs.amount_due,
              rcs.amount_paid,
              rcs.unpaid_amount,
              payment.received_date,
              CASE
                WHEN rcs.amount_paid >= rcs.amount_due THEN 'paid'
                WHEN rcs.amount_paid > 0 THEN 'partial'
                WHEN CURRENT_DATE > rcs.due_date THEN 'overdue'
                ELSE 'unpaid'
              END AS status,
              CASE
                WHEN payment.payment_count IS NULL THEN NULL
                WHEN payment.rejected_count &gt; 0 THEN 'rejected'
                WHEN payment.confirmed_count = payment.payment_count THEN 'confirmed'
                ELSE 'pending'
              END AS confirmation_status
            FROM v_rent_collection_status rcs
            JOIN units u ON u.id = rcs.unit_id
            JOIN projects p ON p.id = u.project_id
            JOIN tenants t ON t.id = rcs.tenant_id
            JOIN leases l ON l.id = rcs.lease_id
            LEFT JOIN (
              SELECT
                rp.rent_invoice_id,
                MAX(CASE WHEN fr.payment_status = 'paid' THEN fr.transaction_date END) AS received_date,
                COUNT(fr.id) AS payment_count,
                SUM(CASE WHEN fr.confirmation_status = 'confirmed' THEN 1 ELSE 0 END) AS confirmed_count,
                SUM(CASE WHEN fr.confirmation_status = 'rejected' THEN 1 ELSE 0 END) AS rejected_count
              FROM rent_payments rp
              JOIN finance_records fr ON fr.id = rp.finance_record_id
              GROUP BY rp.rent_invoice_id
            ) payment ON payment.rent_invoice_id = rcs.rent_invoice_id
            WHERE YEAR(rcs.billing_month) = #{year}
              AND MONTH(rcs.billing_month) = #{month}
              AND EXISTS (
                SELECT 1
                FROM owner_units ou
                JOIN owners o ON o.id = ou.owner_id
                WHERE ou.unit_id = rcs.unit_id
                  AND ou.status = 'active'
                  AND ou.asset_stage = 'OPERATING'
                  AND o.status = 'active'
                  AND o.user_id = #{userId}
              )
            <if test="projectId != null">
              AND p.id = #{projectId}
            </if>
            <if test="status != null and status != ''">
              AND (CASE
                WHEN rcs.amount_paid >= rcs.amount_due THEN 'paid'
                WHEN rcs.amount_paid > 0 THEN 'partial'
                WHEN CURRENT_DATE > rcs.due_date THEN 'overdue'
                ELSE 'unpaid'
              END) = #{status}
            </if>
            ORDER BY rcs.due_date DESC, p.name, u.unit_no
            </script>
            """)
    List<RentRecord> findRecords(
            @Param("userId") Long userId,
            @Param("year") int year,
            @Param("month") int month,
            @Param("projectId") Long projectId,
            @Param("status") String status);

    @Select("""
            SELECT
              fr.id AS transaction_id,
              p.name AS project_name,
              u.unit_no,
              COALESCE(t.full_name, '-') AS tenant_name,
              fr.transaction_date AS received_date,
              fr.amount
            FROM finance_records fr
            JOIN owners o ON o.id = fr.owner_id
            JOIN units u ON u.id = fr.unit_id
            JOIN projects p ON p.id = u.project_id
            LEFT JOIN tenants t ON t.id = fr.tenant_id
            WHERE o.user_id = #{userId}
              AND o.status = 'active'
              AND fr.record_type = 'rent_payment'
              AND fr.payment_status = 'paid'
              AND EXISTS (
                SELECT 1 FROM owner_units ou
                WHERE ou.owner_id = o.id AND ou.unit_id = fr.unit_id
                  AND ou.status = 'active' AND ou.asset_stage = 'OPERATING'
              )
              AND (#{projectId} IS NULL OR p.id = #{projectId})
            ORDER BY fr.transaction_date DESC, fr.id DESC
            LIMIT 5
            """)
    List<RecentReceipt> findRecentReceipts(
            @Param("userId") Long userId,
            @Param("projectId") Long projectId);

    class PeriodTotals {
        private BigDecimal amountDue;
        private BigDecimal amountPaid;
        private BigDecimal unpaidAmount;

        public BigDecimal getAmountDue() {
            return amountDue;
        }

        public void setAmountDue(BigDecimal amountDue) {
            this.amountDue = amountDue;
        }

        public BigDecimal getAmountPaid() {
            return amountPaid;
        }

        public void setAmountPaid(BigDecimal amountPaid) {
            this.amountPaid = amountPaid;
        }

        public BigDecimal getUnpaidAmount() {
            return unpaidAmount;
        }

        public void setUnpaidAmount(BigDecimal unpaidAmount) {
            this.unpaidAmount = unpaidAmount;
        }
    }
}
