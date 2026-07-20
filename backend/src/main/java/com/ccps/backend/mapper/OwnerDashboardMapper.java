package com.ccps.backend.mapper;

import java.math.BigDecimal;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.ccps.backend.dto.OwnerDashboardResponse.Notification;
import com.ccps.backend.dto.OwnerDashboardResponse.Property;

@Mapper
public interface OwnerDashboardMapper {

    @Select("""
            SELECT ou.asset_stage
            FROM owner_units ou
            JOIN owners o ON o.id = ou.owner_id
            WHERE ou.id = #{ownerUnitId}
              AND o.user_id = #{userId}
              AND o.status = 'active'
              AND ou.status = 'active'
            """)
    String findOwnedPropertyStage(@Param("userId") Long userId,
                                  @Param("ownerUnitId") Long ownerUnitId);

    @Update("""
            UPDATE owner_unit_services
            SET status = 'ended', ended_at = CURRENT_DATE
            WHERE owner_unit_id = #{ownerUnitId} AND status <> 'ended'
            """)
    int endOwnedPropertyServices(@Param("ownerUnitId") Long ownerUnitId);

    @Insert("""
            INSERT INTO owner_unit_services (owner_unit_id, service_type, status, started_at, ended_at)
            VALUES (#{ownerUnitId}, #{serviceType}, 'active', CURRENT_DATE, NULL)
            ON DUPLICATE KEY UPDATE status = 'active', ended_at = NULL
            """)
    int activateOwnedPropertyService(@Param("ownerUnitId") Long ownerUnitId,
                                     @Param("serviceType") String serviceType);

