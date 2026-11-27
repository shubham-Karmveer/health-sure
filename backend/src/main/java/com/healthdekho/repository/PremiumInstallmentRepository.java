package com.healthdekho.repository;

import com.healthdekho.entity.PremiumInstallment;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface PremiumInstallmentRepository extends JpaRepository<PremiumInstallment, UUID> {

    List<PremiumInstallment> findAllByPolicy_IdOrderByInstallmentNumberAsc(UUID policyId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<PremiumInstallment> findAllByPolicy_IdAndInstallmentNumberInOrderByInstallmentNumberAsc(
            UUID policyId,
            List<Integer> installmentNumbers
    );

    @Query("""
            select installment.policy.id as policyId,
                   sum(installment.paidAmount) as premiumPaid,
                   count(case
                       when installment.paidAmount < installment.scheduledAmount
                         or installment.paidPenaltyAmount < installment.penaltyAmount
                       then installment.id else null
                   end) as incompleteInstallments,
                   count(case
                       when installment.gracePeriodEndDate < :asOfDate
                        and (installment.paidAmount < installment.scheduledAmount
                          or installment.paidPenaltyAmount < installment.penaltyAmount)
                       then installment.id else null
                   end) as overdueInstallments,
                   count(case
                       when installment.paidAmount > 0 or installment.paidPenaltyAmount > 0
                       then installment.id else null
                   end) as installmentsWithPayment
            from PremiumInstallment installment
            where installment.policy.id in :policyIds
            group by installment.policy.id
            """)
    List<PolicyPaymentSummaryProjection> summarizePaymentsByPolicy(
            @Param("policyIds") List<UUID> policyIds,
            @Param("asOfDate") LocalDate asOfDate
    );
}
