package com.healthdekho.repository;

import com.healthdekho.entity.PremiumPayment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PremiumPaymentRepository extends JpaRepository<PremiumPayment, UUID> {

    List<PremiumPayment> findAllByIdempotencyRecord_IdOrderByInstallment_InstallmentNumberAsc(UUID idempotencyId);

    List<PremiumPayment> findAllByInstallment_Policy_IdOrderByPaymentDateDesc(UUID policyId);

    List<PremiumPayment> findAllByInstallment_Policy_Id(UUID policyId);
}