    @Select("""
            SELECT
              ou.id AS owner_unit_id,
              u.id AS unit_id,
              p.name AS project_name,
              p.city,
              u.unit_no,
              u.unit_type,
              u.area_sqm,
              u.bedroom_count,
              ou.asset_stage,
              ou.expected_handover_date,
              ou.actual_handover_date,
              svc.services_csv,
              COALESCE(pay.purchase_price, 0) AS purchase_price,
              CASE WHEN ou.asset_stage <> 'PRE_HANDOVER' THEN COALESCE(pay.purchase_price, 0)
                   ELSE COALESCE(pay.paid_amount, 0) END AS paid_amount,
              CASE WHEN ou.asset_stage <> 'PRE_HANDOVER' THEN 0
                   ELSE GREATEST(COALESCE(pay.purchase_price, 0) - COALESCE(pay.paid_amount, 0), 0) END AS remaining_amount,
              COALESCE(pay.paid_installment_count, 0) AS paid_installment_count,
              COALESCE(pay.total_installment_count, 0) AS total_installment_count,
              pay.next_due_date,
              lease_info.tenant_name,
              COALESCE(lease_info.monthly_rent, 0) AS monthly_rent,
              lease_info.lease_end_date,
              COALESCE(rent_info.current_month_rent_due, 0) AS current_month_rent_due,
              COALESCE(rent_info.current_month_rent_paid, 0) AS current_month_rent_paid,
              COALESCE(rent_info.current_month_rent_outstanding, 0) AS current_month_rent_outstanding,
              COALESCE(ra.minimum_balance, 0) AS reserve_minimum_balance,
              COALESCE(ra.current_balance, 0) AS reserve_balance,
              COALESCE(cashflow.monthly_income, 0) AS monthly_income,
              COALESCE(cashflow.monthly_expense, 0) AS monthly_expense,
              COALESCE(maintenance.pending_maintenance_count, 0) AS pending_maintenance_count,
              CASE
                WHEN ou.asset_stage <> 'PRE_HANDOVER' THEN 'not_applicable'
                WHEN COALESCE(pay.total_installment_count, 0) = 0 THEN 'not_configured'
                WHEN COALESCE(pay.total_installment_count, 0) > 0
                  AND COALESCE(pay.paid_amount, 0) >= COALESCE(pay.purchase_price, 0) THEN 'paid'
                WHEN COALESCE(pay.has_overdue, 0) = 1 THEN 'overdue'
                WHEN pay.next_due_date BETWEEN CURRENT_DATE AND DATE_ADD(CURRENT_DATE, INTERVAL 30 DAY) THEN 'due_soon'
                ELSE 'paying'
              END AS payment_status
            FROM owners o
            JOIN owner_units ou ON ou.owner_id = o.id AND ou.status = 'active'
            JOIN units u ON u.id = ou.unit_id
            JOIN projects p ON p.id = u.project_id
            LEFT JOIN (
              SELECT owner_unit_id,
                     GROUP_CONCAT(service_type ORDER BY service_type SEPARATOR ',') AS services_csv
              FROM owner_unit_services
              WHERE status = 'active'
              GROUP BY owner_unit_id
            ) svc ON svc.owner_unit_id = ou.id
            LEFT JOIN (
              SELECT
                pc.owner_unit_id,
                MAX(pc.purchase_price) AS purchase_price,
                COALESCE(SUM(pi.amount_paid), 0) AS paid_amount,
                SUM(CASE WHEN pi.amount_due > 0 AND pi.amount_paid >= pi.amount_due THEN 1 ELSE 0 END) AS paid_installment_count,
                COUNT(pi.id) AS total_installment_count,
                MIN(CASE WHEN pi.amount_paid < pi.amount_due THEN pi.due_date END) AS next_due_date,
                MAX(CASE WHEN pi.amount_paid < pi.amount_due AND pi.due_date < CURRENT_DATE THEN 1 ELSE 0 END) AS has_overdue
              FROM purchase_contracts pc
              JOIN owner_units pou ON pou.id = pc.owner_unit_id
              LEFT JOIN payment_plans pp ON pp.purchase_contract_id = pc.id
                AND pp.status = 'active' AND pou.asset_stage = 'PRE_HANDOVER'
              LEFT JOIN payment_installments pi ON pi.payment_plan_id = pp.id
              WHERE pc.status IN ('active', 'completed')
              GROUP BY pc.owner_unit_id
            ) pay ON pay.owner_unit_id = ou.id
            LEFT JOIN (
              SELECT l.unit_id,
                     MAX(t.full_name) AS tenant_name,
                     MAX(l.monthly_rent) AS monthly_rent,
                     MAX(l.end_date) AS lease_end_date
              FROM leases l
              JOIN tenants t ON t.id = l.tenant_id
              WHERE l.status = 'active'
              GROUP BY l.unit_id
            ) lease_info ON lease_info.unit_id = ou.unit_id
            LEFT JOIN (
              SELECT l.unit_id,
                     COALESCE(SUM(ri.amount_due), 0) AS current_month_rent_due,
                     COALESCE(SUM(ri.amount_paid), 0) AS current_month_rent_paid,
                     COALESCE(SUM(GREATEST(ri.amount_due - ri.amount_paid, 0)), 0) AS current_month_rent_outstanding
              FROM leases l
              JOIN rent_invoices ri ON ri.lease_id = l.id
              WHERE ri.billing_month >= DATE_FORMAT(CURRENT_DATE, '%Y-%m-01')
                AND ri.billing_month < DATE_ADD(DATE_FORMAT(CURRENT_DATE, '%Y-%m-01'), INTERVAL 1 MONTH)
              GROUP BY l.unit_id
            ) rent_info ON rent_info.unit_id = ou.unit_id
            LEFT JOIN reserve_accounts ra ON ra.owner_unit_id = ou.id AND ra.status = 'active'
            LEFT JOIN (
              SELECT ce.unit_id,
                     COALESCE(SUM(CASE WHEN ce.direction = 'income' THEN fr.amount ELSE 0 END), 0) AS monthly_income,
                     COALESCE(SUM(CASE WHEN ce.direction = 'expense' THEN fr.amount ELSE 0 END), 0) AS monthly_expense
              FROM cashflow_entries ce
              JOIN finance_records fr ON fr.id = ce.finance_record_id
              WHERE ce.occurred_on >= DATE_FORMAT(CURRENT_DATE, '%Y-%m-01')
                AND ce.occurred_on < DATE_ADD(DATE_FORMAT(CURRENT_DATE, '%Y-%m-01'), INTERVAL 1 MONTH)
              GROUP BY ce.unit_id
            ) cashflow ON cashflow.unit_id = ou.unit_id
            LEFT JOIN (
              SELECT unit_id, COUNT(*) AS pending_maintenance_count
              FROM maintenance_work_orders
              WHERE status NOT IN ('completed', 'cancelled')
              GROUP BY unit_id
            ) maintenance ON maintenance.unit_id = ou.unit_id
            WHERE o.user_id = #{userId}
              AND o.status = 'active'
              AND p.status = 'active'
            ORDER BY p.name, u.unit_no
            """)
    List<Property> findPropertiesByUserId(@Param("userId") Long userId);

