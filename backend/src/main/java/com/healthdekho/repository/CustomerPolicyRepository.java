package com.healthdekho.repository;

import com.healthdekho.entity.CustomerPolicy;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CustomerPolicyRepository extends JpaRepository<CustomerPolicy, UUID> {

    List<CustomerPolicy> findAllByCustomer_IdOrderByCreatedAtDesc(UUID customerId);

    boolean existsByCustomer_IdAndProduct_Id(UUID customerId, UUID productId);

    Optional<CustomerPolicy> findByPolicyNumber(String policyNumber);

    Optional<CustomerPolicy> findByPolicyNumberAndCustomer_Id(String policyNumber, UUID customerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select policy from CustomerPolicy policy
            where policy.policyNumber = :policyNumber and policy.customer.id = :customerId
            """)
    Optional<CustomerPolicy> findForUpdateByPolicyNumberAndCustomerId(
            @Param("policyNumber") String policyNumber,
            @Param("customerId") UUID customerId
    );
}
