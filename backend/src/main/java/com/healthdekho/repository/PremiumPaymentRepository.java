package com.healthdekho.repository;

import com.healthdekho.entity.PremiumPayment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PremiumPaymentRepository extends JpaRepository<PremiumPayment, Long> {

    List<PremiumPayment> findAllByReceiptNumberOrderByInstallment_InstallmentNumberAsc(String receiptNumber);

    List<PremiumPayment> findAllByInstallment_Policy_IdOrderByPaymentDateDesc(Long policyId);

    List<PremiumPayment> findAllByInstallment_Policy_Id(Long policyId);
}