    @Select("""
            SELECT COALESCE(SUM(fr.amount), 0)
            FROM finance_records fr
            JOIN owners o ON o.id = fr.owner_id
            WHERE o.user_id = #{userId}
              AND fr.record_type = 'rent_payment'
              AND fr.payment_status = 'paid'
              AND fr.confirmation_status = 'confirmed'
              AND EXISTS (
                SELECT 1 FROM owner_units ou
                WHERE ou.owner_id = o.id AND ou.unit_id = fr.unit_id
                  AND ou.status = 'active' AND ou.asset_stage = 'OPERATING'
              )
              AND fr.transaction_date >= DATE_FORMAT(CURRENT_DATE, '%Y-%m-01')
              AND fr.transaction_date < DATE_ADD(DATE_FORMAT(CURRENT_DATE, '%Y-%m-01'), INTERVAL 1 MONTH)
            """)
    BigDecimal findMonthlyRentIncome(@Param("userId") Long userId);

    @Select("""
            SELECT COALESCE(SUM(ra.current_balance), 0)
            FROM reserve_accounts ra
            JOIN owner_units ou ON ou.id = ra.owner_unit_id AND ou.status = 'active' AND ou.asset_stage = 'OPERATING'
            JOIN owners o ON o.id = ou.owner_id
            WHERE o.user_id = #{userId}
              AND ra.status = 'active'
            """)
    BigDecimal findReserveBalance(@Param("userId") Long userId);

    @Select("""
            SELECT n.id, n.title, n.body, n.priority, n.status, DATE(n.created_at) AS created_date
            FROM notifications n
            WHERE n.recipient_user_id = #{userId}
               OR n.recipient_owner_id IN (SELECT id FROM owners WHERE user_id = #{userId})
            ORDER BY n.created_at DESC
            LIMIT 3
            """)
    List<Notification> findRecentNotifications(@Param("userId") Long userId);

    @Select("""
            SELECT COUNT(*)
            FROM finance_records fr
            JOIN owners o ON o.id = fr.owner_id
            LEFT JOIN payment_receipts pr ON pr.finance_record_id = fr.id
            WHERE o.user_id = #{userId}
              AND fr.record_type = 'property_payment'
              AND EXISTS (
                SELECT 1 FROM owner_units ou
                WHERE ou.owner_id = o.id AND ou.unit_id = fr.unit_id
                  AND ou.status = 'active' AND ou.asset_stage = 'PRE_HANDOVER'
              )
              AND (pr.id IS NULL OR pr.proof_document_id IS NULL)
            """)
    int countMissingPaymentProofs(@Param("userId") Long userId);

    @Select("""
            SELECT COUNT(*)
            FROM finance_records fr
            JOIN owners o ON o.id = fr.owner_id
            WHERE o.user_id = #{userId}
              AND fr.record_type = 'rent_payment'
              AND fr.confirmation_status <> 'confirmed'
            """)
    int countPendingRentConfirmations(@Param("userId") Long userId);

    @Select("""
            SELECT COUNT(DISTINCT d.id)
            FROM documents d
            JOIN document_links dl ON dl.document_id = d.id
            WHERE d.status IN ('pending_review', 'pending_signature')
              AND (
                (dl.entity_type = 'owner' AND dl.entity_id IN (SELECT id FROM owners WHERE user_id = #{userId}))
                OR
                (dl.entity_type = 'unit' AND dl.entity_id IN (
                  SELECT ou.unit_id FROM owner_units ou
                  JOIN owners o ON o.id = ou.owner_id
                  WHERE o.user_id = #{userId} AND ou.status = 'active'
                ))
              )
            """)
    int countPendingDocuments(@Param("userId") Long userId);
}
