package com.healthdekho.repository;

import com.healthdekho.entity.PremiumInstallment;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface PremiumInstallmentRepository extends JpaRepository<PremiumInstallment, Long> {

    List<PremiumInstallment> findAllByPolicy_IdOrderByInstallmentNumberAsc(Long policyId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<PremiumInstallment> findAllByPolicy_IdAndInstallmentNumberInOrderByInstallmentNumberAsc(
            Long policyId,
            List<Integer> installmentNumbers
    );

    @Query(value = """
            select cp.policy_id as "policyId",
                   coalesce(sum(payment.premium_paid), 0) as "premiumPaid",
                   count(distinct case
                       when coalesce(installment.amount_paid, 0)
                            < installment.premium_amount + coalesce(installment.penalty_amount, 0)
                       then installment.installment_id end) as "incompleteInstallments",
                   count(distinct case
                       when installment.grace_period_end_date < :asOfDate
                        and coalesce(installment.amount_paid, 0)
                            < installment.premium_amount + coalesce(installment.penalty_amount, 0)
                       then installment.installment_id end) as "overdueInstallments",
                   count(distinct case
                       when installment.amount_paid > 0 then installment.installment_id end) as "installmentsWithPayment"
            from customer_policies cp
            join premium_installments installment on installment.policy_id = cp.policy_id
            left join premium_payments payment on payment.installment_id = installment.installment_id
            where cp.policy_id in (:policyIds)
            group by cp.policy_id
            """, nativeQuery = true)
    List<PolicyPaymentSummaryProjection> summarizePaymentsByPolicy(
            @Param("policyIds") List<Long> policyIds,
            @Param("asOfDate") LocalDate asOfDate
    );
}
