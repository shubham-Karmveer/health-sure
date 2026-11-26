package com.healthdekho.repository;

import com.healthdekho.entity.PremiumInstallment;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.UUID;

public interface PremiumInstallmentRepository extends JpaRepository<PremiumInstallment, UUID> {

    List<PremiumInstallment> findAllByPolicy_IdOrderByInstallmentNumberAsc(UUID policyId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<PremiumInstallment> findAllByPolicy_IdAndInstallmentNumberInOrderByInstallmentNumberAsc(
            UUID policyId,
            List<Integer> installmentNumbers
    );
}
